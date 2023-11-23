package com.simplifyd.zerodatavpn.demo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.view.isVisible
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.simplifyd.zerodatavpn.ConnectionStatus
import com.simplifyd.zerodatavpn.ZeroData
import com.simplifyd.zerodatavpn.ZeroData.isVPNConnected
import com.simplifyd.zerodatavpn.demo.databinding.ActivityMainBinding
import com.simplifyd.zerodatavpn.grpc.ZeroDataRegistrationListener
import java.util.UUID

class MainActivity : Activity() {

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private var exoPlayer: ExoPlayer? = null
    private var playbackPosition = 0L
    private var playWhenReady = true
    private var userId: UUID? = null
    private var sessionToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        preparePlayer()
        initializeSDK()
        setUpViews()
    }

    private fun preparePlayer() {
        exoPlayer = ExoPlayer.Builder(this).build()
        exoPlayer?.playWhenReady = false
        binding.playerView.player = exoPlayer
        val defaultHttpDataSourceFactory = DefaultHttpDataSource.Factory()
        val mediaItem = MediaItem.fromUri(URL)
        val mediaSource =
            HlsMediaSource.Factory(defaultHttpDataSourceFactory).createMediaSource(mediaItem)
        exoPlayer?.apply {
            setMediaSource(mediaSource)
            seekTo(playbackPosition)
            playWhenReady = playWhenReady
            prepare()
        }
    }

    private fun initializeSDK() {
        ZeroData.sdk.initialize(API_KEY, false)
        ZeroData.sdk.registerUser(
            USER_IDENTIFIER,
            APP_TOKEN,
            object : ZeroDataRegistrationListener {
                override fun onSuccess(uuid: UUID?, clientToken: String) {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "UserId is available $uuid",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    this@MainActivity.userId = uuid
                    this@MainActivity.sessionToken = clientToken
                }

                override fun onFailure(throwable: Throwable?) {
                    throwable?.let {
                        throwable.printStackTrace()
                        showErrorToastMessage(throwable)
                    }

                    binding.simpleSwitch.isChecked = false
                }

            })
    }

    private fun setUpViews() {
        if (isVPNConnected(this)) {
            binding.simpleSwitch.isChecked = true
        }

        binding.simpleSwitch.setOnCheckedChangeListener { compoundButton, b ->
            if (b) {
                connectToZeroDataTunnel()
            } else {
                disconnectFromZeroDataTunnel()
            }
        }
    }

    private fun connectToZeroDataTunnel() {
        connectVPN()
    }

    private fun connectVPN() {
        userId ?: return
        sessionToken ?: return

        ZeroData.sdk.startConnection(this, MainActivity::class.java, userId, sessionToken)
        ZeroData.sdk.addListener(object : ZeroData.ZeroDataConnectionListener {
            override fun onConnected() {
                Log.d(MainActivity.javaClass.simpleName, "Connected")
                binding.progressBar.isVisible = false
            }

            override fun onStartConnection() {
                Log.d(MainActivity.javaClass.simpleName, "StartConnection")
                startService(ZeroData.sdk.getConnectServiceIntent(this@MainActivity))
            }

            override fun onTimeTicked(time: String?) {
                Log.d(MainActivity.javaClass.simpleName, "time: " + time)
                ZeroData.sdk.connectionStatus.getOrDefault(ConnectionStatus.CONNECTED).let {

                }
            }

            override fun onConnecting() {
                Log.d(MainActivity.javaClass.simpleName, "Connecting")
                binding.progressBar.isVisible = true
            }

            override fun onUrlAvailable(url: String?) {

            }

            override fun onDisconnected() {
                Log.d(MainActivity.javaClass.simpleName, "Disconnected")
                binding.progressBar.isVisible = false
            }

            override fun onFailure(throwable: Throwable) {
                Log.d(MainActivity.javaClass.simpleName, "Failure")
                showErrorToastMessage(throwable)
                binding.simpleSwitch.isChecked = false
                binding.progressBar.isVisible = false
            }
        })
    }

    private fun showErrorToastMessage(throwable: Throwable) {
        Toast.makeText(
            this@MainActivity,
            throwable.message,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun disconnectFromZeroDataTunnel() {
        ZeroData.sdk.stopConnection(this, APP_TOKEN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == ZeroData.VPN_REQUEST_CODE && resultCode == RESULT_OK) {
            startService(ZeroData.sdk.getConnectServiceIntent(this))
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onPause() {
        super.onPause()
        releasePlayer()
    }

    private fun releasePlayer() {
        exoPlayer?.let { player ->
            playbackPosition = player.currentPosition
            playWhenReady = player.playWhenReady
            player.release()
            exoPlayer = null
        }
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }

    companion object {
        private const val API_KEY = "API_KEY"
        private const val USER_IDENTIFIER = "zerodata_test@zerodata.com"
        private const val APP_TOKEN = "APP_TOKEN"

        private const val URL = "https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8"
    }
}
