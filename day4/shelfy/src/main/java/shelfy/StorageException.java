package shelfy;

// Thrown when the storage cannot read or write (Module 06.5). Unchecked, because the caller
// cannot do much about a broken file or database except report it.
public class StorageException extends RuntimeException {

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
