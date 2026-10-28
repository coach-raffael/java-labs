package shelfy;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Locale;

// shelfy 2.0: the console program (Day 2). Compile and run:
//   javac -d out src/shelfy/*.java
//   java -cp out shelfy.Main                       (menu in a loop, data in shelfy.txt)
//   java -cp out shelfy.Main --storage memory      (menu, nothing is saved)
//   java -cp out shelfy.Main list                  (one command, then exit; used by the check scripts)
//   java -cp out shelfy.Main add "Clean Code" "Robert C. Martin" 9780132350884
//   java -cp out shelfy.Main add "The Pragmatic Programmer" "David Thomas" 9780135957059 audiobook 720
//   java -cp out shelfy.Main member "Ada Lovelace"
//   java -cp out shelfy.Main borrow 1 1 [yyyy-mm-dd]
//   java -cp out shelfy.Main return 1 [yyyy-mm-dd]
//   java -cp out shelfy.Main fees 2026-11-20
public class Main {

    private final Library library;

    public Main(Library library) {
        this.library = library;
    }

    public static void main(String[] args) {
        // Optional first pair of arguments: --storage memory|file (default: file)
        String storageKind = "file";
        String[] command = args;
        if (args.length >= 2 && args[0].equals("--storage")) {
            storageKind = args[1];
            command = new String[args.length - 2];
            for (int i = 2; i < args.length; i++) {
                command[i - 2] = args[i];
            }
        }
        Storage storage;
        if (storageKind.equals("memory")) {
            storage = new InMemoryStorage();
        } else if (storageKind.equals("file")) {
            storage = new FileStorage(Path.of(FileStorage.DEFAULT_FILE));
        } else {
            System.out.println("Unknown storage: " + storageKind + " (use memory or file)");
            return;
        }
        Main main = new Main(new Library(storage));
        if (command.length == 0 || command[0].equals("console")) {
            main.console();
        } else {
            main.run(command, false);
        }
    }

