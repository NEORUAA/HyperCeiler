@file:Suppress("unused")
package io.github.kyuubiran.ezxhelper.xposed.dsl

import io.github.kyuubiran.ezxhelper.xposed.api.XposedApi
import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam
import io.github.kyuubiran.ezxhelper.xposed.interfaces.IMethodAfterHookCallback
import io.github.kyuubiran.ezxhelper.xposed.interfaces.IMethodBeforeHookCallback
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Constructor
import java.lang.reflect.Executable
import java.lang.reflect.Member
import java.lang.reflect.Method
import java.util.function.Consumer

class HookFactory private constructor(private val target: Member) {

    private var beforeHook: IMethodBeforeHookCallback? = null
    private var afterHook: IMethodAfterHookCallback? = null

    /**
     * Hook method before invoke
     */
    fun before(callback: IMethodBeforeHookCallback?) {
        beforeHook = callback
    }

    /**
     * Hook method after invoked
     */
    fun after(callback: IMethodAfterHookCallback?) {
        afterHook = callback
    }

    /**
     * Replace the method, just a wrapper of [before]
     */
    fun replace(callback: (param: BeforeHookParam) -> Any?) {
        beforeHook = IMethodBeforeHookCallback { param -> param.result = callback(param) }
    }

    /**
     * Interrupt the method, make method return null
     */
    fun interrupt() {
        beforeHook = IMethodBeforeHookCallback { param -> param.result = null }
    }

    /**
     * Replace the result of the method, just a wrapper of [before]
     */
    fun returnConstant(constant: Any?) {
        beforeHook = IMethodBeforeHookCallback { param -> param.result = constant }
    }

    private fun create(priority: Int = XposedModule.PRIORITY_DEFAULT): XposedInterface.HookHandle {
        val before = beforeHook
        val after = afterHook

        val hooker = object : XposedInterface.Hooker {
            override fun intercept(chain: XposedInterface.Chain): Any? {
                val beforeParam = BeforeHookParam(chain)
                var skipped = false
                var result: Any? = null
                var throwable: Throwable? = null

                if (before != null) {
                    try {
                        before.onMethodHooked(beforeParam)
                    } catch (_: Throwable) {
                        // Ignore callback errors to keep the chain alive.
                    }
                }

                if (beforeParam.isSkipped) {
                    skipped = true
                    throwable = beforeParam.skipThrowable()
                    if (throwable == null) {
                        result = beforeParam.skipResult()
                    }
                } else {
                    try {
                        val args = beforeParam.argsForProceed()
                        result = if (chain.thisObject == null) {
                            chain.proceed(args)
                        } else {
                            chain.proceedWith(chain.thisObject, args)
                        }
                    } catch (t: Throwable) {
                        throwable = t
                    }
                }

                val afterParam = AfterHookParam(chain, beforeParam.argsForProceed(), skipped, result, throwable)
                if (after != null) {
                    val lastResult = afterParam.result
                    val lastThrowable = afterParam.throwable
                    try {
                        after.onMethodHooked(afterParam)
                    } catch (_: Throwable) {
                        if (lastThrowable == null) {
                            afterParam.result = lastResult
                        } else {
                            afterParam.throwable = lastThrowable
                        }
                    }
                }

                val finalThrowable = afterParam.throwable
                if (finalThrowable != null) {
                    throw finalThrowable
                }
                return afterParam.result
            }
        }

        val handle = when (target) {
            is Method -> XposedApi.hook(target, priority, hooker)
            is Constructor<*> -> XposedApi.hook(target, priority, hooker)
            else -> throw IllegalStateException("Unsupported member type: $target")
        }

        beforeHook = null
        afterHook = null

        return handle
    }

