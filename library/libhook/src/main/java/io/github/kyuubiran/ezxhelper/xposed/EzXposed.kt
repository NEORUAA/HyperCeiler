@file:Suppress("unused", "PrivateApi", "DiscouragedPrivateApi")
package io.github.kyuubiran.ezxhelper.xposed

import android.app.Application
import android.content.Context
import android.content.res.AssetManager
import android.content.res.Resources
import io.github.kyuubiran.ezxhelper.core.EzXReflection
import io.github.kyuubiran.ezxhelper.xposed.common.ModuleResources
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface

object EzXposed {
    internal lateinit var base: XposedInterface
        private set

    private var _appContext: Context? = null

    /**
     * Get application context.
     *
     * @throws NullPointerException if you get the appContext too early.
     */
    @JvmStatic
    val appContext: Context
        @Synchronized get() {
            if (_appContext == null) {
                _appContext = getCurrentApplicationContext()
                if (_appContext == null) {
                    throw NullPointerException("Cannot get application context, did application call Application.onCreate?")
                }
            }

            return _appContext!!
        }

    @JvmStatic
    lateinit var hookedPackageName: String
        private set

    @JvmStatic
    lateinit var modulePath: String
        private set

    @JvmStatic
    lateinit var moduleRes: Resources
        private set

    /**
     * Instantiates a new Xposed module in your [XposedModule.onModuleLoaded].
     */
    @JvmStatic
    fun initXposedModule(base: XposedInterface) {
        this.base = base
    }

    /**
     * You should invoke this function in [XposedModule.onPackageLoaded].
     *
     * For API 101, classLoader is available in onPackageReady; this method only
     * initializes reflection if the loader is accessible via reflection.
     */
    @JvmStatic
    fun initOnPackageLoaded(param: XposedModuleInterface.PackageLoadedParam) {
        hookedPackageName = param.packageName
        val loader = try {
            param.javaClass.methods.firstOrNull { it.name == "getClassLoader" && it.parameterTypes.isEmpty() }
                ?.invoke(param) as? ClassLoader
        } catch (_: Throwable) {
            null
        }
        if (loader != null) {
            EzXReflection.init(loader)
        }
    }

    /**
     * You should invoke this function in [XposedModule.onPackageReady].
     */
    @JvmStatic
    fun initOnPackageReady(param: XposedModuleInterface.PackageReadyParam) {
        EzXReflection.init(param.classLoader)
        hookedPackageName = param.packageName
    }

    /**
     * You should invoke this function in [XposedModule.onSystemServerStarting].
     */
    @JvmStatic
    fun initOnSystemServerStarting(param: XposedModuleInterface.SystemServerStartingParam) {
        EzXReflection.init(param.classLoader)
    }

    /**
     * Resolve the module APK path and prepare module-scoped Resources for immediate R access.
     * Call after initXposedModule so the base interface is already captured.
     */
    @JvmStatic
    fun initModuleResources() {
        this.modulePath = base.moduleApplicationInfo.sourceDir
        this.moduleRes = ModuleResources.create(modulePath)
    }

    /**
     * Initialize the application context.
     * Recommended invoke this after [Application.onCreate].
     */
    @JvmStatic
    fun initAppContext(
        context: Context? = getCurrentApplicationContext(),
        injectResources: Boolean = false,
    ) {
        if (context == null) {
            throw NullPointerException("Cannot initialize application context, context is null.")
        }
        _appContext = context
        if (injectResources) addModuleAssetPath(_appContext!!)
    }

    private fun getCurrentApplicationContext(): Context? {
        return try {
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            val currentApplicationMethod = activityThreadClass.getDeclaredMethod("currentApplication")
            currentApplicationMethod.invoke(null) as? Context
        } catch (e: Exception) {
            throw IllegalStateException("Failed to get application context", e)
        }
    }

    /**
     * Add module path to target Context.resources. Allow directly use module resources with R.xx.xxx.
     */
    @JvmStatic
    fun addModuleAssetPath(context: Context) {
        addModuleAssetPath(context.resources)
    }

    private val mAddAddAssertPath by lazy {
        AssetManager::class.java.getDeclaredMethod("addAssetPath", String::class.java).also { it.isAccessible = true }
    }

    /**
     * @see [addModuleAssetPath]
     */
    @JvmStatic
    fun addModuleAssetPath(resources: Resources) {
        mAddAddAssertPath.invoke(resources.assets, modulePath)
    }
}
