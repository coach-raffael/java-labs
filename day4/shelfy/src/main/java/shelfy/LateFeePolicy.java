package shelfy;

// Strategy (Module 12.2): the rule for the late fee is its own object, so that the Library does not
// change when the rule does. Two rules ship with shelfy: StandardPolicy and ChildrenPolicy.
public interface LateFeePolicy {

    String name();

    double fee(Medium medium, long daysOverdue);
}
