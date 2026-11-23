package com.example.blindo.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface BlindoApiService {

    // Autenticación Laravel Sanctum
    @POST("register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("logout")
    suspend fun logout(): Response<MessageResponse>

    @GET("me")
    suspend fun me(): Response<UserDto>

    // CRUD Personal
    @GET("personal")
    suspend fun getPersonal(): Response<List<PersonalDto>>

    @POST("personal")
    suspend fun createPersonal(@Body request: PersonalRequest): Response<PersonalDto>

    @PUT("personal/{id}")
    suspend fun updatePersonal(@Path("id") id: Int, @Body request: PersonalRequest): Response<PersonalDto>

    @DELETE("personal/{id}")
    suspend fun deletePersonal(@Path("id") id: Int): Response<MessageResponse>

    // CRUD Asignaciones EPP
    @GET("asignaciones-epp")
    suspend fun getAsignacionesEpp(): Response<List<AsignacionEppDto>>

    @POST("asignaciones-epp")
    suspend fun createAsignacionEpp(@Body request: AsignacionEppRequest): Response<AsignacionEppStoreResponse>

    @PUT("asignaciones-epp/{id}")
    suspend fun updateAsignacionEpp(
        @Path("id") id: Int,
        @Body request: AsignacionEppUpdateRequest
    ): Response<AsignacionEppDto>

    @DELETE("asignaciones-epp/{id}")
    suspend fun deleteAsignacionEpp(@Path("id") id: Int): Response<MessageResponse>

    // CRUD Validaciones (Órdenes Activas + Bitácora)
    @GET("validaciones")
    suspend fun getValidaciones(@Query("activa") activa: Boolean? = null): Response<List<ValidacionDto>>

    @POST("validaciones")
    suspend fun createValidacion(@Body request: ValidacionCreateRequest): Response<ValidacionDto>

    @PUT("validaciones/{id}")
    suspend fun updateValidacion(
        @Path("id") id: Int,
        @Body request: ValidacionUpdateRequest
    ): Response<ValidacionDto>

    @DELETE("validaciones/{id}")
    suspend fun deleteValidacion(@Path("id") id: Int): Response<MessageResponse>
}
