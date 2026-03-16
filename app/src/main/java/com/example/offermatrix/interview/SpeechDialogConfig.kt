package com.example.offermatrix.interview

import com.example.offermatrix.BuildConfig

/**
 * Speech dialog SDK configuration.
 *
 * Provide credentials through local.properties or environment variables:
 * - speechDialogAppId
 * - speechDialogAppKey
 * - speechDialogAccessToken
 */
object SpeechDialogConfig {
    const val APP_ID = BuildConfig.SPEECH_DIALOG_APP_ID
    const val APP_KEY = BuildConfig.SPEECH_DIALOG_APP_KEY
    const val ACCESS_TOKEN = BuildConfig.SPEECH_DIALOG_ACCESS_TOKEN
    const val RESOURCE_ID = "volc.speech.dialog"

    const val DIALOG_ADDRESS = "wss://openspeech.bytedance.com"
    const val DIALOG_URI = "/api/v3/realtime/dialogue"

    const val UID = "offermatrix_user"

    const val BOT_NAME = "面试官"
    const val GREETING = "你好，我是今天的面试官。让我们开始面试吧，请先简单介绍一下你自己。"

    const val AEC_MODEL_FILE = "aec.model"

    fun hasCredentials(): Boolean {
        return APP_ID.isNotBlank() && APP_KEY.isNotBlank() && ACCESS_TOKEN.isNotBlank()
    }
}
