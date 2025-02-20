package io.legado.app.help.glide

import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.signature.ObjectKey
import io.legado.app.exception.NoStackTraceException
import io.legado.app.model.ReadManga
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.utils.ImageUtils
import java.io.InputStream

class LegadoDataUrlLoader : ModelLoader<String, InputStream> {

    override fun buildLoadData(
        model: String,
        width: Int,
        height: Int,
        options: Options
    ): ModelLoader.LoadData<InputStream>? {
        if (options.get(OkHttpModelLoader.mangaOption) == false) {
            return null
        }
        return ModelLoader.LoadData(ObjectKey(model), LegadoDataUrlFetcher(model))
    }

    override fun handles(model: String): Boolean {
        return model.startsWith("data:")
    }

    class LegadoDataUrlFetcher(private val model: String) : DataFetcher<InputStream> {
        override fun loadData(
            priority: Priority,
            callback: DataFetcher.DataCallback<in InputStream>
        ) {
            try {
                val bytes = AnalyzeUrl(model, source = ReadManga.bookSource).getByteArray()
                val decoded = ImageUtils.decode(
                    model, bytes, isCover = false, ReadManga.bookSource, ReadManga.book
                )?.inputStream()
                if (decoded == null) {
                    throw NoStackTraceException("漫画图片解密失败")
                }
                callback.onDataReady(decoded)
            } catch (e: Exception) {
                callback.onLoadFailed(e)
            }
        }

        override fun cleanup() {
            // do nothing
        }

        override fun cancel() {
            // do nothing
        }

        override fun getDataClass(): Class<InputStream> {
            return InputStream::class.java
        }

        override fun getDataSource(): DataSource {
            return DataSource.LOCAL
        }

    }

    class Factory : ModelLoaderFactory<String, InputStream> {
        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<String, InputStream> {
            return LegadoDataUrlLoader()
        }

        override fun teardown() {
            // do nothing
        }
    }

}
