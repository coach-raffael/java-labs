// Lab Module 03: shelfy 1.0, a console library.
// Starter file. Write the program here, then run:   java Shelfy.java
// Check your work from the repository root with:    ./checks/day1.sh
//
// Acceptance criteria
//
// Part A, the library
//   1. Three arrays with room for 20 books: titles (String), authors (String), borrowed (boolean),
//      and a counter "count" for the number of books on the shelf.
//   2. Three books are on the shelf when the program starts: "Clean Code" by Robert C. Martin,
//      "The Pragmatic Programmer" by David Thomas, "Effective Java" by Joshua Bloch (in this order).
//   3. The program prints "shelfy 1.0", then the command list exactly as
//      Commands: add, list, borrow <nr>, return <nr>, help, quit
//      and reads commands in a loop until "quit" (or until the input ends).
//   4. "add" asks for a title and an author (prompts "Title: " and "Author: ") and adds the book.
//   5. "list" prints one line per book:  <number>. <title> by <author> [available]
//      or [borrowed] instead of [available]. Numbers start at 1. After the last book it prints
//      a summary line:  <count> books, <n> borrowed
//   6. "borrow <nr>" marks the book as borrowed and prints "Borrowed: <title>".
//   7. "return <nr>" marks the book as available again and prints "Returned: <title>".
//
// Part B, robust input
//   8. An unknown command prints "Unknown command: <word>" and shows the command list again.
//   9. "borrow" of a borrowed book prints "Already borrowed".
//  10. A number below 1, above the number of books, not a number at all, or no number at all
//      ("borrow" alone) prints "No such book".
//  11. No exception ends the program: wrong input prints a message, the loop goes on.
//
// Hints: IO.readln returns null when the input ends (that is what the check script sends at the end).
// To tell whether a String is a number without an exception, look at each character
// (charAt, Module 01) in a loop (Module 02): Character.isDigit(c) tells you whether c is a digit.

void main() {
    // your code
}
