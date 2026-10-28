package shelfy;

// Thrown when someone tries to borrow a medium that is already out (Module 06.5).
public class BookNotAvailableException extends RuntimeException {

    public BookNotAvailableException(Medium medium) {
        super("Already borrowed: " + medium.title());
    }
}
