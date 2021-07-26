package io.legado.app.web

import android.graphics.Bitmap
import com.google.gson.Gson
import fi.iki.elonen.NanoHTTPD
import io.legado.app.api.ReturnData
import io.legado.app.api.controller.BookController
import io.legado.app.api.controller.SourceController
import io.legado.app.web.utils.AssetsWeb
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*


class HttpServer(port: Int) : NanoHTTPD(port) {
    private val assetsWeb = AssetsWeb("web")


    override fun serve(session: IHTTPSession): Response {
        var returnData: ReturnData? = null
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
                        "/saveSource" -> SourceController.saveSource(postData)
                        "/saveSources" -> SourceController.saveSources(postData)
                        "/saveBook" -> BookController.saveBook(postData)
                        "/deleteSources" -> SourceController.deleteSources(postData)
                        "/addLocalBook" -> BookController.addLocalBook(session, postData)
                        else -> null
                    }
                }
                Method.GET -> {
                    val parameters = session.parameters

                    returnData = when (uri) {
                        "/getSource" -> SourceController.getSource(parameters)
                        "/getSources" -> SourceController.sources
                        "/getBookshelf" -> BookController.bookshelf
                        "/getChapterList" -> BookController.getChapterList(parameters)
                        "/refreshToc" -> BookController.refreshToc(parameters)
                        "/getBookContent" -> BookController.getBookContent(parameters)
                        "/cover" -> BookController.getCover(parameters)
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
                val inputStream = ByteArrayInputStream(byteArray)
                newFixedLengthResponse(
                    Response.Status.OK,
                    "image/png",
                    inputStream,
                    byteArray.size.toLong()
                )
            } else {
                newFixedLengthResponse(Gson().toJson(returnData))
            }
            response.addHeader("Access-Control-Allow-Methods", "GET, POST")
            response.addHeader("Access-Control-Allow-Origin", session.headers["origin"])
            return response
        } catch (e: Exception) {
            return newFixedLengthResponse(e.message)
        }

    }

}
