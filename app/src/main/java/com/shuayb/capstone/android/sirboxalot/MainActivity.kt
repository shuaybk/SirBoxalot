package com.shuayb.capstone.android.sirboxalot

import android.content.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.preference.PreferenceManager
import android.view.*
import androidx.databinding.DataBindingUtil
import com.shuayb.capstone.android.sirboxalot.Utils.RandomUtils
import com.shuayb.capstone.android.sirboxalot.databinding.ActivityMainBinding
import kotlinx.coroutines.*

/*
This requires further testing
The foreground service indeed runs indefinitely if running the code straight from Android Studio,
but is this still true if not connected to the terminal?  (Terminal displays print output which
may be changing results if it doesn't count it as "background" work)
*/

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
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        setTitle("") //Don't display title
        initSetup()

        val intent = Intent(this, TimerService::class.java)
        bindService(intent, myConnection, Context.BIND_AUTO_CREATE)
        startService(intent)

        if (savedInstanceState != null) {
            updateTimerViews()
        }

        GlobalScope.launch {
            while (true) {
                updateTimerViews()
                delay(250)  //High frequency update to ensure nothing skipped/delayed in timer view
            }
        }
    }

    private fun initSetup() {
        updateTimerViews()
        mBinding.startButton.setOnClickListener {
            timerService?.startCounterThread()
            updateTimerViews()
        }
        mBinding.pauseButton.setOnClickListener {
            timerService?.pauseCounterThread()
            updateTimerViews()
        }
        mBinding.resetButton.setOnClickListener {
            timerService?.resetButtonPressed()
            updateTimerViews()
        }
        mBinding.forwardButton.setOnClickListener {
            timerService?.forwardPressed()
            updateTimerViews()
        }
    }

    private fun updateTimerViews() {
        //Get values from TimerService (or set default values otherwise)
        val temp = timerService
        var currStatus: String = ""
        var timeRemaining: Int = 0
        var roundsRemaining: Int = 0
        var NUM_ROUNDS: Int = 0
        var timerState: String = TIMER_STATE_STOPPED
        if (temp != null) {
            currStatus = temp.getStatus()
            timeRemaining = temp.getTimeRemaining()
            roundsRemaining = temp.getRoundsRemaining()
            NUM_ROUNDS = temp.getNumRounds()
            timerState = temp.getTimerState()
        }

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

    override fun onDestroy() {
        super.onDestroy()
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
