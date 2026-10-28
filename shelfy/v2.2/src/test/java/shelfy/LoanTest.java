package shelfy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

// The compact constructor of the record and the computed status (Module 06).
class LoanTest {

    private static final LocalDate BORROWED = LocalDate.of(2026, 1, 1);
    private static final LocalDate DUE = LocalDate.of(2026, 1, 15);

    @Test
    void theCompactConstructorRejectsBadValues() {
        assertThrows(NullPointerException.class, () -> new Loan(1, 1, 1, null, DUE, null));
        assertThrows(IllegalArgumentException.class, () -> new Loan(1, 0, 1, BORROWED, DUE, null));
        assertThrows(IllegalArgumentException.class, () -> new Loan(1, 1, 0, BORROWED, DUE, null));
        assertThrows(IllegalArgumentException.class, () -> new Loan(1, 1, 1, DUE, BORROWED, null));
    }

    @Test
    void statusIsComputedAgainstTheGivenDate() {
        Loan loan = new Loan(1, 1, 1, BORROWED, DUE, null);

        assertEquals(new LoanStatus.Open(), loan.status(DUE));
        assertEquals(new LoanStatus.Overdue(6), loan.status(DUE.plusDays(6)));
        assertEquals(new LoanStatus.Returned(), loan.withReturnedOn(DUE).status(DUE.plusDays(100)));
    }

    @Test
    void describeNamesEveryStatus() {
        assertEquals("OPEN", LoanStatus.describe(new LoanStatus.Open()));
        assertEquals("RETURNED", LoanStatus.describe(new LoanStatus.Returned()));
        assertEquals("OVERDUE (6 days)", LoanStatus.describe(new LoanStatus.Overdue(6)));
    }
}
