package app.twicast.tv.data.remote

import app.twicast.tv.data.remote.dto.DeviceCodeDto
import app.twicast.tv.data.remote.dto.TokenDto
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

/** https://id.twitch.tv/oauth2 — Device Code Grant for a public client (no secret). */
interface TwitchOAuthApi {

    @FormUrlEncoded
    @POST("device")
    suspend fun requestDeviceCode(
        @Field("client_id") clientId: String,
        @Field("scopes") scopes: String,
    ): DeviceCodeDto

    /** Fails with HTTP 400 "authorization_pending" until the user confirms on the phone. */
    @FormUrlEncoded
    @POST("token")
    suspend fun exchangeDeviceCode(
        @Field("client_id") clientId: String,
        @Field("scopes") scopes: String,
        @Field("device_code") deviceCode: String,
        @Field("grant_type") grantType: String = "urn:ietf:params:oauth:grant-type:device_code",
    ): TokenDto

    /** Public clients get a single-use refresh token: each refresh returns a new one. */
    @FormUrlEncoded
    @POST("token")
    suspend fun refreshToken(
        @Field("client_id") clientId: String,
        @Field("refresh_token") refreshToken: String,
        @Field("grant_type") grantType: String = "refresh_token",
    ): TokenDto

    /** Twitch requires validating stored tokens on start. 401 = token revoked or expired. */
    @GET("validate")
    suspend fun validate(@Header("Authorization") authorization: String)
}
