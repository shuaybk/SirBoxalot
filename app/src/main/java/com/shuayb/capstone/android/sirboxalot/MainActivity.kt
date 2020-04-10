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
import androidx.databinding.DataBindingUtil
import com.shuayb.capstone.android.sirboxalot.databinding.ActivityMainBinding
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private val TIMER_STATE_STOPPED = "stopped"
    private val TIMER_STATE_PAUSED = "paused"
    private val TIMER_STATE_RUNNING = "running"

    //Initialize the real values from settings
    private var NUM_ROUNDS : Int = 0
    private var ROUND_TIME : Int = 0   //In seconds
    private var REST_TIME : Int = 0     //In seconds
    private var PREPARE_TIME : Int = 0     //In seconds

    private lateinit var STATUS_PREP : String
    private lateinit var STATUS_FIGHT : String
    private lateinit var STATUS_REST : String
    private lateinit var STATUS_COMPLETE : String
    private lateinit var STATUS_PAUSED : String

    private lateinit var mBinding : ActivityMainBinding
    private lateinit var mediaPlayer : MediaPlayer
    private lateinit var counterJob : Job
    private var timeRemaining : Int = 0
    private var roundsRemaining : Int = 0
    private var timerState : String = TIMER_STATE_STOPPED     //State can be stopped/running/paused
    private var prepareRequired : Boolean = true
    private lateinit var sharedPreferences : SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        initSetup()
    }

    private fun initSetup() {
        STATUS_PREP = getString(R.string.status_prep)
        STATUS_FIGHT = getString(R.string.status_fight)
        STATUS_REST = getString(R.string.status_rest)
        STATUS_COMPLETE = getString(R.string.status_complete)
        STATUS_PAUSED = getString(R.string.status_paused)

        setValuesFromSettings()

        updateTimerViews("")
        mediaPlayer = MediaPlayer.create(this, R.raw.triple_bell)
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

    private fun updateTimerViews(currStatus : String) {
        runOnUiThread {
            if (timeRemaining < 0) {
                timeRemaining = 0
            }
            mBinding.timerStatusText.setText(currStatus)
            mBinding.roundTimeRemaining.setText("$timeRemaining")
            mBinding.roundsRemaining.setText("$roundsRemaining")

            if (timerState == TIMER_STATE_STOPPED) {
                mBinding.startButton.visibility = View.VISIBLE
                mBinding.pauseButton.visibility = View.INVISIBLE
                mBinding.forwardButton.visibility = View.GONE
            } else if (timerState == TIMER_STATE_PAUSED) {
                mBinding.startButton.visibility = View.VISIBLE
                mBinding.pauseButton.visibility = View.INVISIBLE
                mBinding.forwardButton.visibility = View.VISIBLE
            } else if (timerState == TIMER_STATE_RUNNING) {
                mBinding.startButton.visibility = View.INVISIBLE
                mBinding.pauseButton.visibility = View.VISIBLE
                mBinding.forwardButton.visibility = View.VISIBLE
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
            //play resume sound
            timerState = TIMER_STATE_RUNNING
        }
        updateTimerViews("")
        counterJob = GlobalScope.launch {
            if (prepareRequired && PREPARE_TIME > 0) {
                //This is the prepare timer
                while (timeRemaining > 0) {
                    updateTimerViews(STATUS_PREP)
                    delay(1000)
                    timeRemaining--
                }
                prepareRequired = false
                timeRemaining = ROUND_TIME
                playSound()
            }
            while (roundsRemaining > 0) {
                //This is the fight timer
                while (timeRemaining > 0) {
                    updateTimerViews(STATUS_FIGHT)
                    delay(1000)
                    timeRemaining--
                }
                if (roundsRemaining > 1 && REST_TIME > 0) {
                    timeRemaining = REST_TIME
                    playSound()
                    //This is the rest timer
                    while (timeRemaining > 0) {
                        updateTimerViews(STATUS_REST)
                        delay(1000)
                        timeRemaining--
                    }
                }
                roundsRemaining--
                if (roundsRemaining > 0) {
                    timeRemaining = ROUND_TIME
                    playSound()
                }
            }
            playSound() //End bell
            timerState = TIMER_STATE_STOPPED
            updateTimerViews(STATUS_COMPLETE)
        }
    }

    private fun pauseCounterThread() {
        counterJob.cancel()
        timerState = TIMER_STATE_PAUSED
        updateTimerViews(STATUS_PAUSED)
    }

    private fun resetCounter() {
        if (::counterJob.isInitialized) {
            counterJob.cancel()
        }
        timerState = TIMER_STATE_STOPPED
        updateTimerViews("")
    }

    private fun setValuesFromSettings() {
        NUM_ROUNDS = sharedPreferences.getString("num_rounds_key", "3").toInt()
        ROUND_TIME = sharedPreferences.getString("round_time_key", "15").toInt()
        REST_TIME = sharedPreferences.getString("rest_time_key", "10").toInt()
        PREPARE_TIME = sharedPreferences.getString("prepare_time_key", "5").toInt()
        timeRemaining = ROUND_TIME
        roundsRemaining = NUM_ROUNDS
    }

    private fun resetButtonPressed() {
        setValuesFromSettings()
        resetCounter()
    }

    private fun playSound() {
        GlobalScope.launch {
            val mpSound = MediaPlayer.create(applicationContext, R.raw.triple_bell)
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

}
