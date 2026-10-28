package shelfy;

import java.util.Objects;

// A library member (Module 04). Two members with the same id are the same member, so equals and
// hashCode look at the id only (Module 04.4).
public class Member {

    private final int id;
    private final String name;

    public Member(int id, String name) {
        Objects.requireNonNull(name, "name must not be null");
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("name must not be empty");
        }
        if (trimmed.contains(";")) {
            throw new IllegalArgumentException("name must not contain ';' (it is the separator of shelfy.txt)");
        }
        this.id = id;
        this.name = trimmed;
    }

    public int id() {
        return id;
    }

    public String name() {
        return name;
    }

    public Member withId(int newId) {
        return new Member(newId, name);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Member that)) {
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
        return id + ". " + name;
    }
}
