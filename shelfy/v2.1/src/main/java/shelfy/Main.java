package shelfy;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Arrays;
import java.util.Map;

// shelfy 2.1: the console program as a Maven project (Day 3). Build and run:
//   ./mvnw verify
//   java -cp target/classes shelfy.Main                      (menu in a loop, data in shelfy.txt)
//   java -cp target/classes shelfy.Main --storage memory     (menu, nothing is saved)
//   java -cp target/classes shelfy.Main list                 (one command, then exit; used by the check scripts)
//   java -cp target/classes shelfy.Main add "Clean Code" "Robert C. Martin" 9780132350884
//   java -cp target/classes shelfy.Main add "The Pragmatic Programmer" "David Thomas" 9780135957059 audiobook 720
//   java -cp target/classes shelfy.Main member "Ada Lovelace"
//   java -cp target/classes shelfy.Main borrow 1 1 [yyyy-mm-dd]
//   java -cp target/classes shelfy.Main return 1 [yyyy-mm-dd]
//   java -cp target/classes shelfy.Main fees 2026-11-20
//   java -cp target/classes shelfy.Main available
//   java -cp target/classes shelfy.Main overdue 2026-11-20
//   java -cp target/classes shelfy.Main stats
//   java -cp target/classes shelfy.Main search clean
public class Main {

    public static final String VERSION = "2.1.0";

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
            command = Arrays.copyOfRange(args, 2, args.length);
        }
        Storage storage = switch (storageKind) {
            case "memory" -> new InMemoryStorage();
            case "file" -> new FileStorage(Path.of(FileStorage.DEFAULT_FILE));
            default -> {
                System.out.println("Unknown storage: " + storageKind + " (use memory or file)");
                System.exit(2);
                yield null;
            }
        };
        Main main = new Main(new Library(storage));
        if (command.length == 0 || command[0].equals("console")) {
            main.console();
        } else {
            main.run(command, false);
        }
    }

    private void console() {
        System.out.println("shelfy " + VERSION + " (storage: " + library.storage().name() + ")");
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
        System.out.println("Commands: add, list, member <name>, borrow <mediumNr> <memberNr>, return <mediumNr>, fees <yyyy-mm-dd>,");
        System.out.println("          available, overdue <yyyy-mm-dd>, stats, search <text>, help, quit");
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
                case "available" -> available();
                case "overdue" -> overdue(words);
                case "stats" -> stats();
                case "search" -> search(words);
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
        List<Medium> media = library.media();
        var borrowed = library.borrowedIds();
        for (Medium m : media) {
            System.out.println(m + " [" + (borrowed.contains(m.id()) ? "borrowed" : "available") + "]");
        }
        System.out.println(media.size() + " media, " + borrowed.size() + " borrowed");
        List<Member> members = library.members();
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
        String name = String.join(" ", Arrays.copyOfRange(words, 1, words.length));
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
        System.out.println("Borrowed: " + titleOf(loan) + ", due " + loan.due());
    }

    private void giveBack(String[] words) {
        if (words.length < 2) {
            System.out.println("Usage: return <mediumNr> [yyyy-mm-dd]");
            return;
        }
        LocalDate today = words.length >= 3 ? LocalDate.parse(words[2]) : LocalDate.now();
        Loan loan = library.giveBack(parseInt(words[1], "medium number"), today);
        System.out.println("Returned: " + titleOf(loan) + ", " + library.daysOverdue(loan, today)
                + " days overdue, fee " + money(library.lateFee(loan, today)));
    }

    // Fees for all open loans on the given date, so that you can test without waiting two weeks.
    private void fees(String[] words) {
        if (words.length < 2) {
            System.out.println("Usage: fees <yyyy-mm-dd>");
            return;
        }
        LocalDate date = LocalDate.parse(words[1]);
        List<Loan> open = library.loans().stream().filter(Loan::isOpen).toList();
        for (Loan loan : open) {
            System.out.println(describe(loan, date) + ", fee " + money(library.lateFee(loan, date)));
        }
        System.out.println(open.size() + " open loans on " + date);
    }

    private void available() {
        List<Medium> available = library.available();
        available.forEach(System.out::println);
        System.out.println(available.size() + " available");
    }

    private void overdue(String[] words) {
        if (words.length < 2) {
            System.out.println("Usage: overdue <yyyy-mm-dd>");
            return;
        }
        LocalDate date = LocalDate.parse(words[1]);
        List<Loan> overdue = library.overdue(date);
        for (Loan loan : overdue) {
            System.out.println(describe(loan, date));
        }
        System.out.println(overdue.size() + " overdue on " + date);
    }

    private void stats() {
        Library.Stats stats = library.stats(LocalDate.now());
        System.out.println("media: " + stats.media());
        System.out.println("open loans: " + stats.openLoans());
        System.out.println("overdue loans: " + stats.overdueLoans());
        for (Map.Entry<Integer, Long> e : stats.openLoansPerMember().entrySet()) {
            String name = library.findMember(e.getKey()).map(Member::name).orElse("member " + e.getKey());
            System.out.println("  " + name + ": " + e.getValue());
        }
    }

    private void search(String[] words) {
        if (words.length < 2) {
            System.out.println("Usage: search <text>");
            return;
        }
        String text = String.join(" ", Arrays.copyOfRange(words, 1, words.length));
        List<Medium> found = library.search(text);
        found.forEach(System.out::println);
        System.out.println(found.size() + " found for \"" + text + "\"");
    }

    private String describe(Loan loan, LocalDate date) {
        String member = library.findMember(loan.memberId()).map(Member::name).orElse("member " + loan.memberId());
        return "Loan " + loan.id() + ": " + titleOf(loan) + " (" + kindOf(loan) + "), member " + member
                + ", due " + loan.due() + ", " + LoanStatus.describe(loan.status(date));
    }

    private String titleOf(Loan loan) {
        return library.findMedium(loan.mediumId()).map(Medium::title).orElse("medium " + loan.mediumId());
    }

    private String kindOf(Loan loan) {
        return library.findMedium(loan.mediumId()).map(m -> m.kind().displayName()).orElse("?");
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
