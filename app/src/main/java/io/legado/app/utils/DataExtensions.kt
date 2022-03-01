package io.legado.app.utils

import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

@Suppress("UNCHECKED_CAST")
fun <T : Any> T.deepCopy(): T {
    //如果不是数据类，直接返回
    if (!this::class.isData) {
        return this
    }

    //拿到构造函数
    return this::class.primaryConstructor!!.let { primaryConstructor ->
        //转换类型
        //memberProperties 返回非扩展属性中的第一个并将构造函数赋值给其
        //最终value=第一个参数类型的对象

        //如果当前类(这里的当前类指的是参数对应的类型，比如说这里如果非基本类型时)是数据类

        //最终返回一个新的映射map,即返回一个属性值重新组合的map，并调用callBy返回指定的对象
        primaryConstructor.parameters.associate { parameter ->
            //转换类型
            //memberProperties 返回非扩展属性中的第一个并将构造函数赋值给其
            //最终value=第一个参数类型的对象
            val value = (this::class as KClass<T>).memberProperties.first {
                it.name == parameter.name
            }.get(this)

            //如果当前类(这里的当前类指的是参数对应的类型，比如说这里如果非基本类型时)是数据类
            if ((parameter.type.classifier as? KClass<*>)?.isData == true) {
                parameter to value?.deepCopy()
            } else {
                parameter to value
            }

            //最终返回一个新的映射map,即返回一个属性值重新组合的map，并调用callBy返回指定的对象
        }.let(primaryConstructor::callBy)
    }
}