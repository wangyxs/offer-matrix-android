package com.example.offermatrix.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.DELETE

interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @GET("roles/")
    suspend fun getRoles(): List<Role>

    @POST("roles/assign")
    suspend fun assignRole(@Body request: AssignRoleRequest): ResponseModel<UserRole>

    @GET("roles/user/{userId}")
    suspend fun getUserRoles(@Path("userId") userId: Int): List<UserRole>

    @DELETE("roles/remove/{userId}/{roleId}")
    suspend fun removeRole(@Path("userId") userId: Int, @Path("roleId") roleId: Int): ResponseModel<UserRole>

    @POST("interview/chat")
    suspend fun mockInterview(@Body request: ChatRequest): ChatResponse

    @retrofit2.http.Multipart
    @POST("roles/user/{roleId}/documents")
    suspend fun uploadDocument(
        @Path("roleId") roleId: Int,
        @retrofit2.http.Query("file_type") fileType: String,
        @retrofit2.http.Part file: okhttp3.MultipartBody.Part
    ): ResponseModel<Document>

    @GET("roles/user/{roleId}/documents")
    suspend fun getDocuments(
        @Path("roleId") roleId: Int,
        @retrofit2.http.Query("file_type") fileType: String? = null
    ): List<Document>

    @DELETE("roles/documents/{docId}")
    suspend fun deleteDocument(@Path("docId") docId: Int): ResponseModel<Any>

    @GET("roles/documents/{docId}/content")
    suspend fun getDocumentContent(@Path("docId") docId: Int): ResponseModel<DocumentContent>

    @GET("roles/user/{roleId}/resume")
    suspend fun getResumeContent(@Path("roleId") roleId: Int): ResponseModel<DocumentContent>

    @retrofit2.http.PUT("users/me")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): ResponseModel<UpdateProfileResponse>

    // Question Sets
    @POST("question-sets/generate")
    suspend fun generateQuestionSet(@Body request: QuestionSetRequest): ResponseModel<Map<String, Any>>

    @GET("question-sets/")
    suspend fun getQuestionSets(): List<QuestionSetResponse>

    @GET("question-sets/{setId}")
    suspend fun getQuestionSetDetail(@Path("setId") setId: Int): QuestionSetDetail

    @DELETE("question-sets/{setId}")
    suspend fun deleteQuestionSet(@Path("setId") setId: Int): ResponseModel<Any>

    // Favorites
    @POST("interview/favorites/{questionId}")
    suspend fun addFavorite(@Path("questionId") questionId: Int): ResponseModel<Any>

    @DELETE("interview/favorites/{questionId}")
    suspend fun removeFavorite(@Path("questionId") questionId: Int): ResponseModel<Any>

    @GET("interview/favorites")
    suspend fun getFavorites(): List<QuestionDetail>

    @GET("interview/favorites/check/{questionId}")
    suspend fun checkFavoriteStatus(@Path("questionId") questionId: Int): Map<String, Boolean>

    @GET("interview/questions/{questionId}")
    suspend fun getQuestionById(@Path("questionId") questionId: Int): QuestionDetail

    // TTS - 文字转语音
    @POST("interview/tts")
    @retrofit2.http.Streaming
    suspend fun textToSpeech(@Body request: TTSRequest): okhttp3.ResponseBody

    // --- 面试记录 API ---
    
    // 保存面试记录
    @POST("interview/records")
    suspend fun saveInterviewRecord(@Body request: SaveInterviewRecordRequest): ResponseModel<SaveInterviewRecordResponse>
    
    // 获取面试记录列表
    @GET("interview/records")
    suspend fun getInterviewRecords(
        @retrofit2.http.Query("skip") skip: Int = 0,
        @retrofit2.http.Query("limit") limit: Int = 20
    ): List<InterviewRecordResponse>
    
    // 获取面试记录详情
    @GET("interview/records/{id}")
    suspend fun getInterviewRecordDetail(@Path("id") id: Int): InterviewRecordResponse
    
    // 分析面试记录
    @POST("interview/records/{id}/analyze")
    suspend fun analyzeInterviewRecord(@Path("id") id: Int): ResponseModel<InterviewAnalysisResult>
    
    // 删除面试记录
    @DELETE("interview/records/{id}")
    suspend fun deleteInterviewRecord(@Path("id") id: Int): ResponseModel<Any>
}

// TTS Request
data class TTSRequest(
    val text: String,
    val voice: String = "zh-CN-XiaoyiNeural"
)

data class UpdateProfileRequest(
    val username: String? = null,
    val avatar: String? = null,
    val password: String? = null
)

data class UpdateProfileResponse(
    val user_id: Int,
    val username: String,
    val avatar: String?
)

// Question Set related models
data class QuestionSetRequest(
    val role_id: Int,
    val question_count: Int,
    val question_style: String,  // "short", "medium", "long"
    val extra_requirements: String? = null
)

data class QuestionSetResponse(
    val id: Int,
    val user_id: Int,
    val role_id: Int,
    val title: String,
    val question_count: Int,
    val question_style: String,
    val extra_requirements: String?,
    val created_at: String?,
    val role_name: String?,
    val is_viewed: Boolean = false
)

data class QuestionDetail(
    val id: Int,
    val question_text: String,
    val answer_text: String?,
    val difficulty: String,
    val category: String?,
    val question_style: String,
    val ai_generated: Boolean
)

data class QuestionSetDetail(
    val id: Int,
    val user_id: Int,
    val role_id: Int,
    val title: String,
    val question_count: Int,
    val question_style: String,
    val extra_requirements: String?,
    val created_at: String,
    val role_name: String?,
    val questions: List<QuestionDetail>
)

data class ResponseModel<T>(
    val success: Boolean,
    val message: String,
    val data: T?
)

// --- 面试记录数据模型 ---

data class SaveInterviewRecordRequest(
    val role_id: Int,
    val title: String = "模拟面试",
    val content: String,  // JSON格式的对话记录
    val duration: Int = 0  // 面试时长（秒）
)

data class SaveInterviewRecordResponse(
    val record_id: Int
)

data class InterviewRecordResponse(
    val id: Int,
    val user_id: Int,
    val role_id: Int?,
    val title: String,
    val content: String?,
    val score: Float?,
    val feedback: String?,
    val is_completed: Boolean,
    val created_at: String,
    val role_name: String?
)

data class DimensionScores(
    val expression: Int = 0,
    val technical_depth: Int = 0,
    val logic: Int = 0,
    val communication: Int = 0
)

data class InterviewAnalysisResult(
    val score: Float,
    val strengths: List<String> = emptyList(),
    val weaknesses: List<String> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val detailed_feedback: String = "",
    val dimension_scores: DimensionScores? = null
)

