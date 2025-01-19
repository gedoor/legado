package io.legado.app.help.glide

import com.bumptech.glide.load.Option
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.ModelLoader
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.utils.isAbsUrl
import okhttp3.Call

import java.io.InputStream

class OkHttpModelLoader(private val client: Call.Factory) : ModelLoader<GlideUrl?, InputStream?> {
    companion object {
        val loadOnlyWifiOption = Option.memory("loadOnlyWifi", false)
        val sourceOriginOption = Option.memory<String>("sourceOrigin")
    }

    override fun buildLoadData(
        model: GlideUrl,
        width: Int,
        height: Int,
        options: Options,
    ): ModelLoader.LoadData<InputStream?> {
        val cacheKey = model.toString()
        var modelWithHeader = model
        if (cacheKey.isAbsUrl()) {
            modelWithHeader = AnalyzeUrl(cacheKey).getGlideUrl()
        }
        return ModelLoader.LoadData(
            modelWithHeader,
            OkHttpStreamFetcher(client, modelWithHeader, options)
        )
    }

    override fun handles(model: GlideUrl): Boolean {
        return true
    }

}