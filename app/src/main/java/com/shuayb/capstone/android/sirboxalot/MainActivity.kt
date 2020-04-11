package com.shuayb.capstone.android.sirboxalot

import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.shuayb.capstone.android.sirboxalot.Utils.RandomUtils
import com.shuayb.capstone.android.sirboxalot.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import kotlin.concurrent.timer

class MainActivity : AppCompatActivity() {

    private val TIMER_STATE_STOPPED = "stopped"
    private val TIMER_STATE_PAUSED = "paused"
    private val TIMER_STATE_RUNNING = "running"
    private val SOUND_TYPE_START_MAIN = "start"
    private val SOUND_TYPE_END_MAIN = "end"
    private val SOUND_TYPE_ROUND_END_WARN = "round_end_warning"
    private val SOUND_TYPE_REST_END_WARN = "rest_end_warning"
    private val SOUND_TYPE_INTER_ALERT = "inter_alert"
    private val SOUND_TYPE_PAUSE_RESUME = "pause/resume"
    private val BUNDLE_KEY_TIME_REMAINING = "bundle_key_time_remaining"
    private val BUNDLE_KEY_ROUNDS_REMAINING = "bundle_key_rounds_remaining"
    private val BUNDLE_KEY_TIMER_STATE = "bundle_key_timer_state"
    private val BUNDLE_KEY_PREPARE_REQUIRED = "bundle_key_prepare_required"
    private val BUNDLE_KEY_RESTING = "bundle_key_resting"
    private val BUNDLE_KEY_LAST_MESSAGE = "bundle_key_last_message"
    private val BUNDLE_KEY_NUM_ROUNDS = "bundle_key_num_rounds"
    private val BUNDLE_KEY_ROUND_TIME = "bundle_key_round_time"
    private val BUNDLE_KEY_REST_TIME = "bundle_key_rest_time"
    private val BUNDLE_KEY_PREPARE_TIME = "bundle_key_prepare_time"
    private val BUNDLE_KEY_ROUND_END_WARN_TIME = "bundle_key_round_end_warn_time"
    private val BUNDLE_KEY_REST_END_WARN_TIME = "bundle_key_rest_end_warn_time"
    private val BUNDLE_KEY_INTER_ALERT_TIME = "bundle_key_inter_alert_time"

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

    private lateinit var mBinding : ActivityMainBinding
    private lateinit var counterJob : Job
    private var timeRemaining : Int = 0
    private var roundsRemaining : Int = 0
    private var timerState : String = TIMER_STATE_STOPPED     //State can be stopped/running/paused
    private var prepareRequired : Boolean = true
    private var resting : Boolean = false
    private lateinit var sharedPreferences : SharedPreferences


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        setTitle("") //Don't display title
        initSetup()

