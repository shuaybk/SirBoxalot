package com.shuayb.capstone.android.sirboxalot

import android.content.*
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
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

    private lateinit var mBinding : ActivityMainBinding

    private lateinit var sharedPreferences : SharedPreferences

    var timerService: TimerService? = null
    var isBound = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        setTitle("") //Don't display title
        initSetup()

        val intent = Intent(this, TimerService::class.java)
        bindService(intent, myConnection, Context.BIND_AUTO_CREATE)

        GlobalScope.launch {
            while (true) {
                val temp = timerService
                if (temp != null) {
                    updateTimerViews(temp.getStatus(),
                        temp.getTimeRemaining(),
                        temp.getRoundsRemaining(),
                        temp.getNumRounds(),
                        temp.getTimerState())
                }
                delay(1000)
            }
        }

    }

    private fun initSetup() {
        updateTimerViews("", 0, 0, 0, TIMER_STATE_STOPPED)
        mBinding.startButton.setOnClickListener {
            //startCounterThread()
            timerService?.startCounterThread()
        }
        mBinding.pauseButton.setOnClickListener {
            //pauseCounterThread()
            timerService?.pauseCounterThread()
        }
        mBinding.resetButton.setOnClickListener {
            //resetButtonPressed()
            timerService?.resetButtonPressed()
        }
        mBinding.forwardButton.setOnClickListener {
            //timeRemaining = 0;
            timerService?.forwardPressed()
        }
    }

    private fun updateTimerViews(currStatus : String, timeRemaining : Int, roundsRemaining : Int, NUM_ROUNDS : Int, timerState : String) {
        runOnUiThread {
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


    private val myConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as TimerService.TimerServiceBinder
            timerService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }
}
