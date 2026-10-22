package app.twicast.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import app.twicast.tv.core.designsystem.theme.TwiCastTheme
import app.twicast.tv.navigation.TwiCastApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TwiCastTheme {
                TwiCastApp()
            }
        }
    }
}
