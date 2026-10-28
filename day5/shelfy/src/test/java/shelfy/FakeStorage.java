package shelfy;

import java.util.ArrayList;
import java.util.List;

// A test double (Module 12.8): the simplest Storage that works, no file, no database, no framework.
// Because the Library gets its Storage through the constructor, a test can hand it this one.
class FakeStorage implements Storage {

    final List<Medium> media = new ArrayList<>();
    final List<Member> members = new ArrayList<>();
    final List<Loan> loans = new ArrayList<>();
    int updates = 0;

    @Override
    public String name() {
        return "fake";
    }

    @Override
    public List<Medium> media() {
        return List.copyOf(media);
    }

    @Override
    public List<Member> members() {
        return List.copyOf(members);
    }

    @Override
    public List<Loan> loans() {
        return List.copyOf(loans);
    }

    @Override
    public Medium addMedium(Medium medium) {
        Medium stored = medium.withId(media.size() + 1);
        media.add(stored);
        return stored;
    }

    @Override
    public Member addMember(Member member) {
        Member stored = member.withId(members.size() + 1);
        members.add(stored);
        return stored;
    }

    @Override
    public Loan addLoan(Loan loan) {
        Loan stored = loan.withId(loans.size() + 1);
        loans.add(stored);
        return stored;
    }

    @Override
    public void updateLoan(Loan loan) {
        updates++;
        loans.replaceAll(l -> l.id() == loan.id() ? loan : l);
    }
}
