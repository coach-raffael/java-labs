package shelfy;

// Thrown when a loan is returned a second time (Module 14.8: the API answers 409).
public class AlreadyReturnedException extends RuntimeException {

    public AlreadyReturnedException(int loanId) {
        super("Already returned: loan " + loanId);
    }
}
