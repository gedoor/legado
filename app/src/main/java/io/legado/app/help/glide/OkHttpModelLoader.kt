package io.legado.app.help.glide

import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.ModelLoader
import okhttp3.Call
import java.io.InputStream

class OkHttpModelLoader(private val client: Call.Factory) : ModelLoader<GlideUrl?, InputStream?> {

    override fun buildLoadData(
        model: GlideUrl,
        width: Int,
        height: Int,
        options: Options
    ): ModelLoader.LoadData<InputStream?> {
        return ModelLoader.LoadData(model, OkHttpStreamFetcher(client, model))
    }

    override fun handles(model: GlideUrl): Boolean {
        return true
    }

}