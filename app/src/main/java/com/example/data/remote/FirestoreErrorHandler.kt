package com.example.data.remote

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write")
}

object FirestoreErrorHandler {
    private const val TAG = "FirestoreError"

    fun handleFirestoreError(
        error: Exception?,
        operationType: OperationType,
        path: String?
    ): String {
        val currentUser = try {
            FirebaseAuth.getInstance().currentUser
        } catch (_: Exception) {
            null
        }

        val providerArray = org.json.JSONArray()
        currentUser?.providerData?.forEach { userInfo ->
            val obj = JSONObject().apply {
                put("providerId", userInfo.providerId)
                put("displayName", userInfo.displayName ?: "")
                put("email", userInfo.email ?: "")
                put("phoneNumber", userInfo.phoneNumber ?: "")
                put("photoUrl", userInfo.photoUrl?.toString() ?: "")
            }
            providerArray.put(obj)
        }

        val authInfo = JSONObject().apply {
            put("userId", currentUser?.uid ?: "")
            put("email", currentUser?.email ?: "")
            put("emailVerified", currentUser?.isEmailVerified ?: false)
            put("isAnonymous", currentUser?.isAnonymous ?: false)
            put("tenantId", currentUser?.tenantId ?: "")
            put("providerInfo", providerArray)
        }

        val errInfo = JSONObject().apply {
            put("error", error?.message ?: error?.toString() ?: "Unknown Firestore error")
            put("operationType", operationType.value)
            put("path", path ?: "")
            put("authInfo", authInfo)
        }

        val jsonString = errInfo.toString()
        Log.e(TAG, "Firestore Operation Failed: $jsonString", error)
        return jsonString
    }
}
