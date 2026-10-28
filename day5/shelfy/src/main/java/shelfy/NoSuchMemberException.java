package shelfy;

public class NoSuchMemberException extends RuntimeException {

    public NoSuchMemberException(int id) {
        super("No such member: " + id);
    }

    public NoSuchMemberException() {
        super("No such member");
    }
}
