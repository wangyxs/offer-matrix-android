package com.example.offermatrix.interview

/**
 * 语音对话回调接口
 */
interface SpeechDialogCallback {
    /**
     * 引擎启动成功
     */
    fun onEngineStarted()
    
    /**
     * 引擎已停止
     */
    fun onEngineStopped()
    
    /**
     * 发生错误
     * @param errorCode 错误码
     * @param message 错误信息
     */
    fun onError(errorCode: Int, message: String)
    
    /**
     * ASR语音识别开始（用户开始说话）
     */
    fun onAsrStarted()
    
    /**
     * ASR语音识别结果
     * @param text 识别到的文本
     * @param isFinal 是否为最终结果
     */
    fun onAsrResult(text: String, isFinal: Boolean)
    
    /**
     * ASR语音识别结束（用户停止说话）
     */
    fun onAsrEnded()
    
    /**
     * Chat对话回复
     * @param text 回复内容
     */
    fun onChatResponse(text: String)
    
    /**
     * Chat回复结束
     */
    fun onChatEnded()
    
    /**
     * 面试已完成（达到预定轮数，AI已说完结语）
     */
    fun onInterviewFinished()
}
