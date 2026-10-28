package shelfy;

import java.util.ArrayList;

// Where shelfy keeps its data (Module 05.5). The Library talks to this interface only.
// Implement it twice: InMemoryStorage (forgets everything at the end) and FileStorage (shelfy.txt).
// The storage assigns the ids: pass a medium, member or loan with id 0 and use the returned copy.
// This file compiles once Medium, Member and Loan exist.
public interface Storage {

    String name();

    ArrayList<Medium> media();

    ArrayList<Member> members();

    ArrayList<Loan> loans();

    Medium addMedium(Medium medium);

    Member addMember(Member member);

    Loan addLoan(Loan loan);

    // Called when a loan is returned: the stored loan with this id is replaced.
    void updateLoan(Loan loan);
}
