package io.legado.app.utils

import android.annotation.SuppressLint
import android.text.TextUtils.isEmpty
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow

@Suppress("unused", "MemberVisibilityCanBePrivate")
object StringUtils {
    private const val HOUR_OF_DAY = 24
    private const val DAY_OF_YESTERDAY = 2
    private const val TIME_UNIT = 60
    private val ChnMap = chnMap

    private val chnMap: HashMap<Char, Int>
        get() {
            val map = HashMap<Char, Int>()
            var cnStr = "零一二三四五六七八九十"
            var c = cnStr.toCharArray()
            for (i in 0..10) {
                map[c[i]] = i
            }
            cnStr = "〇壹贰叁肆伍陆柒捌玖拾"
            c = cnStr.toCharArray()
            for (i in 0..10) {
                map[c[i]] = i
            }
            map['两'] = 2
            map['百'] = 100
            map['佰'] = 100
            map['千'] = 1000
            map['仟'] = 1000
            map['万'] = 10000
            map['亿'] = 100000000
            return map
        }

    //将时间转换成日期
    fun dateConvert(time: Long, pattern: String): String {
        val date = Date(time)

        @SuppressLint("SimpleDateFormat")
        val format = SimpleDateFormat(pattern)
        return format.format(date)
    }

    //将日期转换成昨天、今天、明天
    fun dateConvert(source: String, pattern: String): String {
        @SuppressLint("SimpleDateFormat")
        val format = SimpleDateFormat(pattern)
        val calendar = Calendar.getInstance()
        kotlin.runCatching {
            val date = format.parse(source) ?: return ""
            val curTime = calendar.timeInMillis
            calendar.time = date
            //将MISC 转换成 sec
            val difSec = abs((curTime - date.time) / 1000)
            val difMin = difSec / 60
            val difHour = difMin / 60
            val difDate = difHour / 60
            val oldHour = calendar.get(Calendar.HOUR)
            //如果没有时间
            if (oldHour == 0) {
                //比日期:昨天今天和明天
                return when {
                    difDate == 0L -> {
                        "今天"
                    }
                    difDate < DAY_OF_YESTERDAY -> {
                        "昨天"
                    }
                    else -> {
                        @SuppressLint("SimpleDateFormat")
                        val convertFormat = SimpleDateFormat("yyyy-MM-dd")
                        convertFormat.format(date)
                    }
                }
            }

            return when {
                difSec < TIME_UNIT -> difSec.toString() + "秒前"
                difMin < TIME_UNIT -> difMin.toString() + "分钟前"
                difHour < HOUR_OF_DAY -> difHour.toString() + "小时前"
                difDate < DAY_OF_YESTERDAY -> "昨天"
                else -> {
                    @SuppressLint("SimpleDateFormat")
                    val convertFormat = SimpleDateFormat("yyyy-MM-dd")
                    convertFormat.format(date)
                }
            }
        }.onFailure {
            it.printStackTrace()
        }

        return ""
    }

    fun toSize(length: Long): String {
        if (length <= 0) return "0"
        val units = arrayOf("b", "kb", "M", "G", "T")
        //计算单位的，原理是利用lg,公式是 lg(1024^n) = nlg(1024)，最后 nlg(1024)/lg(1024) = n。
        val digitGroups =
            (log10(length.toDouble()) / log10(1024.0)).toInt()
        //计算原理是，size/单位值。单位值指的是:比如说b = 1024,KB = 1024^2
        return DecimalFormat("#,##0.##")
            .format(length / 1024.0.pow(digitGroups.toDouble())) + " " + units[digitGroups]
    }

    @SuppressLint("DefaultLocale")
    fun toFirstCapital(str: String): String {
        return str.substring(0, 1).uppercase(Locale.getDefault()) + str.substring(1)
    }

    /**
     * 将文本中的半角字符，转换成全角字符
     */
    fun halfToFull(input: String): String {
        val c = input.toCharArray()
        for (i in c.indices) {
            if (c[i].code == 32)
            //半角空格
            {
                c[i] = 12288.toChar()
                continue
            }
            //根据实际情况，过滤不需要转换的符号
            //if (c[i] == 46) //半角点号，不转换
            // continue;

            if (c[i].code in 33..126)
            //其他符号都转换为全角
                c[i] = (c[i].code + 65248).toChar()
        }
        return String(c)
    }

    //功能：字符串全角转换为半角
    fun fullToHalf(input: String): String {
        val c = input.toCharArray()
        for (i in c.indices) {
            if (c[i].code == 12288)
            //全角空格
            {
                c[i] = 32.toChar()
                continue
            }

            if (c[i].code in 65281..65374)
                c[i] = (c[i].code - 65248).toChar()
        }
        return String(c)
    }

