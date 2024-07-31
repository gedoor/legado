package io.legado.app

import com.script.ScriptBindings
import com.script.rhino.RhinoScriptEngine
import io.legado.app.data.entities.BookChapter
import org.intellij.lang.annotations.Language
import org.junit.Assert
import org.junit.Test

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
        val bindings = ScriptBindings()
        bindings["result"] = map
        @Language("js")
        val jsMap = "$=result;id=$.id;id"
        val result = RhinoScriptEngine.eval(jsMap, bindings)
        Assert.assertEquals("3242532321", result)
        @Language("js")
        val jsMap1 = """result.get("id")"""
        val result1 = RhinoScriptEngine.eval(jsMap1, bindings)
        Assert.assertEquals("3242532321", result1)
    }

    @Test
    fun testFor() {
        val scope = RhinoScriptEngine.run {
            val scope = getRuntimeScope(ScriptBindings())
            eval(printJs, scope)
            scope
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
        val result = RhinoScriptEngine.eval(jsFor, scope)
        Assert.assertEquals("12012", result)
    }

    @Test
    fun testReturnNull() {
        val result = RhinoScriptEngine.eval("null")
        Assert.assertEquals(null, result)
    }

    @Test
    fun testReplace() {
        @Language("js")
        val js = """
            s=result.match(/(.{1,6}?)(第.*)/);
            n=s[2].length-parseInt(6-s[1].length);
            s[2].substr(0,n);
        """.trimIndent()
        val x = RhinoScriptEngine.run {
            val bindings = ScriptBindings()
            bindings["result"] = "筳彩涫第七百一十四章 人头树鮺舦綸"
            eval(js, bindings)
        }
        Assert.assertEquals(x, "第七百一十四章 人头树")
    }


    @Test
    fun chapterText() {
        val chapter = BookChapter(title = "xxxyyy")
        val bindings = ScriptBindings()
        bindings["chapter"] = chapter
        @Language("js")
        val js = "chapter.title"
        val result = RhinoScriptEngine.eval(js, bindings)
        Assert.assertEquals(result, "xxxyyy")
    }

    @Test
    fun javaListForEach() {
        val list = arrayListOf(1, 2, 3)
        val bindings = ScriptBindings()
        bindings["list"] = list
        @Language("js")
        val js = """
            var result = 0
            list.forEach(item => {result = result + item})
            result
        """.trimIndent()
        val result = RhinoScriptEngine.eval(js, bindings)
        Assert.assertEquals(result, 6.0)
    }

    @Test
    fun typeofString() {
        val bindings = ScriptBindings()
        @Language("js")
        val js = """
            s = "" + String()
            typeof s
        """.trimIndent()
        val result = RhinoScriptEngine.eval(js, bindings)
        Assert.assertEquals(result, "string")
    }

}