    @Suppress("ClassName")
    companion object `-Static` {
        // region Internal-API
        @JvmSynthetic
        private fun <T : Executable> T.internalCreateHook(
            priority: Int,
            block: HookFactory.() -> Unit
        ): XposedInterface.HookHandle = HookFactory(this).also(block).create(priority)

        @JvmSynthetic
        private fun <T : Executable> T.internalCreateHook(
            priority: Int,
            block: Consumer<HookFactory>
        ): XposedInterface.HookHandle = HookFactory(this).also { block.accept(it) }.create(priority)

        @JvmSynthetic
        private fun <T : Executable> T.internalCreateBeforeHook(
            priority: Int,
            block: IMethodBeforeHookCallback
        ): XposedInterface.HookHandle = HookFactory(this).apply { beforeHook = block }.create(priority)

        @JvmSynthetic
        private fun <T : Executable> T.internalCreateAfterHook(
            priority: Int,
            block: IMethodAfterHookCallback
        ): XposedInterface.HookHandle = HookFactory(this).apply { afterHook = block }.create(priority)
        // endregion

        @JvmName("-createMethodHook")
        @JvmSynthetic
        fun Method.createHook(
            priority: Int = XposedModule.PRIORITY_DEFAULT,
            block: HookFactory.() -> Unit
        ): XposedInterface.HookHandle = internalCreateHook(priority, block)

        @JvmStatic
        fun createMethodHook(method: Method, block: Consumer<HookFactory>): XposedInterface.HookHandle =
            createMethodHook(method, XposedModule.PRIORITY_DEFAULT, block)

        @JvmStatic
        fun createMethodHook(
            method: Method,
            priority: Int,
            block: Consumer<HookFactory>
        ): XposedInterface.HookHandle = method.internalCreateHook(priority, block)

        @JvmName("-createMethodBeforeHook")
        @JvmSynthetic
        fun Method.createBeforeHook(
            priority: Int = XposedModule.PRIORITY_DEFAULT,
            block: IMethodBeforeHookCallback
        ): XposedInterface.HookHandle = internalCreateBeforeHook(priority, block)

        @JvmName("-createMethodAfterHook")
        @JvmSynthetic
        fun Method.createAfterHook(
            priority: Int = XposedModule.PRIORITY_DEFAULT,
            block: IMethodAfterHookCallback
        ): XposedInterface.HookHandle = internalCreateAfterHook(priority, block)

        @JvmName("-createConstructorHook")
        @JvmSynthetic
        fun Constructor<*>.createHook(
            priority: Int = XposedModule.PRIORITY_DEFAULT,
            block: HookFactory.() -> Unit
        ): XposedInterface.HookHandle = internalCreateHook(priority, block)

        @JvmName("-createConstructorBeforeHook")
        @JvmSynthetic
        fun Constructor<*>.createBeforeHook(
            priority: Int = XposedModule.PRIORITY_DEFAULT,
            block: IMethodBeforeHookCallback
        ): XposedInterface.HookHandle = internalCreateBeforeHook(priority, block)

        @JvmName("-createConstructorAfterHook")
        @JvmSynthetic
        fun Constructor<*>.createAfterHook(
            priority: Int = XposedModule.PRIORITY_DEFAULT,
            block: IMethodAfterHookCallback
        ): XposedInterface.HookHandle = internalCreateAfterHook(priority, block)

        @JvmName("-createConstructorHooks")
        @JvmSynthetic
        fun Iterable<Constructor<*>>.createHooks(
            priority: Int = XposedModule.PRIORITY_DEFAULT,
            block: HookFactory.() -> Unit
        ): List<XposedInterface.HookHandle> =
            map { it.createHook(priority, block) }

        @JvmName("-createConstructorBeforeHooks")
        @JvmSynthetic
        fun Iterable<Constructor<*>>.createBeforeHooks(
            priority: Int = XposedModule.PRIORITY_DEFAULT,
            block: IMethodBeforeHookCallback
        ): List<XposedInterface.HookHandle> =
            map { it.createBeforeHook(priority, block) }

        @JvmName("-createConstructorAfterHooks")
        @JvmSynthetic
        fun Iterable<Constructor<*>>.createAfterHooks(
            priority: Int = XposedModule.PRIORITY_DEFAULT,
            block: IMethodAfterHookCallback
        ): List<XposedInterface.HookHandle> =
            map { it.createAfterHook(priority, block) }

        @JvmName("-createMethodHooks")
        @JvmSynthetic
        fun Iterable<Method>.createHooks(
            priority: Int = XposedModule.PRIORITY_DEFAULT,
            block: HookFactory.() -> Unit
        ): List<XposedInterface.HookHandle> =
            map { it.createHook(priority, block) }

        @JvmName("-createMethodBeforeHooks")
        @JvmSynthetic
        fun Iterable<Method>.createBeforeHooks(
            priority: Int = XposedModule.PRIORITY_DEFAULT,
            block: IMethodBeforeHookCallback
        ): List<XposedInterface.HookHandle> =
            map { it.createBeforeHook(priority, block) }

        @JvmName("-createMethodAfterHooks")
        @JvmSynthetic
        fun Iterable<Method>.createAfterHooks(
            priority: Int = XposedModule.PRIORITY_DEFAULT,
            block: IMethodAfterHookCallback
        ): List<XposedInterface.HookHandle> =
            map { it.createAfterHook(priority, block) }

        @JvmName("-createMethodHooks")
        @JvmSynthetic
        fun Array<Method>.createHooks(
            priority: Int = XposedModule.PRIORITY_DEFAULT,
            block: HookFactory.() -> Unit
        ): List<XposedInterface.HookHandle> =
            map { it.createHook(priority, block) }

        @JvmName("-createMethodBeforeHooks")
        @JvmSynthetic
        fun Array<Method>.createBeforeHooks(
            priority: Int = XposedModule.PRIORITY_DEFAULT,
            block: IMethodBeforeHookCallback
        ): List<XposedInterface.HookHandle> =
            map { it.createBeforeHook(priority, block) }

        @JvmName("-createMethodAfterHooks")
        @JvmSynthetic
        fun Array<Method>.createAfterHooks(
            priority: Int = XposedModule.PRIORITY_DEFAULT,
            block: IMethodAfterHookCallback
        ): List<XposedInterface.HookHandle> =
            map { it.createAfterHook(priority, block) }

        @JvmName("-createConstructorHooks")
        @JvmSynthetic
        fun Array<Constructor<*>>.createHooks(
            priority: Int = XposedModule.PRIORITY_DEFAULT,
            block: HookFactory.() -> Unit
        ): List<XposedInterface.HookHandle> =
            map { it.createHook(priority, block) }

        @JvmName("-createConstructorBeforeHooks")
        @JvmSynthetic
        fun Array<Constructor<*>>.createBeforeHooks(
            priority: Int = XposedModule.PRIORITY_DEFAULT,
            block: IMethodBeforeHookCallback
        ): List<XposedInterface.HookHandle> =
            map { it.createBeforeHook(priority, block) }

        @JvmName("-createConstructorAfterHooks")
        @JvmSynthetic
        fun Array<Constructor<*>>.createAfterHooks(
            priority: Int = XposedModule.PRIORITY_DEFAULT,
            block: IMethodAfterHookCallback
        ): List<XposedInterface.HookHandle> =
            map { it.createAfterHook(priority, block) }
    }
}