        if (savedInstanceState != null) {
            restoreState(savedInstanceState)
        }
    }

    private fun initSetup() {
        STATUS_PREP = getString(R.string.status_prep)
        STATUS_FIGHT = getString(R.string.status_fight)
        STATUS_REST = getString(R.string.status_rest)
        STATUS_COMPLETE = getString(R.string.status_complete)
        STATUS_PAUSED = getString(R.string.status_paused)

        setValuesFromSettings()

        updateTimerViews("")
        mBinding.startButton.setOnClickListener {
            startCounterThread()
        }
        mBinding.pauseButton.setOnClickListener {
            pauseCounterThread()
        }
        mBinding.resetButton.setOnClickListener {
            resetButtonPressed()
        }
        mBinding.forwardButton.setOnClickListener {
            timeRemaining = 0;
        }
    }

    private fun restoreState(savedInstanceState: Bundle) {
        timeRemaining = savedInstanceState.getInt(BUNDLE_KEY_TIME_REMAINING)
        roundsRemaining = savedInstanceState.getInt(BUNDLE_KEY_ROUNDS_REMAINING)
        timerState = savedInstanceState.getString(BUNDLE_KEY_TIMER_STATE)
        prepareRequired = savedInstanceState.getBoolean(BUNDLE_KEY_PREPARE_REQUIRED)
        resting = savedInstanceState.getBoolean(BUNDLE_KEY_RESTING)
        val lastMessageDisplayed = savedInstanceState.getString(BUNDLE_KEY_LAST_MESSAGE)
        NUM_ROUNDS = savedInstanceState.getInt(BUNDLE_KEY_NUM_ROUNDS)
        ROUND_TIME = savedInstanceState.getInt(BUNDLE_KEY_ROUND_TIME)
        REST_TIME = savedInstanceState.getInt(BUNDLE_KEY_REST_TIME)
        PREPARE_TIME = savedInstanceState.getInt(BUNDLE_KEY_PREPARE_TIME)
        ROUND_END_WARNING_TIME = savedInstanceState.getInt(BUNDLE_KEY_ROUND_END_WARN_TIME)
        REST_END_WARNING_TIME = savedInstanceState.getInt(BUNDLE_KEY_REST_END_WARN_TIME)
        INTER_ALTERT_TIME = savedInstanceState.getInt(BUNDLE_KEY_INTER_ALERT_TIME)

        updateTimerViews(lastMessageDisplayed)
        when (timerState) {
            TIMER_STATE_RUNNING -> startCounterThread()
        }
    }

    private fun updateTimerViews(currStatus : String) {
        runOnUiThread {
            if (timeRemaining < 0) {
                timeRemaining = 0
            }
            mBinding.timerStatusText.setText(currStatus)
            mBinding.roundTimeRemaining.setText(RandomUtils.secondsToFormattedTime(timeRemaining))

            var currRoundInt = NUM_ROUNDS - roundsRemaining + 1
            if (currRoundInt > NUM_ROUNDS) { currRoundInt = NUM_ROUNDS }  //Otherwise completion will show an extra round
            val currRoundStr = getString(R.string.round_header) + " " + currRoundInt + " / " + NUM_ROUNDS
            mBinding.roundsRemaining.setText(currRoundStr)

            if (timerState == TIMER_STATE_STOPPED) {
                mBinding.startButton.visibility = View.VISIBLE
                mBinding.pauseButton.visibility = View.INVISIBLE
                mBinding.forwardButton.visibility = View.GONE
                mBinding.roundsRemaining.visibility = View.INVISIBLE
            } else if (timerState == TIMER_STATE_PAUSED) {
                mBinding.startButton.visibility = View.VISIBLE
                mBinding.pauseButton.visibility = View.INVISIBLE
                mBinding.forwardButton.visibility = View.VISIBLE
                mBinding.roundsRemaining.visibility = View.VISIBLE
            } else if (timerState == TIMER_STATE_RUNNING) {
                mBinding.startButton.visibility = View.INVISIBLE
                mBinding.pauseButton.visibility = View.VISIBLE
                mBinding.forwardButton.visibility = View.VISIBLE
                mBinding.roundsRemaining.visibility = View.VISIBLE
            }
        }
    }

    private fun startCounterThread() {
        if (roundsRemaining > 0 && timerState == TIMER_STATE_STOPPED) {
            timerState = TIMER_STATE_RUNNING
            prepareRequired = true
            timeRemaining = PREPARE_TIME
        }
        if (timerState == TIMER_STATE_PAUSED) {
            playSound(SOUND_TYPE_PAUSE_RESUME)
            timerState = TIMER_STATE_RUNNING
        }
        updateTimerViews("")
        counterJob = GlobalScope.launch {
            if (prepareRequired && PREPARE_TIME > 0) {
                if (timeRemaining == PREPARE_TIME) {
                    playSound(SOUND_TYPE_PAUSE_RESUME)
                }
                //This is the prepare timer
                while (timeRemaining > 0) {
                    updateTimerViews(STATUS_PREP)
                    delay(1000)
                    timeRemaining--
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
                    updateTimerViews(STATUS_FIGHT)
                    delay(1000)
                    timeRemaining--
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
                        updateTimerViews(STATUS_REST)
                        delay(1000)
                        timeRemaining--
                    }
                    resting = false
                }
                roundsRemaining--
                if (roundsRemaining > 0) {
                    timeRemaining = ROUND_TIME
                    playSound(SOUND_TYPE_START_MAIN)
                }
            }
            playSound(SOUND_TYPE_END_MAIN)
            timerState = TIMER_STATE_STOPPED
            updateTimerViews(STATUS_COMPLETE)
        }
    }

    private fun pauseCounterThread() {
        playSound(SOUND_TYPE_PAUSE_RESUME)
        counterJob.cancel()
        timerState = TIMER_STATE_PAUSED
        updateTimerViews(STATUS_PAUSED)
    }

    private fun resetCounter() {
        if (::counterJob.isInitialized) {
            counterJob.cancel()
        }
        resting = false
        timerState = TIMER_STATE_STOPPED
        updateTimerViews("")
    }

    private fun setValuesFromSettings() {
        NUM_ROUNDS = sharedPreferences.getString("num_rounds_key", "3").toInt()
        ROUND_TIME = sharedPreferences.getString("round_time_key", "15").toInt()
        REST_TIME = sharedPreferences.getString("rest_time_key", "10").toInt()
        PREPARE_TIME = sharedPreferences.getString("prepare_time_key", "5").toInt()
        ROUND_END_WARNING_TIME = sharedPreferences.getString("round_end_warn_time_key", "5").toInt()
        REST_END_WARNING_TIME = sharedPreferences.getString("rest_end_warn_time_key", "3").toInt()
        INTER_ALTERT_TIME = sharedPreferences.getString("inter_round_alert_time_key", "10").toInt()
        timeRemaining = 0
        roundsRemaining = NUM_ROUNDS
    }

    private fun resetButtonPressed() {
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                var intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::counterJob.isInitialized) {
            counterJob.cancel()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt(BUNDLE_KEY_TIME_REMAINING, timeRemaining)
        outState.putInt(BUNDLE_KEY_ROUNDS_REMAINING, roundsRemaining)
        outState.putString(BUNDLE_KEY_TIMER_STATE, timerState)
        outState.putBoolean(BUNDLE_KEY_PREPARE_REQUIRED, prepareRequired)
        outState.putBoolean(BUNDLE_KEY_RESTING, resting)
        outState.putString(BUNDLE_KEY_LAST_MESSAGE, mBinding.timerStatusText.text.toString())
        outState.putInt(BUNDLE_KEY_NUM_ROUNDS, NUM_ROUNDS)
        outState.putInt(BUNDLE_KEY_ROUND_TIME, ROUND_TIME)
        outState.putInt(BUNDLE_KEY_REST_TIME, REST_TIME)
        outState.putInt(BUNDLE_KEY_PREPARE_TIME, PREPARE_TIME)
        outState.putInt(BUNDLE_KEY_ROUND_END_WARN_TIME, ROUND_END_WARNING_TIME)
        outState.putInt(BUNDLE_KEY_REST_END_WARN_TIME, REST_END_WARNING_TIME)
        outState.putInt(BUNDLE_KEY_INTER_ALERT_TIME, INTER_ALTERT_TIME)
    }
}
