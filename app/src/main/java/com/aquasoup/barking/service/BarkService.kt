package com.aquasoup.barking.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioRecord
import android.media.MediaPlayer
import android.net.Uri
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.aquasoup.barking.activities.MainActivity
import com.aquasoup.barking.R
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import java.util.Timer
import kotlin.concurrent.fixedRateTimer

const val LISTENER_WHAT = "DATA_LISTEN"
const val LISTENER_SENS = "DATA_SENS"
const val LISTENER_URI = "DATA_URI"

class BarkService : Service() {
    private val maxTime = 30000
    private var playing = false
    private val modelPath = "1.tflite"
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var record: AudioRecord
    private lateinit var timer: Timer

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
        record.stop()
        timer.cancel()
    }

    override fun onCreate() {
        super.onCreate()
        startForeground()
    }

    private fun createNotificationChannel(): NotificationChannel {

        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel("main", "main", importance).apply {
            description = "main"
        }
        // Register the channel with the system.
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        return channel
    }

    private fun startForeground() {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent: PendingIntent =
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)


            val notification = NotificationCompat.Builder(this, createNotificationChannel().id)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle(getString(R.string.listening))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .setContentText(getString(R.string.noises))
                .setContentIntent(pendingIntent)
                .build()

            ServiceCompat.startForeground(
                this,
                100,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST,
            )
        } catch (e: Exception) {
                stopSelf()
        }
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val what = intent?.getStringExtra(LISTENER_WHAT)
        var sens = intent?.getIntExtra(LISTENER_SENS, 100)
        val uri = intent?.getStringExtra(LISTENER_URI)

        if(sens == null){
            sens = 100
        }
        mediaPlayer = if(uri.isNullOrEmpty()){
            MediaPlayer.create(this, R.raw.relax)
        }else{
            MediaPlayer.create(this, Uri.parse(uri))
        }
        val classifier = AudioClassifier.createFromFile(this, modelPath)
        val tensor = classifier.createInputTensorAudio()

        record = classifier.createAudioRecord()
        record.startRecording()

        var count = maxTime
        val period = 200L

        timer = fixedRateTimer("class", false, 0, period) {
            tensor.load(record)
            val output = classifier.classify(tensor)
            val filteredModelOutput = output[0].categories.filter {
                it.score > sens.toFloat() / 100
            }

            val barking = filteredModelOutput.filter { it.label == what }

            if (barking.isNotEmpty() && !playing) {
                count = maxTime
                playing = true
                mediaPlayer.start()
            }

            if(barking.isNotEmpty() && playing){
                count = maxTime
            }

            if (playing) {
                count -= period.toInt()
            }

            if(count == 0){
                count = maxTime
                playing = false
                mediaPlayer.seekTo(0)
                mediaPlayer.pause()
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }
}