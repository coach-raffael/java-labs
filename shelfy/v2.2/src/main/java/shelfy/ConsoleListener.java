package shelfy;

// Prints one line per library event to the console.
public class ConsoleListener implements LibraryListener {

    @Override
    public void onBorrowed(Loan loan, Medium medium, Member member) {
        System.out.println("[event] " + member.name() + " borrowed " + medium.title() + ", due " + loan.due());
    }

    @Override
    public void onReturned(Loan loan, Medium medium, Member member, double lateFee) {
        System.out.println("[event] " + member.name() + " returned " + medium.title() + ", fee " + Money.format(lateFee));
    }
}
