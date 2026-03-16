package com.example.offermatrix.interview

import android.content.Context
import android.util.Log
import com.bytedance.speech.speechengine.SpeechEngine
import com.bytedance.speech.speechengine.SpeechEngineDefines
import com.bytedance.speech.speechengine.SpeechEngineGenerator
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/**
 * 字节端到端语音对话管理器
 * 
 * 封装SDK调用，提供简洁的API
 */
class SpeechDialogManager(private val context: Context) : SpeechEngine.SpeechListener {
    
    companion object {
        private const val TAG = "SpeechDialogManager"
        
        /**
         * 在Application中调用一次，初始化SDK环境
         */
        fun prepareEnvironment(context: Context, application: android.app.Application) {
            SpeechEngineGenerator.PrepareEnvironment(context, application)
            Log.i(TAG, "SDK environment prepared")
        }
    }
    
    private var engine: SpeechEngine? = null
    private var callback: SpeechDialogCallback? = null
    private var isEngineRunning = false
    private var resumeContext: String? = null
    private var pendingGreeting = false  // 标记是否需要在引擎启动后发送问候
    private var aiTurnCount = 0 // 对话轮数计数器
    
    /**
     * 从简历内容中提取用户名
     */
    private fun extractUserNameFromResume(resume: String): String? {
        val patterns = listOf(
            Regex("姓名[：:：\\s]*([\\u4e00-\\u9fa5\\w\\s]{2,10})"),
            Regex("Name[：:：\\s]*([A-Za-z\\s]{2,30})"),
            Regex("姓[：:：\\s]*([\\u4e00-\\u9fa5\\w]{1,2})[\\s\\t]*名[：:：\\s]*([\\u4e00-\\u9fa5\\w]{1,3})")
        )
        
        for (pattern in patterns) {
            val matchResult = pattern.find(resume)
            if (matchResult != null && matchResult.groupValues.isNotEmpty()) {
                val name = matchResult.groupValues[1].trim()
                if (name.isNotEmpty()) {
                    Log.i(TAG, "Extracted user name from resume: $name")
                    return name
                }
            }
        }
        return null
    }
    
    /**
     * 设置简历上下文（在start之前调用）
     */
    fun setResumeContext(resume: String?) {
        this.resumeContext = resume
        Log.i(TAG, "Resume context set, length: ${resume?.length ?: 0}")
    }
    
    /**
     * 设置回调监听
     */
    fun setCallback(callback: SpeechDialogCallback) {
        this.callback = callback
    }
    
    /**
     * 初始化并启动引擎
     */
    fun start(): Boolean {
        if (isEngineRunning) {
            Log.w(TAG, "Engine is already running")
            return true
        }

        if (!SpeechDialogConfig.hasCredentials()) {
            val message = "Speech dialog credentials are missing. Add them to local.properties before running the app."
            Log.e(TAG, message)
            callback?.onError(-1, message)
            return false
        }

        try {
            // 创建引擎实例
            engine = SpeechEngineGenerator.getInstance()
            engine?.createEngine()
            
            // 配置引擎参数
            configureEngine()
            
            // 初始化引擎
            val initRet = engine?.initEngine() ?: -1
            if (initRet != SpeechEngineDefines.ERR_NO_ERROR) {
                Log.e(TAG, "Engine init failed, code: $initRet")
                callback?.onError(initRet, "引擎初始化失败")
                return false
            }
            
            engine?.setContext(context)
            engine?.setListener(this)
            
            // 启动引擎
            engine?.sendDirective(SpeechEngineDefines.DIRECTIVE_SYNC_STOP_ENGINE, "")
            
            // 构建启动参数，包含面试官人设
            val startParams = JSONObject().apply {
                put("dialog", JSONObject().apply {
                    put("bot_name", SpeechDialogConfig.BOT_NAME)
                    // 设置面试官系统角色（O版本使用 system_role）
                    put("system_role", "你是一名严谨、专业的技术面试官。你的目标是评估候选人的真实技术深度。\n" +
                            "面试规则：\n" +
                            "1. 全程只进行专业技术问答，不要闲聊。\n" +
                            "2. 严格控制面试流程为5个技术问题。你需要依次提出5个问题，每个问题只针对一个技术点。\n" +
                            "3. 当用户回答后，如果回答不够深入，你可以进行一次追问，然后再进入下一个问题。\n" +
                            "4. 在用户回答完第5个问题（或追问）后，必须进行简短的总结，并明确说出'面试结束'，绝对不要再提出新的问题。")
                })
                // 指定模型版本支持RAG
                put("model", "O")
            }
            
            Log.i(TAG, "StartSession params: ${startParams.toString()}")
            
            val startRet = engine?.sendDirective(
                SpeechEngineDefines.DIRECTIVE_START_ENGINE, 
                startParams.toString()
            ) ?: -1
            
            if (startRet != SpeechEngineDefines.ERR_NO_ERROR) {
                Log.e(TAG, "Engine start failed, code: $startRet")
                callback?.onError(startRet, "引擎启动失败")
                return false
            }
            
            isEngineRunning = true
            aiTurnCount = 0 // 重置计数器
            Log.i(TAG, "Engine started successfully")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start engine", e)
            callback?.onError(-1, "启动异常: ${e.message}")
            return false
        }
    }
    
