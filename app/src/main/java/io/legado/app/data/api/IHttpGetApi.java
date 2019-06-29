package io.legado.app.data.api;

import java.util.Map;

import kotlinx.coroutines.Deferred;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.HeaderMap;
import retrofit2.http.QueryMap;
import retrofit2.http.Url;

/**
 * Created by GKF on 2018/1/21.
 * get web content
 */

public interface IHttpGetApi {
    @GET
    Deferred<Response<String>> get(@Url String url,
                                   @HeaderMap Map<String, String> headers);

    @GET
    Deferred<Response<String>> getMap(@Url String url,
                                      @QueryMap(encoded = true) Map<String, String> queryMap,
                                      @HeaderMap Map<String, String> headers);

}