    private void console() {
        System.out.println("shelfy 2.0 (storage: " + library.storage().name() + ")");
        printMenu();
        while (true) {
            String line = IO.readln("> ");
            if (line == null) {
                break;
            }
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.equals("quit")) {
                break;
            }
            if (line.equals("help")) {
                printMenu();
                continue;
            }
            run(line.split(" "), true);
        }
        System.out.println("Bye.");
    }

    private void printMenu() {
        System.out.println("Commands: add, list, member <name>, borrow <mediumNr> <memberNr>, return <mediumNr>, fees <yyyy-mm-dd>, help, quit");
    }

    // One command. In the console, "add" asks for its values; on the command line they are arguments.
    // Every exception from the library is caught here and shown as one line (Module 06.4).
    private void run(String[] words, boolean interactive) {
        try {
            switch (words[0]) {
                case "list" -> list();
                case "add" -> {
                    if (interactive) {
                        addInteractive();
                    } else {
                        addFromArgs(words);
                    }
                }
                case "member" -> addMember(words);
                case "borrow" -> borrow(words);
                case "return" -> giveBack(words);
                case "fees" -> fees(words);
                default -> {
                    System.out.println("Unknown command: " + words[0]);
                    printMenu();
                }
            }
        } catch (NoSuchMediumException | NoSuchMemberException | NoSuchLoanException
                 | BookNotAvailableException | IllegalArgumentException e) {
            System.out.println(e.getMessage());
        } catch (DateTimeParseException e) {
            System.out.println("Not a date (use yyyy-mm-dd): " + e.getParsedString());
        } catch (StorageException e) {
            System.out.println("Storage problem: " + e.getMessage());
        }
    }

    private void list() {
        ArrayList<Medium> media = library.media();
        int borrowed = 0;
        for (Medium m : media) {
            boolean available = library.isAvailable(m.id());
            if (!available) {
                borrowed++;
            }
            System.out.println(m + " [" + (available ? "available" : "borrowed") + "]");
        }
        System.out.println(media.size() + " media, " + borrowed + " borrowed");
        ArrayList<Member> members = library.members();
        for (Member m : members) {
            System.out.println("Member " + m);
        }
        System.out.println(members.size() + " members");
    }

    private void addInteractive() {
        String title = IO.readln("Title: ");
        String author = IO.readln("Author: ");
        String isbn = IO.readln("ISBN: ");
        String kind = IO.readln("Kind (book or audiobook, default book): ");
        if (title == null || author == null || isbn == null || kind == null) {
            return;
        }
        Medium stored;
        if (kind.trim().equalsIgnoreCase("audiobook")) {
            String minutes = IO.readln("Minutes: ");
            if (minutes == null) {
                return;
            }
            stored = library.addAudiobook(title, author, isbn, parseInt(minutes, "minutes"));
        } else {
            stored = library.addBook(title, author, isbn);
        }
        System.out.println("Added: " + stored);
    }

    private void addFromArgs(String[] words) {
        if (words.length < 4) {
            System.out.println("Usage: add <title> <author> <isbn> [audiobook <minutes>]");
            return;
        }
        Medium stored;
        if (words.length >= 6 && words[4].equalsIgnoreCase("audiobook")) {
            stored = library.addAudiobook(words[1], words[2], words[3], parseInt(words[5], "minutes"));
        } else {
            stored = library.addBook(words[1], words[2], words[3]);
        }
        System.out.println("Added: " + stored);
    }

    private void addMember(String[] words) {
        if (words.length < 2) {
            System.out.println("Usage: member <name>");
            return;
        }
        String name = words[1];
        for (int i = 2; i < words.length; i++) {
            name = name + " " + words[i];
        }
        Member stored = library.addMember(name);
        System.out.println("Added member: " + stored);
    }

    private void borrow(String[] words) {
        if (words.length < 3) {
            System.out.println("Usage: borrow <mediumNr> <memberNr> [yyyy-mm-dd]");
            return;
        }
        LocalDate today = words.length >= 4 ? LocalDate.parse(words[3]) : LocalDate.now();
        Loan loan = library.borrow(parseInt(words[1], "medium number"), parseInt(words[2], "member number"), today);
        Medium medium = library.findMedium(loan.mediumId());
        System.out.println("Borrowed: " + medium.title() + ", due " + loan.due());
    }

    private void giveBack(String[] words) {
        if (words.length < 2) {
            System.out.println("Usage: return <mediumNr> [yyyy-mm-dd]");
            return;
        }
        LocalDate today = words.length >= 3 ? LocalDate.parse(words[2]) : LocalDate.now();
        Loan loan = library.giveBack(parseInt(words[1], "medium number"), today);
        Medium medium = library.findMedium(loan.mediumId());
        double fee = library.lateFee(loan, today);
        System.out.println("Returned: " + medium.title() + ", " + library.daysOverdue(loan, today)
                + " days overdue, fee " + money(fee));
    }

    // Fees for all open loans on the given date, so that you can test without waiting two weeks.
    private void fees(String[] words) {
        if (words.length < 2) {
            System.out.println("Usage: fees <yyyy-mm-dd>");
            return;
        }
        LocalDate date = LocalDate.parse(words[1]);
        int open = 0;
        for (Loan loan : library.loans()) {
            if (!loan.isOpen()) {
                continue;
            }
            open++;
            Medium medium = library.findMedium(loan.mediumId());
            Member member = library.findMember(loan.memberId());
            System.out.println("Loan " + loan.id() + ": " + medium.title() + " (" + medium.kind().displayName()
                    + "), member " + member.name() + ", due " + loan.due() + ", "
                    + LoanStatus.describe(loan.status(date)) + ", fee " + money(library.lateFee(loan, date)));
        }
        System.out.println(open + " open loans on " + date);
    }

    private static int parseInt(String text, String what) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Not a number for " + what + ": " + text);
        }
    }

    // Always a dot as decimal separator, whatever language your machine speaks (Module 01.6 box).
    static String money(double amount) {
        return String.format(Locale.ROOT, "%.2f", amount);
    }
}
