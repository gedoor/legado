package io.legado.app.web

import android.graphics.Bitmap
import com.google.gson.Gson
import fi.iki.elonen.NanoHTTPD
import io.legado.app.api.ReturnData
import io.legado.app.api.controller.BookController
import io.legado.app.api.controller.BookSourceController
import io.legado.app.api.controller.RssSourceController
import io.legado.app.utils.FileUtils
import io.legado.app.utils.externalFiles
import io.legado.app.web.utils.AssetsWeb
import splitties.init.appCtx
import java.io.*


class HttpServer(port: Int) : NanoHTTPD(port) {
    private val assetsWeb = AssetsWeb("web")


    override fun serve(session: IHTTPSession): Response {
        var returnData: ReturnData? = null
        val ct = ContentType(session.headers["content-type"]).tryUTF8()
        session.headers["content-type"] = ct.contentTypeHeader
        var uri = session.uri

        try {
            when (session.method) {
                Method.OPTIONS -> {
                    val response = newFixedLengthResponse("")
                    response.addHeader("Access-Control-Allow-Methods", "POST")
                    response.addHeader("Access-Control-Allow-Headers", "content-type")
                    response.addHeader("Access-Control-Allow-Origin", session.headers["origin"])
                    //response.addHeader("Access-Control-Max-Age", "3600");
                    return response
                }
                Method.POST -> {
                    val files = HashMap<String, String>()
                    session.parseBody(files)
                    val postData = files["postData"]

                    returnData = when (uri) {
                        "/saveBookSource" -> BookSourceController.saveSource(postData)
                        "/saveBookSources" -> BookSourceController.saveSources(postData)
                        "/deleteBookSources" -> BookSourceController.deleteSources(postData)
                        "/saveBook" -> BookController.saveBook(postData)
                        "/saveBookProgress" -> BookController.saveBookProgress(postData)
                        "/addLocalBook" -> BookController.addLocalBook(session.parameters)
                        "/saveReadConfig" -> BookController.saveWebReadConfig(postData)
                        "/saveRssSource" -> RssSourceController.saveSource(postData)
                        "/saveRssSources" -> RssSourceController.saveSources(postData)
                        "/deleteRssSources" -> RssSourceController.deleteSources(postData)
                        else -> null
                    }
                }
                Method.GET -> {
                    val parameters = session.parameters

                    returnData = when (uri) {
                        "/getBookSource" -> BookSourceController.getSource(parameters)
                        "/getBookSources" -> BookSourceController.sources
                        "/getBookshelf" -> BookController.bookshelf
                        "/getChapterList" -> BookController.getChapterList(parameters)
                        "/refreshToc" -> BookController.refreshToc(parameters)
                        "/getBookContent" -> BookController.getBookContent(parameters)
                        "/cover" -> BookController.getCover(parameters)
                        "/image" -> BookController.getImg(parameters)
                        "/getReadConfig" -> BookController.getWebReadConfig()
                        "/getRssSource" -> RssSourceController.getSource(parameters)
                        "/getRssSources" -> RssSourceController.sources
                        else -> null
                    }
                }
                else -> Unit
            }

            if (returnData == null) {
                if (uri.endsWith("/"))
                    uri += "index.html"
                return assetsWeb.getResponse(uri)
            }

            val response = if (returnData.data is Bitmap) {
                val outputStream = ByteArrayOutputStream()
                (returnData.data as Bitmap).compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                val byteArray = outputStream.toByteArray()
                outputStream.close()
                val inputStream = ByteArrayInputStream(byteArray)
                newFixedLengthResponse(
                    Response.Status.OK,
                    "image/png",
                    inputStream,
                    byteArray.size.toLong()
                )
            } else {
                try {
                    newFixedLengthResponse(Gson().toJson(returnData))
                } catch (e: OutOfMemoryError) {
                    val path = FileUtils.getPath(
                        appCtx.externalFiles,
                        "book_cache",
                        "bookSources.json"
                    )
                    val file = FileUtils.createFileIfNotExist(path)
                    BufferedWriter(FileWriter(file)).use {
                        Gson().toJson(returnData, it)
                    }
                    val fis = FileInputStream(file)
                    newFixedLengthResponse(
                        Response.Status.OK,
                        "application/json",
                        fis,
                        fis.available().toLong()
                    )
                }
            }
            response.addHeader("Access-Control-Allow-Methods", "GET, POST")
            response.addHeader("Access-Control-Allow-Origin", session.headers["origin"])
            return response
        } catch (e: Exception) {
            return newFixedLengthResponse(e.message)
        }

    }

}
