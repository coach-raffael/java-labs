package shelfy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

// shelfy.txt: what is written, and that a second FileStorage reads it back (Module 06.6).
class FileStorageTest {

    @TempDir
    Path dir;

    @Test
    void aMissingFileMeansAnEmptyLibrary() {
        FileStorage storage = new FileStorage(dir.resolve("none.txt"));

        assertTrue(storage.media().isEmpty());
        assertTrue(storage.members().isEmpty());
        assertTrue(storage.loans().isEmpty());
    }

    @Test
    void writesOneLinePerRecordAndReadsItBack() throws IOException {
        Path file = dir.resolve("shelfy.txt");
        Library library = new Library(new FileStorage(file));
        Medium book = library.addBook("Clean Code", "Robert C. Martin", "9780132350884");
        library.addAudiobook("The Pragmatic Programmer", "David Thomas", "9780135957059", 720);
        Member ada = library.addMember("Ada Lovelace");
        library.borrow(book.id(), ada.id(), LocalDate.of(2026, 1, 1));

        assertEquals(List.of(
                "MEDIUM;1;BOOK;Clean Code;Robert C. Martin;9780132350884;0",
                "MEDIUM;2;AUDIOBOOK;The Pragmatic Programmer;David Thomas;9780135957059;720",
                "MEMBER;1;Ada Lovelace",
                "LOAN;1;1;1;2026-01-01;2026-01-15;"), Files.readAllLines(file));

        Library reloaded = new Library(new FileStorage(file));
        assertEquals(2, reloaded.media().size());
        assertEquals(List.of(ada), reloaded.members());
        assertEquals(1, reloaded.loans().size());
        assertTrue(reloaded.loans().get(0).isOpen());
        assertEquals(720, reloaded.media().get(1).durationMinutes());
    }
}
