package com.ksidelta.library.google

import com.ksidelta.library.books.UrlBuilder
import com.ksidelta.library.google.SpreadsheetClient.BatchUpdate.Request
import com.ksidelta.library.google.SpreadsheetClient.BatchUpdate.Request.AddSheet
import com.ksidelta.library.google.SpreadsheetClient.BatchUpdate.Request.AddSheet.SheetProperties
import com.ksidelta.library.http.HttpClient

class SpreadsheetClient(val httpClient: HttpClient) {
    fun create(token: String, title: String = "Unknown Lol"): NewSpreadsheetResponse =
        UrlBuilder.queryUrl("https://sheets.googleapis.com/v4/spreadsheets", mapOf()).let { url ->
            httpClient.post(url, NewSpreadsheetResponse::class.java) {
                it
                    .setToken(token)
                    .body(NewSpreadsheet(NewSpreadsheet.Properties(title)))
            }
        }

    data class NewSpreadsheet(val properties: Properties) {
        data class Properties(val title: String);
    }

    data class NewSpreadsheetResponse(val spreadsheetId: String, val spreadsheetUrl: String);

    fun createSheet(token: String, spreadsheetId: SpreadsheetId, sheetName: String, sheetId: Int) {
        val url = "https://sheets.googleapis.com/v4/spreadsheets/${spreadsheetId}:batchUpdate"
        httpClient.post(url, String::class.java) {
            it.body(
                BatchUpdate(
                    listOf(
                        Request(
                            AddSheet(
                                SheetProperties(sheetName, sheetId)
                            )
                        )
                    )
                )
            ).setToken(token)
        }
    }


    data class BatchUpdate(val requests: List<Request>) {
        data class Request(val addSheet: AddSheet) {
            data class AddSheet(
                val properties: SheetProperties
            ) {
                data class SheetProperties(val title: String, val sheetId: Int = 0);
            }
        }
    }

    fun updateValues(token: String, spreadsheetId: SpreadsheetId, range: String, values: List<List<String>>) {
        val url = "https://sheets.googleapis.com/v4/spreadsheets/${spreadsheetId}/values:batchUpdate"
        httpClient.post(url, String::class.java) {
            it.setToken(token)
                .body(
                    SheetBatchUpdate(
                        SheetBatchUpdate.ValueInputOption.USER_ENTERED,
                        listOf(
                            SheetBatchUpdate.ValueRange(
                                range,
                                values
                            )
                        )
                    )
                )
        }

    }


    data class SheetBatchUpdate(
        val valueInputOption: ValueInputOption,
        val data: List<ValueRange>
    ) {
        enum class ValueInputOption {
            USER_ENTERED,
            RAW
        }

        data class ValueRange(val range: String, val values: List<List<String>>);

    }
}


typealias SpreadsheetId = String;