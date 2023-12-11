package io.legado.app.utils

import com.github.liuyueyi.quick.transfer.Trie
import com.github.liuyueyi.quick.transfer.TrieNode
import java.util.HashMap

fun <T> Trie<T>.getRoot(): TrieNode<T> {
    val rootField = javaClass.getDeclaredField("root")
        .apply { isAccessible = true }
    @Suppress("UNCHECKED_CAST")
    return rootField.get(this) as TrieNode<T>
}

fun <T> TrieNode<T>.getChildren(): HashMap<Char, TrieNode<T>> {
    val childrenField = javaClass.getDeclaredField("children")
        .apply { isAccessible = true }
    @Suppress("UNCHECKED_CAST")
    return childrenField.get(this) as HashMap<Char, TrieNode<T>>
}

fun <T> Trie<T>.remove(value: String) {
    kotlin.runCatching {
        var node = getRoot()
        val nodes = arrayListOf<TrieNode<T>>()
        val chars = value.toCharArray()
        for (c in chars) {
            nodes.add(node)
            node = node.getChildren()[c] ?: break
            if (!node.isLeaf) {
                continue
            }
            for ((ch, n) in chars.reversed().zip(nodes.reversed())) {
                val children = n.getChildren()
                children.remove(ch)
                if (children.isNotEmpty()) {
                    break
                }
            }
        }
    }
}
