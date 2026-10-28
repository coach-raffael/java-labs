package shelfy;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;

// Keeps the data in a text file, one line per record (Module 06.6 and 06.7):
//   MEDIUM;1;BOOK;Clean Code;Robert C. Martin;9780132350884;0
//   MEDIUM;2;AUDIOBOOK;The Pragmatic Programmer;David Thomas;9780135957059;720
//   MEMBER;1;Ada Lovelace
//   LOAN;1;2;1;2026-10-27;2026-11-10;
// The last field of a LOAN line is the return date and stays empty while the loan is open.
// FileStorage does not inherit from InMemoryStorage; it holds one and writes the file after
// every change (composition, Module 05.7).
public class FileStorage implements Storage {

    public static final String DEFAULT_FILE = "shelfy.txt";

    private final Path file;
    private final InMemoryStorage memory = new InMemoryStorage();

    public FileStorage(Path file) {
        this.file = file;
        load();
    }

    @Override
    public String name() {
        return "file";
    }

    @Override
    public ArrayList<Medium> media() {
        return memory.media();
    }

    @Override
    public ArrayList<Member> members() {
        return memory.members();
    }

    @Override
    public ArrayList<Loan> loans() {
        return memory.loans();
    }

    @Override
    public Medium addMedium(Medium medium) {
        Medium stored = memory.addMedium(medium);
        save();
        return stored;
    }

    @Override
    public Member addMember(Member member) {
        Member stored = memory.addMember(member);
        save();
        return stored;
    }

    @Override
    public Loan addLoan(Loan loan) {
        Loan stored = memory.addLoan(loan);
        save();
        return stored;
    }

    @Override
    public void updateLoan(Loan loan) {
        memory.updateLoan(loan);
        save();
    }

    private void load() {
        ArrayList<String> lines;
        try {
            lines = new ArrayList<String>(Files.readAllLines(file));
        } catch (NoSuchFileException e) {
            // First start: no file yet means an empty library, not an error.
            return;
        } catch (IOException e) {
            throw new StorageException("cannot read " + file, e);
        }
        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            String[] f = line.split(";", -1);
            switch (f[0]) {
                case "MEDIUM" -> memory.restoreMedium(parseMedium(f));
                case "MEMBER" -> memory.restoreMember(new Member(Integer.parseInt(f[1]), f[2]));
                case "LOAN" -> memory.restoreLoan(parseLoan(f));
                default -> throw new StorageException("unknown line in " + file + ": " + line, null);
            }
        }
    }

    private static Medium parseMedium(String[] f) {
        int id = Integer.parseInt(f[1]);
        MediaKind kind = MediaKind.valueOf(f[2]);
        return switch (kind) {
            case BOOK -> new Book(id, f[3], f[4], f[5]);
            case AUDIOBOOK -> new Audiobook(id, f[3], f[4], f[5], Integer.parseInt(f[6]));
        };
    }

    private static Loan parseLoan(String[] f) {
        LocalDate returnedOn = f[6].isEmpty() ? null : LocalDate.parse(f[6]);
        return new Loan(Integer.parseInt(f[1]), Integer.parseInt(f[2]), Integer.parseInt(f[3]),
                LocalDate.parse(f[4]), LocalDate.parse(f[5]), returnedOn);
    }

    private void save() {
        try (BufferedWriter out = Files.newBufferedWriter(file)) {
            for (Medium m : memory.media()) {
                out.write("MEDIUM;" + m.id() + ";" + m.kind() + ";" + m.title() + ";" + m.author() + ";"
                        + m.isbn() + ";" + m.durationMinutes());
                out.newLine();
            }
            for (Member m : memory.members()) {
                out.write("MEMBER;" + m.id() + ";" + m.name());
                out.newLine();
            }
            for (Loan l : memory.loans()) {
                String returned = l.returnedOn() == null ? "" : l.returnedOn().toString();
                out.write("LOAN;" + l.id() + ";" + l.mediumId() + ";" + l.memberId() + ";" + l.borrowedOn() + ";"
                        + l.due() + ";" + returned);
                out.newLine();
            }
        } catch (IOException e) {
            throw new StorageException("cannot write " + file, e);
        }
    }
}
