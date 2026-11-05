package com.example.iptvplayerpro

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.audiofx.Visualizer
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.abs

class MainActivity : Activity() {
    private data class Channel(val name: String, val url: String)
    private val channels = mutableListOf<Channel>()
    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView
    private lateinit var status: TextView
    private lateinit var vu: TextView
    private lateinit var vuBar: ProgressBar
    private lateinit var volume: SeekBar
    private var muted = false
    private var visualizer: Visualizer? = null
    private var lastSessionId = 0

    private val playlistPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { importPlaylist(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = android.graphics.Color.rgb(8, 13, 23)
        window.navigationBarColor = android.graphics.Color.rgb(8, 13, 23)
        player = ExoPlayer.Builder(this).build()
        buildUi()
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                status.text = if (isPlaying) "REPRODUCIENDO" else "EN PAUSA"
                if (isPlaying) startVu() else stopVu()
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    lastSessionId = player.audioSessionId
                    if (player.isPlaying) startVu()
                }
                if (playbackState == Player.STATE_BUFFERING) status.text = "CARGANDO…"
                if (playbackState == Player.STATE_ENDED) status.text = "FINALIZADO"
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                status.text = "ERROR DE REPRODUCCIÓN"
                Toast.makeText(this@MainActivity, "No se pudo reproducir este enlace", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setBackgroundColor(android.graphics.Color.rgb(8, 13, 23))
        }
        val header = TextView(this).apply {
            text = "IPTV PLAYER PRO  •  ANDROID"
            textSize = 18f; setTextColor(0xFF49D7B0.toInt()); gravity = Gravity.CENTER_VERTICAL
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        root.addView(header, LinearLayout.LayoutParams(-1, dp(42)))
        playerView = PlayerView(this).apply {
            player = this@MainActivity.player
            useController = true
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
            setBackgroundColor(android.graphics.Color.BLACK)
        }
        root.addView(playerView, LinearLayout.LayoutParams(-1, 0, 1f))
        status = TextView(this).apply {
            text = "LISTO PARA REPRODUCIR"; textSize = 11f
            setTextColor(0xFF9AA8BD.toInt()); gravity = Gravity.CENTER_VERTICAL
        }
        root.addView(status, LinearLayout.LayoutParams(-1, dp(28)))
        val vuRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        vu = TextView(this).apply { text = "VU  — dB"; textSize = 12f; setTextColor(0xFF49D7B0.toInt()) }
        vuRow.addView(vu, LinearLayout.LayoutParams(dp(85), dp(32)))
        vuBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100; progress = 0
        }
        vuRow.addView(vuBar, LinearLayout.LayoutParams(0, dp(8), 1f))
        root.addView(vuRow)

        val volumeRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val volLabel = TextView(this).apply { text = "VOL"; setTextColor(-1); textSize = 12f }
        volumeRow.addView(volLabel, LinearLayout.LayoutParams(dp(42), dp(42)))
        volume = SeekBar(this).apply {
            max = 100; progress = 75
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, user: Boolean) { player.volume = if (muted) 0f else p / 100f }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        volumeRow.addView(volume, LinearLayout.LayoutParams(0, dp(42), 1f))
        val mute = Button(this).apply { text = "MUTE"; setOnClickListener {
            muted = !muted; player.volume = if (muted) 0f else volume.progress / 100f
            text = if (muted) "UNMUTE" else "MUTE"
        } }
        volumeRow.addView(mute)
        root.addView(volumeRow)

        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        controls.addView(actionButton("＋ M3U") { playlistPicker.launch(arrayOf("audio/x-mpegurl", "application/vnd.apple.mpegurl", "text/plain", "*/*")) })
        controls.addView(actionButton("＋ URL") { promptStreamUrl() })
        controls.addView(actionButton("CANALES") { showChannels() })
        controls.addView(actionButton("⛶ FULL") { toggleFullscreen() })
        controls.addView(actionButton("STOP") { player.stop(); status.text = "DETENIDO" })
        root.addView(controls)
        val note = TextView(this).apply {
            text = "Importá tu archivo M3U/M3U8 o abrí una URL de canal desde la lista. Usá únicamente fuentes autorizadas."
            textSize = 10f; setTextColor(0xFF7F8CA3.toInt()); gravity = Gravity.CENTER
        }
        root.addView(note, LinearLayout.LayoutParams(-1, dp(34)))
        setContentView(root)
    }

