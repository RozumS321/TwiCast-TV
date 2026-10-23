package app.twicast.tv.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatParserTest {

    @Test
    fun `parses name, color and text from a tagged PRIVMSG`() {
        val message = parseChatLine(
            "@color=#1E90FF;display-name=NightOwl;id=abc-1 :nightowl!nightowl@nightowl.tmi.twitch.tv PRIVMSG #sasavot :привет : всем",
        )!!
        assertEquals("abc-1", message.id)
        assertEquals("NightOwl", message.author)
        assertEquals(0xFF1E90FF.toInt(), message.color)
        assertEquals("привет : всем", message.text)
    }

    @Test
    fun `no color tag, action message`() {
        val message = parseChatLine("@color=;display-name= :kot!kot@kot.tmi.twitch.tv PRIVMSG #a :\u0001ACTION машет\u0001")!!
        assertEquals("kot", message.author)
        assertNull(message.color)
        assertEquals("машет", message.text)
    }

    @Test
    fun `service lines are skipped`() {
        assertNull(parseChatLine(":tmi.twitch.tv 001 justinfan12345 :Welcome, GLHF!"))
        assertNull(parseChatLine("PING :tmi.twitch.tv"))
    }
}
