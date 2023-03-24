package io.legado.app

import com.script.SimpleBindings
import com.script.rhino.RhinoScriptEngine
import org.junit.Assert.assertEquals
import org.junit.Test


/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun jsTest() {
        val scriptEngine = RhinoScriptEngine()
        val map = hashMapOf("id" to "3242532321")
        map["id"] = "12314123"
        val bindings = SimpleBindings()
        bindings["result"] = map
        val js = "$=result;id=$.id;id"
        val result = scriptEngine.eval(js, bindings)?.toString()
        assertEquals("12314123", result)
    }


}
