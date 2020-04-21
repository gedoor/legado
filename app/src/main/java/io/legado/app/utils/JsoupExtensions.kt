package io.legado.app.utils

import org.jsoup.internal.StringUtil
import org.jsoup.nodes.CDataNode
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.select.NodeTraversor
import org.jsoup.select.NodeVisitor


fun Element.textArray(): Array<String> {
    val accum = StringUtil.borrowBuilder()
    NodeTraversor.traverse(object : NodeVisitor {
        override fun head(node: Node, depth: Int) {
            if (node is TextNode) {
                appendNormalisedText(accum, node)
            } else if (node is Element) {
                if (accum.isNotEmpty() &&
                    (node.isBlock || node.tag().name == "br") &&
                    !lastCharIsWhitespace(accum)
                ) accum.append("\n")
            }
        }

        override fun tail(node: Node, depth: Int) {
            if (node is Element) {
                if (node.isBlock && node.nextSibling() is TextNode && !lastCharIsWhitespace(
                        accum
                    )
                ) accum.append("\n")
            }
        }
    }, this)
    val text = StringUtil.releaseBuilder(accum).trim { it <= ' ' }
    return text.splitNotBlank("\n")
}

private fun appendNormalisedText(accum: StringBuilder, textNode: TextNode) {
    val text = textNode.wholeText
    if (preserveWhitespace(textNode.parentNode()) || textNode is CDataNode)
        accum.append(text)
    else StringUtil.appendNormalisedWhitespace(
        accum,
        text,
        lastCharIsWhitespace(accum)
    )
}

private fun preserveWhitespace(node: Node?): Boolean {
    if (node is Element) {
        var el = node as Element?
        var i = 0
        do {
            if (el!!.tag().preserveWhitespace()) return true
            el = el.parent()
            i++
        } while (i < 6 && el != null)
    }
    return false
}

private fun lastCharIsWhitespace(sb: java.lang.StringBuilder): Boolean {
    return sb.isNotEmpty() && sb[sb.length - 1] == ' '
}

