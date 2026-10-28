package shelfy;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

// The rules of the library (Modules 04 to 12). It keeps no data of its own; everything goes through
// the Storage. The late-fee rule is a LateFeePolicy (Strategy), events go to LibraryListeners
// (Observer), and everything the Library needs arrives in its constructor (dependency injection
// by hand, Module 12.6). No method calls "now": the date is always a parameter (Module 10.8).
public class Library {

    public static final int LOAN_DAYS = 14;

    // How many loans are open right now, kept up to date by borrow and the returns.
    private static int openLoans = 0;

    private final Storage storage;
    private final LateFeePolicy policy;
    private final int loanDays;
    private final List<LibraryListener> listeners = new ArrayList<>();

    public Library(Storage storage) {
        this(storage, new StandardPolicy(), LOAN_DAYS);
    }

    public Library(Storage storage, LateFeePolicy policy, int loanDays) {
        if (loanDays < 1) {
            throw new IllegalArgumentException("loanDays must be at least 1");
        }
        this.storage = storage;
        this.policy = policy;
        this.loanDays = loanDays;
    }

    public Storage storage() {
        return storage;
    }

    public LateFeePolicy policy() {
        return policy;
    }

    public int loanDays() {
        return loanDays;
    }

    public void addListener(LibraryListener listener) {
        listeners.add(listener);
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

    public synchronized Medium addBook(String title, String author, String isbn) {
        return addMedium(new Book(0, title, author, isbn));
    }

    public synchronized Medium addAudiobook(String title, String author, String isbn, int durationMinutes) {
        return addMedium(new Audiobook(0, title, author, isbn, durationMinutes));
    }

    public synchronized Medium addMedium(Medium medium) {
        if (findByIsbn(medium.isbn()).isPresent()) {
            throw new DuplicateIsbnException(medium.isbn());
        }
        return storage.addMedium(medium);
    }

    public synchronized Member addMember(String name) {
        return storage.addMember(new Member(0, name));
    }

    // Optional instead of null for "maybe not there" (Module 09.4).
    public Optional<Medium> findMedium(int id) {
        return storage.media().stream().filter(m -> m.id() == id).findFirst();
    }

    public Optional<Medium> findByIsbn(String isbn) {
        return storage.media().stream().filter(m -> m.isbn().equals(isbn)).findFirst();
    }

    public Member findMember(int id) {
        for (Member m : storage.members()) {
            if (m.id() == id) {
                return m;
            }
        }
        return null;
    }

    // The name shown in lists and log lines.
    public String memberName(int id) {
        return findMember(id).name();
    }

    public Optional<Loan> findLoan(int id) {
        return storage.loans().stream().filter(l -> l.id() == id).findFirst();
    }

    private Medium requireMedium(int id) {
        return findMedium(id).orElseThrow(() -> new NoSuchMediumException(id));
    }

    private Member requireMember(int id) {
        Member member = findMember(id);
        if (member == null) {
            throw new NoSuchMemberException(id);
        }
        return member;
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

    public synchronized Loan borrow(int mediumId, int memberId, LocalDate today) {
        Medium medium = requireMedium(mediumId);
        Member member = requireMember(memberId);
        if (!isAvailable(mediumId)) {
            throw new BookNotAvailableException(medium);
        }
        Loan loan = storage.addLoan(new Loan(0, mediumId, memberId, today, today.plusDays(LOAN_DAYS), null));
        openLoans++;
        for (LibraryListener listener : listeners) {
            listener.onBorrowed(loan, medium, member);
        }
        return loan;
    }

    // Return by medium: what the console does ("return 2").
    public synchronized Loan giveBack(int mediumId, LocalDate today) {
        Medium medium = requireMedium(mediumId);
        Loan open = openLoanOf(mediumId).orElseThrow(() -> NoSuchLoanException.forMedium(medium));
        return close(open, today);
    }

    // Return by loan id: what PATCH /loans/<id> does (Module 14.8).
    public synchronized Loan returnLoan(int loanId, LocalDate today) {
        Loan loan = findLoan(loanId).orElseThrow(() -> new NoSuchLoanException(loanId));
        if (!loan.isOpen()) {
            throw new AlreadyReturnedException(loanId);
        }
        return close(loan, today);
    }

    private Loan close(Loan open, LocalDate today) {
        Loan returned = open.withReturnedOn(today);
        storage.updateLoan(returned);
        openLoans--;
        Medium medium = requireMedium(returned.mediumId());
        Member member = requireMember(returned.memberId());
        double fee = lateFee(returned, today);
        for (LibraryListener listener : listeners) {
            listener.onReturned(returned, medium, member, fee);
        }
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

    // The policy decides (Strategy); the medium still says what one day costs (polymorphism).
    public double lateFee(Loan loan, LocalDate today) {
        return policy.fee(requireMedium(loan.mediumId()), daysOverdue(loan, today));
    }

    public List<Medium> available() {
        return search(SearchQuery.builder().onlyAvailable().build());
    }

    public List<Loan> overdue(LocalDate today) {
        return storage.loans().stream()
                .filter(Loan::isOpen)
                .filter(l -> l.due().isBefore(today))
                .toList();
    }

    // Finds in title and author, ignoring case; sorted by title, then author (Module 09.1).
    @SuppressWarnings("unchecked")
    public List<Medium> search(String text) {
        List results = search(SearchQuery.builder().text(text).build());
        return (List<Medium>) results;
    }

    // The same search with every option of the Builder (Module 12.5).
    public List<Medium> search(SearchQuery query) {
        String needle = query.text().toLowerCase();
        Set<Integer> borrowed = query.onlyAvailable() ? borrowedIds() : Set.of();
        return storage.media().stream()
                .filter(m -> m.title().toLowerCase().contains(needle) || m.author().toLowerCase().contains(needle))
                .filter(m -> !borrowed.contains(m.id()))
                .filter(m -> query.kind().map(k -> m.kind() == k).orElse(true))
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
        long overdue = overdue(today).size();
        return new Stats(storage.media().size(), openLoans, overdue, openLoansPerMember());
    }

    // The numbers behind "stats" and GET /health (a record, Module 06.1). The map goes from member id to open loans.
    public record Stats(int media, long openLoans, long overdueLoans, Map<Integer, Long> openLoansPerMember) {
    }
}
