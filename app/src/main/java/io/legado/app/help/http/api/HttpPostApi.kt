package io.legado.app.help.http.api

import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

/**
 * Created by GKF on 2018/1/29.
 * post
 */
@Suppress("unused")
interface HttpPostApi {

    @FormUrlEncoded
    @POST
    suspend fun postMapAsync(
        @Url url: String,
        @FieldMap(encoded = true) fieldMap: Map<String, String>,
        @HeaderMap headers: Map<String, String>
    ): Response<String>

    @POST
    suspend fun postBodyAsync(
        @Url url: String,
        @Body body: RequestBody,
        @HeaderMap headers: Map<String, String>
    ): Response<String>

    @FormUrlEncoded
    @POST
    fun postMap(
        @Url url: String,
        @FieldMap(encoded = true) fieldMap: Map<String, String>,
        @HeaderMap headers: Map<String, String>
    ): Call<String>

    @POST
    fun postBody(
        @Url url: String,
        @Body body: RequestBody,
        @HeaderMap headers: Map<String, String>
    ): Call<String>

    @FormUrlEncoded
    @POST
    suspend fun postMapByteAsync(
        @Url url: String,
        @FieldMap(encoded = true) fieldMap: Map<String, String>,
        @HeaderMap headers: Map<String, String>
    ): Response<ByteArray>

    @POST
    suspend fun postBodyByteAsync(
        @Url url: String,
        @Body body: RequestBody,
        @HeaderMap headers: Map<String, String>
    ): Response<ByteArray>

}
