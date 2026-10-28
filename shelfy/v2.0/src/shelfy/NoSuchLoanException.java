package shelfy;

public class NoSuchLoanException extends RuntimeException {

    public NoSuchLoanException(int id) {
        super("No such loan: " + id);
    }

    public static NoSuchLoanException forMedium(Medium medium) {
        return new NoSuchLoanException("Not borrowed: " + medium.title());
    }

    private NoSuchLoanException(String message) {
        super(message);
    }
}
