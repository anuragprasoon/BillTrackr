package com.example.billtrackr

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object GeminiApi {

    private val client = OkHttpClient()

    fun extractBillText(
        base64Image: String,
        onResult: (String) -> Unit
    ) {

        val partsArray = org.json.JSONArray().apply {
            put(
                org.json.JSONObject().apply {
                    put(
                        "text",
                        "Read this bill image and extract merchant name, total amount, and date. " +
                                "Respond in JSON format with keys: merchant, amount, date."
                    )
                }
            )
            put(
                org.json.JSONObject().apply {
                    put(
                        "inline_data",
                        org.json.JSONObject().apply {
                            put("mime_type", "image/jpeg")
                            put("data", base64Image)
                        }
                    )
                }
            )
        }

        val contentsArray = org.json.JSONArray().apply {
            put(
                org.json.JSONObject().apply {
                    put("parts", partsArray)
                }
            )
        }

        val rootJson = org.json.JSONObject().apply {
            put("contents", contentsArray)
        }

        val requestBody =
            rootJson.toString()
                .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(
                "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=${BuildConfig.GEMINI_API_KEY}"
            )
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {

            override fun onFailure(call: okhttp3.Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val body = response.body?.string().orEmpty()
                onResult(body)
            }
        })

    }

    suspend fun extractBillTextSuspend(base64Image: String): String =
        kotlinx.coroutines.suspendCancellableCoroutine { cont ->

            extractBillText(base64Image) { result ->
                if (cont.isActive) {
                    cont.resume(result, onCancellation = {})
                }
            }

            cont.invokeOnCancellation {
                // Optional: cancel OkHttp call later if you track it
            }
        }


}
