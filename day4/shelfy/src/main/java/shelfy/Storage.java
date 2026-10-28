package shelfy;

import java.util.List;

// Where shelfy keeps its data (Module 05.5). The Library talks to this interface only.
// Three implementations since Day 4: InMemoryStorage, FileStorage (shelfy.txt) and PostgresStorage.
// The storage assigns the ids: pass a medium, member or loan with id 0 and use the returned copy.
public interface Storage {

    String name();

    // Called once at start. PostgresStorage creates the tables when they are missing; the others do nothing.
    default void init() {
    }

    List<Medium> media();

    List<Member> members();

    List<Loan> loans();

    Medium addMedium(Medium medium);

    Member addMember(Member member);

    Loan addLoan(Loan loan);

    // Called when a loan is returned: the stored loan with this id is replaced.
    void updateLoan(Loan loan);
}
