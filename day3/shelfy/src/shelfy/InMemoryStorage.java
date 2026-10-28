package shelfy;

import java.util.ArrayList;

// Keeps everything in three lists (Module 04.8). Gone when the program ends.
public class InMemoryStorage implements Storage {

    private final ArrayList<Medium> media = new ArrayList<Medium>();
    private final ArrayList<Member> members = new ArrayList<Member>();
    private final ArrayList<Loan> loans = new ArrayList<Loan>();

    @Override
    public String name() {
        return "memory";
    }

    @Override
    public ArrayList<Medium> media() {
        return new ArrayList<Medium>(media);
    }

    @Override
    public ArrayList<Member> members() {
        return new ArrayList<Member>(members);
    }

    @Override
    public ArrayList<Loan> loans() {
        return new ArrayList<Loan>(loans);
    }

    @Override
    public Medium addMedium(Medium medium) {
        Medium stored = medium.withId(nextMediumId());
        media.add(stored);
        return stored;
    }

    @Override
    public Member addMember(Member member) {
        Member stored = member.withId(nextMemberId());
        members.add(stored);
        return stored;
    }

    @Override
    public Loan addLoan(Loan loan) {
        Loan stored = loan.withId(nextLoanId());
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
        media.add(medium);
    }

    void restoreMember(Member member) {
        members.add(member);
    }

    void restoreLoan(Loan loan) {
        loans.add(loan);
    }

    private int nextMediumId() {
        int max = 0;
        for (Medium m : media) {
            if (m.id() > max) {
                max = m.id();
            }
        }
        return max + 1;
    }

    private int nextMemberId() {
        int max = 0;
        for (Member m : members) {
            if (m.id() > max) {
                max = m.id();
            }
        }
        return max + 1;
    }

    private int nextLoanId() {
        int max = 0;
        for (Loan l : loans) {
            if (l.id() > max) {
                max = l.id();
            }
        }
        return max + 1;
    }
}
