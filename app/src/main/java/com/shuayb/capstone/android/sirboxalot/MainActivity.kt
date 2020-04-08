package com.shuayb.capstone.android.sirboxalot

import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.shuayb.capstone.android.sirboxalot.databinding.ActivityMainBinding
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private val ROUND_TIME : Int = 15   //In seconds
    private val REST_TIME : Int = 10     //In seconds
    private val NUM_ROUNDS : Int = 3

    private lateinit var mBinding : ActivityMainBinding
    private lateinit var mediaPlayer : MediaPlayer
    private var timeRemaining : Int = 0
    private var roundsRemaining : Int = NUM_ROUNDS
    private var timerRunning : Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        initSetup()
    }

    private fun initSetup() {
        mediaPlayer = MediaPlayer.create(this, R.raw.triple_bell)
        mBinding.startButton.setOnClickListener {
            if (!timerRunning) {
                startCounterThread()
            }
        }
    }

    private fun updateTimerViews() {
        runOnUiThread {
            mBinding.roundTimeRemaining.setText("$timeRemaining")
            mBinding.roundsRemaining.setText("$roundsRemaining")
        }
    }

    private fun startCounterThread() {
        timerRunning = true
        GlobalScope.launch {
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
    }
}
