package shelfy;

// The three states a loan can be in (Module 06.3). "sealed" means: these three and no others,
// so a switch over a LoanStatus is complete without a default branch, and the compiler tells
// you when you forget one.
public sealed interface LoanStatus permits LoanStatus.Open, LoanStatus.Returned, LoanStatus.Overdue {

    record Open() implements LoanStatus {
    }

    record Returned() implements LoanStatus {
    }

    record Overdue(long daysOverdue) implements LoanStatus {
    }

    // The name shown in lists and, from Day 4 on, in the JSON field "status".
    static String describe(LoanStatus status) {
        return switch (status) {
            case Open o -> "OPEN";
            case Returned r -> "RETURNED";
            case Overdue(long days) -> "OVERDUE (" + days + " days)";
        };
    }
}
