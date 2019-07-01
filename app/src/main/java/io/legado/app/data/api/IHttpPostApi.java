package io.legado.app.data.api;

import java.util.Map;

import kotlinx.coroutines.Deferred;
import okhttp3.RequestBody;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.HeaderMap;
import retrofit2.http.POST;
import retrofit2.http.Url;

/**
 * Created by GKF on 2018/1/29.
 * post
 */

public interface IHttpPostApi {

    @FormUrlEncoded
    @POST
    Deferred<Response<String>> postMap(@Url String url,
                                       @FieldMap(encoded = true) Map<String, String> fieldMap,
                                       @HeaderMap Map<String, String> headers);

    @POST
    Deferred<Response<String>> postJson(@Url String url,
                                        @Body RequestBody body,
                                        @HeaderMap Map<String, String> headers);
}
