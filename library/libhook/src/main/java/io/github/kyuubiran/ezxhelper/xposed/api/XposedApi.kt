@file:Suppress("unused")
package io.github.kyuubiran.ezxhelper.xposed.api

import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.os.ParcelFileDescriptor
import android.util.Log
import io.github.kyuubiran.ezxhelper.xposed.EzXposed
import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Constructor
import java.lang.reflect.Executable
import java.lang.reflect.Method

object XposedApi {
    private val base get() = EzXposed.base

    @JvmStatic
    fun getFrameworkName(): String = base.frameworkName

    @JvmStatic
    fun getFrameworkVersion(): String = base.frameworkVersion

    @JvmStatic
    fun getFrameworkVersionCode(): Long = base.frameworkVersionCode

    @JvmStatic
    fun getFrameworkProperties(): Long = base.frameworkProperties

    @JvmStatic
    fun hook(method: Method, priority: Int, hooker: XposedInterface.Hooker): XposedInterface.HookHandle {
        return base.hook(method)
            .setPriority(priority)
            .intercept(hooker)
    }

    @JvmStatic
    fun <T> hook(constructor: Constructor<T>, priority: Int, hooker: XposedInterface.Hooker): XposedInterface.HookHandle {
        return base.hook(constructor)
            .setPriority(priority)
            .intercept(hooker)
    }

    @JvmStatic
    fun <T> hookClassInitializer(
        clazz: Class<T>,
        priority: Int,
        hooker: XposedInterface.Hooker
    ): XposedInterface.HookHandle {
        return base.hookClassInitializer(clazz)
            .setPriority(priority)
            .intercept(hooker)
    }

    @JvmStatic
    fun deoptimize(member: Executable): Boolean {
        return base.deoptimize(member)
    }

    @JvmStatic
    fun log(msg: String) {
        base.log(Log.INFO, "EzXHelper", msg)
    }

    @JvmStatic
    fun log(msg: String, thr: Throwable) {
        base.log(Log.ERROR, "EzXHelper", msg, thr)
    }

    @JvmStatic
    fun getRemotePreferences(group: String): SharedPreferences = base.getRemotePreferences(group)

    @JvmStatic
    fun getModuleApplicationInfo(): ApplicationInfo = base.moduleApplicationInfo

    @JvmStatic
    fun listRemoteFiles(): Array<String> = base.listRemoteFiles()

    @JvmStatic
    fun openRemoteFile(name: String): ParcelFileDescriptor = base.openRemoteFile(name)
}
