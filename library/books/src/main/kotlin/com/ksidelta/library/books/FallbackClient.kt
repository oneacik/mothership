package com.ksidelta.library.books

class FallbackClient(val clients: List<BookClient>) : BookClient {
    override fun fetchByIsbn(isbn: String): BookClient.Book? =
        clients.stream()
            .map { it.fetchByIsbn(isbn) }
            .filter { it != null }
            .findFirst()
            .orElse(null)
}