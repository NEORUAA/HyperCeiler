package com.sevtinge.hyperceiler.utils

import com.sevtinge.hyperceiler.common.log.AndroidLog
import io.github.libxposed.service.XposedService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.concurrent.Volatile

object ScopeManager {

    private const val TAG = "ScopeManager"
    @Volatile
    private var sService: XposedService? = null

    @JvmStatic
    fun setService(service: XposedService) {
        sService = service
    }

    @JvmStatic
    fun getService(): XposedService? {
        try {
            return sService
        } catch (e: Exception) {
            AndroidLog.e(TAG, "getService failed", e)
        }
        return requireService()
    }

    fun requireService(): XposedService {
        val s: XposedService? = sService
        checkNotNull(s) { "XposedService not bound yet" }
        return s
    }

    interface ScopeCallback {
        fun onScopeOperationSuccess(message: String)
        fun onScopeOperationFail(message: String)
    }

    suspend fun getScope(): List<String>? = withContext(Dispatchers.IO) {
        val service = requireService()
        if (service == null) {
            AndroidLog.e(TAG, "getScope: LSPosed service not available.")
            return@withContext null
        }
        return@withContext try {
            service.scope
        } catch (e: Exception) {
            AndroidLog.e(TAG, "getScope failed", e)
            null
        }
    }

    /**
     * 为指定应用请求作用域（启用模块）。
     * @param packageName 要启用的应用包名。
     * @param callback 用于接收操作结果的回调。
     */
    suspend fun addScope(packageName: String, callback: ScopeCallback) {
        withContext(Dispatchers.Main) {
            val service = requireService()
            if (service == null) {
                callback.onScopeOperationFail("LSPosed service not available.")
                return@withContext
            }

            val serviceCallback = object : XposedService.OnScopeEventListener {
                override fun onScopeRequestApproved(packages: MutableList<String>) {
                    if (packages.contains(packageName)) {
                        callback.onScopeOperationSuccess("$packageName enabled successfully.")
                    } else {
                        callback.onScopeOperationFail("Scope request approved, but $packageName not in result.")
                    }
                }

                override fun onScopeRequestFailed(message: String) {
                    callback.onScopeOperationFail("Failed to enable $packageName: $message")
                }
            }

            try {
                service.requestScope(listOf(packageName), serviceCallback)
            } catch (e: Exception) {
                AndroidLog.e(TAG, "addScope failed", e)
                callback.onScopeOperationFail(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * 为指定应用移除作用域（禁用模块）。
     * @param packageName 要禁用的应用包名。
     * @return 成功则返回 null，失败则返回错误信息字符串。
     */
    suspend fun removeScope(packageName: String): String? = withContext(Dispatchers.IO) {
        val service = requireService()
        if (service == null) {
            AndroidLog.e(TAG, "removeScope: LSPosed service not available.")
            return@withContext "LSPosed service not available."
        }
        return@withContext try {
            service.removeScope(listOf(packageName))
            null
        } catch (e: Exception) {
            AndroidLog.e(TAG, "removeScope failed", e)
            e.message
        }
    }
}