    private fun actionButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label; textSize = 10f; isAllCaps = false; setOnClickListener { action() }
    }

    private fun promptStreamUrl() {
        val field = EditText(this).apply {
            hint = "https://servidor/stream.m3u8"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
            setSingleLine(true)
        }
        android.app.AlertDialog.Builder(this)
            .setTitle("Agregar stream")
            .setView(field)
            .setPositiveButton("Reproducir") { _, _ ->
                val url = field.text.toString().trim()
                if (url.startsWith("https://", true)) {
                    val channel = Channel(url.substringAfterLast('/').ifBlank { "Stream" }, url)
                    channels.add(channel)
                    playChannel(channel)
                } else Toast.makeText(this, "Ingresá una URL HTTPS válida", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun importPlaylist(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { stream ->
                val reader = BufferedReader(InputStreamReader(stream))
                var pendingName: String? = null
                reader.forEachLine { line ->
                    val s = line.trim()
                    if (s.startsWith("#EXTINF", true)) {
                        pendingName = s.substringAfter(',', "Canal").trim().ifBlank { "Canal" }
                    } else if (s.isNotEmpty() && !s.startsWith("#") && s.startsWith("https://", true)) {
                        channels.add(Channel(pendingName ?: "Canal ${channels.size + 1}", s))
                        pendingName = null
                    }
                }
            }
            Toast.makeText(this, "Canales cargados: ${channels.size}", Toast.LENGTH_LONG).show()
            showChannels()
        } catch (_: Exception) {
            Toast.makeText(this, "No se pudo leer la lista M3U", Toast.LENGTH_LONG).show()
        }
    }

    private fun showChannels() {
        if (channels.isEmpty()) {
            Toast.makeText(this, "Primero importá una lista M3U/M3U8", Toast.LENGTH_SHORT).show(); return
        }
        val names = channels.map { it.name }.toTypedArray()
        android.app.AlertDialog.Builder(this)
            .setTitle("Canales (${channels.size})")
            .setItems(names) { _, which -> playChannel(channels[which]) }
            .setNegativeButton("Cerrar", null).show()
    }

    private fun playChannel(channel: Channel) {
        if (!channel.url.startsWith("https://", true)) return
        player.setMediaItem(MediaItem.fromUri(channel.url))
        player.prepare(); player.play()
        status.text = channel.name
    }

    private fun toggleFullscreen() {
        val decor = window.decorView
        @Suppress("DEPRECATION")
        if (decor.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN != 0) {
            decor.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        } else {
            @Suppress("DEPRECATION")
            decor.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        }
    }

    private fun startVu() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            vu.text = "VU: permiso opcional"; vuBar.progress = 0
            android.app.AlertDialog.Builder(this)
                .setTitle("Activar medidor VU")
                .setMessage("Android requiere permiso de audio para medir el nivel de sonido que reproduce el reproductor. La app no graba ni transmite el micrófono. Podés rechazarlo y seguir viendo IPTV sin el medidor.")
                .setPositiveButton("Permitir") { _, _ -> requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 41) }
                .setNegativeButton("Ahora no", null).show()
            return
        }
        stopVu()
        try {
            val v = Visualizer(player.audioSessionId)
            v.captureSize = Visualizer.getCaptureSizeRange()[1]
            v.setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(viz: Visualizer?, waveform: ByteArray?, samplingRate: Int) {
                    if (waveform != null) {
                        var peak = 0
                        waveform.forEach { peak = maxOf(peak, abs(it.toInt() - 128)) }
                        val level = (peak * 100 / 128).coerceIn(0, 100)
                        runOnUiThread { vuBar.progress = level; vu.text = "VU  $level%" }
                    }
                }
                override fun onFftDataCapture(viz: Visualizer?, fft: ByteArray?, samplingRate: Int) {}
            }, Visualizer.getMaxCaptureRate() / 2, true, false)
            v.enabled = true; visualizer = v
        } catch (_: Exception) { vu.text = "VU no disponible" }
    }

    private fun stopVu() {
        try { visualizer?.enabled = false; visualizer?.release() } catch (_: Exception) {}
        visualizer = null; vuBar.progress = 0
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 41 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED && player.isPlaying) startVu()
    }

    override fun onDestroy() {
        stopVu(); player.release(); super.onDestroy()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
