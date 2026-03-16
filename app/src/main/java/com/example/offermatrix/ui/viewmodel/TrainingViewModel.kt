package com.example.offermatrix.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.offermatrix.network.QuestionDetail
import com.example.offermatrix.network.QuestionSetRequest
import com.example.offermatrix.network.QuestionSetResponse
import com.example.offermatrix.network.RetrofitClient
import com.example.offermatrix.network.UserRole
import com.example.offermatrix.network.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TrainingUiState(
    val questionSets: List<QuestionSetResponse> = emptyList(),
    val favorites: List<QuestionDetail> = emptyList(),
    val roles: List<UserRole> = emptyList(),
    val selectedRole: UserRole? = null,
    val isLoading: Boolean = false,
    val generationStatus: String? = null // For toast messages
)

class TrainingViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(TrainingUiState())
    val uiState: StateFlow<TrainingUiState> = _uiState.asStateFlow()

    init {
        // Initial data load
        loadData()
        loadRoles()
    }

    fun loadData() {
        viewModelScope.launch {
            try {
                // Fetch in parallel for efficiency
                val sets = RetrofitClient.apiService.getQuestionSets()
                val favs = RetrofitClient.apiService.getFavorites()
                
                _uiState.value = _uiState.value.copy(
                    questionSets = sets,
                    favorites = favs
                )
                
                // 同时重新加载角色数据
                loadRoles()
            } catch (e: Exception) {
                e.printStackTrace()
                // Handle error state if needed
            }
        }
    }
    
    fun loadRoles() {
         viewModelScope.launch {
            try {
                val roles = RetrofitClient.apiService.getUserRoles(UserSession.userId)
                val currentRole = roles.find { it.role_id == UserSession.currentRole?.id } ?: roles.firstOrNull()
                _uiState.value = _uiState.value.copy(
                    roles = roles,
                    selectedRole = currentRole
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setSelectedRole(role: UserRole) {
        _uiState.value = _uiState.value.copy(selectedRole = role)
    }

    fun generateQuestionSet(
        questionCount: String,
        questionStyle: String,
        description: String
    ) {
        val role = _uiState.value.selectedRole ?: return
        
        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            try {
                val count = questionCount.replace("题", "").toInt()
                val styleParam = when {
                    questionStyle.contains("短文本") -> "short"
                    questionStyle.contains("长文本") -> "long"
                    else -> "medium"
                }

                val request = QuestionSetRequest(
                    role_id = role.role_id,
                    question_count = count,
                    question_style = styleParam,
                    extra_requirements = description.ifEmpty { null }
                )

                val response = RetrofitClient.apiService.generateQuestionSet(request)

                if (response.success) {
                    // Update list silently in the background
                    loadData()
                    _uiState.value = _uiState.value.copy(generationStatus = "题组生成成功！")
                } else {
                    _uiState.value = _uiState.value.copy(generationStatus = "生成失败: ${response.message}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(generationStatus = "网络错误，生成失败")
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
    
    fun clearGenerationStatus() {
        _uiState.value = _uiState.value.copy(generationStatus = null)
    }
}
