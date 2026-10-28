package shelfy;

import java.util.logging.Logger;

// The Observer of Module 12 as the log writer of Module 14: one INFO line per library event on stdout.
public class LogListener implements LibraryListener {

    private static final Logger LOG = Logger.getLogger("shelfy.library");

    @Override
    public void onBorrowed(Loan loan, Medium medium, Member member) {
        LOG.info(() -> "borrowed loan=" + loan.id() + " medium=" + medium.id() + " member=" + member.id()
                + " due=" + loan.due());
    }

    @Override
    public void onReturned(Loan loan, Medium medium, Member member, double lateFee) {
        LOG.info(() -> "returned loan=" + loan.id() + " medium=" + medium.id() + " member=" + member.id()
                + " fee=" + Money.format(lateFee));
    }
}
