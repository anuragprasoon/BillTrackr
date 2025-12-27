package com.example.billtrackr

import org.json.JSONObject

data class ParsedBill(
    val merchant: String,
    val amount: String,
    val date: String
)

fun parseBillFields(text: String): ParsedBill {
    val cleanText = text
        .replace("```json", "")
        .replace("```", "")
        .trim()
    val json = JSONObject(cleanText)

    return ParsedBill(
        merchant = json.optString("merchant"),
        amount = json.optString("amount"),
        date = json.optString("date")
    )
}
