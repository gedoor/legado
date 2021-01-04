package io.legado.app.web

import com.google.gson.Gson
import fi.iki.elonen.NanoHTTPD
import io.legado.app.api.ReturnData
import io.legado.app.api.controller.BookshelfController
import io.legado.app.api.controller.SourceController
import io.legado.app.web.utils.AssetsWeb
import java.util.*

class HttpServer(port: Int) : NanoHTTPD(port) {
    private val assetsWeb = AssetsWeb("web")


    override fun serve(session: IHTTPSession): Response {
        var returnData: ReturnData? = null
        var uri = session.uri

        try {
            when (session.method.name) {
                "OPTIONS" -> {
                    val response = newFixedLengthResponse("")
                    response.addHeader("Access-Control-Allow-Methods", "POST")
                    response.addHeader("Access-Control-Allow-Headers", "content-type")
                    response.addHeader("Access-Control-Allow-Origin", session.headers["origin"])
                    //response.addHeader("Access-Control-Max-Age", "3600");
                    return response
                }

                "POST" -> {
                    val files = HashMap<String, String>()
                    session.parseBody(files)
                    val postData = files["postData"]

                    when (uri) {
                        "/saveSource" -> returnData = SourceController.saveSource(postData)
                        "/saveSources" -> returnData = SourceController.saveSources(postData)
                        "/saveBook" -> returnData = BookshelfController.saveBook(postData)
                        "/deleteSources" -> returnData = SourceController.deleteSources(postData)
                    }
                }

                "GET" -> {
                    val parameters = session.parameters

                    when (uri) {
                        "/getSource" -> returnData = SourceController.getSource(parameters)
                        "/getSources" -> returnData = SourceController.sources
                        "/getBookshelf" -> returnData = BookshelfController.bookshelf
                        "/getChapterList" ->
                            returnData = BookshelfController.getChapterList(parameters)
                        "/getBookContent" ->
                            returnData = BookshelfController.getBookContent(parameters)
                    }
                }
            }

            if (returnData == null) {
                if (uri.endsWith("/"))
                    uri += "index.html"
                return assetsWeb.getResponse(uri)
            }

            val response = newFixedLengthResponse(Gson().toJson(returnData))
            response.addHeader("Access-Control-Allow-Methods", "GET, POST")
            response.addHeader("Access-Control-Allow-Origin", session.headers["origin"])
            return response
        } catch (e: Exception) {
            return newFixedLengthResponse(e.message)
        }

    }

}
