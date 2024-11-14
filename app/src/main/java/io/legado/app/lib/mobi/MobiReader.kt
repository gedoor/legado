package io.legado.app.lib.mobi

import android.os.ParcelFileDescriptor
import io.legado.app.lib.mobi.entities.ExthRecordType
import io.legado.app.lib.mobi.entities.KF8Header
import io.legado.app.lib.mobi.entities.MobiEntryHeaders
import io.legado.app.lib.mobi.entities.MobiHeader
import io.legado.app.lib.mobi.entities.PalmDocHeader
import io.legado.app.lib.mobi.utils.readString
import io.legado.app.lib.mobi.utils.readUInt16
import io.legado.app.lib.mobi.utils.readUInt32
import io.legado.app.lib.mobi.utils.readUInt8
import java.nio.ByteBuffer
import java.nio.charset.Charset

class MobiReader {

    fun readMobi(pfd: ParcelFileDescriptor): MobiBook {
        val pdbFile = PDBFile(pfd)
        val record0 = pdbFile.getRecordData(0)

        var mobiEntryHeaders = readMobiEntryHeaders(record0)
        val mobi = mobiEntryHeaders.mobi
        val exth = mobiEntryHeaders.exth
        val resourceStart = mobi.resourceStart

        var isKF8 = mobi.version >= 8

        var kf8BoundaryOffset = 0

        if (!isKF8) {
            val boundary = exth["boundary"] as? Int
            if (boundary != null && boundary != -1) {
                try {
                    val buffer = pdbFile.getRecordData(boundary)
                    mobiEntryHeaders = readMobiEntryHeaders(buffer)
                    kf8BoundaryOffset = boundary
                    isKF8 = true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        return if (isKF8) {
            KF8Book(pdbFile, mobiEntryHeaders, kf8BoundaryOffset, resourceStart)
        } else {
            KF6Book(pdbFile, mobiEntryHeaders, kf8BoundaryOffset, resourceStart)
        }
    }

    private fun readMobiEntryHeaders(buffer: ByteBuffer): MobiEntryHeaders {
        val palmDocHeader = readPalmDocHeader(buffer)
        val mobiHeader = readMobiHeader(buffer)
        val exth = if (mobiHeader.exthFlag and 0b100_0000 != 0) {
            buffer.position(mobiHeader.length + 16)
            readExth(buffer.slice())
        } else {
            emptyMap()
        }
        val kF8Header = if (mobiHeader.version >= 8) {
            readKF8Header(buffer)
        } else {
            null
        }
        return MobiEntryHeaders(palmDocHeader, mobiHeader, exth, kF8Header)
    }

    private fun readExth(buffer: ByteBuffer): Map<String, Any> {
        val magic = buffer.readString(0, 4)
        check(magic == "EXTH") { "Invalid EXTH header" }
        val count = buffer.readUInt32(8)
        var offset = 12
        val map = HashMap<String, Any>()
        for (i in 0..<count) {
            val type = buffer.readUInt32(offset)
            val length = buffer.readUInt32(offset + 4)
            if (type in exthRecordTypeMap) {
                val exthRecordType = exthRecordTypeMap[type]!!
                val name = exthRecordType.name
                val data: Any = if (exthRecordType.type == "uint") {
                    buffer.readUInt32(offset + 8)
                } else {
                    buffer.readString(offset + 8, length - 8)
                }
                if (exthRecordType.many) {
                    if (!map.contains(name)) {
                        map[name] = arrayListOf<String>()
                    }
                    @Suppress("UNCHECKED_CAST")
                    val array = map[name] as ArrayList<String>
                    array.add(data as String)
                } else {
                    map[name] = data
                }
            }
            offset += length
        }
        return map
    }

    private fun readPalmDocHeader(content: ByteBuffer): PalmDocHeader {

        val compression = content.readUInt16(0)
        val numTextRecords = content.readUInt16(8)
        val recordSize = content.readUInt16(10)
        val encryption = content.readUInt16(12)

        return PalmDocHeader(compression, numTextRecords, recordSize, encryption)
    }

    private fun readMobiHeader(content: ByteBuffer): MobiHeader {

        val identifier = content.readString(16, 4)

        check(identifier == "MOBI") { "Missing MOBI header" }

        val length = content.readUInt32(20)
        val type = content.readUInt32(24)
        val encoding = content.readUInt32(28)
        val uid = content.readUInt32(32)
        val version = content.readUInt32(36)
        val titleOffset = content.readUInt32(84)
        val titleLength = content.readUInt32(88)
        val localeRegion = content.readUInt8(94)
        val localeLanguage = content.readUInt8(95)
        val resourceStar = content.readUInt32(108)
        val huffcdic = content.readUInt32(112)
        val numHuffcdic = content.readUInt32(116)
        val exthFlag = content.readUInt32(128)
        val trailingFlags = content.readUInt32(240)
        val indx = content.readUInt32(244)
        val charset: Charset = when (encoding) {
            65001 -> Charsets.UTF_8
            1252 -> Charset.forName("windows-1252")
            else -> error("unknown charset $encoding")
        }
        val title = content.readString(titleOffset, titleLength, charset)

        val lang = mobiLangMap[localeLanguage]
        val language = lang?.getOrNull(localeRegion shr 2) ?: lang?.first() ?: ""

        return MobiHeader(
            identifier, length, type, encoding, uid, version, titleOffset, titleLength,
            localeRegion, localeLanguage, resourceStar, huffcdic, numHuffcdic, exthFlag,
            trailingFlags, indx, title, language
        )
    }

    private fun readKF8Header(content: ByteBuffer): KF8Header {
        val fdst = content.readUInt32(192)
        val numFdst = content.readUInt32(196)
        val frag = content.readUInt32(248)
        val skel = content.readUInt32(252)
        val guide = content.readUInt32(260)

        return KF8Header(fdst, numFdst, frag, skel, guide)
    }

    companion object {
        val exthRecordTypeMap = mapOf(
            100 to ExthRecordType("creator", "string", true),
            101 to ExthRecordType("publisher"),
            103 to ExthRecordType("description"),
            104 to ExthRecordType("isbn"),
            105 to ExthRecordType("subject", "string", true),
            106 to ExthRecordType("date"),
            108 to ExthRecordType("contributor", "string", true),
            109 to ExthRecordType("rights"),
            110 to ExthRecordType("subjectCode", "string", true),
            112 to ExthRecordType("source", "string", true),
            113 to ExthRecordType("asin"),
            121 to ExthRecordType("boundary", "uint"),
            122 to ExthRecordType("fixedLayout"),
            125 to ExthRecordType("numResources", "uint"),
            126 to ExthRecordType("originalResolution"),
            127 to ExthRecordType("zeroGutter"),
            128 to ExthRecordType("zeroMargin"),
            129 to ExthRecordType("coverURI"),
            132 to ExthRecordType("regionMagnification"),
            201 to ExthRecordType("coverOffset", "uint"),
            202 to ExthRecordType("thumbnailOffset", "uint"),
            204 to ExthRecordType("creatorSoftware", "uint"),
            503 to ExthRecordType("title"),
            524 to ExthRecordType("language", "string", true),
            527 to ExthRecordType("pageProgressionDirection"),
        )

        val mobiLangMap = mapOf(
            1 to listOf(
                "ar", "ar-SA", "ar-IQ", "ar-EG", "ar-LY", "ar-DZ", "ar-MA", "ar-TN", "ar-OM",
                "ar-YE", "ar-SY", "ar-JO", "ar-LB", "ar-KW", "ar-AE", "ar-BH", "ar-QA"
            ),
            2 to listOf("bg"), 3 to listOf("ca"),
            4 to listOf("zh", "zh-TW", "zh-CN", "zh-HK", "zh-SG"),
            5 to listOf("cs"), 6 to listOf("da"),
            7 to listOf("de", "de-DE", "de-CH", "de-AT", "de-LU", "de-LI"), 8 to listOf("el"),
            9 to listOf(
                "en", "en-US", "en-GB", "en-AU", "en-CA", "en-NZ", "en-IE", "en-ZA",
                "en-JM", null, "en-BZ", "en-TT", "en-ZW", "en-PH"
            ),
            10 to listOf(
                "es", "es-ES", "es-MX", null, "es-GT", "es-CR", "es-PA", "es-DO",
                "es-VE", "es-CO", "es-PE", "es-AR", "es-EC", "es-CL", "es-UY", "es-PY",
                "es-BO", "es-SV", "es-HN", "es-NI", "es-PR"
            ),
            11 to listOf("fi"),
            12 to listOf("fr", "fr-FR", "fr-BE", "fr-CA", "fr-CH", "fr-LU", "fr-MC"),
            13 to listOf("he"), 14 to listOf("hu"), 15 to listOf("is"),
            16 to listOf("it", "it-IT", "it-CH"), 17 to listOf("ja"), 18 to listOf("ko"),
            19 to listOf("nl", "nl-NL", "nl-BE"), 20 to listOf("no", "nb", "nn"),
            21 to listOf("pl"), 22 to listOf("pt", "pt-BR", "pt-PT"), 23 to listOf("rm"),
            24 to listOf("ro"), 25 to listOf("ru"), 26 to listOf("hr", null, "sr"),
            27 to listOf("sk"), 28 to listOf("sq"), 29 to listOf("sv", "sv-SE", "sv-FI"),
            30 to listOf("th"), 31 to listOf("tr"), 32 to listOf("ur"), 33 to listOf("id"),
            34 to listOf("uk"), 35 to listOf("be"), 36 to listOf("sl"), 37 to listOf("et"),
            38 to listOf("lv"), 39 to listOf("lt"), 41 to listOf("fa"), 42 to listOf("vi"),
            43 to listOf("hy"), 44 to listOf("az"), 45 to listOf("eu"), 46 to listOf("hsb"),
            47 to listOf("mk"), 48 to listOf("st"), 49 to listOf("ts"), 50 to listOf("tn"),
            52 to listOf("xh"), 53 to listOf("zu"), 54 to listOf("af"), 55 to listOf("ka"),
            56 to listOf("fo"), 57 to listOf("hi"), 58 to listOf("mt"), 59 to listOf("se"),
            62 to listOf("ms"), 63 to listOf("kk"), 65 to listOf("sw"),
            67 to listOf("uz", null, "uz-UZ"), 68 to listOf("tt"), 69 to listOf("bn"),
            70 to listOf("pa"), 71 to listOf("gu"), 72 to listOf("or"), 73 to listOf("ta"),
            74 to listOf("te"), 75 to listOf("kn"), 76 to listOf("ml"), 77 to listOf("as"),
            78 to listOf("mr"), 79 to listOf("sa"), 82 to listOf("cy", "cy-GB"),
            83 to listOf("gl", "gl-ES"), 87 to listOf("kok"), 97 to listOf("ne"),
            98 to listOf("fy")
        )
    }

}
