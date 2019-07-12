package io.legado.app.data.api

import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.QueryMap
import retrofit2.http.Url

/**
 * Created by GKF on 2018/1/21.
 * get web content
 */

interface IHttpGetApi {
    @GET
    operator fun get(
        @Url url: String,
        @HeaderMap headers: Map<String, String>
    ): Deferred<Response<String>>

    @GET
    fun getMap(
        @Url url: String,
        @QueryMap(encoded = true) queryMap: Map<String, String>,
        @HeaderMap headers: Map<String, String>
    ): Deferred<Response<String>>

}
