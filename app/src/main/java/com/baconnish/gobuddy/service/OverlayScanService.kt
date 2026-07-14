package com.baconnish.gobuddy.service

import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.IntentCompat
import androidx.core.graphics.createBitmap
import com.baconnish.gobuddy.GoBuddyApp
import com.baconnish.gobuddy.R
import com.baconnish.gobuddy.data.LiveScanProcessor
import com.baconnish.gobuddy.data.ScanCapture
import com.baconnish.gobuddy.domain.ScanConsensus
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("AppCompatCustomView")
private class BubbleView(context: Context) : ImageView(context) {
    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}

class OverlayScanService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var windowManager: WindowManager? = null
    private var bubble: View? = null
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var processor: LiveScanProcessor? = null
    private var screenWidth = 0
    private var screenHeight = 0
    private var scanning = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (projection != null) return START_NOT_STICKY

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
            ?: Activity.RESULT_CANCELED
        val resultData = intent?.let {
            IntentCompat.getParcelableExtra(it, EXTRA_RESULT_DATA, Intent::class.java)
        }
        if (resultCode != Activity.RESULT_OK || resultData == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        startAsForeground()
        val container = (application as GoBuddyApp).container
        processor = LiveScanProcessor(container.pokemonDao, container.speciesRepository)
        if (!setUpProjection(resultCode, resultData)) {
            stopSelf()
            return START_NOT_STICKY
        }
        showBubble()
        isRunning = true
        return START_NOT_STICKY
    }

    private fun startAsForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Game overlay", NotificationManager.IMPORTANCE_LOW),
        )
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, OverlayScanService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Go Buddy overlay active")
            .setContentText("Tap the bubble over the game to scan the Pokémon on screen.")
            .setOngoing(true)
            .addAction(0, "Stop", stopIntent)
            .build()
        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, notification,
            if (Build.VERSION.SDK_INT >= 29) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            } else {
                0
            },
        )
    }

    private fun setUpProjection(resultCode: Int, resultData: Intent): Boolean {
        val manager = getSystemService(MediaProjectionManager::class.java)
        val proj = manager.getMediaProjection(resultCode, resultData) ?: return false
        projection = proj
        proj.registerCallback(
            object : MediaProjection.Callback() {
                override fun onStop() {
                    stopSelf()
                }
            },
            Handler(Looper.getMainLooper()),
        )

        val wm = getSystemService(WindowManager::class.java)
        windowManager = wm
        if (Build.VERSION.SDK_INT >= 30) {
            val bounds = wm.currentWindowMetrics.bounds
            screenWidth = bounds.width()
            screenHeight = bounds.height()
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(metrics)
            screenWidth = metrics.widthPixels
            screenHeight = metrics.heightPixels
        }

        val reader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2)
        imageReader = reader
        virtualDisplay = proj.createVirtualDisplay(
            "gobuddy-scan",
            screenWidth,
            screenHeight,
            resources.displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null,
            null,
        )
        return true
    }

    private fun showBubble() {
        val wm = windowManager ?: return
        val size = (56 * resources.displayMetrics.density).roundToInt()
        val view = BubbleView(this).apply {
            setImageResource(R.mipmap.ic_launcher_round)
            alpha = 0.9f
            contentDescription = "Scan the Pokémon on screen"
        }
        val params = WindowManager.LayoutParams(
            size,
            size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenWidth - size - size / 4
            y = screenHeight / 3
        }

        view.setOnClickListener { scan() }
        val slop = ViewConfiguration.get(this).scaledTouchSlop
        var downRawX = 0f
        var downRawY = 0f
        var startX = 0
        var startY = 0
        var dragging = false
        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = params.x
                    startY = params.y
                    dragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY
                    if (abs(dx) > slop || abs(dy) > slop) dragging = true
                    if (dragging) {
                        params.x = (startX + dx).roundToInt()
                        params.y = (startY + dy).roundToInt()
                        wm.updateViewLayout(v, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragging) v.performClick()
                    true
                }
                else -> false
            }
        }

        wm.addView(view, params)
        bubble = view
    }

    private fun scan() {
        if (scanning) return
        val reader = imageReader ?: return
        val proc = processor ?: return
        scanning = true
        val view = bubble
        scope.launch {
            try {
                view?.visibility = View.INVISIBLE
                delay(300)
                val container = (application as GoBuddyApp).container
                val frames = mutableListOf<ScanCapture>()
                var attempts = 0
                while (attempts < 3) {
                    if (attempts > 0) delay(500)
                    attempts++
                    val bitmap = acquireBitmap(reader) ?: continue
                    saveCapture(bitmap)
                    val next = container.screenshotScanner.scan(bitmap)
                    frames += next
                    if (frames.size > 1 &&
                        ScanConsensus.settled(frames[frames.size - 2].result, next.result)
                    ) {
                        break
                    }
                }
                view?.visibility = View.VISIBLE
                val capture = frames.getOrNull(ScanConsensus.pick(frames.map { it.result }))
                if (capture == null) {
                    Toast.makeText(this@OverlayScanService, "Couldn't capture the screen", Toast.LENGTH_LONG).show()
                    return@launch
                }
                val message = proc.process(capture)
                Toast.makeText(this@OverlayScanService, message, Toast.LENGTH_LONG).show()
            } catch (_: Exception) {
                Toast.makeText(this@OverlayScanService, "Scan failed; try again", Toast.LENGTH_LONG).show()
            } finally {
                view?.visibility = View.VISIBLE
                scanning = false
            }
        }
    }

    private suspend fun saveCapture(bitmap: Bitmap) {
        withContext(Dispatchers.IO) {
            try {
                val dir = getExternalFilesDir(null)
                File(dir, "capture-3.png").delete()
                for (i in 2 downTo 1) {
                    val file = File(dir, "capture-$i.png")
                    if (file.exists()) file.renameTo(File(dir, "capture-${i + 1}.png"))
                }
                File(dir, "capture-1.png").outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            } catch (_: Exception) {
            }
        }
    }

    private suspend fun acquireBitmap(reader: ImageReader): Bitmap? {
        var image: Image? = null
        repeat(10) {
            if (image == null) {
                image = reader.acquireLatestImage()
                if (image == null) delay(100)
            }
        }
        val captured = image ?: return null
        val bitmap = captured.toBitmap()
        captured.close()
        return bitmap
    }

    private fun Image.toBitmap(): Bitmap {
        val plane = planes[0]
        val rowPaddingPixels = (plane.rowStride - plane.pixelStride * screenWidth) / plane.pixelStride
        val padded = createBitmap(screenWidth + rowPaddingPixels, screenHeight)
        padded.copyPixelsFromBuffer(plane.buffer)
        return if (rowPaddingPixels == 0) {
            padded
        } else {
            Bitmap.createBitmap(padded, 0, 0, screenWidth, screenHeight)
        }
    }

    override fun onDestroy() {
        isRunning = false
        scope.cancel()
        bubble?.let { windowManager?.removeViewImmediate(it) }
        bubble = null
        virtualDisplay?.release()
        imageReader?.close()
        projection?.stop()
        super.onDestroy()
    }

    companion object {
        @Volatile
        var isRunning: Boolean = false
            private set

        private const val ACTION_STOP = "com.baconnish.gobuddy.OVERLAY_STOP"
        private const val EXTRA_RESULT_CODE = "resultCode"
        private const val EXTRA_RESULT_DATA = "resultData"
        private const val CHANNEL_ID = "overlay"
        private const val NOTIFICATION_ID = 42

        fun start(context: Context, resultCode: Int, resultData: Intent) {
            val intent = Intent(context, OverlayScanService::class.java)
                .putExtra(EXTRA_RESULT_CODE, resultCode)
                .putExtra(EXTRA_RESULT_DATA, resultData)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, OverlayScanService::class.java).setAction(ACTION_STOP),
            )
        }
    }
}
