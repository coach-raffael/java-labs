package shelfy;

// Thrown when a medium with this ISBN is already in the library (Module 13.5: the API answers 409).
public class DuplicateIsbnException extends RuntimeException {

    public DuplicateIsbnException(String isbn) {
        super("ISBN already in the library: " + isbn);
    }
}
