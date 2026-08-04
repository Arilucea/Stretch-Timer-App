package com.example.stretchtimer

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Binder
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class TimerService : Service() {

    private val binder = TimerBinder()

    // LiveData for UI
    private val _timeLeft = MutableLiveData<Long?>()
    val timeLeft: LiveData<Long?> = _timeLeft

    private val _currentRound = MutableLiveData<Int>(1)
    val currentRound: LiveData<Int> = _currentRound

    private val _totalRoundsLeft = MutableLiveData<Int?>(null)
    val totalRoundsLeft: LiveData<Int?> = _totalRoundsLeft

    private val _isTimerRunning = MutableLiveData<Boolean>(false)
    val isTimerRunning: LiveData<Boolean> = _isTimerRunning

    private val _isIntermediate = MutableLiveData<Boolean>(false)
    val isIntermediate: LiveData<Boolean> = _isIntermediate

    // Internal state
    private var isIntermediateInternal = false
    private var currentRoundInternal = 1
    private var roundsLeftInternal = 0
    private var isRunningInternal = false

    private var visualTimer: CountDownTimer? = null
    private var roundSeconds: Int = 0
    private var intermediateSeconds: Int = 0
    private var thereIsIntermediateRound: Boolean = false

    private var sound: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var alarmManager: AlarmManager

    private val NOTIFICATION_ID = 1
    // Updated Channel ID to reset any existing system notification settings
    private val CHANNEL_ID = "StretchTimer_Silent_v50"

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_ALARM = "ACTION_ALARM"
        const val EXTRA_ROUNDS = "EXTRA_ROUNDS"
        const val EXTRA_ROUND_SECS = "EXTRA_ROUND_SECS"
        const val EXTRA_INTER_SECS = "EXTRA_INTER_SECS"
    }

    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "StretchTimer::ServiceWakeLock")
        wakeLock?.setReferenceCounted(false)
        
        initializeMediaPlayer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val rounds = intent.getIntExtra(EXTRA_ROUNDS, 1)
                val roundSecs = intent.getIntExtra(EXTRA_ROUND_SECS, 1)
                val interSecs = intent.getIntExtra(EXTRA_INTER_SECS, 0)
                startTimerInternal(rounds, roundSecs, interSecs)
            }
            ACTION_STOP -> {
                stopTimerInternal()
            }
            ACTION_ALARM -> {
                handleAlarm()
            }
            else -> {
                startForeground(NOTIFICATION_ID, getNotification("Timer ready"))
            }
        }
        return START_STICKY
    }

    private fun initializeMediaPlayer() {
        try {
            // Standard Notification sound as requested
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            
            sound?.release()
            sound = MediaPlayer().apply {
                setDataSource(this@TimerService, notificationUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setWakeMode(this@TimerService, PowerManager.PARTIAL_WAKE_LOCK)
                prepare()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            // IMPORTANCE_LOW ensures it doesn't beep or pop up on updates
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Timer Countdown Updates",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Silent background updates for the stretch timer"
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
                setShowBadge(false)
            }
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun getNotification(content: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(this, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getService(this, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Stretch Timer")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true) // Crucial: don't beep when text changes
            .setSilent(true)        // Enforce silence at the builder level
            .setSound(null)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", stopPendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun startTimerInternal(rounds: Int, rSecs: Int, iSecs: Int) {
        cancelAlarm()
        
        roundSeconds = rSecs
        intermediateSeconds = iSecs
        thereIsIntermediateRound = iSecs > 0
        roundsLeftInternal = rounds
        currentRoundInternal = 1
        isIntermediateInternal = false
        isRunningInternal = true

        _totalRoundsLeft.postValue(roundsLeftInternal)
        _currentRound.postValue(currentRoundInternal)
        _isIntermediate.postValue(isIntermediateInternal)
        _timeLeft.postValue(roundSeconds.toLong())
        _isTimerRunning.postValue(isRunningInternal)

        if (wakeLock?.isHeld == false) {
            wakeLock?.acquire()
        }

        startForeground(NOTIFICATION_ID, getNotification("Round 1 started"))
        scheduleNextEvent(roundSeconds)
        startVisualTimer(roundSeconds)
    }

    private fun startVisualTimer(seconds: Int) {
        visualTimer?.cancel()
        visualTimer = object : CountDownTimer(seconds * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secs = millisUntilFinished / 1000
                _timeLeft.postValue(secs)
                val prefix = if (isIntermediateInternal) "Break" else "Round $currentRoundInternal"
                updateNotification("$prefix: $secs seconds left")
            }
            override fun onFinish() { _timeLeft.postValue(0) }
        }.start()
    }

    private fun scheduleNextEvent(seconds: Int) {
        val intent = Intent(this, TimerService::class.java).apply {
            action = ACTION_ALARM
        }
        
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(this, 2, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getService(this, 2, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        val triggerTime = SystemClock.elapsedRealtime() + (seconds * 1000L)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, pendingIntent)
        }
    }

    private fun handleAlarm() {
        if (!isRunningInternal) return

        if (isIntermediateInternal) {
            // Break finished -> Start Round
            playSound()
            isIntermediateInternal = false
            currentRoundInternal++
            
            _isIntermediate.postValue(false)
            _currentRound.postValue(currentRoundInternal)
            
            updateNotification("Round $currentRoundInternal started")
            scheduleNextEvent(roundSeconds)
            startVisualTimer(roundSeconds)
        } else {
            // Round finished -> Start Break or next Round
            roundsLeftInternal--
            _totalRoundsLeft.postValue(roundsLeftInternal)
            playSound()

            if (roundsLeftInternal > 0) {
                if (thereIsIntermediateRound) {
                    isIntermediateInternal = true
                    _isIntermediate.postValue(true)
                    updateNotification("Break started")
                    scheduleNextEvent(intermediateSeconds)
                    startVisualTimer(intermediateSeconds)
                } else {
                    currentRoundInternal++
                    _currentRound.postValue(currentRoundInternal)
                    updateNotification("Round $currentRoundInternal started")
                    scheduleNextEvent(roundSeconds)
                    startVisualTimer(roundSeconds)
                }
            } else {
                finishTimer()
            }
        }
    }

    private fun finishTimer() {
        isRunningInternal = false
        _isTimerRunning.postValue(false)
        _timeLeft.postValue(0)
        updateNotification("Timer Finished!")
        cancelAlarm()
        visualTimer?.cancel()
        
        if (wakeLock?.isHeld == true) {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (!isRunningInternal) {
                    wakeLock?.release()
                    stopForeground(STOP_FOREGROUND_DETACH)
                    stopSelf()
                }
            }, 5000)
        } else {
            stopForeground(STOP_FOREGROUND_DETACH)
            stopSelf()
        }
    }

    private fun updateNotification(content: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, getNotification(content))
    }

    private fun playSound() {
        try {
            sound?.let {
                if (it.isPlaying) {
                    it.pause()
                }
                it.seekTo(0)
                it.start()
            }
        } catch (e: Exception) {
            initializeMediaPlayer()
            sound?.start()
        }
    }

    private fun stopTimerInternal() {
        isRunningInternal = false
        cancelAlarm()
        visualTimer?.cancel()
        _timeLeft.postValue(null)
        _totalRoundsLeft.postValue(null)
        _isTimerRunning.postValue(false)
        
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun cancelAlarm() {
        val intent = Intent(this, TimerService::class.java).apply {
            action = ACTION_ALARM
        }
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(this, 2, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getService(this, 2, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        alarmManager.cancel(pendingIntent)
    }

    override fun onDestroy() {
        stopTimerInternal()
        sound?.release()
        super.onDestroy()
    }
}
