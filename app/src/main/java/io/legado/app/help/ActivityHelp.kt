package io.legado.app.help

import android.app.Activity
import android.app.Application
import android.os.Bundle
import io.legado.app.utils.LanguageUtils
import java.lang.ref.WeakReference
import java.util.*

/**
 * Activity管理器,管理项目中Activity的状态
 */
@Suppress("unused")
object ActivityHelp : Application.ActivityLifecycleCallbacks {

    private val activities: MutableList<WeakReference<Activity>> = arrayListOf()

    fun size(): Int {
        return activities.size
    }

    /**
     * 判断指定Activity是否存在
     */
    fun isExist(activityClass: Class<*>): Boolean {
        activities.forEach { item ->
            if (item.get()?.javaClass == activityClass) {
                return true
            }
        }
        return false
    }

    /**
     * 添加Activity
     */
    fun add(activity: Activity) {
        activities.add(WeakReference(activity))
    }

    /**
     * 移除Activity
     */
    fun remove(activity: Activity) {
        for (temp in activities) {
            if (null != temp.get() && temp.get() === activity) {
                activities.remove(temp)
                break
            }
        }
    }

    /**
     * 移除Activity
     */
    fun remove(activityClass: Class<*>) {
        val iterator = activities.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (item.get()?.javaClass == activityClass) {
                iterator.remove()
            }
        }
    }

    /**
     * 关闭指定 activity
     */
    fun finishActivity(vararg activities: Activity) {
        activities.forEach { activity ->
            activity.finish()
        }
    }

    /**
     * 关闭指定 activity(class)
     */
    fun finishActivity(vararg activityClasses: Class<*>) {
        val waitFinish = ArrayList<WeakReference<Activity>>()
        for (temp in activities) {
            for (activityClass in activityClasses) {
                if (temp.get()?.javaClass == activityClass) {
                    waitFinish.add(temp)
                    break
                }
            }
        }
        waitFinish.forEach {
            it.get()?.finish()
        }
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivityResumed(activity: Activity) {
    }

    override fun onActivityStarted(activity: Activity) {

    }

    override fun onActivityDestroyed(activity: Activity) {
        remove(activity)
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        add(activity)
        if (!LanguageUtils.isSameWithSetting(activity)){
            LanguageUtils.setConfiguration(activity)
        }
    }
}