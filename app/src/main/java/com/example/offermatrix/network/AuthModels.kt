package com.example.offermatrix.network

data class Document(
    val id: Int,
    val name: String,
    val size: String?,
    val date: String,
    val url: String
)

data class DocumentContent(
    val content: String
)

data class LoginRequest(
    val username: String,
    val password: String
)

data class RegisterRequest(
    val username: String,
    val password: String,
    val email: String? = null
)

data class LoginResponse(
    val access_token: String,
    val token_type: String,
    val user_id: Int,
    val username: String,
    val avatar: String?,
    val roles: List<Role> = emptyList()
)

data class Role(
    val id: Int,
    val name: String,
    val category: String? = null,
    val description: String? = null,
    val prompt: String? = null
)

data class AssignRoleRequest(
    val user_id: Int,
    val role_id: Int
)

data class UserRole(
    val id: Int,
    val user_id: Int,
    val role_id: Int,
    val role: Role? = null
)

data class RegisterData(
    val user_id: Int,
    val username: String
)

data class RegisterResponse(
    val success: Boolean,
    val message: String,
    val data: RegisterData?
)

data class ChatRequest(
    val user_input: String,
    val target_position: String,
    val current_question: String? = null,
    val history: List<Map<String, String>> = emptyList() // Added history
)

data class ChatResponse(
    val reply: String,
    val action: String, // speak, listen
    val data: Map<String, Any>? = null
)
