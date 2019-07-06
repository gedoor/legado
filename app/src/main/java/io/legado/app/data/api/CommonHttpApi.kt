package io.legado.app.data.api

import kotlinx.coroutines.Deferred
import retrofit2.http.*

interface CommonHttpApi {

    @GET
    fun get(@Url url: String, @QueryMap map: Map<String, String>): Deferred<String>

    @FormUrlEncoded
    @POST
    fun post(@Url url: String, @FieldMap map: Map<String, String>): Deferred<String>


    @GET
    fun get(@Url url: String) : Deferred<String>
}