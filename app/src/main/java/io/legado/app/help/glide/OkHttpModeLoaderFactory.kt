package io.legado.app.help.glide

import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import okhttp3.Call
import java.io.InputStream


class OkHttpModeLoaderFactory(private val client: Call.Factory) : ModelLoaderFactory<GlideUrl?, InputStream?> {

    override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<GlideUrl?, InputStream?> {
        return OkHttpModelLoader(client)
    }

    override fun teardown() {
        // Do nothing, this instance doesn't own the client.
    }

}