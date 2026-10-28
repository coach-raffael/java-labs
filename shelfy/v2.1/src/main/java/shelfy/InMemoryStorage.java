package shelfy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

// Keeps everything in memory (Module 08): media and members in a Map by id, so that a lookup
// does not need a loop, loans in a List. A TreeMap keeps its keys sorted, so media() comes out
// in id order. Gone when the program ends.
public class InMemoryStorage implements Storage {

    private final Map<Integer, Medium> media = new TreeMap<>();
    private final Map<Integer, Member> members = new TreeMap<>();
    private final List<Loan> loans = new ArrayList<>();

    @Override
    public String name() {
        return "memory";
    }

    @Override
    public List<Medium> media() {
        return new ArrayList<>(media.values());
    }

    @Override
    public List<Member> members() {
        return new ArrayList<>(members.values());
    }

    @Override
    public List<Loan> loans() {
        return new ArrayList<>(loans);
    }

    @Override
    public Medium addMedium(Medium medium) {
        Medium stored = medium.withId(nextId(media));
        media.put(stored.id(), stored);
        return stored;
    }

    @Override
    public Member addMember(Member member) {
        Member stored = member.withId(nextId(members));
        members.put(stored.id(), stored);
        return stored;
    }

    @Override
    public Loan addLoan(Loan loan) {
        int max = loans.stream().mapToInt(Loan::id).max().orElse(0);
        Loan stored = loan.withId(max + 1);
        loans.add(stored);
        return stored;
    }

    @Override
    public void updateLoan(Loan loan) {
        for (int i = 0; i < loans.size(); i++) {
            if (loans.get(i).id() == loan.id()) {
                loans.set(i, loan);
                return;
            }
        }
        throw new NoSuchLoanException(loan.id());
    }

    // Used by FileStorage when it reads shelfy.txt: the ids in the file stay as they are.
    void restoreMedium(Medium medium) {
        media.put(medium.id(), medium);
    }

    void restoreMember(Member member) {
        members.put(member.id(), member);
    }

    void restoreLoan(Loan loan) {
        loans.add(loan);
    }

    // A generic method (Module 08.5): works for any Map with Integer keys, whatever the values are.
    private static <V> int nextId(Map<Integer, V> byId) {
        int max = byId.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
        return max + 1;
    }
}
