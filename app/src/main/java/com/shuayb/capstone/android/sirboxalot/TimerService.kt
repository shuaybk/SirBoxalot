package com.shuayb.capstone.android.sirboxalot

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import android.preference.PreferenceManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.shuayb.capstone.android.sirboxalot.Utils.RandomUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TimerService : Service() {

    private val TIMER_STATE_STOPPED = "stopped"
    private val TIMER_STATE_PAUSED = "paused"
    private val TIMER_STATE_RUNNING = "running"
    private val SOUND_TYPE_START_MAIN = "start"
    private val SOUND_TYPE_END_MAIN = "end"
    private val SOUND_TYPE_ROUND_END_WARN = "round_end_warning"
    private val SOUND_TYPE_REST_END_WARN = "rest_end_warning"
    private val SOUND_TYPE_INTER_ALERT = "inter_alert"
    private val SOUND_TYPE_PAUSE_RESUME = "pause/resume"
    private val NOTIFICATION_ID = 1

    //Initialize the real values from settings
    private var NUM_ROUNDS : Int = 0
    private var ROUND_TIME : Int = 0   //In seconds
    private var REST_TIME : Int = 0     //In seconds
    private var PREPARE_TIME : Int = 0     //In seconds
    private var ROUND_END_WARNING_TIME : Int = 0     //In seconds
    private var REST_END_WARNING_TIME : Int = 0     //In seconds
    private var INTER_ALTERT_TIME : Int = 0     //In seconds

    private lateinit var STATUS_PREP : String
    private lateinit var STATUS_FIGHT : String
    private lateinit var STATUS_REST : String
    private lateinit var STATUS_COMPLETE : String
    private lateinit var STATUS_PAUSED : String

    private lateinit var counterJob : Job
    private var timeRemaining : Int = 0
    private var roundsRemaining : Int = 0
    private var timerState : String = TIMER_STATE_STOPPED     //State can be stopped/running/paused
    private var prepareRequired : Boolean = true
    private var resting : Boolean = false
    private lateinit var sharedPreferences : SharedPreferences
    private var currStatus : String = ""

    private val mBinder = TimerServiceBinder()
    private lateinit var notificationManager : NotificationManagerCompat
    private lateinit var notificationBuilder : NotificationCompat.Builder
    private lateinit var notification : Notification


    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        println("Creating service")
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        initSetup()
    }

    private fun createNotificationAndStartForegroundService() {
        notificationManager = NotificationManagerCompat.from(this)
        notificationBuilder = NotificationCompat.Builder(this, BaseApp.CHANNEL_1_ID)

        var intent = Intent(applicationContext, MainActivity::class.java)
        var pIntent = PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        notification = notificationBuilder
            .setSmallIcon(R.drawable.ic_timer_white_24dp)
            .setContentTitle(RandomUtils.secondsToFormattedTime(0))
            .setPriority(NotificationCompat.PRIORITY_LOW)      //Redundant for Orea+ because we already defined it on the channel, but for anything below Oreo there is no channel, so define it here again
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pIntent)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun updateNotification(time : Int) {
        notification = notificationBuilder
            .setContentTitle(RandomUtils.secondsToFormattedTime(time))
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun stopForegroundService() {
        stopForeground(true);
    }

    private fun initSetup() {
        println("Setting up!!")
        STATUS_PREP = getString(R.string.status_prep)
        STATUS_FIGHT = getString(R.string.status_fight)
        STATUS_REST = getString(R.string.status_rest)
        STATUS_COMPLETE = getString(R.string.status_complete)
        STATUS_PAUSED = getString(R.string.status_paused)

        setValuesFromSettings()
        currStatus = ""
    }

    fun getStatus() : String {
        return currStatus
    }

    fun getTimeRemaining() : Int {
        return timeRemaining
    }

    fun getRoundsRemaining() : Int {
        return roundsRemaining
    }

    fun getNumRounds() : Int {
        return NUM_ROUNDS
    }

    fun getTimerState() : String {
        return timerState
    }

    fun startCounterThread() {
        if (timerState == TIMER_STATE_STOPPED) {
            setValuesFromSettings()
        }
        createNotificationAndStartForegroundService()

        if (roundsRemaining > 0 && timerState == TIMER_STATE_STOPPED) {
            timerState = TIMER_STATE_RUNNING
            prepareRequired = true
            timeRemaining = PREPARE_TIME
        }
        if (timerState == TIMER_STATE_PAUSED) {
            playSound(SOUND_TYPE_PAUSE_RESUME)
            timerState = TIMER_STATE_RUNNING
        }
        counterJob = GlobalScope.launch {
            if (prepareRequired && PREPARE_TIME > 0) {
                if (timeRemaining == PREPARE_TIME) {
                    playSound(SOUND_TYPE_PAUSE_RESUME)
                }
                //This is the prepare timer
                while (timeRemaining > 0) {
                    currStatus = STATUS_PREP
                    delay(1000)
                    timeRemaining--
                    updateNotification(timeRemaining)
                }
                prepareRequired = false
                timeRemaining = ROUND_TIME
                playSound(SOUND_TYPE_START_MAIN)
            }
            while (roundsRemaining > 0) {
                //This is the fight timer
                while (timeRemaining > 0 && !resting) {
                    if (timeRemaining == ROUND_END_WARNING_TIME) {
                        playSound(SOUND_TYPE_ROUND_END_WARN)
                    }
                    if (timeRemaining == INTER_ALTERT_TIME) {
                        playSound(SOUND_TYPE_INTER_ALERT)
                    }
                    currStatus = STATUS_FIGHT
                    delay(1000)
                    timeRemaining--
                    updateNotification(timeRemaining)
                }
                if (roundsRemaining > 1 && REST_TIME > 0) {
                    if (!resting) {
                        timeRemaining = REST_TIME
                        playSound(SOUND_TYPE_END_MAIN)
                    }
                    resting = true
                    //This is the rest timer
                    while (timeRemaining > 0) {
                        if (timeRemaining == REST_END_WARNING_TIME) {
                            playSound(SOUND_TYPE_REST_END_WARN)
                        }
                        currStatus = STATUS_REST
                        delay(1000)
                        timeRemaining--
                        updateNotification(timeRemaining)
                    }
                    resting = false
                }
                roundsRemaining--
                if (roundsRemaining > 0) {
                    timeRemaining = ROUND_TIME
                    updateNotification(timeRemaining)
                    playSound(SOUND_TYPE_START_MAIN)
                }
            }
            playSound(SOUND_TYPE_END_MAIN)
            timerState = TIMER_STATE_STOPPED
            currStatus = STATUS_COMPLETE
            stopForegroundService()
        }
    }

    fun pauseCounterThread() {
        playSound(SOUND_TYPE_PAUSE_RESUME)
        counterJob.cancel()
        timerState = TIMER_STATE_PAUSED
        currStatus = STATUS_PAUSED
    }

    private fun resetCounter() {
        stopForegroundService()
        if (::counterJob.isInitialized) {
            counterJob.cancel()
        }
        resting = false
        timerState = TIMER_STATE_STOPPED
        currStatus = ""
    }

    fun forwardPressed() {
        timeRemaining = 0
    }

    private fun setValuesFromSettings() {
        NUM_ROUNDS = sharedPreferences.getString("num_rounds_key", "10").toInt()
        ROUND_TIME = sharedPreferences.getString("round_time_key", "180").toInt()
        REST_TIME = sharedPreferences.getString("rest_time_key", "60").toInt()
        PREPARE_TIME = sharedPreferences.getString("prepare_time_key", "30").toInt()
        ROUND_END_WARNING_TIME = sharedPreferences.getString("round_end_warn_time_key", "30").toInt()
        REST_END_WARNING_TIME = sharedPreferences.getString("rest_end_warn_time_key", "15").toInt()
        INTER_ALTERT_TIME = sharedPreferences.getString("inter_round_alert_time_key", "90").toInt()
        timeRemaining = 0
        roundsRemaining = NUM_ROUNDS
    }

    fun resetButtonPressed() {
        setValuesFromSettings()
        resetCounter()
    }

    private fun playSound(soundType : String) {
        val soundValues = getResources().obtainTypedArray(R.array.sound_values)
        val strArray = resources.getStringArray(R.array.sound_values)
        var strSoundValue : String? = ""
        var soundId : Int = 0

        //Determine the string value of the raw resource we are going to play the sound for
        when (soundType) {
            SOUND_TYPE_START_MAIN -> strSoundValue = sharedPreferences.getString("round_start_sound", strArray[9])
            SOUND_TYPE_END_MAIN -> strSoundValue = sharedPreferences.getString("round_end_sound", strArray[9])
            SOUND_TYPE_ROUND_END_WARN -> strSoundValue = sharedPreferences.getString("round_end_warn_sound", strArray[2])
            SOUND_TYPE_REST_END_WARN -> strSoundValue = sharedPreferences.getString("rest_end_warn_sound", strArray[2])
            SOUND_TYPE_INTER_ALERT -> strSoundValue = sharedPreferences.getString("inter_round_alert_sound", strArray[4])
            SOUND_TYPE_PAUSE_RESUME -> strSoundValue = sharedPreferences.getString("pause_resume_sound", strArray[5])
        }

        //Determine the resource ID for the string raw resource
        for (i in 0 .. strArray.size - 1) {
            if (strArray[i] == strSoundValue) {
                soundId = soundValues.getResourceId(i, 0)
                break
            }
        }
        soundValues.recycle()
        launchSoundThread(soundId)
    }

    private fun launchSoundThread(rawResId : Int) {
        GlobalScope.launch {
            val mpSound = MediaPlayer.create(applicationContext, rawResId)
            mpSound.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::counterJob.isInitialized) {
            counterJob.cancel()
        }

    }

    inner class TimerServiceBinder : Binder() {
        fun getService() : TimerService {
            return this@TimerService
        }
    }
}
