package shelfy;

// A library for children charges nothing. Same interface, different rule: the Library does not notice.
public class ChildrenPolicy implements LateFeePolicy {

    @Override
    public String name() {
        return "children";
    }

    @Override
    public double fee(Medium medium, long daysOverdue) {
        return 0.0;
    }
}
