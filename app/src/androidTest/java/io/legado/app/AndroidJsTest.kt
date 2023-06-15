package io.legado.app

import cn.hutool.core.lang.JarClassLoader
import com.script.SimpleBindings
import com.script.rhino.RhinoScriptEngine
import dalvik.system.DexClassLoader
import org.intellij.lang.annotations.Language
import org.junit.Assert
import org.junit.Test
import org.mozilla.javascript.DefiningClassLoader
import java.net.URLClassLoader

class AndroidJsTest {

    @Test
    fun testPackages() {
        @Language("js")
        val js = """
            var accessKeyId = '1111';
            var accessKeySecret = '2222';
            var timestamp = '3333';
            var aly = new JavaImporter(Packages.javax.crypto.Mac, Packages.javax.crypto.spec.SecretKeySpec, Packages.javax.xml.bind.DatatypeConverter, Packages.java.net.URLEncoder, Packages.java.lang.String, Packages.android.util.Base64);
            with (aly) {
                function percentEncode(value) {
                    return URLEncoder.encode(value, "UTF-8").replace("+", "%20")
                        .replace("*", "%2A").replace("%7E", "~")
                }
            
                function sign(stringToSign, accessKeySecret) {
                    var mac = Mac.getInstance('HmacSHA1');
                    mac.init(new SecretKeySpec(String(accessKeySecret + '&').getBytes("UTF-8"), "HmacSHA1"));
                    var signData = mac.doFinal(String(stringToSign).getBytes("UTF-8"));
                    var signBase64 = Base64.encodeToString(signData, Base64.NO_WRAP);
                    var signUrlEncode = percentEncode(signBase64);
                    return signUrlEncode;
                }
            }
            var query = 'AccessKeyId=' + accessKeyId + '&Action=CreateToken&Format=JSON&RegionId=cn-shanghai&SignatureMethod=HMAC-SHA1&SignatureNonce=' + "xxccrr" + '&SignatureVersion=1.0&Timestamp=' + percentEncode(timestamp) + '&Version=2019-02-28';
            var signStr = sign('GET&' + percentEncode('/') + '&' + percentEncode(query), accessKeySecret);
            var queryStringWithSign = "Signature=" + signStr + "&" + query;
            queryStringWithSign
        """.trimIndent()
        RhinoScriptEngine.eval(js)
        @Language("js")
        val js1 = """
            var returnData = new Packages.io.legado.app.api.ReturnData()
            returnData.getErrorMsg()
        """.trimIndent()
        val result1 = RhinoScriptEngine.eval(js1)
        Assert.assertEquals(result1, "未知错误,请联系开发者!")
    }

    @Test
    fun testPackages1() {
        URLClassLoader.getSystemClassLoader()
        DefiningClassLoader.getSystemClassLoader()
        JarClassLoader.getSystemClassLoader()
        DexClassLoader.getSystemClassLoader()
        @Language("js")
        val js = """
            var ji = new JavaImporter(Packages.org.mozilla.javascript.DefiningClassLoader)
            with(ji) {
              let x = DefiningClassLoader.getSystemClassLoader()
            }
        """.trimIndent()
        RhinoScriptEngine.eval(js)
    }

    @Test
    fun testMap() {
        val map = hashMapOf("id" to "3242532321")
        val bindings = SimpleBindings()
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

}