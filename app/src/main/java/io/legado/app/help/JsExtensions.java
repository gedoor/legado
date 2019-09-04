package io.legado.app.help;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.legado.app.model.analyzeRule.AnalyzeUrl;
import io.legado.app.utils.Encoder;
import io.legado.app.utils.StringUtils;

public class JsExtensions {


    /**
     * js实现跨域访问,不能删
     */
    public String ajax(String urlStr) {
        try {
            AnalyzeUrl analyzeUrl = new AnalyzeUrl(urlStr, null, null, null, null, null);
/*            Response response = analyzeUrl.getResponseAsync().await();
            return response.body().toString();*/
        } catch (Exception e) {
            return e.getLocalizedMessage();
        }
        return null;
    }

    /**
     * js实现解码,不能删
     */
    public String base64Decoder(String str) {
        return Encoder.INSTANCE.base64Decoder(str);
    }

    /**
     * 章节数转数字
     */
    public String toNumChapter(String s) {
        if (s == null) {
            return null;
        }
        Pattern pattern = Pattern.compile("(第)(.+?)(章)");
        Matcher matcher = pattern.matcher(s);
        if (matcher.find()) {
            return matcher.group(1) + StringUtils.INSTANCE.stringToInt(matcher.group(2)) + matcher.group(3);
        } else {
            return s;
        }
    }

}
