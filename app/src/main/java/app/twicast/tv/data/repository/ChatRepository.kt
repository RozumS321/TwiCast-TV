package app.twicast.tv.data.repository

import android.util.Log
import app.twicast.tv.BuildConfig
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/** One chat line; [color] is the author's Twitch name color (ARGB), null when they never picked one. */
data class ChatMessage(val id: String, val author: String, val color: Int?, val text: String)

interface ChatRepository {
    /** Live messages of [channelLogin]'s chat while collected; fails on a dropped connection (the caller retries). */
    fun messages(channelLogin: String): Flow<ChatMessage>
}

private const val ChatLogTag = "TwiCastChat"
private const val DebugLineLength = 160
private const val IrcUrl = "wss://irc-ws.chat.twitch.tv:443"
private const val NormalClosure = 1000
private const val AnonymousNickMin = 10_000
private const val AnonymousNickMax = 99_999
private const val ColorLength = 7
private const val OpaqueAlpha = 0xFF000000.toInt()
private const val ActionPrefix = "\u0001ACTION "
private const val ActionSuffix = "\u0001"

/**
 * Twitch chat over IRC (https://dev.twitch.tv/docs/chat/irc/): read-only, as an anonymous "justinfan" user,
 * which Twitch allows without a token. A plain WebSocket: far lighter than the chat web page on weak TVs.
 */
@Singleton
class TwitchIrcChatRepository @Inject constructor(private val client: OkHttpClient) : ChatRepository {

    override fun messages(channelLogin: String): Flow<ChatMessage> = callbackFlow {
        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(ChatLogTag, "chat connected to #$channelLogin")
                webSocket.send("CAP REQ :twitch.tv/tags")
                webSocket.send("PASS SCHMOOPIIE")
                webSocket.send("NICK justinfan${Random.nextInt(AnonymousNickMin, AnonymousNickMax)}")
                webSocket.send("JOIN #${channelLogin.lowercase()}")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (BuildConfig.DEBUG) Log.d(ChatLogTag, text.take(DebugLineLength))
                text.split("\r\n").forEach { line ->
                    if (line.startsWith("PING")) {
                        webSocket.send("PONG :tmi.twitch.tv")
                    } else {
                        parseChatLine(line)?.let { trySend(it) }
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(ChatLogTag, "chat connection failed", t)
                close(IOException("chat connection lost", t))
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                close(IOException("chat closed: $code"))
            }
        }
        val socket = client.newWebSocket(Request.Builder().url(IrcUrl).build(), listener)
        awaitClose { socket.close(NormalClosure, null) }
    }
}

/**
 * A PRIVMSG line to a message, anything else to null:
 * `@color=#FF0000;display-name=Foo;id=… :foo!foo@foo.tmi.twitch.tv PRIVMSG #chan :hello`.
 */
internal fun parseChatLine(line: String): ChatMessage? {
    if (!line.contains(" PRIVMSG ")) return null
    var rest = line
    var tags = emptyMap<String, String>()
    if (rest.startsWith("@")) {
        val tagsEnd = rest.indexOf(' ')
        if (tagsEnd < 0) return null
        tags = rest.substring(1, tagsEnd).split(';').associate { it.substringBefore('=') to it.substringAfter('=', "") }
        rest = rest.substring(tagsEnd + 1)
    }
    if (!rest.startsWith(":")) return null
    val prefixEnd = rest.indexOf(' ')
    val textStart = rest.indexOf(" :", prefixEnd)
    if (prefixEnd < 0 || textStart < 0) return null
    val nick = rest.substring(1, prefixEnd).substringBefore('!')
    var text = rest.substring(textStart + 2)
    // "/me" messages arrive wrapped in CTCP ACTION.
    if (text.startsWith(ActionPrefix)) text = text.removePrefix(ActionPrefix).removeSuffix(ActionSuffix)
    val color = tags["color"]
        ?.takeIf { it.length == ColorLength && it.startsWith('#') }
        ?.substring(1)
        ?.toIntOrNull(radix = 16)
        ?.or(OpaqueAlpha)
    return ChatMessage(
        id = tags["id"]?.takeIf { it.isNotEmpty() } ?: UUID.randomUUID().toString(),
        author = tags["display-name"]?.takeIf { it.isNotEmpty() } ?: nick,
        color = color,
        text = text,
    )
}
