package com.shuayb.capstone.android.sirboxalot

import android.content.Intent
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.databinding.DataBindingUtil
import com.shuayb.capstone.android.sirboxalot.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private val TIMER_STATE_STOPPED = "stopped"
    private val TIMER_STATE_PAUSED = "paused"
    private val TIMER_STATE_RUNNING = "running"
    private val ROUND_TIME : Int = 15   //In seconds
    private val REST_TIME : Int = 10     //In seconds
    private val NUM_ROUNDS : Int = 3

    private lateinit var mBinding : ActivityMainBinding
    private lateinit var mediaPlayer : MediaPlayer
    private lateinit var counterJob : Job
    private lateinit var counterThread : Thread
    private var timeRemaining : Int = ROUND_TIME
    private var roundsRemaining : Int = NUM_ROUNDS
    private var timerState : String = TIMER_STATE_STOPPED     //State can be stopped/running/paused

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        initSetup()
    }

    private fun initSetup() {
        updateTimerViews()
        mediaPlayer = MediaPlayer.create(this, R.raw.triple_bell)
        mBinding.startButton.setOnClickListener {
            startCounterThread()
        }
        mBinding.pauseButton.setOnClickListener {
            pauseCounterThread()
        }
        mBinding.resetButton.setOnClickListener {
            resetCounter()
        }
    }

    private fun updateTimerViews() {
        runOnUiThread {
            mBinding.roundTimeRemaining.setText("$timeRemaining")
            mBinding.roundsRemaining.setText("$roundsRemaining")

            if (timerState == TIMER_STATE_STOPPED) {
                mBinding.startButton.visibility = View.VISIBLE
                mBinding.pauseButton.visibility = View.INVISIBLE
            } else if (timerState == TIMER_STATE_PAUSED) {
                mBinding.startButton.visibility = View.VISIBLE
                mBinding.pauseButton.visibility = View.INVISIBLE
            } else if (timerState == TIMER_STATE_RUNNING) {
                mBinding.startButton.visibility = View.INVISIBLE
                mBinding.pauseButton.visibility = View.VISIBLE
            }
        }
    }

    private fun startCounterThread() {
        if (roundsRemaining > 0 && timerState == TIMER_STATE_STOPPED) {
            mediaPlayer.start()
            timerState = TIMER_STATE_RUNNING
        }
        if (timerState == TIMER_STATE_PAUSED) {
            //play resume sound
            timerState = TIMER_STATE_RUNNING
        }
        updateTimerViews()
        counterJob = GlobalScope.launch {
            while (roundsRemaining > 0) {
                while (timeRemaining > 0) {
                    updateTimerViews()
                    delay(1000)
                    timeRemaining--
                }
                if (roundsRemaining > 1 && REST_TIME > 0) {
                    timeRemaining += REST_TIME
                    mediaPlayer.start()

                    while (timeRemaining > 0) {
                        updateTimerViews()
                        delay(1000)
                        timeRemaining--
                    }
                }
                roundsRemaining--
                if (roundsRemaining > 0) {
                    timeRemaining += ROUND_TIME
                    mediaPlayer.start()
                }
            }
            mediaPlayer.start() //End bell
            timerState = TIMER_STATE_STOPPED
            updateTimerViews()
        }
        /*
        counterJob = GlobalScope.launch {
            while (roundsRemaining > 0) {
                timeRemaining += ROUND_TIME
                mediaPlayer.start()
                while (timeRemaining > 0) {
                    updateTimerViews()
                    delay(1000)
                    timeRemaining--
                }
                if (roundsRemaining > 1 && REST_TIME > 0) {
                    timeRemaining += REST_TIME
                    mediaPlayer.start()

                    while (timeRemaining > 0) {
                        updateTimerViews()
                        delay(1000)
                        timeRemaining--
                    }
                }
                roundsRemaining--
            }
            updateTimerViews()
            mediaPlayer.start() //End bell
            timerRunning = false
        }
         */
    }

    private fun pauseCounterThread() {
        counterJob.cancel()
        timerState = TIMER_STATE_PAUSED
        updateTimerViews()
    }

    private fun resetCounter() {
        if (::counterJob.isInitialized) {
            counterJob.cancel()
        }
        timerState = TIMER_STATE_STOPPED
        roundsRemaining = NUM_ROUNDS
        timeRemaining = ROUND_TIME
        updateTimerViews()
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
