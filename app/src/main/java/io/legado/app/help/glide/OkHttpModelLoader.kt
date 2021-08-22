package io.legado.app.help.glide

import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.ModelLoader
import java.io.InputStream

object OkHttpModelLoader : ModelLoader<GlideUrl?, InputStream?> {

    override fun buildLoadData(
        model: GlideUrl,
        width: Int,
        height: Int,
        options: Options
    ): ModelLoader.LoadData<InputStream?> {
        return ModelLoader.LoadData(model, OkHttpStreamFetcher(model))
    }

    override fun handles(model: GlideUrl): Boolean {
        return true
    }

}