// Module 01, exercise 1: one compiler error in this file. Run:  java WrongType.java
// Read the message, open the line it names, fix it, run again.
void main() {
    int lateDays = "5";
    double fee = lateDays * 0.5;
    IO.println("Late fee: " + fee);
}
