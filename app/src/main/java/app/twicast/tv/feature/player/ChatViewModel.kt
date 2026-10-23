package app.twicast.tv.feature.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.twicast.tv.data.repository.ChatMessage
import app.twicast.tv.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Lines kept on screen; older ones scroll off for good. */
private const val MaxChatMessages = 150
private const val RetryBaseMillis = 2_000L
private const val RetryMaxMillis = 30_000L
private const val StopTimeoutMillis = 5_000L

/** At most ~4 panel updates a second on a busy chat; a quarter second is not noticeable in a chat. */
private const val ChatBatchMillis = 250L

/**
 * Chat panel of the player (mockup "Плеер · чат"). Connected only while the panel is on screen,
 * reconnects by itself after a dropped connection. Newest message first.
 */
@HiltViewModel
class ChatViewModel internal constructor(channelLogin: String, repository: ChatRepository) : ViewModel() {

    @Inject
    constructor(savedStateHandle: SavedStateHandle, repository: ChatRepository) :
        this(savedStateHandle.toRoute<PlayerDestination>().channelLogin, repository)

    val messages: StateFlow<PersistentList<ChatMessage>> = repository.messages(channelLogin)
        .retryWhen { _, attempt ->
            delay((RetryBaseMillis * (attempt + 1)).coerceAtMost(RetryMaxMillis))
            true
        }
        .batchedBy(ChatBatchMillis)
        .runningFold(persistentListOf<ChatMessage>()) { lines, batch ->
            lines.mutate { newestFirst ->
                batch.forEach { newestFirst.add(0, it) }
                while (newestFirst.size > MaxChatMessages) newestFirst.removeAt(newestFirst.lastIndex)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), persistentListOf())
}
