package com.example.stretchtimer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TimerViewModel : ViewModel() {
    private val _timerService = MutableLiveData<TimerService?>()
    val timerService: LiveData<TimerService?> = _timerService

    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as TimerService.TimerBinder
            _timerService.value = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            _timerService.value = null
            isBound = false
        }
    }

    fun bindService(context: Context) {
        if (!isBound) {
            Intent(context, TimerService::class.java).also { intent ->
                context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            }
        }
    }

    fun unbindService(context: Context) {
        if (isBound) {
            context.unbindService(connection)
            isBound = false
            _timerService.value = null
        }
    }

    fun startTimer(context: Context, rounds: Int, rSecs: Int, iSecs: Int) {
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_START
            putExtra(TimerService.EXTRA_ROUNDS, rounds)
            putExtra(TimerService.EXTRA_ROUND_SECS, rSecs)
            putExtra(TimerService.EXTRA_INTER_SECS, iSecs)
        }
        context.startForegroundService(intent)
    }

    fun stopTimer(context: Context) {
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_STOP
        }
        context.startForegroundService(intent)
    }
}
