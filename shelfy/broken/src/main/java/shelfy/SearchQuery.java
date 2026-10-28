package shelfy;

import java.util.Optional;

// Builder (Module 12.5): a search has several optional parts. Instead of five constructors you
// write SearchQuery.builder().text("clean").onlyAvailable().build().
public record SearchQuery(String text, boolean onlyAvailable, Optional<MediaKind> kind) {

    public static Builder builder() {
        return new Builder();
    }

    public static SearchQuery all() {
        return builder().build();
    }

    public static class Builder {

        private String text = "";
        private boolean onlyAvailable = false;
        private MediaKind kind = null;

        public Builder text(String text) {
            this.text = text == null ? "" : text;
            return this;
        }

        public Builder onlyAvailable() {
            this.onlyAvailable = true;
            return this;
        }

        public Builder kind(MediaKind kind) {
            this.kind = kind;
            return this;
        }

        public SearchQuery build() {
            return new SearchQuery(text, onlyAvailable, Optional.ofNullable(kind));
        }
    }
}
