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
data class EndWorkRequest(val employeeId: String)
data class ExtendWorkRequest(val employeeId: String, val extendMinutes: Int)
data class BaseResponse(val success: Boolean, val message: String?)
data class LocationReportRequest(val employeeId: String, val latitude: Double, val longitude: Double)
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

    @POST("/api/work/end")
    suspend fun endWork(@Body request: EndWorkRequest): BaseResponse

    @POST("/api/work/extend")
    suspend fun extendWork(@Body request: ExtendWorkRequest): BaseResponse

    @POST("/api/emergency")
    suspend fun reportEmergency(@Body request: EmergencyRequest): BaseResponse

    @POST("/api/location/report")
    suspend fun reportLocation(@Body request: LocationReportRequest): BaseResponse
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
    private const val KEY_START_TIME_MILLIS = "start_time_millis"
    private const val KEY_DURATION_MINUTES = "duration_minutes"

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

    fun startWork(context: Context, area: Area, durationMinutes: Int) {
        currentArea = area
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_IS_WORKING, true)
            .putString(KEY_AREA_ID, area.id)
            .putString(KEY_AREA_NAME, area.name)
            .putLong(KEY_START_TIME_MILLIS, System.currentTimeMillis())
            .putInt(KEY_DURATION_MINUTES, durationMinutes)
            .apply()
    }

    fun addDuration(context: Context, minutes: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentDuration = prefs.getInt(KEY_DURATION_MINUTES, 0)
        prefs.edit()
            .putInt(KEY_DURATION_MINUTES, currentDuration + minutes)
            .apply()
    }

    fun getStartTimeMillis(context: Context): Long {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_START_TIME_MILLIS, 0L)
    }

    fun getDurationMinutes(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_DURATION_MINUTES, 0)
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
