package shelfy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

// Modules 16 and 18: this class is red until you have found and fixed the planted mistakes.
// It does not need a database. Two of the eight mistakes have no test here: the compiler
// (-Xlint:all, see the pom) and 120 requests against PostgreSQL (Module 16, exercise 3) find them.
class BrokenTest {

    private static final LocalDate DAY_ONE = LocalDate.of(2026, 1, 1);

    @Test
    void theAvailableFilterLeavesOutBorrowedMedia() throws Exception {
        Library library = new Library(new InMemoryStorage(), new StandardPolicy(), 14);
        library.addBook("Clean Code", "Robert C. Martin", "9780132350884");
        library.addAudiobook("The Pragmatic Programmer", "David Thomas", "9780135957059", 720);
        Member ada = library.addMember("Ada Lovelace");
        library.borrow(1, ada.id(), DAY_ONE);
        HttpApi api = new HttpApi(library, new Json(), "test", Clock.systemUTC());
        api.start(0);
        try {
            HttpResponse<String> response = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + api.port() + "/books?available=true")).build(),
                    HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("\"id\":2"), response.body());
            assertFalse(response.body().contains("\"id\":1,"), "a borrowed medium is listed as available: " + response.body());
        } finally {
            api.stop();
        }
    }

    @Test
    void twoMembersWithTheSameNumberAreOneEntryInASet() {
        Set<Member> members = new HashSet<>();
        members.add(new Member(1, "Ada Lovelace"));
        members.add(new Member(1, "Ada Lovelace"));

        assertEquals(1, members.size(), "equal members must have equal hash codes");
    }

    @Test
    void aDatabaseThatDoesNotAnswerIsReportedNotHidden() {
        PostgresStorage storage = new PostgresStorage("jdbc:postgresql://127.0.0.1:1/none", "nobody", "nothing");

        assertThrows(StorageException.class, storage::media, "an empty list is not an answer to a broken database");
    }

    @Test
    void anUnknownMemberNumberDoesNotCrashTheProgram() {
        Library library = new Library(new InMemoryStorage(), new StandardPolicy(), 14);

        assertEquals("member 42", library.memberName(42));
    }

    @Test
    void twoLibrariesDoNotShareTheirCounters() {
        Library first = new Library(new InMemoryStorage(), new StandardPolicy(), 14);
        Library second = new Library(new InMemoryStorage(), new StandardPolicy(), 14);
        Medium book = first.addBook("Clean Code", "Robert C. Martin", "9780132350884");
        Member ada = first.addMember("Ada Lovelace");
        first.borrow(book.id(), ada.id(), DAY_ONE);

        assertEquals(1, first.stats(DAY_ONE).openLoans());
        assertEquals(0, second.stats(DAY_ONE).openLoans(), "the second library has no loans at all");
    }

    @Test
    void theLoanPeriodFromTheConfigurationIsUsed() {
        Library sevenDays = new Library(new InMemoryStorage(), new StandardPolicy(), 7);
        Medium book = sevenDays.addBook("Clean Code", "Robert C. Martin", "9780132350884");
        Member ada = sevenDays.addMember("Ada Lovelace");

        Loan loan = sevenDays.borrow(book.id(), ada.id(), DAY_ONE);

        assertEquals(DAY_ONE.plusDays(7), loan.due(), "LOAN_DAYS=7 must give a due date 7 days later");
    }
}
