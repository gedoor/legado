package io.legado.app.help.glide

import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.signature.ObjectKey
import java.io.File

class FilePathLoader : ModelLoader<String, File> {
    override fun buildLoadData(
        model: String,
        width: Int,
        height: Int,
        options: com.bumptech.glide.load.Options
    ): ModelLoader.LoadData<File>? {
        return ModelLoader.LoadData(ObjectKey(model), FilePathFetcher(model))
    }

    override fun handles(model: String): Boolean {
        return true
    }

    class FilePathFetcher(private val filePath: String) : DataFetcher<File> {
        override fun loadData(
            priority: Priority,
            callback: DataFetcher.DataCallback<in File>
        ) {
            val file = File(filePath)
            if (file.exists() && file.isFile) {
                callback.onDataReady(file)
            } else {
                callback.onLoadFailed(Exception("File not found: $filePath"))
            }
        }

        override fun cleanup() {}

        override fun cancel() {}

        override fun getDataClass(): Class<File> = File::class.java

        override fun getDataSource(): DataSource = DataSource.LOCAL
    }

    class Factory : ModelLoaderFactory<String, File> {
        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<String, File> {
            return FilePathLoader()
        }

        override fun teardown() {}
    }
}


