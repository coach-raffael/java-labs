package shelfy;

// A request the API cannot accept (Module 13.5): missing title, unknown kind, no JSON. Answered with 400.
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
