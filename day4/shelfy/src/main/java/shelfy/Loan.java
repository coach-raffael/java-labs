package shelfy;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

// One loan of one medium to one member (Module 06.1). A record: the data is the whole point,
// it never changes after creation, and the compact constructor validates it.
// returnedOn stays null while the loan is open.
public record Loan(int id, int mediumId, int memberId, LocalDate borrowedOn, LocalDate due, LocalDate returnedOn) {

    public Loan {
        Objects.requireNonNull(borrowedOn, "borrowedOn must not be null");
        Objects.requireNonNull(due, "due must not be null");
        if (mediumId <= 0) {
            throw new IllegalArgumentException("mediumId must be greater than 0");
        }
        if (memberId <= 0) {
            throw new IllegalArgumentException("memberId must be greater than 0");
        }
        if (due.isBefore(borrowedOn)) {
            throw new IllegalArgumentException("due must not be before borrowedOn");
        }
    }

    public boolean isOpen() {
        return returnedOn == null;
    }

    // Records are immutable: "changing" the return date means making a copy with that one field set.
    public Loan withReturnedOn(LocalDate date) {
        return new Loan(id, mediumId, memberId, borrowedOn, due, date);
    }

    public Loan withId(int newId) {
        return new Loan(newId, mediumId, memberId, borrowedOn, due, returnedOn);
    }

    // The status is never stored; it is computed against the date you pass in (Module 06.3).
    public LoanStatus status(LocalDate today) {
        if (returnedOn != null) {
            return new LoanStatus.Returned();
        }
        if (due.isBefore(today)) {
            return new LoanStatus.Overdue(ChronoUnit.DAYS.between(due, today));
        }
        return new LoanStatus.Open();
    }
}
