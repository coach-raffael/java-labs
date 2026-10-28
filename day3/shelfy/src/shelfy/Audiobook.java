package shelfy;

// An audiobook (Module 05.2): it knows its duration and charges 1.00 per day of late fee.
public class Audiobook extends Medium {

    private final int durationMinutes;

    public Audiobook(int id, String title, String author, String isbn, int durationMinutes) {
        super(id, title, author, isbn);
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("durationMinutes must be greater than 0");
        }
        this.durationMinutes = durationMinutes;
    }

    @Override
    public MediaKind kind() {
        return MediaKind.AUDIOBOOK;
    }

    @Override
    public double lateFeePerDay() {
        return 1.00;
    }

    @Override
    public int durationMinutes() {
        return durationMinutes;
    }

    @Override
    public Medium withId(int newId) {
        return new Audiobook(newId, title(), author(), isbn(), durationMinutes);
    }

    @Override
    public String toString() {
        return super.toString() + ", " + durationMinutes + " min";
    }
}
