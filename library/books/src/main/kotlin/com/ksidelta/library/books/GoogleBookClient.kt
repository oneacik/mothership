package com.ksidelta.library.books

import com.ksidelta.library.http.HttpClient
import com.ksidelta.library.http.KtorHttpClient

class GoogleBookClient private constructor(val httpClient: HttpClient) : BookClient {

    override fun fetchByIsbn(isbn: String): BookClient.Book? {
        val response = httpClient.get("https://www.googleapis.com/books/v1/volumes?q=isbn:${isbn}", GoogleBookResponseDto::class.java).data
        if (response.totalItems.equals(0))
            return null;

        val fetchedVolume = response.items!![0].volumeInfo
        return BookClient.Book(
            isbn,
            fetchedVolume.title,
            fetchedVolume.authors.joinToString(", "),
            fetchedVolume.publishedDate.substring(0, 4)
        );
    }


    companion object {
        fun unauthenticated(httpClient: HttpClient = KtorHttpClient()) = GoogleBookClient(httpClient)
    }

    data class GoogleBookResponseDto(
        val totalItems: Integer,
        val items: List<GoogleBook>?
    );

    data class GoogleBook(
        val volumeInfo: Volume
    );

    data class Volume(
        val title: String,
        val publishedDate: String,
        val authors: List<String>
    );
}