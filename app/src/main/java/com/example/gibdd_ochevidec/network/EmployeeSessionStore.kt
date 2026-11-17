package com.example.gibdd_ochevidec.network

import android.content.Context

data class EmployeeSession(val deviceId: String, val role: String?, val accessToken: String) {
    val authorization: String get() = "Bearer $accessToken"
}

class EmployeeSessionStore(context: Context) {
    private val preferences = context.getSharedPreferences("employee_session", Context.MODE_PRIVATE)

    fun save(response: RegisterDeviceResponse) {
        preferences.edit().putString("device_id", response.deviceId)
            .putString("role", response.role).putString("access_token", com.example.gibdd_ochevidec.SecretStore.encrypt(response.accessToken)).apply()
    }

    fun load(): EmployeeSession? {
        val deviceId = preferences.getString("device_id", null) ?: return null
        val stored = preferences.getString("access_token", null) ?: return null
        val accessToken = com.example.gibdd_ochevidec.SecretStore.decrypt(stored) ?: return null
        if (!stored.startsWith("v1:")) preferences.edit().putString("access_token", com.example.gibdd_ochevidec.SecretStore.encrypt(accessToken)).apply()
        return EmployeeSession(deviceId, preferences.getString("role", null), accessToken)
    }
}
