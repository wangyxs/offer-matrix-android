package com.example.offermatrix.interview

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.offermatrix.R
import com.example.offermatrix.network.RetrofitClient
import com.example.offermatrix.network.SaveInterviewRecordRequest
import com.example.offermatrix.network.UserSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * 面试通话界面
 * 
 * 简洁的语音通话风格，无文字显示
 * 支持根据用户简历进行个性化面试
 */
class InterviewCallActivity : AppCompatActivity(), SpeechDialogCallback {
    
    companion object {
        private const val TAG = "InterviewCallActivity"
        private const val PERMISSION_REQUEST_CODE = 1001
    }
    
    private lateinit var dialogManager: SpeechDialogManager
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // UI组件
    private lateinit var tvStatus: TextView
    private lateinit var tvDuration: TextView
    private lateinit var voiceIndicator: View
    private lateinit var btnHangup: ImageButton
    
    // 隐藏的文本组件（保留用于回调）
    private lateinit var tvUserText: TextView
    private lateinit var tvAiText: TextView
    private lateinit var scrollView: ScrollView
    
    // 通话计时
    private var callStartTime: Long = 0
    private var durationRunnable: Runnable? = null
    
    // 语音波动动画
    private var voiceAnimator: ObjectAnimator? = null
    
    // 简历内容
    private var resumeContent: String? = null
    
