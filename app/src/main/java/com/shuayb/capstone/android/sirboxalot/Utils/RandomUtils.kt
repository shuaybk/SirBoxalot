package com.shuayb.capstone.android.sirboxalot.Utils

class RandomUtils {

    companion object {
        //Assume time is less than 100 minutes (ie. <6000 seconds)
        fun secondsToFormattedTime(timeSeconds: Int): String {
            var result: String = ""
            val minutes = timeSeconds / 60
            val seconds = timeSeconds % 60

            if (minutes < 10) {
                result += "0" + minutes + ":"
            } else {
                result += minutes.toString() + ":"
            }
            if (seconds < 10) {
                result += "0" + seconds
            } else {
                result += seconds.toString()
            }

            return result
        }
    }
}