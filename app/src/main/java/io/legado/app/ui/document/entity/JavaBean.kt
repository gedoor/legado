package io.legado.app.ui.document.entity

import java.io.Serializable
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.*

/**
 * JavaBean类
 *
 * @author 李玉江[QQ:1032694760]
 * @since 2014-04-23 16:14
 */
open class JavaBean : Serializable {

    /**
     * 反射出所有字段值
     */
    override fun toString(): String {
        val list = ArrayList<Field>()
        var clazz: Class<*>? = javaClass
        list.addAll(listOf(*clazz!!.declaredFields))//得到自身的所有字段
        val sb = StringBuilder()
        while (clazz != Any::class.java) {
            clazz = clazz!!.superclass//得到继承自父类的字段
            val fields = clazz!!.declaredFields
            for (field in fields) {
                val modifier = field.modifiers
                if (Modifier.isPublic(modifier) || Modifier.isProtected(modifier)) {
                    list.add(field)
                }
            }
        }
        val fields = list.toTypedArray()
        for (field in fields) {
            val fieldName = field.name
            kotlin.runCatching {
                val obj = field.get(this)
                sb.append(fieldName)
                sb.append("=")
                sb.append(obj)
                sb.append("\n")
            }
        }
        return sb.toString()
    }

    companion object {
        private const val serialVersionUID = -6111323241670458039L
    }

}
