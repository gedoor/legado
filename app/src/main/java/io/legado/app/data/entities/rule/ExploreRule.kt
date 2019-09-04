package io.legado.app.data.entities.rule

import io.legado.app.constant.AppConst
import io.legado.app.help.JsExtensions
import javax.script.SimpleBindings

data class ExploreRule(
    var exploreUrl: String? = null,
    override var bookList: String? = null,
    override var name: String? = null,
    override var author: String? = null,
    override var intro: String? = null,
    override var kind: String? = null,
    override var lastChapter: String? = null,
    override var updateTime: String? = null,
    override var bookUrl: String? = null,
    override var coverUrl: String? = null,
    override var wordCount: String? = null
) : BookListRule {

    fun getExploreKinds(baseUrl: String): ArrayList<ExploreKind>? {
        exploreUrl?.let {
            var a = it
            if (a.isNotBlank()) {
                try {
                    if (it.startsWith("<js>", false)) {
                        val bindings = SimpleBindings()
                        bindings["baseUrl"] = baseUrl
                        bindings["java"] = JsExtensions()
                        a = AppConst.SCRIPT_ENGINE.eval(
                            a.substring(4, a.lastIndexOf("<")),
                            bindings
                        ).toString()
                    }
                    val exploreKinds = arrayListOf<ExploreKind>()
                    val b = a.split("(&&|\n)+".toRegex())
                    b.map { c ->
                        val d = c.split("::")
                        if (d.size > 1)
                            exploreKinds.add(ExploreKind(d[0], d[1]))
                    }
                    return exploreKinds
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return null
    }

    data class ExploreKind(
        var title: String,
        var url: String
    )
}