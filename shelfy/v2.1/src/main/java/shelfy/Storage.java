package shelfy;

import java.util.List;

// Where shelfy keeps its data (Module 05.5). The Library talks to this interface only;
// InMemoryStorage forgets everything when the program ends, FileStorage keeps it in shelfy.txt.
// Since Module 08 the methods return List, the interface, not ArrayList, the implementation.
// The storage assigns the ids: pass a medium, member or loan with id 0 and use the returned copy.
public interface Storage {

    String name();

    List<Medium> media();

    List<Member> members();

    List<Loan> loans();

    Medium addMedium(Medium medium);

    Member addMember(Member member);

    Loan addLoan(Loan loan);

    // Called when a loan is returned: the stored loan with this id is replaced.
    void updateLoan(Loan loan);
}
