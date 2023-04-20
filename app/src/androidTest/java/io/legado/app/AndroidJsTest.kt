package io.legado.app

import io.legado.app.rhino.Rhino
import io.legado.app.rhino.putBinding
import org.intellij.lang.annotations.Language
import org.junit.Assert
import org.junit.Test

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
        Rhino.use {
            evaluateString(initStandardObjects(), js, "yy", 1, null)
        }
        @Language("js")
        val js1 = """
            var returnData = new Packages.io.legado.app.api.ReturnData()
            returnData.getErrorMsg()
        """.trimIndent()
        val result1 = Rhino.use {
            evaluateString(initStandardObjects(), js1, "xx", 1, null)
        }
        Assert.assertEquals(result1, "未知错误,请联系开发者!").let {

        }
    }

    @Test
    fun testMap() {
        val map = hashMapOf("id" to "3242532321")

        @Language("js")
        val jsMap = "$=result;id=$.id;id"
        val result = Rhino.use {
            val scope = initStandardObjects()
            scope.putBinding("result", map)
            evaluateString(scope, jsMap, "xxx", 1, null)
        }
        Assert.assertEquals("3242532321", result)
        @Language("js")
        val jsMap1 = """result.get("id")"""
        val result1 = Rhino.use {
            val scope = initStandardObjects()
            scope.putBinding("result", map)
            evaluateString(scope, jsMap1, "xxx", 1, null)
        }
        Assert.assertEquals("3242532321", result1)
    }

}