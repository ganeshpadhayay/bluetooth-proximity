package com.example.bluetoothproximity.util

import android.content.Context
import android.content.SharedPreferences
import com.example.bluetoothproximity.MyApplication

object SharedPreferenceHelper {
    fun getSharedPreferenceHelper(): SharedPreferences {
        return MyApplication.context.getSharedPreferences(Constants.SHARED_PREF_FILE_NAME, Context.MODE_PRIVATE)
    }
}