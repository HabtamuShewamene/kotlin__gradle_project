package org.dbu.library.service

import org.dbu.library.model.Book
import org.dbu.library.model.Patron
import org.dbu.library.repository.InMemoryLibraryRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LibraryServiceTest {

    private lateinit var repository: InMemoryLibraryRepository
    private lateinit var service: DefaultLibraryService

    @BeforeEach
    fun setUp() {
        repository = InMemoryLibraryRepository()
        service = DefaultLibraryService(repository)

        // Seed initial data
        repository.addBook(Book("101", "Kotlin in Action", "Dmitry Jemerov", 2017))
        repository.addBook(Book("102", "Effective Kotlin", "Marcin Moskala", 2019))
        repository.addPatron(Patron("P1", "Alice"))
    }

    @Test
    fun testAddBookSuccessfully() {
        val newBook = Book("103", "Atomic Kotlin", "Bruce Eckel", 2021)
        val added = service.addBook(newBook)
        assertTrue(added, "Should successfully add a new book")
        assertEquals(newBook, repository.findBook("103"))
    }

    @Test
    fun testAddDuplicateBookFails() {
        val duplicateBook = Book("101", "Duplicate Book", "Author", 2022)
        val added = service.addBook(duplicateBook)
        assertFalse(added, "Should not add book with existing ISBN")
    }

    @Test
    fun testBorrowBookSuccess() {
        val result = service.borrowBook("P1", "101")
        assertEquals(BorrowResult.SUCCESS, result)

        val book = repository.findBook("101")
        assertNotNull(book)
        assertFalse(book!!.isAvailable, "Book should be marked as unavailable")

        val patron = repository.findPatron("P1")
        assertNotNull(patron)
        assertTrue(patron!!.borrowedBooks.contains("101"), "Patron should have book in borrowed list")
    }

    @Test
    fun testBorrowBookNotFound() {
        val result = service.borrowBook("P1", "999")
        assertEquals(BorrowResult.BOOK_NOT_FOUND, result)
    }

    @Test
    fun testBorrowPatronNotFound() {
        val result = service.borrowBook("P999", "101")
        assertEquals(BorrowResult.PATRON_NOT_FOUND, result)
    }

    @Test
    fun testBorrowAlreadyBorrowedBook() {
        service.borrowBook("P1", "101")
        val result = service.borrowBook("P1", "101")
        assertEquals(BorrowResult.NOT_AVAILABLE, result)
    }

    @Test
    fun testBorrowLimitReached() {
        // Add 5 more books and borrow them all
        for (i in 1..5) {
            val isbn = "B$i"
            repository.addBook(Book(isbn, "Book $i", "Author $i", 2020))
            val res = service.borrowBook("P1", isbn)
            assertEquals(BorrowResult.SUCCESS, res)
        }

        // Attempting to borrow a 6th book
        val res6 = service.borrowBook("P1", "101")
        assertEquals(BorrowResult.LIMIT_REACHED, res6)
    }

    @Test
    fun testReturnBookSuccess() {
        service.borrowBook("P1", "101")
        val returned = service.returnBook("P1", "101")
        assertTrue(returned, "Returning a borrowed book should succeed")

        val book = repository.findBook("101")
        assertNotNull(book)
        assertTrue(book!!.isAvailable, "Book should be marked as available again")

        val patron = repository.findPatron("P1")
        assertNotNull(patron)
        assertFalse(patron!!.borrowedBooks.contains("101"), "Book should be removed from patron's borrowed list")
    }

    @Test
    fun testReturnUnborrowedBookFails() {
        val returned = service.returnBook("P1", "102")
        assertFalse(returned, "Cannot return a book that was not borrowed")
    }

    @Test
    fun testSearchByTitleAndAuthor() {
        val byTitle = service.search("Action")
        assertEquals(1, byTitle.size)
        assertEquals("Kotlin in Action", byTitle[0].title)

        val byAuthor = service.search("moskala")
        assertEquals(1, byAuthor.size)
        assertEquals("Effective Kotlin", byAuthor[0].title)

        val notFound = service.search("Python")
        assertTrue(notFound.isEmpty())
    }
}
