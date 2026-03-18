@file:Suppress("unused", "UNCHECKED_CAST", "NOTHING_TO_INLINE")
package io.github.kyuubiran.ezxhelper.xposed.common

import io.github.libxposed.api.XposedInterface

/**
 * Wraps [XposedInterface.Chain] with ergonomic helpers around `this`, arguments.
 */
class BeforeHookParam(
    private val chain: XposedInterface.Chain,
) {

    private var skipped: Boolean = false
    private var resultValue: Any? = null
    private var throwableValue: Throwable? = null
    private val argsArray: Array<Any?> = chain.args.toTypedArray()

    internal val isSkipped: Boolean
        get() = skipped

    internal fun skipResult(): Any? = resultValue

    internal fun skipThrowable(): Throwable? = throwableValue

    internal fun argsForProceed(): Array<Any?> = argsArray

    /**
     * Gets the method / constructor to be hooked.
     */
    val member
        get() = chain.executable

    /**
     * Non-null receiver instance for instance methods.
     */
    val thisObject: Any
        get() = chain.thisObject
            ?: throw NullPointerException("static method should not have thisObject")

    /**
     * Receiver instance or `null` for static methods.
     */
    val thisObjectOrNull: Any?
        get() = chain.thisObject

    /**
     * Convenience cast of [thisObject] to a specific type T.
     */
    inline fun <T> thisObjectAs(): T = thisObject as T

    /**
     * Arguments passed to the hooked method or constructor.
     *
     * Modifications to this array will be applied when proceeding.
     */
    val args: Array<Any?>
        get() = argsArray

    /**
     * Assign a return value and skip the original method or constructor.
     * For constructors the `result` is ignored.
     * Note: the after invocation callback will still be invoked.
     */
    var result: Any?
        get() = resultValue
        set(value) {
            skipped = true
            resultValue = value
            throwableValue = null
        }

    /**
     * Throws the given exception and skips the original method or constructor.
     * Note: the after invocation callback will still be invoked.
     */
    var throwable: Throwable?
        get() = throwableValue
        set(value) {
            skipped = true
            throwableValue = value
            resultValue = null
        }
}

/**
 * Wraps execution result for post-execution callbacks.
 */
class AfterHookParam(
    private val chain: XposedInterface.Chain,
    private val argsSnapshot: Array<Any?>,
    skipped: Boolean,
    result: Any?,
    throwable: Throwable?,
) {

    /**
     * Gets the method / constructor to be hooked.
     */
    val member
        get() = chain.executable

    /**
     * Non-null receiver instance for instance methods.
     */
    val thisObject: Any
        get() = chain.thisObject
            ?: throw NullPointerException("static method should not have thisObject")

    /**
     * Receiver instance or `null` for static methods.
     */
    val thisObjectOrNull: Any?
        get() = chain.thisObject

    /**
     * Convenience cast of [thisObject] to a specific type T.
     */
    inline fun <T> thisObjectAs(): T = thisObject as T

    /**
     * Arguments passed to the hooked method or constructor.
     */
    val args: Array<Any?>
        get() = argsSnapshot

    /**
     * Indicates whether before invocation skipped the original invocation.
     */
    val isSkipped: Boolean = skipped

    /**
     * Read or replace the method result observed after execution.
     */
    var result: Any? = result

    /**
     * Read, replace, or clear the thrown exception; setting `null` suppresses it.
     */
    var throwable: Throwable? = throwable
}
