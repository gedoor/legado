package io.legado.app

import com.script.SimpleBindings
import io.legado.app.constant.SCRIPT_ENGINE
import org.intellij.lang.annotations.Language
import org.junit.Assert
import org.junit.Test
import org.mozilla.javascript.Context

class JsTest {

    @Language("js")
    private val printJs = """
        function print(str, newline) {
            if (typeof(str) == 'undefined') {
                str = 'undefined';
            } else if (str == null) {
                str = 'null';
            } 
            java.lang.System.out.print(String(str));
            if (newline) java.lang.System.out.print("\n");
        }
        function println(str) { 
            print(str, true);
        }
    """.trimIndent()

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
        val context = SCRIPT_ENGINE.getScriptContext(SimpleBindings())
        val scope = SCRIPT_ENGINE.getRuntimeScope(context)
        try {
            Context.enter().evaluateString(scope, printJs, "print", 1, null)
        } finally {
            Context.exit()
        }
        @Language("js")
        val jsFor = """
            let result = 0
            let a=[1,2,3]
            let l=a.length
            for (let i = 0;i<l;i++){
            	result = result + a[i]
                println(i)
            }
            for (let o of a){
            	result = result + o
                println(o)
            }
            for (let o in a){
            	result = result + o
                println(o)
            }
            result
        """.trimIndent()
        val result = SCRIPT_ENGINE.eval(jsFor, scope).toString()
        Assert.assertEquals("12012", result)
    }

    @Test
    fun testReturnNull() {
        val result = SCRIPT_ENGINE.eval("null")
        Assert.assertEquals(null, result)
    }

    @Test
    fun testReplace() {
        @Language("js")
        val js = """
          result.replace(/\,/g,"，")
            .replace(/\./g,"。")
            .replace(/\!/g,"！")
            .replace(/\?/g,"？")
            .replace(/\…/g,"……")
            .replace(/\;/g,"；")
            .replace(/\:/g,"：")
        """.trimIndent()
        val bindings = SimpleBindings()
        bindings["result"] = ",.!?…;:"
        val result = SCRIPT_ENGINE.eval(js, bindings).toString()
        Assert.assertEquals(result, "，。！？……；：")
    }

}