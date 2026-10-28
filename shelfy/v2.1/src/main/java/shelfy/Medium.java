package shelfy;

import java.util.Objects;

// Something the library lends: a Book or an Audiobook (Module 05.3). The class is abstract
// because "a medium" never exists on its own; the subclasses decide the kind and the late fee.
public abstract class Medium {

    private final int id;
    private final String title;
    private final String author;
    private final String isbn;

    protected Medium(int id, String title, String author, String isbn) {
        this.id = id;
        this.title = requireText(title, "title");
        this.author = requireText(author, "author");
        this.isbn = requireText(isbn, "isbn");
        if (this.title.length() > 200) {
            throw new IllegalArgumentException("title must have at most 200 characters");
        }
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        if (trimmed.contains(";")) {
            throw new IllegalArgumentException(name + " must not contain ';' (it is the separator of shelfy.txt)");
        }
        return trimmed;
    }

    public int id() {
        return id;
    }

    public String title() {
        return title;
    }

    public String author() {
        return author;
    }

    public String isbn() {
        return isbn;
    }

    // Each kind answers for itself: polymorphism (Module 05.4).
    public abstract MediaKind kind();

    public abstract double lateFeePerDay();

    // Audiobooks have a duration, books do not; the file format and the database store it for both.
    public abstract int durationMinutes();

    // The storage assigns the id; a medium with id 0 is "not stored yet" (Module 04.5 explains static,
    // Module 06.1 the idea of a copy with one field changed).
    public abstract Medium withId(int newId);

    // Two media are the same medium when they have the same id (Module 04.4).
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Medium that)) {
            return false;
        }
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return id + ". " + title + " by " + author + " (" + kind().displayName() + ")";
    }
}
