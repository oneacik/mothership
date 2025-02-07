package com.ksidelta.library.books

interface BookClient {
    fun fetchByIsbn(isbn: String): Book?

    data class Book(val isbn: String, val title: String, val author: String, val year: String)
}