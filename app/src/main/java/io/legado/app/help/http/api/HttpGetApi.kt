package io.legado.app.help.http.api

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.QueryMap
import retrofit2.http.Url

/**
 * Created by GKF on 2018/1/21.
 * get web content
 */
@Suppress("unused")
interface HttpGetApi {
    @GET
    suspend fun getAsync(
        @Url url: String,
        @HeaderMap headers: Map<String, String>
    ): Response<String>

    @GET
    suspend fun getMapAsync(
        @Url url: String,
        @QueryMap(encoded = true) queryMap: Map<String, String>,
        @HeaderMap headers: Map<String, String>
    ): Response<String>

    @GET
    fun get(
        @Url url: String,
        @HeaderMap headers: Map<String, String>
    ): Call<String>

    @GET
    fun getByte(
        @Url url: String,
        @HeaderMap headers: Map<String, String>
    ): Call<ByteArray>

    @GET
    fun getMap(
        @Url url: String,
        @QueryMap(encoded = true) queryMap: Map<String, String>,
        @HeaderMap headers: Map<String, String>
    ): Call<String>

    @GET
    fun getMapByte(
        @Url url: String,
        @QueryMap(encoded = true) queryMap: Map<String, String>,
        @HeaderMap headers: Map<String, String>
    ): Call<ByteArray>

    @GET
    suspend fun getByteAsync(
        @Url url: String,
        @HeaderMap headers: Map<String, String>
    ): Response<ByteArray>

    @GET
    suspend fun getMapByteAsync(
        @Url url: String,
        @QueryMap(encoded = true) queryMap: Map<String, String>,
        @HeaderMap headers: Map<String, String>
    ): Response<ByteArray>
}