    /**
     * 配置引擎参数
     */
    private fun configureEngine() {
        engine?.apply {
            // Engine Name
            setOptionString(SpeechEngineDefines.PARAMS_KEY_ENGINE_NAME_STRING, SpeechEngineDefines.DIALOG_ENGINE)
            
            // Debug log (开发阶段开启)
            val logPath = context.getExternalFilesDir(null)?.absolutePath ?: ""
            setOptionString(SpeechEngineDefines.PARAMS_KEY_DEBUG_PATH_STRING, logPath)
            setOptionString(SpeechEngineDefines.PARAMS_KEY_LOG_LEVEL_STRING, SpeechEngineDefines.LOG_LEVEL_TRACE)
            
            // 鉴权配置
            setOptionString(SpeechEngineDefines.PARAMS_KEY_APP_ID_STRING, SpeechDialogConfig.APP_ID)
            setOptionString(SpeechEngineDefines.PARAMS_KEY_APP_KEY_STRING, SpeechDialogConfig.APP_KEY)
            setOptionString(SpeechEngineDefines.PARAMS_KEY_APP_TOKEN_STRING, SpeechDialogConfig.ACCESS_TOKEN)
            setOptionString(SpeechEngineDefines.PARAMS_KEY_RESOURCE_ID_STRING, SpeechDialogConfig.RESOURCE_ID)
            setOptionString(SpeechEngineDefines.PARAMS_KEY_UID_STRING, SpeechDialogConfig.UID)
            
            // 服务地址
            setOptionString(SpeechEngineDefines.PARAMS_KEY_DIALOG_ADDRESS_STRING, SpeechDialogConfig.DIALOG_ADDRESS)
            setOptionString(SpeechEngineDefines.PARAMS_KEY_DIALOG_URI_STRING, SpeechDialogConfig.DIALOG_URI)
            
            // AEC回声消除
            setOptionBoolean(SpeechEngineDefines.PARAMS_KEY_ENABLE_AEC_BOOL, true)
            val aecModelPath = copyAecModelToInternal()
            if (aecModelPath != null) {
                setOptionString(SpeechEngineDefines.PARAMS_KEY_AEC_MODEL_PATH_STRING, aecModelPath)
                Log.i(TAG, "AEC model path: $aecModelPath")
            } else {
                Log.w(TAG, "AEC model not found, disabling AEC")
                setOptionBoolean(SpeechEngineDefines.PARAMS_KEY_ENABLE_AEC_BOOL, false)
            }
            
            // 录音配置（使用设备麦克风）
            setOptionString(SpeechEngineDefines.PARAMS_KEY_RECORDER_TYPE_STRING, SpeechEngineDefines.RECORDER_TYPE_RECORDER)
            
            // 播放器配置（开启内置播放器）
            setOptionBoolean(SpeechEngineDefines.PARAMS_KEY_DIALOG_ENABLE_PLAYER_BOOL, true)
        }
    }
    
