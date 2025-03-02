package com.oxad

import com.oxad.attendanceregister.UserRegisterModel
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface ApiService {
    @FormUrlEncoded
    @POST("markAttendance") // Adjust this to match your backend endpoint
   suspend fun markAttendance(
        @Field("userid") userid: Long,
        @Field("gymid") gymid: Long
    ): UserRegisterModel
}
