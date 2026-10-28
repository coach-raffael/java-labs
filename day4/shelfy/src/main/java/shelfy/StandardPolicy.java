package shelfy;

// The rule from Day 2: the first FREE_DAYS days cost nothing, then each day costs what the medium
// charges (books 0.50, audiobooks 1.00), and the fee never exceeds MAX_FEE.
public class StandardPolicy implements LateFeePolicy {

    public static final int FREE_DAYS = 3;
    public static final double MAX_FEE = 10.00;

    @Override
    public String name() {
        return "standard";
    }

    @Override
    public double fee(Medium medium, long daysOverdue) {
        if (daysOverdue <= FREE_DAYS) {
            return 0.0;
        }
        double fee = (daysOverdue - FREE_DAYS) * medium.lateFeePerDay();
        return Math.min(fee, MAX_FEE);
    }
}
