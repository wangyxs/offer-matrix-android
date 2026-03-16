package com.example.offermatrix

import android.app.Application
import android.util.Log
import com.example.offermatrix.interview.SpeechDialogManager

class OfferMatrixApplication : Application() {
    companion object {
        const val TAG = "OfferMatrixApp"
    }

    override fun onCreate() {
        super.onCreate()
         
        // 初始化字节语音对话SDK环境
        // 应用生命周期内仅需执行一次
        SpeechDialogManager.prepareEnvironment(applicationContext, this)

        Log.d(TAG, "App Created")
    }
}