    /**
     * 将AEC模型从assets复制到内部存储
     */
    private fun copyAecModelToInternal(): String? {
        try {
            val aecFile = File(context.filesDir, SpeechDialogConfig.AEC_MODEL_FILE)
            if (!aecFile.exists()) {
                context.assets.open(SpeechDialogConfig.AEC_MODEL_FILE).use { input ->
                    FileOutputStream(aecFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            return aecFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy AEC model", e)
            return null
        }
    }
    
    /**
     * 播报开场白（外部调用入口）
     * 标记需要发送问候，实际发送在 MESSAGE_TYPE_ENGINE_START 回调中执行
     */
    fun sayHello() {
        pendingGreeting = true
        Log.i(TAG, "sayHello called, pendingGreeting set to true")
        // 如果引擎已经启动，直接发送
        if (isEngineRunning) {
            sendGreeting()
        }
        // 否则等待 MESSAGE_TYPE_ENGINE_START 回调
    }
    
    /**
     * 实际发送问候的逻辑（在引擎启动后调用）
     */
    private fun sendGreeting() {
        if (!pendingGreeting) return
        pendingGreeting = false
        
        if (!resumeContext.isNullOrBlank()) {
            // 有简历时，使用 ChatRAGText 作为开场白
            sendResumeRagGreeting()
        } else {
            // 无简历时，使用普通 SayHello
            sendSimpleGreeting()
        }
    }
    
    /**
     * 发送普通开场白（无简历）
     *
     * 策略升级：如果不传简历，直接根据当前角色，让 AI 提出一个专业技术问题 (不讲开场白)
     */
    private fun sendSimpleGreeting() {
        try {
            val roleName = com.example.offermatrix.network.UserSession.currentRole?.name ?: "Android开发"

            val prompt = buildString {
                append("【系统指令】\n")
                append("当前情境：用户未提供简历。\n")
                append("你的角色：资深 ${roleName} 面试官。\n")
                append("你的任务：\n")
                append("1. 跳过任何寒暄和自我介绍环节。\n")
                append("2. 直接向用户提出一个 ${roleName} 岗位常见且核心的技术面试题（这是第1个问题，共5个）。\n")
                append("3. 这个问题应当具有一定的区分度，能考察候选人的基础是否扎实。\n")
                append("4. 语气保持专业、直接。")
            }

            val queryJson = JSONObject().apply {
                put("content", prompt)
            }.toString()

            Log.i(TAG, "Sending ChatTextQuery (No Resume) for role: $roleName")

            // 使用 ChatTextQuery 让 AI 执行指令
            val ret = engine?.sendDirective(
                SpeechEngineDefines.DIRECTIVE_EVENT_CHAT_TEXT_QUERY,
                queryJson
            ) ?: -1

            if (ret != SpeechEngineDefines.ERR_NO_ERROR) {
                Log.e(TAG, "ChatTextQuery (No Resume) failed, code: $ret, fallback to SayHello")
                // 降级策略
                val params = JSONObject().apply {
                    put("content", "你好，我是今天的技术面试官。既然没有简历，那我们直接开始吧。请介绍一下你最擅长的技术栈。")
                }
                engine?.sendDirective(SpeechEngineDefines.DIRECTIVE_EVENT_SAY_HELLO, params.toString())
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to send simple greeting", e)
        }
    }
    
    /**
     * 使用 ChatTextQuery 发送带简历的开场指令
     *
     * 正确姿势：把简历作为"背景信息"，把"打招呼+提问"作为"任务"
     * 使用 ChatTextQuery 让 AI 主动执行任务，而不是朗读文本
     */
    private fun sendResumeRagGreeting() {
        try {
            val roleName = com.example.offermatrix.network.UserSession.currentRole?.name ?: "Android开发"
            
            // 从简历中提取用户名
            val userName = resumeContext?.let { extractUserNameFromResume(it) }
            
            // 截取简历，确保不超过长度限制
            val truncatedResume = if ((resumeContext?.length ?: 0) > 3000) {
                resumeContext?.take(3000) + "..."
            } else {
                resumeContext ?: ""
            }

            // 构造 Prompt：背景信息 + 任务指令
            val prompt = buildString {
                append("【系统指令】\n")
                append("你现在的角色是：资深 ${roleName} 面试官。\n")
                if (!userName.isNullOrBlank()) {
                    append("候选人的名字是：$userName。在打招呼时请直接称呼他的名字，不要用\"候选人\"、\"用户\"等称呼。\n")
                }
                append("用户的简历内容如下（仅供你参考，不要复述）：\n")
                append("[$truncatedResume]\n\n")
                append("【当前任务】\n")
                append("1. 简单打个招呼，告知用户你已看过简历。\n")
                append("2. 仔细阅读简历中的【项目经历】或【专业技能】部分。\n")
                append("3. 根据简历内容，针对性地提出第1个技术问题（共5题）。\n")
                append("4. 必须追问简历中提到的难点或具体实现细节，不要问宽泛的问题。\n")
            }

            // 使用 JSONObject 正确构建 JSON，避免控制字符问题
            val queryJson = JSONObject().apply {
                put("content", prompt)
            }.toString()

            Log.i(TAG, "Sending ChatTextQuery with resume, role: $roleName, prompt length: ${prompt.length}")
            Log.d(TAG, "ChatTextQuery params: $queryJson")

            // 使用 ChatTextQuery 让 AI 执行任务
            val ret = engine?.sendDirective(
                SpeechEngineDefines.DIRECTIVE_EVENT_CHAT_TEXT_QUERY,
                queryJson
            )

            if (ret == SpeechEngineDefines.ERR_NO_ERROR) {
                Log.i(TAG, "ChatTextQuery with resume sent successfully")
            } else {
                Log.e(TAG, "ChatTextQuery failed, code: $ret, falling back to SayHello")
                sendSimpleGreeting()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send resume greeting", e)
            sendSimpleGreeting()
        }
    }
    
    /**
     * 发送文本消息（不通过语音输入）
     */
    fun sendTextQuery(text: String) {
        val params = JSONObject().apply {
            put("content", text)
        }
        val ret = engine?.sendDirective(SpeechEngineDefines.DIRECTIVE_EVENT_CHAT_TEXT_QUERY, params.toString())
        if (ret != SpeechEngineDefines.ERR_NO_ERROR) {
            Log.e(TAG, "Send text query failed, code: $ret")
        }
    }
    
    /**
     * 停止引擎
     */
    fun stop() {
        if (!isEngineRunning) {
            return
        }
        
        try {
            engine?.sendDirective(SpeechEngineDefines.DIRECTIVE_SYNC_STOP_ENGINE, "")
            isEngineRunning = false
            Log.i(TAG, "Engine stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop engine", e)
        }
    }
    
    /**
     * 销毁引擎
     */
    fun destroy() {
        stop()
        try {
            engine?.destroyEngine()
            engine = null
            Log.i(TAG, "Engine destroyed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to destroy engine", e)
        }
    }
    
    // ==================== SpeechEngine.SpeechListener ====================
    
    // AI回复内容累积，用于判断是否结束面试
    private val currentAiResponse = StringBuilder()

    override fun onSpeechMessage(type: Int, data: ByteArray, len: Int) {
        val strData = String(data)
        
        when (type) {
            SpeechEngineDefines.MESSAGE_TYPE_ENGINE_START -> {
                Log.i(TAG, "Callback: 引擎启动成功 (SessionStarted): $strData")
                isEngineRunning = true
                
                // 【关键】在引擎真正启动后发送问候
                if (pendingGreeting) {
                    Log.i(TAG, "Engine started, now sending pending greeting...")
                    sendGreeting()
                }
                
                callback?.onEngineStarted()
            }
            
            SpeechEngineDefines.MESSAGE_TYPE_ENGINE_STOP -> {
                Log.i(TAG, "Callback: 引擎关闭: $strData")
                isEngineRunning = false
                callback?.onEngineStopped()
            }
            
            SpeechEngineDefines.MESSAGE_TYPE_ENGINE_ERROR -> {
                Log.e(TAG, "Callback: 错误信息: type=$type data=$strData")
                callback?.onError(type, strData)
            }
            
            SpeechEngineDefines.MESSAGE_TYPE_DIALOG_ASR_INFO -> {
                Log.i(TAG, "Callback: ASR 识别开始 (用户开始说话)")
                // 用户开始说话，清除上一轮AI的回复记录
                currentAiResponse.clear()
                callback?.onAsrStarted()
            }
            
            SpeechEngineDefines.MESSAGE_TYPE_DIALOG_ASR_RESPONSE -> {
                Log.d(TAG, "Callback: ASR 识别结果原始数据: $strData")
                try {
                    val json = JSONObject(strData)
                    
                    // SDK返回格式: {"results": [{"text": "用户说的话", "is_interim": false}], "extra": {...}}
                    val results = json.optJSONArray("results")
                    if (results != null && results.length() > 0) {
                        val firstResult = results.getJSONObject(0)
                        val text = firstResult.optString("text", "")
                        // is_interim=true 表示中间结果，is_interim=false 表示最终结果
                        val isInterim = firstResult.optBoolean("is_interim", true)
                        val isFinal = !isInterim
                        
                        Log.i(TAG, "ASR parsed from results: text='$text' isFinal=$isFinal")
                        
                        if (text.isNotEmpty()) {
                            callback?.onAsrResult(text, isFinal)
                        }
                    } else {
                        // 兼容旧格式：直接在根对象中查找
                        val text = json.optString("text", json.optString("content", ""))
                        val isFinal = json.optBoolean("is_final", false)
                        if (text.isNotEmpty()) {
                            Log.i(TAG, "ASR parsed from root: text='$text' isFinal=$isFinal")
                            callback?.onAsrResult(text, isFinal)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "ASR response parse error: ${e.message}, raw: $strData")
                    if (strData.isNotBlank() && !strData.startsWith("{")) {
                        callback?.onAsrResult(strData, false)
                    }
                }
            }
            
            SpeechEngineDefines.MESSAGE_TYPE_DIALOG_ASR_ENDED -> {
                Log.i(TAG, "Callback: ASR 识别结束 (用户停止说话)")
                callback?.onAsrEnded()
            }
            
            SpeechEngineDefines.MESSAGE_TYPE_DIALOG_CHAT_RESPONSE -> {
                Log.i(TAG, "Callback: Chat 回复内容: $strData")
                try {
                    val json = JSONObject(strData)
                    // SDK返回格式: {"content":"xxx", "question_id":"...", "reply_id":"..."}
                    // 优先取 content，其次取 text
                    val text = json.optString("content", json.optString("text", ""))
                    if (text.isNotEmpty()) {
                        // 累积回复内容
                        currentAiResponse.append(text)
                        callback?.onChatResponse(text)
                    }
                } catch (e: Exception) {
                    // 如果不是JSON，直接传递原始数据
                    if (strData.isNotBlank() && !strData.startsWith("{")) {
                        currentAiResponse.append(strData)
                        callback?.onChatResponse(strData)
                    }
                }
            }
            
            SpeechEngineDefines.MESSAGE_TYPE_DIALOG_CHAT_ENDED -> {
                Log.i(TAG, "Callback: Chat 回复结束")
                callback?.onChatEnded()
                
                val fullResponse = currentAiResponse.toString()
                Log.i(TAG, "Full AI Response for this turn: $fullResponse")
                
                // 检查是否包含结束语 "面试结束"
                if (fullResponse.contains("面试结束")) {
                    Log.i(TAG, "Detected '面试结束', scheduling hangup...")
                    // 延迟3秒挂断，确保TTS播报完成
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        Log.i(TAG, "Executing delayed stop due to interview finish keyword...")
                        // 不直接 stop，而是通知 UI 层去模拟点击挂断
                        callback?.onInterviewFinished() 
                    }, 4000) // 给予稍长一点的时间(4s)确保结语说完
                }
            }
            
            else -> {
                Log.d(TAG, "Callback: unknown type=$type")
            }
        }
    }
}
