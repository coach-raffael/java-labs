package shelfy;

// A printed book (Module 05.2): 0.50 per day of late fee after the free days.
public class Book extends Medium {

    public Book(int id, String title, String author, String isbn) {
        super(id, title, author, isbn);
    }

    @Override
    public MediaKind kind() {
        return MediaKind.BOOK;
    }

    @Override
    public double lateFeePerDay() {
        return 0.50;
    }

    @Override
    public int durationMinutes() {
        return 0;
    }

    @Override
    public Medium withId(int newId) {
        return new Book(newId, title(), author(), isbn());
    }
}
