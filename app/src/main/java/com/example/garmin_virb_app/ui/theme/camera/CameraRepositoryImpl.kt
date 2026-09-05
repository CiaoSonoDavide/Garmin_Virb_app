package com.example.garmin_virb_app.ui.theme.camera

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody

class CameraRepositoryImpl(
    private val baseUrlProvider: () -> String,
    private val client: OkHttpClient = OkHttpClient()
): CameraRepository {
    override suspend fun connectToCamera(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val base = baseUrlProvider().trimEnd('/')
            val statusReq = Request.Builder().url("$base/virb").get().build()
            client.newCall(statusReq).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext Result.failure(Exception("Status failed: ${resp.code}"))
            }
            val rtsp = buildRtspFromBaseHttp(base)
            return@withContext Result.success(rtsp)
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    override suspend fun takePhoto(): Result<String> = withContext(Dispatchers.IO) {
       try{
           val base = baseUrlProvider()
           val req = Request.Builder()
               .url("$base/commands/takePhoto")
               .post(RequestBody.create("application/json".toMediaTypeOrNull(), "{}"))
               .build()
           client.newCall(req).execute().use { resp ->
               if(!resp.isSuccessful) return@withContext Result.failure(Exception("takePhoto failed: ${resp.code}"))
               // parse resp.body() to obtain real URL/path
               val body = resp.body?.string().orEmpty()
               Result.success(body)
           }
       }
       catch (e: Exception){
           Result.failure(e)
       }
    }

    override suspend fun startRecording(): Result<Unit> = withContext(Dispatchers.IO){
        try{
            val base = baseUrlProvider()
            val req = Request.Builder().url("$base/commands/startRecording").post(RequestBody.create(null, ByteArray(0))).build()
            client.newCall(req).execute().use { resp ->
                if(!resp.isSuccessful) return@withContext Result.failure(Exception("startRecording failed: ${resp.code}"))
            }
            Result.success(Unit)
        }
        catch (e: Exception){
            Result.failure(e)
        }
    }

    override suspend fun stopRecording(): Result<Unit> = withContext(Dispatchers.IO){
        try{
            val base = baseUrlProvider()
            val req = Request.Builder().url("$base/commands/stopRecording").post(RequestBody.create(null, ByteArray(0))).build()
            client.newCall(req).execute().use { resp ->
                if(!resp.isSuccessful) return@withContext Result.failure(Exception("stopRecording failed: ${resp.code}"))
            }
            Result.success(Unit)
        }
        catch (e: Exception){
            Result.failure(e)
        }
    }

    override suspend fun fetchGallery(): Result<List<String>> = withContext(Dispatchers.IO) {
        try{
            val base = baseUrlProvider()
            val req = Request.Builder().url("$base/files").get().build()
            client.newCall(req).execute().use { resp ->
                if(!resp.isSuccessful) return@withContext Result.failure(Exception("fetchGallery failed: ${resp.code}"))
                val body = resp.body?.string().orEmpty()
                //parse JSON --> URI list
                Result.success(listOf(body))
            }
        }
        catch (e: Exception){
            Result.failure(e)
        }
    }

    fun buildRtspFromBaseHttp(baseHttp: String): String {
        val host = Uri.parse(baseHttp).host ?: baseHttp
        return "rtsp://$host/livePreviewStream"
    }
}