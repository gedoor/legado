package io.legado.app.lib.mobi

import io.legado.app.lib.mobi.entities.KF6Section
import io.legado.app.lib.mobi.entities.MobiEntryHeaders
import io.legado.app.lib.mobi.entities.NCX
import io.legado.app.lib.mobi.entities.TOC
import java.nio.CharBuffer


/**
 * Kindle Format 6 Book
 */
class KF6Book(
    pdbFile: PDBFile,
    headers: MobiEntryHeaders,
    kf8BoundaryOffset: Int,
    resourceStart: Int
) : MobiBook(pdbFile, headers, kf8BoundaryOffset, resourceStart) {

    lateinit var sections: List<KF6Section>
    lateinit var sectionIdMap: LinkedHashMap<Int, ArrayList<TOC>>

    init {
        processSections()
        processNCX()
        processSectionsMap()
    }

    fun getResourceByHref(href: String): ByteArray? {
        val recindex = href.substringAfter("recindex:").toIntOrNull() ?: return null
        return getResource(recindex - 1).array()
    }

    fun getSectionText(section: KF6Section): String {
        val inputStream = getTextRecordInputStream()
        val byteArray = ByteArray(section.length)
        inputStream.skip(section.start.toLong())
        inputStream.read(byteArray)
        return String(byteArray, charset)
    }

    fun getSectionByHref(href: String): KF6Section? {
        val index = getIndexByHref(href)
        return sections.getOrNull(index)
    }

    private fun processSectionsMap() {
        sectionIdMap = linkedMapOf()
        if (toc == null) {
            return
        }
        fun fmap(item: TOC) {
            val index = getIndexByHref(item.href)
            if (index == -1) return
            val array = sectionIdMap.getOrPut(index) { arrayListOf() }
            array.add(item)
            item.subitems?.forEach(::fmap)
        }
        toc!!.forEach(::fmap)
    }

    private fun getIndexByHref(href: String): Int {
        val filepos = href.substringAfter("filepos:").toIntOrNull() ?: return -1
        return sections.indexOfFirst { it.end > filepos }
    }

    private fun processNCX() {
        val ncx = getNCX() ?: return
        fun fmap(item: NCX): TOC {
            val filepos = item.offset!!
            val href = "filepos:${filepos.toString().padStart(10, '0')}"
            return TOC(item.label, href, item.children?.map(::fmap))
        }
        toc = ncx.map(::fmap)
    }

    private fun processSections() {
        val sections = arrayListOf<KF6Section>()
        val pattern = mbpPagebreakRegex.toPattern()
        val inputStream = getTextRecordInputStream()
        val available = inputStream.available()
        val reader = inputStream.reader(Charsets.ISO_8859_1)
        var buffer = CharBuffer.allocate(4096)
        reader.read(buffer)
        buffer.flip()
        val matcher = pattern.matcher(buffer)
        var droppedOffset = 0
        var nextStart = 0
        var position = 0
        while (true) {
            if (!matcher.find()) {
                buffer.position(position)
                if (buffer.limit() == buffer.capacity()) {
                    if (position > 0) {
                        buffer.compact()
                    } else {
                        val newBuf = CharBuffer.allocate(buffer.capacity() * 2)
                        newBuf.put(buffer)
                        buffer = newBuf
                    }
                    droppedOffset += position
                    position = 0
                }
                if (reader.read(buffer) == -1) {
                    break
                }
                buffer.flip()
                matcher.reset(buffer)
            } else {
                val last = sections.lastOrNull()
                val index = sections.size
                val start = nextStart
                val end = matcher.start() + droppedOffset
                nextStart = matcher.end() + droppedOffset
                position = matcher.end()
                val length = end - start
                val href = "filepos:${start.toString().padStart(10, '0')}"
                val section = KF6Section(index, start, end, length, href)
                last?.next = section
                sections.add(section)
            }
        }
        if (nextStart > 0) {
            val last = sections.lastOrNull()
            val index = sections.size
            val start = nextStart
            val length = available - start
            val href = "filepos:${start.toString().padStart(10, '0')}"
            val section = KF6Section(index, start, available, length, href)
            last?.next = section
            sections.add(section)
        }
        this.sections = sections
    }

    companion object {
        val mbpPagebreakRegex = "(?i)<\\s*(?:mbp:)?pagebreak[^>]*>".toRegex()
    }

}
