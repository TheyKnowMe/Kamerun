package com.ME.kamerun

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ME.kamerun.player.MusicPlayer
import com.ME.kamerun.ui.navigation.AppNavGraph
import com.ME.kamerun.ui.theme.KamerunTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var musicPlayer: MusicPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KamerunTheme {
                AppNavGraph(musicPlayer = musicPlayer)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
