package org.dbu.library.service

import org.dbu.library.model.Book
import org.dbu.library.repository.LibraryRepository

class DefaultLibraryService(
    private val repository: LibraryRepository
) : LibraryService {

    override fun addBook(book: Book): Boolean {
        return repository.addBook(book)
    }

    override fun borrowBook(patronId: String, isbn: String): BorrowResult {
        val book = repository.findBook(isbn) ?: return BorrowResult.BOOK_NOT_FOUND
        val patron = repository.findPatron(patronId) ?: return BorrowResult.PATRON_NOT_FOUND

        if (!book.isAvailable) {
            return BorrowResult.NOT_AVAILABLE
        }

        if (patron.borrowedBooks.size >= 5) {
            return BorrowResult.LIMIT_REACHED
        }

        repository.updateBook(book.copy(isAvailable = false))
        repository.updatePatron(patron.copy(borrowedBooks = patron.borrowedBooks + isbn))

        return BorrowResult.SUCCESS
    }

    override fun returnBook(patronId: String, isbn: String): Boolean {
        val book = repository.findBook(isbn) ?: return false
        val patron = repository.findPatron(patronId) ?: return false

        if (!patron.borrowedBooks.contains(isbn)) {
            return false
        }

        repository.updateBook(book.copy(isAvailable = true))
        repository.updatePatron(patron.copy(borrowedBooks = patron.borrowedBooks - isbn))

        return true
    }

    override fun search(query: String): List<Book> {
        val lower = query.lowercase()
        return repository.getAllBooks().filter {
            it.title.lowercase().contains(lower) || it.author.lowercase().contains(lower)
        }
    }
}