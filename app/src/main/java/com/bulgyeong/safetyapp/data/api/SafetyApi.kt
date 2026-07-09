package com.bulgyeong.safetyapp.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import android.content.Context

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
    // Local IP address of the host machine (Macbook) for device debugging
    private const val BASE_URL = "http://192.168.2.82:3000"

    val api: SafetyApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SafetyApiService::class.java)
    }
}

object SessionManager {
    private const val PREFS_NAME = "safety_app_prefs"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_EMPLOYEE_ID = "employee_id"
    private const val KEY_EMPLOYEE_NAME = "employee_name"
    private const val KEY_IS_WORKING = "is_working"
    private const val KEY_AREA_ID = "area_id"
    private const val KEY_AREA_NAME = "area_name"

    var currentUser: User? = null
    var currentArea: Area? = null

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val empId = prefs.getString(KEY_EMPLOYEE_ID, null)
        val empName = prefs.getString(KEY_EMPLOYEE_NAME, null)
        if (empId != null && empName != null) {
            currentUser = User(empId, empName)
        }
        val areaId = prefs.getString(KEY_AREA_ID, null)
        val areaName = prefs.getString(KEY_AREA_NAME, null)
        if (areaId != null && areaName != null) {
            currentArea = Area(areaId, areaName, 0)
        }
    }

    fun login(context: Context, user: User) {
        currentUser = user
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_EMPLOYEE_ID, user.employeeId)
            .putString(KEY_EMPLOYEE_NAME, user.name)
            .apply()
    }

    fun startWork(context: Context, area: Area) {
        currentArea = area
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_IS_WORKING, true)
            .putString(KEY_AREA_ID, area.id)
            .putString(KEY_AREA_NAME, area.name)
            .apply()
    }

    fun endWork(context: Context) {
        currentUser = null
        currentArea = null
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .clear()
            .apply()
    }

    fun isWorking(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_IS_WORKING, false)
    }

    fun isLoggedIn(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_IS_LOGGED_IN, false)
    }
}
