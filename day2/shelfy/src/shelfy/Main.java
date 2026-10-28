package shelfy;

// shelfy 2.0: the console program. Starter with the menu loop; fill in the commands.
// Compile and run:
//   javac -d out src/shelfy/*.java
//   java -cp out shelfy.Main                 (menu in a loop)
//   java -cp out shelfy.Main list            (one command, then exit)
public class Main {

    public static void main(String[] args) {
        // TODO: read an optional "--storage memory|file" from the arguments, create the Storage
        //       and the Library, then either run the console loop or the single command in args.
        console();
    }

    private static void console() {
        System.out.println("shelfy 2.0");
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
            run(line.split(" "));
        }
        System.out.println("Bye.");
    }

    private static void printMenu() {
        System.out.println("Commands: add, list, member <name>, borrow <mediumNr> <memberNr>, return <mediumNr>, fees <yyyy-mm-dd>, help, quit");
    }

    // One command. Catch the exceptions of your Library here and print their message as one line,
    // so that a wrong input never ends the program (Module 06.4).
    private static void run(String[] words) {
        switch (words[0]) {
            case "list" -> System.out.println("TODO: list");
            case "add" -> System.out.println("TODO: add");
            case "member" -> System.out.println("TODO: member");
            case "borrow" -> System.out.println("TODO: borrow");
            case "return" -> System.out.println("TODO: return");
            case "fees" -> System.out.println("TODO: fees");
            default -> {
                System.out.println("Unknown command: " + words[0]);
                printMenu();
            }
        }
    }
}
