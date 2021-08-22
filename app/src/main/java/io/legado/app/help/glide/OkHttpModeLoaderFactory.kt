package io.legado.app.help.glide

import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import io.legado.app.help.http.okHttpClient
import java.io.InputStream


class OkHttpModeLoaderFactory : ModelLoaderFactory<GlideUrl?, InputStream?> {

    override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<GlideUrl?, InputStream?> {
        return OkHttpModelLoader(okHttpClient)
    }

    override fun teardown() {
        // Do nothing, this instance doesn't own the client.
    }

}