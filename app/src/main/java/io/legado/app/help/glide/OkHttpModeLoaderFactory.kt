package io.legado.app.help.glide

import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import java.io.InputStream


object OkHttpModeLoaderFactory : ModelLoaderFactory<GlideUrl?, InputStream?> {

    override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<GlideUrl?, InputStream?> {
        return OkHttpModelLoader
    }

    override fun teardown() {
        // Do nothing, this instance doesn't own the client.
    }

}