package com.example.offermatrix.network

object UserSession {
    var userId: Int = -1
    var username: String = ""
    var avatar: String? = null
    var roles: List<Role> = emptyList()
    var currentRole: Role? = null
    var token: String = ""

    fun clear() {
        userId = -1
        username = ""
        roles = emptyList()
        currentRole = null
        token = ""
    }
}
