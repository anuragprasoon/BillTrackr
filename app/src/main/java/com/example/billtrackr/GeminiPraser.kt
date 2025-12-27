package com.example.billtrackr

import org.json.JSONObject

fun extractGeminiText(response: String): String {
    val root = JSONObject(response)
    val candidates = root.getJSONArray("candidates")
    val content = candidates
        .getJSONObject(0)
        .getJSONObject("content")
        .getJSONArray("parts")
        .getJSONObject(0)
        .getString("text")

    return content
}

