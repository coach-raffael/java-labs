package shelfy;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

// The rules of the library (Modules 04 to 09): what can be borrowed, when it is due, what it costs
// when it comes back late, and the questions you can ask (available, overdue, stats, search).
// It keeps no data of its own; everything goes through the Storage. No method calls
// "now": the date is always a parameter, which keeps the rules testable (Module 10.8).
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

    public List<Medium> media() {
        return storage.media();
    }

    public List<Member> members() {
        return storage.members();
    }

    public List<Loan> loans() {
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

    // Optional instead of null for "maybe not there" (Module 09.4).
    public Optional<Medium> findMedium(int id) {
        return storage.media().stream().filter(m -> m.id() == id).findFirst();
    }

    public Optional<Member> findMember(int id) {
        return storage.members().stream().filter(m -> m.id() == id).findFirst();
    }

    private Medium requireMedium(int id) {
        return findMedium(id).orElseThrow(() -> new NoSuchMediumException(id));
    }

    private Member requireMember(int id) {
        return findMember(id).orElseThrow(() -> new NoSuchMemberException(id));
    }

    public Optional<Loan> openLoanOf(int mediumId) {
        return storage.loans().stream().filter(l -> l.mediumId() == mediumId && l.isOpen()).findFirst();
    }

    // The ids of all media that are out right now: a Set, because each id appears once (Module 08.4).
    public Set<Integer> borrowedIds() {
        return storage.loans().stream().filter(Loan::isOpen).map(Loan::mediumId).collect(Collectors.toSet());
    }

    public boolean isAvailable(int mediumId) {
        return !borrowedIds().contains(mediumId);
    }

    public Loan borrow(int mediumId, int memberId, LocalDate today) {
        Medium medium = requireMedium(mediumId);
        requireMember(memberId);
        if (!isAvailable(mediumId)) {
            throw new BookNotAvailableException(medium);
        }
        Loan loan = new Loan(0, mediumId, memberId, today, today.plusDays(LOAN_DAYS), null);
        return storage.addLoan(loan);
    }

    public Loan giveBack(int mediumId, LocalDate today) {
        Medium medium = requireMedium(mediumId);
        Loan open = openLoanOf(mediumId).orElseThrow(() -> NoSuchLoanException.forMedium(medium));
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
        Medium medium = requireMedium(loan.mediumId());
        double fee = (days - FREE_DAYS) * medium.lateFeePerDay();
        return Math.min(fee, MAX_FEE);
    }

    // The questions of Module 09, each one a stream pipeline.

    public List<Medium> available() {
        Set<Integer> borrowed = borrowedIds();
        return storage.media().stream().filter(m -> !borrowed.contains(m.id())).toList();
    }

    public List<Loan> overdue(LocalDate today) {
        return storage.loans().stream()
                .filter(Loan::isOpen)
                .filter(l -> l.due().isBefore(today))
                .toList();
    }

    // Finds in title and author, ignoring case; sorted by title, then author (Comparator.comparing, Module 09.1).
    public List<Medium> search(String text) {
        String needle = text.toLowerCase();
        return storage.media().stream()
                .filter(m -> m.title().toLowerCase().contains(needle) || m.author().toLowerCase().contains(needle))
                .sorted(Comparator.comparing(Medium::title).thenComparing(Medium::author))
                .toList();
    }

    // How many open loans each member has (Collectors.groupingBy and counting, Module 09.3).
    public Map<Integer, Long> openLoansPerMember() {
        return storage.loans().stream()
                .filter(Loan::isOpen)
                .collect(Collectors.groupingBy(Loan::memberId, Collectors.counting()));
    }

    public Stats stats(LocalDate today) {
        long open = storage.loans().stream().filter(Loan::isOpen).count();
        long overdue = overdue(today).size();
        return new Stats(storage.media().size(), open, overdue, openLoansPerMember());
    }

    // The numbers behind "stats" (a record, Module 06.1). The map goes from member id to open loans.
    public record Stats(int media, long openLoans, long overdueLoans, Map<Integer, Long> openLoansPerMember) {
    }
}
