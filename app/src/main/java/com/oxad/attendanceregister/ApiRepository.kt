package com.oxad.attendanceregister

import android.util.Log
import com.oxad.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ApiRepository() {
    suspend fun markMyAttendance( gymid: String,userid:Long): Result<UserRegisterModel> {
        Log.e("jadfadsf","Api called $gymid $userid")

        return safeApiCall {
            RetrofitClient.instance.markAttendance(userid,gymid)      }

    }

    private suspend fun <T> safeApiCall(apiCall: suspend () -> T): Result<T> {
        return withContext(Dispatchers.IO) {
            try {
                Result.success(apiCall())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}