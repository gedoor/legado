package io.legado.app.help

import android.app.Activity
import java.lang.ref.WeakReference
import java.util.*

/**
 * Activity管理器,管理项目中Activity的状态
 */
class ActivityHelp private constructor() {

    companion object {

        private var activities: MutableList<WeakReference<Activity>> = arrayListOf()

        @Volatile
        private var instance: ActivityHelp? = null

        fun getInstance(): ActivityHelp? {
            if (null == instance) {
                synchronized(ActivityHelp::class.java) {
                    if (null == instance) {
                        instance = ActivityHelp()
                    }
                }
            }
            return instance
        }
    }

    fun getActivities(): List<WeakReference<Activity>> {
        return activities
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
        for (activity in activities) {
            activity?.finish()
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
        for (activityWeakReference in waitFinish) {
            activityWeakReference.get()?.finish()
        }
    }

    /**
     * 判断指定Activity是否存在
     */
    fun isExist(activityClass: Class<*>): Boolean? {
        var result = false
        for (item in activities) {
            if (item.get()?.javaClass == activityClass) {
                result = true
                break
            }
        }
        return result
    }

}