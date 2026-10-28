// shelfy 1.0: a console library in one compact source file (Day 1, Module 03).
// Run it with:  java Shelfy.java
// Everything here uses only what Modules 01 and 02 teach: variables, strings, if and switch,
// loops, methods and arrays. The books live in three parallel arrays and are gone when you quit.

final int CAPACITY = 20;

String[] titles = new String[CAPACITY];
String[] authors = new String[CAPACITY];
boolean[] borrowed = new boolean[CAPACITY];
int count = 0;

void main() {
    addBook("Clean Code", "Robert C. Martin");
    addBook("The Pragmatic Programmer", "David Thomas");
    addBook("Effective Java", "Joshua Bloch");

    IO.println("shelfy 1.0");
    printMenu();
    boolean running = true;
    while (running) {
        String line = IO.readln("> ");
        if (line == null) {
            break;
        }
        line = line.trim();
        if (line.isEmpty()) {
            continue;
        }
        String[] parts = line.split(" ");
        String command = parts[0];
        switch (command) {
            case "add" -> add();
            case "list" -> list();
            case "borrow" -> borrow(parts);
            case "return" -> giveBack(parts);
            case "help" -> printMenu();
            case "quit" -> running = false;
            default -> {
                IO.println("Unknown command: " + command);
                printMenu();
            }
        }
    }
    IO.println("Bye.");
}

void printMenu() {
    IO.println("Commands: add, list, borrow <nr>, return <nr>, help, quit");
}

void addBook(String title, String author) {
    if (count == CAPACITY) {
        IO.println("The shelf is full (" + CAPACITY + " books).");
        return;
    }
    titles[count] = title;
    authors[count] = author;
    borrowed[count] = false;
    count++;
}

void add() {
    String title = IO.readln("Title: ");
    String author = IO.readln("Author: ");
    if (title == null || author == null || title.isBlank() || author.isBlank()) {
        IO.println("Title and author must not be empty.");
        return;
    }
    addBook(title.trim(), author.trim());
    IO.println("Added book " + count + ": " + title.trim());
}

void list() {
    for (int i = 0; i < count; i++) {
        String state = "available";
        if (borrowed[i]) {
            state = "borrowed";
        }
        IO.println((i + 1) + ". " + titles[i] + " by " + authors[i] + " [" + state + "]");
    }
    IO.println(count + " books, " + borrowedCount() + " borrowed");
}

int borrowedCount() {
    int n = 0;
    for (int i = 0; i < count; i++) {
        if (borrowed[i]) {
            n++;
        }
    }
    return n;
}

// Returns the array index for a book number typed by the user, or -1 if there is no such book.
int indexOf(String[] parts) {
    if (parts.length < 2 || !isNumber(parts[1])) {
        IO.println("No such book");
        return -1;
    }
    int number = Integer.parseInt(parts[1]);
    if (number < 1 || number > count) {
        IO.println("No such book");
        return -1;
    }
    return number - 1;
}

boolean isNumber(String text) {
    if (text.isEmpty()) {
        return false;
    }
    for (int i = 0; i < text.length(); i++) {
        if (!Character.isDigit(text.charAt(i))) {
            return false;
        }
    }
    return true;
}

void borrow(String[] parts) {
    int index = indexOf(parts);
    if (index < 0) {
        return;
    }
    if (borrowed[index]) {
        IO.println("Already borrowed");
        return;
    }
    borrowed[index] = true;
    IO.println("Borrowed: " + titles[index]);
}

void giveBack(String[] parts) {
    int index = indexOf(parts);
    if (index < 0) {
        return;
    }
    if (!borrowed[index]) {
        IO.println("Not borrowed");
        return;
    }
    borrowed[index] = false;
    IO.println("Returned: " + titles[index]);
}
