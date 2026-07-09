package com.bulgyeong.safetyapp.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

// Models
data class LoginRequest(val employeeId: String)
data class LoginResponse(val success: Boolean, val user: User?, val message: String?)
data class User(val employeeId: String, val name: String)

data class AreaResponse(val success: Boolean, val areas: List<Area>)
data class Area(val id: String, val name: String, val isDanger: Int)

data class LotoResponse(val success: Boolean, val lotos: List<Loto>)
data class Loto(val id: String, val areaId: String, val text: String)

data class ChecklistResponse(val success: Boolean, val checklists: List<Checklist>)
data class Checklist(val id: String, val text: String)

data class StartWorkRequest(val employeeId: String, val areaId: String, val durationMinutes: Int)
data class BaseResponse(val success: Boolean, val message: String?)

data class EmergencyRequest(val employeeId: String, val type: String)

interface SafetyApiService {
    @POST("/api/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("/api/areas")
    suspend fun getAreas(): AreaResponse

    @GET("/api/lotos")
    suspend fun getLotos(@Query("areaId") areaId: String): LotoResponse

    @GET("/api/checklists")
    suspend fun getChecklists(): ChecklistResponse

    @POST("/api/work/start")
    suspend fun startWork(@Body request: StartWorkRequest): BaseResponse

    @POST("/api/emergency")
    suspend fun reportEmergency(@Body request: EmergencyRequest): BaseResponse
}

object RetrofitClient {
    // 10.0.2.2 points to localhost of the host machine from Android Emulator
    private const val BASE_URL = "http://10.0.2.2:3000"

    val api: SafetyApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SafetyApiService::class.java)
    }
}

object SessionManager {
    var currentUser: User? = null
    var currentArea: Area? = null
}
