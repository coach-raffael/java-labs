package shelfy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

// Runs only when TEST_DATABASE_URL points to a PostgreSQL database you may empty, for example
//   TEST_DATABASE_URL=jdbc:postgresql://localhost:5432/shelfy TEST_DATABASE_USER=shelfy TEST_DATABASE_PASSWORD=shelfy-lab ./mvnw verify
// Without the variable the class is skipped, so ./mvnw verify works on a machine without a database.
@EnabledIfEnvironmentVariable(named = "TEST_DATABASE_URL", matches = ".+")
class PostgresStorageTest {

    private static final String URL = System.getenv("TEST_DATABASE_URL");
    private static final String USER = env("TEST_DATABASE_USER", "shelfy");
    private static final String PASSWORD = env("TEST_DATABASE_PASSWORD", "shelfy-lab");

    private PostgresStorage storage;

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    @BeforeEach
    void emptyTables() throws SQLException {
        storage = new PostgresStorage(URL, USER, PASSWORD);
        storage.init();
        try (Connection c = DriverManager.getConnection(URL, USER, PASSWORD); Statement s = c.createStatement()) {
            s.executeUpdate("TRUNCATE loans, members, media RESTART IDENTITY");
        }
    }

    @Test
    void storesAndReadsMediaMembersAndLoans() {
        Library library = new Library(storage);
        Medium book = library.addBook("Clean Code", "Robert C. Martin", "9780132350884");
        Medium audio = library.addAudiobook("The Pragmatic Programmer", "David Thomas", "9780135957059", 720);
        Member ada = library.addMember("Ada Lovelace");
        Loan loan = library.borrow(book.id(), ada.id(), LocalDate.of(2026, 1, 1));

        assertEquals(1, book.id());
        assertEquals(2, audio.id());
        assertEquals(book, storage.media().get(0));
        assertEquals(720, storage.media().get(1).durationMinutes());
        assertEquals(MediaKind.AUDIOBOOK, storage.media().get(1).kind());
        assertEquals("Ada Lovelace", storage.members().get(0).name());
        assertEquals(loan, storage.loans().get(0));
        assertEquals(LocalDate.of(2026, 1, 15), storage.loans().get(0).due());
        assertTrue(storage.loans().get(0).isOpen());
    }

    @Test
    void updateLoanSetsTheReturnDateAndRejectsUnknownIds() {
        Library library = new Library(storage);
        Medium book = library.addBook("Clean Code", "Robert C. Martin", "9780132350884");
        Member ada = library.addMember("Ada Lovelace");
        Loan loan = library.borrow(book.id(), ada.id(), LocalDate.of(2026, 1, 1));

        library.giveBack(book.id(), LocalDate.of(2026, 1, 20));

        assertEquals(LocalDate.of(2026, 1, 20), storage.loans().get(0).returnedOn());
        assertTrue(library.isAvailable(book.id()));
        assertThrows(NoSuchLoanException.class, () -> storage.updateLoan(loan.withId(99)));
    }

    @Test
    void theDatabaseEnforcesTheUniqueIsbn() {
        storage.addMedium(new Book(0, "Clean Code", "Robert C. Martin", "9780132350884"));

        StorageException e = assertThrows(StorageException.class,
                () -> storage.addMedium(new Book(0, "Clean Code", "Robert C. Martin", "9780132350884")));
        assertTrue(e.getMessage().contains("media_isbn_key"), e.getMessage());
    }

    @Test
    void aWrongPasswordIsReportedAsAStorageException() {
        PostgresStorage wrong = new PostgresStorage(URL, USER, "not-the-password");

        StorageException e = assertThrows(StorageException.class, wrong::media);
        assertTrue(e.getMessage().contains("password authentication failed"), e.getMessage());
    }
}
