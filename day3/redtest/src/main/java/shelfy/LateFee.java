package shelfy;

// Module 10, exercise 3: the late fee of a book, cut out of shelfy for this exercise.
// One test in LateFeeTest is red. Run ./mvnw test, read the stack trace down to the line in this
// file, find the mistake, fix it, run again.
public class LateFee {

    public static final int FREE_DAYS = 3;
    public static final double MAX_FEE = 10.00;

    public static double forBook(long daysOverdue) {
        if (daysOverdue <= FREE_DAYS) {
            return 0.0;
        }
        double perDay = 1 / 2;
        double fee = (daysOverdue - FREE_DAYS) * perDay;
        return Math.min(fee, MAX_FEE);
    }
}
