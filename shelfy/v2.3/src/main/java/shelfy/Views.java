package shelfy;

import java.time.LocalDate;

// The JSON shapes of the API (Module 13.4): records that exist only to be written as JSON or read
// from it. The domain classes of Day 2 stay as they are; these views translate. Dates travel as
// text in ISO form (2026-10-27), and the JSON field for the medium is called bookId.
public final class Views {

    private Views() {
    }

    // GET /health
    public record Health(String status, String version, String storage, int books, long openLoans, long overdueLoans) {
    }

    public record HealthError(String status, String version, String storage, String error) {
    }

    // Every error: {"error": "..."}. Not called Error, because java.lang.Error already has that name.
    public record ErrorView(String error) {
    }

    // GET /books, POST /books (response)
    public record MediumView(int id, String kind, String title, String author, String isbn, int durationMinutes,
                             boolean available) {

        public static MediumView of(Medium medium, boolean available) {
            return new MediumView(medium.id(), medium.kind().name(), medium.title(), medium.author(), medium.isbn(),
                    medium.durationMinutes(), available);
        }
    }

    // POST /books (request). kind is optional (default BOOK); durationMinutes is needed for an AUDIOBOOK.
    // Integer and Boolean instead of int and boolean in the request records: a field the client did not
    // send arrives as null, and the handler can say "bookId is required" instead of silently using 0.
    public record NewBook(String title, String author, String isbn, String kind, Integer durationMinutes) {
    }

    // POST /loans (request)
    public record NewLoan(Integer bookId, Integer memberId) {
    }

    // POST /loans (response)
    public record LoanView(int id, int bookId, int memberId, String borrowedOn, String due, String status) {

        public static LoanView of(Loan loan, LocalDate today) {
            return new LoanView(loan.id(), loan.mediumId(), loan.memberId(), loan.borrowedOn().toString(),
                    loan.due().toString(), statusName(loan, today));
        }
    }

    // PATCH /loans/<id> (request)
    public record ReturnRequest(Boolean returned) {
    }

    // PATCH /loans/<id> (response)
    public record ReturnedLoanView(int id, int bookId, int memberId, String borrowedOn, String due, String returnedOn,
                                   String status, long daysOverdue, double lateFee) {

        public static ReturnedLoanView of(Loan loan, long daysOverdue, double lateFee) {
            return new ReturnedLoanView(loan.id(), loan.mediumId(), loan.memberId(), loan.borrowedOn().toString(),
                    loan.due().toString(), loan.returnedOn().toString(), "RETURNED", daysOverdue, lateFee);
        }
    }

    // OPEN, OVERDUE or RETURNED: the sealed LoanStatus of Module 06 as one word for the JSON field.
    static String statusName(Loan loan, LocalDate today) {
        return switch (loan.status(today)) {
            case LoanStatus.Open o -> "OPEN";
            case LoanStatus.Returned r -> "RETURNED";
            case LoanStatus.Overdue o -> "OVERDUE";
        };
    }
}
