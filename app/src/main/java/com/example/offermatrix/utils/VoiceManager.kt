package com.example.offermatrix.utils

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.example.offermatrix.network.RetrofitClient
import com.example.offermatrix.network.TTSRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * VoiceManager - 语音管理器
 * 使用 Android SpeechRecognizer 进行语音识别
 * 使用后端 Edge TTS API 进行语音合成（更自然的语音）
 */
class VoiceManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onStateChange: (String) -> Unit,
    private val onResult: (String) -> Unit,
    private val onPartialResult: ((String) -> Unit)? = null
) : RecognitionListener {

    companion object {
        private const val TAG = "VoiceManager"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isListening = false
    private var retryCount = 0
    private val maxRetries = 3
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    init {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer?.setRecognitionListener(this)
        Log.d(TAG, "VoiceManager initialized")
    }

    /**
     * 开始语音识别
     */
    fun startListening() {
        stopSpeaking() // 如果正在播放，先停止
        
        // 重置状态
        isListening = false
        retryCount = 0
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000L)
        }
        
        try {
            Log.d(TAG, "Starting listening...")
            speechRecognizer?.startListening(intent)
            isListening = true
            dispatchState("listening")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting listening", e)
            dispatchState("error")
        }
    }

    /**
     * 停止语音识别
     */
    fun stopListening() {
        if (isListening) {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping listening", e)
            }
            isListening = false
            dispatchState("idle")
        }
    }

    /**
     * 停止语音播放
     */
    fun stopSpeaking() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media player", e)
        }
    }

    /**
     * 使用后端 Edge TTS 播放语音
     */
    fun speak(text: String) {
        Log.d(TAG, "Speak requested: ${text.take(50)}...")
        dispatchState("speaking")
        
        scope.launch {
            try {
                // 调用后端 TTS API
                val response = RetrofitClient.apiService.textToSpeech(TTSRequest(text))
                
                // 将音频流保存到临时文件
                val tempFile = withContext(Dispatchers.IO) {
                    val file = File(context.cacheDir, "tts_audio_${System.currentTimeMillis()}.mp3")
                    response.byteStream().use { inputStream ->
                        FileOutputStream(file).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    file
                }
                
                Log.d(TAG, "TTS audio downloaded: ${tempFile.absolutePath}")
                
                // 播放音频
                withContext(Dispatchers.Main) {
                    playAudioFile(tempFile)
                }
            } catch (e: Exception) {
                Log.e(TAG, "TTS API error: ${e.message}", e)
                // 发送完成状态，让对话继续
                dispatchState("finished_speaking")
            }
        }
    }

    /**
     * 播放音频文件
     */
    private fun playAudioFile(file: File) {
        try {
            stopSpeaking() // 确保之前的播放已停止
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnPreparedListener { mp ->
                    Log.d(TAG, "MediaPlayer prepared, starting playback")
                    mp.start()
                }
                setOnCompletionListener { mp ->
                    Log.d(TAG, "MediaPlayer completed")
                    mp.release()
                    mediaPlayer = null
                    // 删除临时文件
                    file.delete()
                    // 通知播放完成
                    dispatchState("finished_speaking")
                }
                setOnErrorListener { mp, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    mp.release()
                    mediaPlayer = null
                    file.delete()
                    dispatchState("error")
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio file", e)
            file.delete()
            dispatchState("finished_speaking") // 即使出错也继续对话
        }
    }

    /**
     * 在主线程派发状态变化
     */
    private fun dispatchState(state: String) {
        Log.d(TAG, "State changed: $state")
        mainHandler.post {
            onStateChange(state)
        }
    }

    /**
     * 关闭并释放资源
     */
    fun shutdown() {
        Log.d(TAG, "Shutting down...")
        mainHandler.removeCallbacksAndMessages(null)
        try {
            stopListening()
            stopSpeaking()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error during shutdown", e)
        }
    }

    // ==================== SpeechRecognizer 回调 ====================
    
    override fun onReadyForSpeech(params: Bundle?) {
        Log.d(TAG, "Ready for speech")
    }

    override fun onBeginningOfSpeech() {
        Log.d(TAG, "Beginning of speech detected")
        dispatchState("detecting_voice")
    }

    override fun onRmsChanged(rmsdB: Float) {}

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        Log.d(TAG, "End of speech")
        isListening = false
        dispatchState("processing")
    }

    override fun onError(error: Int) {
        Log.e(TAG, "Speech recognition error: $error")
        isListening = false

        when (error) {
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
            SpeechRecognizer.ERROR_NO_MATCH -> {
                // 用户没说话或没听清，重试
                if (retryCount < maxRetries) {
                    retryCount++
                    Log.d(TAG, "Timeout/NoMatch, retrying... (attempt $retryCount)")
                    mainHandler.postDelayed({ startListening() }, 300)
                } else {
                    Log.d(TAG, "Max retries reached, going idle")
                    retryCount = 0
                    dispatchState("idle")
                }
            }
            SpeechRecognizer.ERROR_CLIENT -> {
                Log.d(TAG, "Client error, retrying...")
                mainHandler.postDelayed({ startListening() }, 500)
            }
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                Log.d(TAG, "Recognizer busy, waiting...")
                mainHandler.postDelayed({ startListening() }, 1000)
            }
            else -> {
                Log.e(TAG, "Unhandled error: $error")
                dispatchState("error")
            }
        }
    }

    override fun onResults(results: Bundle?) {
        Log.d(TAG, "Got results")
        isListening = false
        retryCount = 0

        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val text = matches[0]
            Log.d(TAG, "Recognized: $text")
            mainHandler.post { onResult(text) }
        } else {
            Log.w(TAG, "No matches in results")
            // 没有结果，重新开始监听
            mainHandler.postDelayed({ startListening() }, 300)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val partialText = matches[0]
            mainHandler.post { onPartialResult?.invoke(partialText) }
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}
}
