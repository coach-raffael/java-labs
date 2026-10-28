package shelfy;

public class NoSuchMediumException extends RuntimeException {

    public NoSuchMediumException(int id) {
        super("No such medium: " + id);
    }
}