    // 对话记录
    private data class ConversationTurn(
        val role: String,  // "user" 或 "assistant"
        val content: String,
        val timestamp: Long = System.currentTimeMillis()
    )
    private val conversationHistory = mutableListOf<ConversationTurn>()
    private var currentUserText = StringBuilder()
    private var currentAiText = StringBuilder()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_interview_call)
        
        initViews()
        initDialogManager()
        
        // 先获取简历，再检查权限启动面试
        fetchResumeAndStart()
    }
    
    private fun initViews() {
        tvStatus = findViewById(R.id.tvStatus)
        tvDuration = findViewById(R.id.tvDuration)
        voiceIndicator = findViewById(R.id.voiceIndicator)
        btnHangup = findViewById(R.id.btnHangup)
        
        // 隐藏的文本组件
        tvUserText = findViewById(R.id.tvUserText)
        tvAiText = findViewById(R.id.tvAiText)
        scrollView = findViewById(R.id.scrollView)
        
        btnHangup.setOnClickListener {
            endInterview()
        }
        
        updateStatus("正在准备...")
    }
    
    private fun initDialogManager() {
        dialogManager = SpeechDialogManager(this)
        dialogManager.setCallback(this)
    }
    
    /**
     * 获取用户简历内容，然后启动面试
     */
    private fun fetchResumeAndStart() {
        val currentRole = UserSession.currentRole
        
        if (currentRole == null) {
            Log.w(TAG, "No current role selected, starting without resume")
            checkPermissionsAndStart()
            return
        }
        
        updateStatus("正在加载简历...")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.i(TAG, "Fetching resume for role: ${currentRole.name} (id=${currentRole.id})")
                val response = RetrofitClient.apiService.getResumeContent(currentRole.id)
                Log.i(TAG, "Resume API response: success=${response.success}, message=${response.message}")
                
                if (response.success && response.data?.content != null) {
                    resumeContent = response.data.content
                    val preview = resumeContent?.take(500) ?: ""
                    Log.i(TAG, "========== RESUME CONTENT ==========")
                    Log.i(TAG, "Resume length: ${resumeContent?.length} chars")
                    Log.i(TAG, "Resume preview: $preview")
                    Log.i(TAG, "=====================================")
                    
                    withContext(Dispatchers.Main) {
                        // 将简历传递给 DialogManager
                        dialogManager.setResumeContext(resumeContent)
                        updateStatus("简历已加载")
                    }
                } else {
                    Log.w(TAG, "No resume found: ${response.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch resume", e)
            }
            
            // 无论是否获取到简历，都继续启动面试
            withContext(Dispatchers.Main) {
                checkPermissionsAndStart()
            }
        }
    }
    
    private fun checkPermissionsAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                PERMISSION_REQUEST_CODE
            )
        } else {
            startInterview()
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startInterview()
            } else {
                Toast.makeText(this, "需要录音权限才能进行面试", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    
    private fun startInterview() {
        updateStatus("正在连接...")
        
        Thread {
            val success = dialogManager.start()
            if (!success) {
                mainHandler.post {
                    Toast.makeText(this, "面试启动失败", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }.start()
    }
    
    private fun startDurationTimer() {
        callStartTime = System.currentTimeMillis()
        durationRunnable = object : Runnable {
            override fun run() {
                val elapsed = (System.currentTimeMillis() - callStartTime) / 1000
                val minutes = elapsed / 60
                val seconds = elapsed % 60
                tvDuration.text = String.format("%02d:%02d", minutes, seconds)
                mainHandler.postDelayed(this, 1000)
            }
        }
        mainHandler.post(durationRunnable!!)
    }
    
    private fun stopDurationTimer() {
        durationRunnable?.let { mainHandler.removeCallbacks(it) }
    }
    
    private fun showVoiceAnimation() {
        mainHandler.post {
            voiceIndicator.visibility = View.VISIBLE
            voiceAnimator = ObjectAnimator.ofFloat(voiceIndicator, "scaleX", 1f, 1.3f, 1f).apply {
                duration = 1000
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
            }
            val scaleY = ObjectAnimator.ofFloat(voiceIndicator, "scaleY", 1f, 1.3f, 1f).apply {
                duration = 1000
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
            }
            voiceAnimator?.start()
            scaleY.start()
        }
    }
    
    private fun hideVoiceAnimation() {
        mainHandler.post {
            voiceAnimator?.cancel()
            voiceIndicator.visibility = View.GONE
        }
    }
    
    private fun endInterview() {
        updateStatus("面试结束")
        stopDurationTimer()
        hideVoiceAnimation()
        dialogManager.stop()
        
        // 保存面试记录
        saveInterviewRecord()
        
        mainHandler.postDelayed({
            finish()
        }, 2000) // 延长以等待保存完成
    }
    
    /**
     * 保存面试记录到后端
     */
    private fun saveInterviewRecord() {
        val roleId = UserSession.currentRole?.id ?: return
        val duration = ((System.currentTimeMillis() - callStartTime) / 1000).toInt()
        
        // 打印对话历史详情用于调试
        Log.i(TAG, "=== Conversation History Debug ===")
        Log.i(TAG, "Total turns: ${conversationHistory.size}")
        conversationHistory.forEachIndexed { index, turn ->
            Log.i(TAG, "Turn $index: [${turn.role}] ${turn.content.take(100)}...")
        }
        Log.i(TAG, "=== End Conversation History ===")
        
        if (conversationHistory.isEmpty()) {
            Log.w(TAG, "No conversation to save")
            return
        }
        
        // 构建对话JSON
        val conversationJson = buildConversationJson()
        Log.i(TAG, "Saving interview record: ${conversationHistory.size} turns, ${duration}s")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = SaveInterviewRecordRequest(
                    role_id = roleId,
                    title = "${UserSession.currentRole?.name ?: "模拟"}面试",
                    content = conversationJson,
                    duration = duration
                )
                
                val response = RetrofitClient.apiService.saveInterviewRecord(request)
                
                if (response.success && response.data != null) {
                    Log.i(TAG, "Interview record saved: id=${response.data.record_id}")
                    
                    // 自动触发分析
                    try {
                        Log.i(TAG, "Triggering analysis for record ${response.data.record_id}")
                        val analyzeResponse = RetrofitClient.apiService.analyzeInterviewRecord(response.data.record_id)
                        if (analyzeResponse.success) {
                            Log.i(TAG, "Interview analyzed: score=${analyzeResponse.data?.score}")
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@InterviewCallActivity, 
                                    "面试分析完成，得分: ${analyzeResponse.data?.score?.toInt() ?: 0}", 
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            Log.e(TAG, "Analysis API returned failure: ${analyzeResponse.message}")
                        }
                    } catch (analyzeError: Exception) {
                        Log.e(TAG, "Error calling analyze API", analyzeError)
                    }
                } else {
                    Log.e(TAG, "Failed to save record: ${response.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving interview record", e)
            }
        }
    }
    
    /**
     * 构建对话记录的JSON字符串
     */
    private fun buildConversationJson(): String {
        val jsonArray = JSONArray()
        for (turn in conversationHistory) {
            val jsonObj = JSONObject().apply {
                put("role", turn.role)
                put("content", turn.content)
                put("timestamp", turn.timestamp)
            }
            jsonArray.put(jsonObj)
        }
        return jsonArray.toString()
    }
    
    private fun updateStatus(status: String) {
        mainHandler.post {
            tvStatus.text = status
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopDurationTimer()
        hideVoiceAnimation()
        dialogManager.destroy()
    }
    
    // ==================== SpeechDialogCallback ====================
    
    override fun onEngineStarted() {
        Log.i(TAG, "Engine started")
        val statusText = if (resumeContent != null) "通话中 (已加载简历)" else "通话中"
        updateStatus(statusText)
        startDurationTimer()
        
        // 延迟播报开场白
        mainHandler.postDelayed({
            dialogManager.sayHello()
        }, 500)
    }
    
    override fun onEngineStopped() {
        Log.i(TAG, "Engine stopped")
        updateStatus("通话已结束")
        stopDurationTimer()
    }
    
    override fun onError(errorCode: Int, message: String) {
        Log.e(TAG, "Error: code=$errorCode, message=$message")
        mainHandler.post {
            Toast.makeText(this, "连接出错", Toast.LENGTH_SHORT).show()
        }
        updateStatus("连接出错")
    }
    
    override fun onAsrStarted() {
        Log.i(TAG, "ASR started - user speaking")
        updateStatus("聆听中...")
        showVoiceAnimation()
    }
    
    override fun onAsrResult(text: String, isFinal: Boolean) {
        Log.i(TAG, "ASR result: $text (final=$isFinal)")
        // 记录用户发言
        // ASR返回的是增量或全量文本，最终结果才是完整的用户发言
        if (text.isNotBlank()) {
            if (isFinal) {
                // 最终结果，使用完整文本
                currentUserText.clear()
                currentUserText.append(text)
                Log.i(TAG, "Final ASR text captured: $text")
            } else {
                // 中间结果，暂存（会被下一个结果覆盖）
                currentUserText.clear()
                currentUserText.append(text)
            }
        }
    }
    
    override fun onAsrEnded() {
        Log.i(TAG, "ASR ended - user stopped speaking")
        updateStatus("思考中...")
        hideVoiceAnimation()
        
        // 保存用户这轮发言
        val userText = currentUserText.toString().trim()
        if (userText.isNotBlank()) {
            conversationHistory.add(ConversationTurn("user", userText))
            Log.i(TAG, "Saved user turn (${userText.length} chars): ${userText.take(100)}")
        } else {
            Log.w(TAG, "No user text to save - ASR may not have recognized speech")
        }
        currentUserText.clear()
    }
    
    override fun onChatResponse(text: String) {
        Log.i(TAG, "Chat response: $text")
        updateStatus("回复中...")
        showVoiceAnimation()
        
        // 累积AI回复
        currentAiText.append(text)
    }
    
    override fun onChatEnded() {
        Log.i(TAG, "Chat ended")
        updateStatus("通话中")
        hideVoiceAnimation()
        
        // 保存AI这轮回复
        if (currentAiText.isNotBlank()) {
            conversationHistory.add(ConversationTurn("assistant", currentAiText.toString()))
            Log.i(TAG, "Saved assistant turn: ${currentAiText.toString().take(50)}...")
            currentAiText.clear()
        }
    }
    
    override fun onInterviewFinished() {
        Log.i(TAG, "Interview finished signal received")
        mainHandler.post {
            // 模拟用户点击挂断按钮
            btnHangup.performClick()
            Toast.makeText(this, "面试已结束", Toast.LENGTH_SHORT).show()
        }
    }
}
