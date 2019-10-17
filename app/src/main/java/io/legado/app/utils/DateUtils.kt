package io.legado.app.utils

import android.annotation.SuppressLint
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


object DateUtils {

    /**
     * @Description: 任意时间字符串转换成时间，无需指定解析模板
     * */
    @SuppressLint("SimpleDateFormat")
    @Throws(ParseException::class)
    fun parseStringToDate(date: String): Date {
        val result: Date
        var parse = date.replaceFirst("[0-9]{4}([^0-9]?)".toRegex(), "yyyy$1")
        parse = parse.replaceFirst("^[0-9]{2}([^0-9]?)".toRegex(), "yy$1")
        parse = parse.replaceFirst("([^0-9]?)[0-9]{1,2}([^0-9]?)".toRegex(), "$1MM$2")
        parse = parse.replaceFirst("([^0-9]?)[0-9]{1,2}( ?)".toRegex(), "$1dd$2")
        parse = parse.replaceFirst("( )[0-9]{1,2}([^0-9]?)".toRegex(), "$1HH$2")
        parse = parse.replaceFirst("([^0-9]?)[0-9]{1,2}([^0-9]?)".toRegex(), "$1mm$2")
        parse = parse.replaceFirst("([^0-9]?)[0-9]{1,2}([^0-9]?)".toRegex(), "$1ss$2")
        val format = SimpleDateFormat(parse)
        result = format.parse(date)
        return result
    }

}