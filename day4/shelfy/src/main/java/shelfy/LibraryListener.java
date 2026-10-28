package shelfy;

// Observer (Module 12.4): the Library tells its listeners what happened and does not care who listens.
// ConsoleListener prints, LogListener writes a log line (Module 14.6), a test can count.
public interface LibraryListener {

    void onBorrowed(Loan loan, Medium medium, Member member);

    void onReturned(Loan loan, Medium medium, Member member, double lateFee);
}