    fun chineseNumToInt(chNum: String): Int {
        var result = 0
        var tmp = 0
        var billion = 0
        val cn = chNum.toCharArray()

        // "一零二五" 形式
        if (cn.size > 1 && chNum.matches("^[〇零一二三四五六七八九壹贰叁肆伍陆柒捌玖]$".toRegex())) {
            for (i in cn.indices) {
                cn[i] = (48 + ChnMap[cn[i]]!!).toChar()
            }
            return Integer.parseInt(String(cn))
        }

        // "一千零二十五", "一千二" 形式
        return kotlin.runCatching {
            for (i in cn.indices) {
                val tmpNum = ChnMap[cn[i]]!!
                when {
                    tmpNum == 100000000 -> {
                        result += tmp
                        result *= tmpNum
                        billion = billion * 100000000 + result
                        result = 0
                        tmp = 0
                    }
                    tmpNum == 10000 -> {
                        result += tmp
                        result *= tmpNum
                        tmp = 0
                    }
                    tmpNum >= 10 -> {
                        if (tmp == 0)
                            tmp = 1
                        result += tmpNum * tmp
                        tmp = 0
                    }
                    else -> {
                        tmp = if (i >= 2 && i == cn.size - 1 && ChnMap[cn[i - 1]]!! > 10)
                            tmpNum * ChnMap[cn[i - 1]]!! / 10
                        else
                            tmp * 10 + tmpNum
                    }
                }
            }
            result += tmp + billion
            result
        }.getOrDefault(-1)
    }

    fun stringToInt(str: String?): Int {
        if (str != null) {
            val num = fullToHalf(str).replace("\\s+".toRegex(), "")
            return kotlin.runCatching {
                Integer.parseInt(num)
            }.getOrElse {
                chineseNumToInt(num)
            }
        }
        return -1
    }

    fun isContainNumber(company: String): Boolean {
        val p = Pattern.compile("[0-9]+")
        val m = p.matcher(company)
        return m.find()
    }

    fun isNumeric(str: String): Boolean {
        val pattern = Pattern.compile("[0-9]+")
        val isNum = pattern.matcher(str)
        return isNum.matches()
    }

    fun wordCountFormat(wc: String?): String {
        if (wc == null) return ""
        var wordsS = ""
        if (isNumeric(wc)) {
            val words: Int = wc.toInt()
            if (words > 0) {
                wordsS = words.toString() + "字"
                if (words > 10000) {
                    val df = DecimalFormat("#.#")
                    wordsS = df.format(words * 1.0f / 10000f.toDouble()) + "万字"
                }
            }
        } else {
            wordsS = wc
        }
        return wordsS
    }

    // 移除字符串首尾空字符的高效方法(利用ASCII值判断,包括全角空格)
    fun trim(s: String): String {
        if (isEmpty(s)) return ""
        var start = 0
        val len = s.length
        var end = len - 1
        while (start < end && (s[start].code <= 0x20 || s[start] == '　')) {
            ++start
        }
        while (start < end && (s[end].code <= 0x20 || s[end] == '　')) {
            --end
        }
        if (end < len) ++end
        return if (start > 0 || end < len) s.substring(start, end) else s
    }

    fun repeat(str: String, n: Int): String {
        val stringBuilder = StringBuilder()
        for (i in 0 until n) {
            stringBuilder.append(str)
        }
        return stringBuilder.toString()
    }

    fun removeUTFCharacters(data: String?): String? {
        if (data == null) return null
        val p = Pattern.compile("\\\\u(\\p{XDigit}{4})")
        val m = p.matcher(data)
        val buf = StringBuffer(data.length)
        while (m.find()) {
            val ch = Integer.parseInt(m.group(1)!!, 16).toChar().toString()
            m.appendReplacement(buf, Matcher.quoteReplacement(ch))
        }
        m.appendTail(buf)
        return buf.toString()
    }

    fun byteToHexString(bytes: ByteArray?): String {
        if (bytes == null) return ""
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val hex = 0xff and b.toInt()
            if (hex < 16) {
                sb.append('0')
            }
            sb.append(Integer.toHexString(hex))
        }
        return sb.toString()
    }

    fun hexStringToByte(hexString: String): ByteArray {
        val hexStr = hexString.replace(" ", "")
        val len = hexStr.length
        val bytes = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            // 两位一组，表示一个字节,把这样表示的16进制字符串，还原成一个字节
            bytes[i / 2] = ((Character.digit(hexString[i], 16) shl 4) +
                    Character.digit(hexString[i + 1], 16)).toByte()
            i += 2
        }
        return bytes
    }
}
