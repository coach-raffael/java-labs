package shelfy;

// The two kinds of media shelfy lends (Module 06.2). An enum with a field: each constant carries
// the name shown to the user.
public enum MediaKind {
    BOOK("Book"),
    AUDIOBOOK("Audiobook");

    private final String displayName;

    MediaKind(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
