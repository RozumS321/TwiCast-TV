package app.twicast.tv.feature.search

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.twicast.tv.R
import app.twicast.tv.data.model.Category
import app.twicast.tv.data.model.ChannelSearchResult

@Composable
fun SearchRoute(
    onChannelClick: (ChannelSearchResult) -> Unit,
    onCategoryClick: (Category) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val prompt = stringResource(R.string.search_voice_prompt)
    // System voice input (the remote's mic on Android / Google TV); hidden when the device has none.
    val voiceIntent = remember(prompt) {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
    }
    val voiceAvailable = remember(context) { voiceIntent.resolveActivity(context.packageManager) != null }
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let(viewModel::onVoiceResult)
    }

    SearchScreen(
        state = state,
        voiceAvailable = voiceAvailable,
        onKey = viewModel::onKey,
        onSpace = viewModel::onSpace,
        onErase = viewModel::onErase,
        onOpenLayoutMenu = viewModel::onOpenLayoutMenu,
        onCloseLayoutMenu = viewModel::onCloseLayoutMenu,
        onLayoutSelected = viewModel::onLayoutSelected,
        onVoice = { voiceLauncher.launch(voiceIntent) },
        onChannelClick = { channel ->
            viewModel.onChannelOpened(channel)
            onChannelClick(channel)
        },
        onCategoryClick = { category ->
            viewModel.onCategoryOpened()
            onCategoryClick(category)
        },
        onRetry = viewModel::onRetry,
        onHistoryClick = { item ->
            val login = item.entry.channelLogin
            if (login == null) {
                viewModel.onHistoryQuery(item.entry)
            } else {
                // Opened again: back to the top of "Недавние", and live or not decides where it goes.
                val channel = item.toChannel(login)
                viewModel.onChannelOpened(channel)
                onChannelClick(channel)
            }
        },
        onClearHistory = viewModel::onClearHistory,
    )
}

/** A remembered channel as a search result: the app opens its stream if live, else its page. */
private fun SearchHistoryItem.toChannel(login: String) = ChannelSearchResult(
    id = "",
    login = login,
    displayName = entry.text,
    avatarUrl = entry.avatarUrl,
    isLive = live,
    categoryName = entry.categoryName,
    title = "",
)
