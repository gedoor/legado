package io.legado.app.utils

import org.jsoup.internal.StringUtil
import org.jsoup.nodes.CDataNode
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.select.Elements
import org.jsoup.select.NodeTraversor
import org.jsoup.select.NodeVisitor


fun Element.textArray(): Array<String> {
    val sb = StringUtil.borrowBuilder()
    NodeTraversor.traverse(object : NodeVisitor {
        override fun head(node: Node, depth: Int) {
            if (node is TextNode) {
                appendNormalisedText(sb, node)
            } else if (node is Element) {
                if (sb.isNotEmpty() &&
                    (node.isBlock || node.tag().name == "br") &&
                    !lastCharIsWhitespace(sb)
                ) sb.append("\n")
            }
        }

        override fun tail(node: Node, depth: Int) {
            if (node is Element) {
                if (node.isBlock && node.nextSibling() is TextNode
                    && !lastCharIsWhitespace(sb)
                ) {
                    sb.append("\n")
                }
            }
        }
    }, this)
    val text = StringUtil.releaseBuilder(sb).trim { it <= ' ' }
    return text.splitNotBlank("\n")
}

fun Element.findNS(tag: String, namespace: HashSet<String>): Elements {
    return select("*|$tag").filter { el ->
        namespace.contains(el.tagName().substringBefore(":"))
    }.toElements()
}

fun Element.findNSPrefix(namespaceURI: String): HashSet<String> {
    return select("[^xmlns:]").map { element ->
        element.attributes().filter { it.value == namespaceURI }.map { it.key.substring(6) }
    }.flatten().toHashSet()
}

fun List<Element>.toElements() = Elements(this)

private fun appendNormalisedText(sb: StringBuilder, textNode: TextNode) {
    val text = textNode.wholeText
    if (preserveWhitespace(textNode.parentNode()) || textNode is CDataNode)
        sb.append(text)
    else StringUtil.appendNormalisedWhitespace(sb, text, lastCharIsWhitespace(sb))
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

