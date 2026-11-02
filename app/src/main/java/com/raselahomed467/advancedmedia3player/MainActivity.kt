package com.raselahomed467.advancedmedia3player

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.ui.PlayerView
import okhttp3.OkHttpClient

class MainActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var etVideoUrl: EditText
    private lateinit var etCookie: EditText
    private lateinit var etReferer: EditText
    private lateinit var etUserAgent: EditText
    private lateinit var etAuthorization: EditText
    private lateinit var btnPlay: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        playerView = findViewById(R.id.playerView)
        etVideoUrl = findViewById(R.id.etVideoUrl)
        etCookie = findViewById(R.id.etCookie)
        etReferer = findViewById(R.id.etReferer)
        etUserAgent = findViewById(R.id.etUserAgent)
        etAuthorization = findViewById(R.id.etAuthorization)
        btnPlay = findViewById(R.id.btnPlay)

        btnPlay.setOnClickListener {
            val url = etVideoUrl.text.toString().trim()
            if (url.isNotEmpty()) {
                playVideo(url)
            }
        }
    }

    private fun playVideo(url: String) {
        player?.release()

        player = ExoPlayer.Builder(this).build().also { exoPlayer ->
            playerView.player = exoPlayer

            val headers = mutableMapOf<String, String>()
            val cookie = etCookie.text.toString().trim()
            val referer = etReferer.text.toString().trim()
            val userAgent = etUserAgent.text.toString().trim()
            val auth = etAuthorization.text.toString().trim()

            if (cookie.isNotEmpty()) headers["Cookie"] = cookie
            if (referer.isNotEmpty()) headers["Referer"] = referer
            if (userAgent.isNotEmpty()) headers["User-Agent"] = userAgent
            if (auth.isNotEmpty()) headers["Authorization"] = auth

            val okHttpClient = OkHttpClient.Builder().build()
            val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient).apply {
                if (headers.isNotEmpty()) {
                    setDefaultRequestProperties(headers)
                }
            }

            val mediaItem = MediaItem.fromUri(Uri.parse(url))
            val mediaSourceFactory = ProgressiveMediaSource.Factory(dataSourceFactory)
            val mediaSource = mediaSourceFactory.createMediaSource(mediaItem)

            exoPlayer.setMediaSource(mediaSource)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }
    }

    override fun onStop() {
        super.onStop()
        player?.release()
        player = null
    }
}
