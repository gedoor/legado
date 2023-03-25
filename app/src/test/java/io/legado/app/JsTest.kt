package io.legado.app

import com.script.SimpleBindings
import io.legado.app.constant.SCRIPT_ENGINE
import org.intellij.lang.annotations.Language
import org.junit.Assert
import org.junit.Test

class JsTest {

    @Test
    fun testMap() {
        val map = hashMapOf("id" to "3242532321")
        map["id"] = "12314123"
        val bindings = SimpleBindings()
        bindings["result"] = map
        @Language("js")
        val jsMap = "$=result;id=$.id;id"
        val result = SCRIPT_ENGINE.eval(jsMap, bindings)?.toString()
        Assert.assertEquals("12314123", result)
    }


    @Test
    fun testFor() {
        @Language("js")
        val jsFor = """
            let result = 0
            let a=[1,2,3]
            let l=a.length
            for (let i = 0;i<l;i++){
            	result = result + a[i]
            }
            for (let o of a){
            	result = result + o
            }
            for (let o in a){
            	result = result + o
            }
            result
        """.trimIndent()
        val result = SCRIPT_ENGINE.eval(jsFor).toString()
        Assert.assertEquals("12012", result)
    }

    @Test
    fun testReturnNull() {
        val result = SCRIPT_ENGINE.eval("null")
        Assert.assertEquals(null, result)
    }

}