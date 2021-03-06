package com.exyui.android.debugbottle.components

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import com.squareup.leakcanary.LeakCanary
import com.exyui.android.debugbottle.ui.BlockCanary
import com.exyui.android.debugbottle.ui.BlockCanaryContext

import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.GINGERBREAD
import android.os.Bundle
import android.os.Process
import android.os.StrictMode
import android.util.Log
import android.widget.Toast
import com.exyui.android.debugbottle.components.crash.DTCrashHandler
import com.squareup.okhttp.OkHttpClient
import com.exyui.android.debugbottle.components.injector.Injector
import com.exyui.android.debugbottle.components.okhttp.LoggingInterceptor

/**
 * Created by yuriel on 8/10/16.
 */
object DTInstaller : Application.ActivityLifecycleCallbacks {

    private var installed: Boolean = false
    private var enabled = true
    //private var notification: Notification? = null
    private val NOTIFICATION_ID = 12030
    private var blockCanary: BlockCanaryContext? = null
        set(value) {
            if (!installed) field = value
        }
    private var app: Application? = null
        set(value) {
            if (!installed) field = value
        }
    private var injector: Injector? = null
        set(value) {
            if (!installed) field = value
        }
    private var httpClient: OkHttpClient? = null
        set(value) {
            if (!installed) field = value
        }

    internal var injectorClassName: String? = null

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {

    }

    override fun onActivityStarted(activity: Activity) {

    }

    override fun onActivityResumed(activity: Activity) {
        DTActivityManager.topActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {
        if (DTActivityManager.topActivity === activity) {
            DTActivityManager.topActivity = null
        }
    }

    override fun onActivityStopped(activity: Activity) {

    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle?) {

    }

    override fun onActivityDestroyed(activity: Activity) {

    }

    @JvmStatic fun install(app: Application): DTInstaller {
        this.app = app
        return this
    }

    fun setBlockCanary(context: BlockCanaryContext): DTInstaller {
        blockCanary = context
        return this
    }

    fun setInjector(injector: Injector): DTInstaller {
        this.injector = injector
        return this
    }

    fun setInjector(packageName: String): DTInstaller {
        injectorClassName = packageName
        try {
            val injectorClass = Class.forName(packageName)
            injector = injectorClass.newInstance() as Injector
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return this
    }

    fun setOkHttpClient(client: OkHttpClient): DTInstaller {
        httpClient = client
        return this
    }

    fun enable(): DTInstaller {
        enabled = true
        return this
    }

    fun disable(): DTInstaller {
        enabled = false
        return this
    }

    fun run() {
        RunningFeatureMgr.clear()
        if(!DTSettings.getBottleEnable() && enabled)
            return
        RunningFeatureMgr.add(RunningFeatureMgr.DEBUG_BOTTLE)
        installed = true
        if (null != blockCanary) {
            val blockCanary = BlockCanary.install(blockCanary!!)
            if (DTSettings.getBlockCanaryEnable()) {
                blockCanary.start()
                RunningFeatureMgr.add(RunningFeatureMgr.BLOCK_CANARY)
            } else {
                blockCanary.stop()
                RunningFeatureMgr.remove(RunningFeatureMgr.BLOCK_CANARY)
            }
        }
        if (null != app) {
            if (DTSettings.getStrictMode()) {
                enableStrictMode()
                RunningFeatureMgr.add(RunningFeatureMgr.STRICT_MODE)
            } else {
                RunningFeatureMgr.remove(RunningFeatureMgr.STRICT_MODE)
            }

            if (DTSettings.getLeakCanaryEnable()) {
                LeakCanary.install(app)
                RunningFeatureMgr.add(RunningFeatureMgr.LEAK_CANARY)
            } else {
                RunningFeatureMgr.remove(RunningFeatureMgr.LEAK_CANARY)
            }

            showNotification(app!!)
            registerActivityLifecycleCallbacks(app!!)
        }
        if (null != httpClient) {
            httpClient!!.interceptors().add(LoggingInterceptor())
            DTSettings.getNetworkSniff()
        }
        DTCrashHandler.install()
    }

    internal fun startInject(): Boolean {
        if (null != injector) {
            try {
                injector?.inject()
                return true
            } catch(e: Exception) {
                return false
            }
        }
        return true
    }

    internal fun setNotificationDisplay(display: Boolean) {
        if (display) {
            showNotification(app!!)
        } else {
            val mNotifyMgr = app?.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
            mNotifyMgr.cancel(NOTIFICATION_ID)
        }
    }

    private fun enableStrictMode() {
        if (SDK_INT >= GINGERBREAD) {
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .penaltyDeath()
                    .build())
            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build())
        }
    }

    @Suppress("DEPRECATION")
    private fun showNotification(app: Application) {
        //val view = RemoteViews(app.packageName, R.layout.__notification_main)
        //view.setTextViewText(R.id.notify_title, "start")
        val pi = PendingIntent.getActivity(app, 0, Intent(app, DTDrawerActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT)
        val notification: Notification
        val notify = Notification.Builder(app)
                .setSmallIcon(R.drawable.__dt_notification_bt)
                .setTicker("debug tool")
                .setContentIntent(pi)
                .setContentTitle("Debug Bottle")
                .setContentText("Debug Bottle is running correctly")
                .setOngoing(true)
        if (SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            notification = notify.notification
        } else {
            notification = notify.build()
        }

        val mNotifyMgr = app.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
        mNotifyMgr.notify(NOTIFICATION_ID, notification)
        Log.d(javaClass.simpleName, "started")
    }

    private fun registerActivityLifecycleCallbacks(app: Application) {
        app.registerActivityLifecycleCallbacks(this)
    }

    fun kill() {
        val intent = Intent()
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        try {
            DTActivityManager.topActivity?.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        Toast.makeText(app, R.string.__dt_kill_success, Toast.LENGTH_SHORT).show()
        Process.killProcess(Process.myPid())
    }

    internal fun getSP(fileName: String): SharedPreferences? {
        return app?.getSharedPreferences(fileName, Context.MODE_PRIVATE)
    }
}