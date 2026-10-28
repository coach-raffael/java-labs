package shelfy;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

// The rules of the library (Module 04 to 06): what can be borrowed, when it is due, what it costs
// when it comes back late. It keeps no data of its own; everything goes through the Storage.
public class Library {

    public static final int LOAN_DAYS = 14;
    public static final int FREE_DAYS = 3;
    public static final double MAX_FEE = 10.00;

    private final Storage storage;

    public Library(Storage storage) {
        this.storage = storage;
    }

    public Storage storage() {
        return storage;
    }

    public ArrayList<Medium> media() {
        return storage.media();
    }

    public ArrayList<Member> members() {
        return storage.members();
    }

    public ArrayList<Loan> loans() {
        return storage.loans();
    }

    public Medium addBook(String title, String author, String isbn) {
        return storage.addMedium(new Book(0, title, author, isbn));
    }

    public Medium addAudiobook(String title, String author, String isbn, int durationMinutes) {
        return storage.addMedium(new Audiobook(0, title, author, isbn, durationMinutes));
    }

    public Member addMember(String name) {
        return storage.addMember(new Member(0, name));
    }

    public Medium findMedium(int id) {
        for (Medium m : storage.media()) {
            if (m.id() == id) {
                return m;
            }
        }
        throw new NoSuchMediumException(id);
    }

    public Member findMember(int id) {
        for (Member m : storage.members()) {
            if (m.id() == id) {
                return m;
            }
        }
        throw new NoSuchMemberException(id);
    }

    // The open loan of a medium, or null when the medium is on the shelf.
    private Loan openLoanOf(int mediumId) {
        for (Loan l : storage.loans()) {
            if (l.mediumId() == mediumId && l.isOpen()) {
                return l;
            }
        }
        return null;
    }

    public boolean isAvailable(int mediumId) {
        return openLoanOf(mediumId) == null;
    }

    public Loan borrow(int mediumId, int memberId, LocalDate today) {
        Medium medium = findMedium(mediumId);
        findMember(memberId);
        if (!isAvailable(mediumId)) {
            throw new BookNotAvailableException(medium);
        }
        Loan loan = new Loan(0, mediumId, memberId, today, today.plusDays(LOAN_DAYS), null);
        return storage.addLoan(loan);
    }

    public Loan giveBack(int mediumId, LocalDate today) {
        Medium medium = findMedium(mediumId);
        Loan open = openLoanOf(mediumId);
        if (open == null) {
            throw NoSuchLoanException.forMedium(medium);
        }
        Loan returned = open.withReturnedOn(today);
        storage.updateLoan(returned);
        return returned;
    }

    // Days past the due date, 0 when the loan is not overdue. For a returned loan the return date counts.
    public long daysOverdue(Loan loan, LocalDate today) {
        LocalDate end = loan.returnedOn() == null ? today : loan.returnedOn();
        if (!loan.due().isBefore(end)) {
            return 0;
        }
        return ChronoUnit.DAYS.between(loan.due(), end);
    }

    // The first FREE_DAYS days cost nothing; after that each day costs what the medium charges,
    // and the fee never exceeds MAX_FEE. Audiobooks cost more per day than books: polymorphism.
    public double lateFee(Loan loan, LocalDate today) {
        long days = daysOverdue(loan, today);
        if (days <= FREE_DAYS) {
            return 0.0;
        }
        Medium medium = findMedium(loan.mediumId());
        double fee = (days - FREE_DAYS) * medium.lateFeePerDay();
        return Math.min(fee, MAX_FEE);
    }
}
