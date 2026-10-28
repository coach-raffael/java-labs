package shelfy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

// The rules of the library, tested without a file (InMemoryStorage) and without LocalDate.now():
// every test passes the date it wants (Module 10.8).
class LibraryTest {

    private static final LocalDate DAY_ONE = LocalDate.of(2026, 1, 1);

    private Library library;
    private Medium book;
    private Medium audiobook;
    private Member ada;

    @BeforeEach
    void setUp() {
        library = new Library(new InMemoryStorage());
        book = library.addBook("Clean Code", "Robert C. Martin", "9780132350884");
        audiobook = library.addAudiobook("The Pragmatic Programmer", "David Thomas", "9780135957059", 720);
        ada = library.addMember("Ada Lovelace");
    }

    @Test
    void borrowCreatesAnOpenLoanDueAfterLoanDays() {
        Loan loan = library.borrow(book.id(), ada.id(), DAY_ONE);

        assertTrue(loan.isOpen());
        assertEquals(DAY_ONE.plusDays(Library.LOAN_DAYS), loan.due());
        assertFalse(library.isAvailable(book.id()));
    }

    @Test
    void borrowingABorrowedMediumThrows() {
        library.borrow(book.id(), ada.id(), DAY_ONE);

        BookNotAvailableException e = assertThrows(BookNotAvailableException.class,
                () -> library.borrow(book.id(), ada.id(), DAY_ONE));
        assertEquals("Already borrowed: Clean Code", e.getMessage());
    }

    @Test
    void borrowingAnUnknownMediumOrMemberThrows() {
        assertThrows(NoSuchMediumException.class, () -> library.borrow(99, ada.id(), DAY_ONE));
        assertThrows(NoSuchMemberException.class, () -> library.borrow(book.id(), 99, DAY_ONE));
    }

    @Test
    void giveBackClosesTheLoanAndMakesTheMediumAvailable() {
        library.borrow(book.id(), ada.id(), DAY_ONE);

        Loan returned = library.giveBack(book.id(), DAY_ONE.plusDays(10));

        assertFalse(returned.isOpen());
        assertEquals(DAY_ONE.plusDays(10), returned.returnedOn());
        assertTrue(library.isAvailable(book.id()));
    }

    @Test
    void returningAMediumThatIsNotBorrowedThrows() {
        assertThrows(NoSuchLoanException.class, () -> library.giveBack(book.id(), DAY_ONE));
    }

    @ParameterizedTest(name = "{0} days overdue -> fee {1} for a book")
    @CsvSource({
            "0, 0.00",
            "3, 0.00",
            "4, 0.50",
            "6, 1.50",
            "30, 10.00"
    })
    void lateFeeForABook(int daysOverdue, double expectedFee) {
        Loan loan = library.borrow(book.id(), ada.id(), DAY_ONE);
        LocalDate today = loan.due().plusDays(daysOverdue);

        assertEquals(expectedFee, library.lateFee(loan, today), 0.001);
    }

    @Test
    void anAudiobookCostsTwiceAsMuchPerDay() {
        Loan bookLoan = library.borrow(book.id(), ada.id(), DAY_ONE);
        Loan audioLoan = library.borrow(audiobook.id(), ada.id(), DAY_ONE);
        LocalDate sixDaysLate = bookLoan.due().plusDays(6);

        assertEquals(1.50, library.lateFee(bookLoan, sixDaysLate), 0.001);
        assertEquals(3.00, library.lateFee(audioLoan, sixDaysLate), 0.001);
    }

    @Test
    void theFeeOfAReturnedLoanCountsTheReturnDateNotToday() {
        Loan loan = library.borrow(book.id(), ada.id(), DAY_ONE);
        Loan returned = library.giveBack(book.id(), loan.due().plusDays(5));

        assertEquals(1.00, library.lateFee(returned, loan.due().plusDays(300)), 0.001);
    }

    @Test
    void overdueListsOnlyOpenLoansPastTheirDueDate() {
        Loan late = library.borrow(book.id(), ada.id(), DAY_ONE);
        library.borrow(audiobook.id(), ada.id(), DAY_ONE.plusDays(10));

        List<Loan> overdue = library.overdue(late.due().plusDays(1));

        assertEquals(List.of(late), overdue);
        assertEquals(LoanStatus.Overdue.class, late.status(late.due().plusDays(1)).getClass());
    }

    @Test
    void availableLeavesOutBorrowedMedia() {
        library.borrow(book.id(), ada.id(), DAY_ONE);

        assertEquals(List.of(audiobook), library.available());
    }

    @Test
    void searchFindsInTitleAndAuthorIgnoringCase() {
        assertEquals(List.of(book), library.search("MARTIN"));
        assertEquals(List.of(audiobook), library.search("pragmatic"));
        assertEquals(2, library.search("t").size());
        assertTrue(library.search("kubernetes").isEmpty());
    }

    @Test
    void statsCountMediaOpenAndOverdueLoansPerMember() {
        Member grace = library.addMember("Grace Hopper");
        library.borrow(book.id(), ada.id(), DAY_ONE);
        Loan second = library.borrow(audiobook.id(), grace.id(), DAY_ONE.plusDays(10));

        Library.Stats stats = library.stats(second.due().minusDays(1));

        assertEquals(2, stats.media());
        assertEquals(2, stats.openLoans());
        assertEquals(1, stats.overdueLoans());
        assertEquals(1L, stats.openLoansPerMember().get(ada.id()));
        assertEquals(1L, stats.openLoansPerMember().get(grace.id()));
    }

    @Test
    void findMemberReturnsAnEmptyOptionalForAnUnknownNumber() {
        assertTrue(library.findMember(42).isEmpty());
        assertEquals("Ada Lovelace", library.findMember(ada.id()).map(Member::name).orElse("nobody"));
    }
}
