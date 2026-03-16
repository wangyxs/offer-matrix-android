package com.example.offermatrix.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.offermatrix.network.InterviewAnalysisResult
import com.example.offermatrix.network.InterviewRecordResponse
import com.example.offermatrix.network.RetrofitClient
import com.example.offermatrix.network.SaveInterviewRecordRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class InterviewRecordsUiState(
    val records: List<InterviewRecordResponse> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val analysisResult: InterviewAnalysisResult? = null,
    val isSaving: Boolean = false,
    val isAnalyzing: Boolean = false,
    val lastSavedRecordId: Int? = null
)

class InterviewRecordsViewModel : ViewModel() {
    
    companion object {
        private const val TAG = "InterviewRecordsVM"
    }
    
    private val _uiState = MutableStateFlow(InterviewRecordsUiState())
    val uiState: StateFlow<InterviewRecordsUiState> = _uiState.asStateFlow()
    
    init {
        loadRecords()
    }
    
    /**
     * 加载面试记录列表
     */
    fun loadRecords() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val records = RetrofitClient.apiService.getInterviewRecords()
                _uiState.value = _uiState.value.copy(
                    records = records,
                    isLoading = false
                )
                Log.i(TAG, "Loaded ${records.size} interview records")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load records", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "加载面试记录失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 保存面试记录
     */
    fun saveRecord(
        roleId: Int,
        title: String,
        content: String,
        duration: Int,
        onSuccess: (recordId: Int) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            try {
                val request = SaveInterviewRecordRequest(
                    role_id = roleId,
                    title = title,
                    content = content,
                    duration = duration
                )
                val response = RetrofitClient.apiService.saveInterviewRecord(request)
                if (response.success && response.data != null) {
                    val recordId = response.data.record_id
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        lastSavedRecordId = recordId
                    )
                    Log.i(TAG, "Interview record saved: id=$recordId")
                    onSuccess(recordId)
                    // 重新加载记录列表
                    loadRecords()
                } else {
                    onError(response.message)
                    _uiState.value = _uiState.value.copy(isSaving = false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save record", e)
                _uiState.value = _uiState.value.copy(isSaving = false)
                onError("保存失败: ${e.message}")
            }
        }
    }
    
    /**
     * 分析面试记录
     */
    fun analyzeRecord(
        recordId: Int,
        onSuccess: (InterviewAnalysisResult) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAnalyzing = true, analysisResult = null)
            try {
                val response = RetrofitClient.apiService.analyzeInterviewRecord(recordId)
                if (response.success && response.data != null) {
                    _uiState.value = _uiState.value.copy(
                        isAnalyzing = false,
                        analysisResult = response.data
                    )
                    Log.i(TAG, "Interview analyzed: score=${response.data.score}")
                    onSuccess(response.data)
                    // 重新加载记录列表以获取更新后的分数
                    loadRecords()
                } else {
                    onError(response.message)
                    _uiState.value = _uiState.value.copy(isAnalyzing = false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to analyze record", e)
                _uiState.value = _uiState.value.copy(isAnalyzing = false)
                onError("分析失败: ${e.message}")
            }
        }
    }
    
    /**
     * 保存并分析面试记录（一站式）
     */
    fun saveAndAnalyzeRecord(
        roleId: Int,
        title: String,
        content: String,
        duration: Int,
        onAnalysisComplete: (InterviewAnalysisResult) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        saveRecord(
            roleId = roleId,
            title = title,
            content = content,
            duration = duration,
            onSuccess = { recordId ->
                analyzeRecord(
                    recordId = recordId,
                    onSuccess = onAnalysisComplete,
                    onError = onError
                )
            },
            onError = onError
        )
    }
    
    /**
     * 删除面试记录（本地优化，避免闪烁）
     */
    fun deleteRecord(
        recordId: Int,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.deleteInterviewRecord(recordId)
                if (response.success) {
                    // 直接从本地列表中移除，避免重新请求
                    _uiState.value = _uiState.value.copy(
                        records = _uiState.value.records.filter { it.id != recordId }
                    )
                    Log.i(TAG, "Interview record deleted: id=$recordId")
                    onSuccess()
                } else {
                    onError(response.message)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete record", e)
                onError("删除失败: ${e.message}")
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun clearAnalysisResult() {
        _uiState.value = _uiState.value.copy(analysisResult = null)
    }
}
