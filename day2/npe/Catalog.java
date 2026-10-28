// Module 04, exercise 3: this program ends with a NullPointerException. Run:  java Catalog.java
// Read the message from the top: it names the method that was called and the field that was null.
// Write down the cause in one sentence, then fix it in the constructor.
public class Catalog {

    public static void main(String[] args) {
        Book first = new Book("Clean Code", "Robert C. Martin");
        Book second = new Book(null, "David Thomas");
        System.out.println(first.label());
        System.out.println(second.label());
    }
}

class Book {

    private final String title;
    private final String author;

    Book(String title, String author) {
        this.title = title;
        this.author = author;
    }

    String label() {
        return title.toUpperCase() + " (" + title.length() + " characters) by " + author;
    }
}
