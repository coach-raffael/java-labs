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

// The rules of the library, tested with a FakeStorage (Module 12.8) and without LocalDate.now():
// every test passes the date it wants (Module 10.8).
class LibraryTest {

    private static final LocalDate DAY_ONE = LocalDate.of(2026, 1, 1);

    private FakeStorage storage;
    private Library library;
    private Medium book;
    private Medium audiobook;
    private Member ada;

    @BeforeEach
    void setUp() {
        storage = new FakeStorage();
        library = new Library(storage, new StandardPolicy(), Library.LOAN_DAYS);
        book = library.addBook("Clean Code", "Robert C. Martin", "9780132350884");
        audiobook = library.addAudiobook("The Pragmatic Programmer", "David Thomas", "9780135957059", 720);
        ada = library.addMember("Ada Lovelace");
    }

    @Test
    void borrowCreatesAnOpenLoanDueAfterLoanDays() {
        Loan loan = library.borrow(book.id(), ada.id(), DAY_ONE);

        assertTrue(loan.isOpen());
        assertEquals(DAY_ONE.plusDays(14), loan.due());
        assertFalse(library.isAvailable(book.id()));
        assertEquals(1, storage.loans.size());
    }

    @Test
    void theLoanPeriodIsConfigurable() {
        Library sevenDays = new Library(storage, new StandardPolicy(), 7);

        Loan loan = sevenDays.borrow(book.id(), ada.id(), DAY_ONE);

        assertEquals(DAY_ONE.plusDays(7), loan.due());
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
    void aDuplicateIsbnIsRejected() {
        assertThrows(DuplicateIsbnException.class,
                () -> library.addBook("Clean Code, second copy", "Robert C. Martin", "9780132350884"));
    }

    @Test
    void giveBackClosesTheLoanAndMakesTheMediumAvailable() {
        library.borrow(book.id(), ada.id(), DAY_ONE);

        Loan returned = library.giveBack(book.id(), DAY_ONE.plusDays(10));

        assertFalse(returned.isOpen());
        assertEquals(DAY_ONE.plusDays(10), returned.returnedOn());
        assertTrue(library.isAvailable(book.id()));
        assertEquals(1, storage.updates);
    }

    @Test
    void returnLoanByIdRejectsASecondReturn() {
        Loan loan = library.borrow(book.id(), ada.id(), DAY_ONE);

        library.returnLoan(loan.id(), DAY_ONE.plusDays(1));

        assertThrows(AlreadyReturnedException.class, () -> library.returnLoan(loan.id(), DAY_ONE.plusDays(2)));
        assertThrows(NoSuchLoanException.class, () -> library.returnLoan(99, DAY_ONE));
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
    void theChildrenPolicyChargesNothing() {
        Library children = new Library(storage, new ChildrenPolicy(), Library.LOAN_DAYS);
        Loan loan = children.borrow(book.id(), ada.id(), DAY_ONE);

        assertEquals(0.0, children.lateFee(loan, loan.due().plusDays(30)), 0.001);
    }

    @Test
    void theFeeOfAReturnedLoanCountsTheReturnDateNotToday() {
        Loan loan = library.borrow(book.id(), ada.id(), DAY_ONE);
        Loan returned = library.giveBack(book.id(), loan.due().plusDays(5));

        assertEquals(1.00, library.lateFee(returned, loan.due().plusDays(300)), 0.001);
    }

    @Test
    void listenersHearAboutLoansAndReturns() {
        CountingListener counting = new CountingListener();
        library.addListener(counting);

        library.borrow(book.id(), ada.id(), DAY_ONE);
        library.borrow(audiobook.id(), ada.id(), DAY_ONE);
        library.giveBack(book.id(), DAY_ONE.plusDays(20));

        assertEquals(2, counting.borrowed);
        assertEquals(1, counting.returned);
        assertEquals(1.50, counting.lastFee, 0.001);
    }

    @Test
    void overdueListsOnlyOpenLoansPastTheirDueDate() {
        Loan late = library.borrow(book.id(), ada.id(), DAY_ONE);
        library.borrow(audiobook.id(), ada.id(), DAY_ONE.plusDays(10));

        assertEquals(List.of(late), library.overdue(late.due().plusDays(1)));
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
    void theSearchQueryBuilderCombinesTextAvailabilityAndKind() {
        library.borrow(book.id(), ada.id(), DAY_ONE);

        assertEquals(List.of(audiobook), library.search(SearchQuery.builder().onlyAvailable().build()));
        assertEquals(List.of(book), library.search(SearchQuery.builder().kind(MediaKind.BOOK).build()));
        assertEquals(List.of(), library.search(SearchQuery.builder().text("clean").onlyAvailable().build()));
        assertEquals(2, library.search(SearchQuery.all()).size());
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

    // An Observer for the test: it only counts (Module 12, exercise 3).
    static class CountingListener implements LibraryListener {

        int borrowed = 0;
        int returned = 0;
        double lastFee = -1;

        @Override
        public void onBorrowed(Loan loan, Medium medium, Member member) {
            borrowed++;
        }

        @Override
        public void onReturned(Loan loan, Medium medium, Member member, double lateFee) {
            returned++;
            lastFee = lateFee;
        }
    }
}
