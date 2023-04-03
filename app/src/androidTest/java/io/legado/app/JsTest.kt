package io.legado.app

import com.script.SimpleBindings
import io.legado.app.constant.SCRIPT_ENGINE
import io.legado.app.data.entities.BookChapter
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
        val bindings = SimpleBindings()
        bindings["result"] = map
        @Language("js")
        val jsMap = "$=result;id=$.id;id"
        val result = SCRIPT_ENGINE.eval(jsMap, bindings)?.toString()
        Assert.assertEquals("3242532321", result)
        @Language("js")
        val jsMap1 = """result.get("id")"""
        val result1 = SCRIPT_ENGINE.eval(jsMap1, bindings)?.toString()
        Assert.assertEquals("3242532321", result1)
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
        SCRIPT_ENGINE.eval(js)
        @Language("js")
        val js1 = """
            var returnData = new Packages.io.legado.app.api.ReturnData()
            returnData.getErrorMsg()
        """.trimIndent()
        val result1 = SCRIPT_ENGINE.eval(js1)
        Assert.assertEquals(result1, "未知错误,请联系开发者!")
    }

    @Test
    fun chapterText() {
        val chapter = BookChapter(title = "xxxyyy")
        val bindings = SimpleBindings()
        bindings["chapter"] = chapter
        val js = "chapter.title"
        val result = SCRIPT_ENGINE.eval(js, bindings)
        Assert.assertEquals(result, "xxxyyy")
    }

}