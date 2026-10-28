package shelfy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// The five endpoints, called over a real socket with the JDK's HttpClient (Module 13.9).
// The server starts on port 0 (any free port) with an InMemoryStorage and a fixed clock.
class HttpApiTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-01T12:00:00Z"), ZoneOffset.UTC);

    private HttpApi api;
    private Library library;
    private HttpClient client;
    private String base;

    @BeforeEach
    void start() throws IOException {
        library = new Library(new InMemoryStorage(), new StandardPolicy(), 14);
        api = new HttpApi(library, new Json(), "2.2.0-test", CLOCK);
        api.start(0);
        client = HttpClient.newHttpClient();
        base = "http://127.0.0.1:" + api.port();
    }

    @AfterEach
    void stop() {
        api.stop();
    }

    private HttpResponse<String> send(String method, String path, String body) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(base + path));
        if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.method(method, HttpRequest.BodyPublishers.ofString(body)).header("Content-Type", "application/json");
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void healthReportsStatusVersionStorageAndCounters() throws Exception {
        HttpResponse<String> response = send("GET", "/health", null);

        assertEquals(200, response.statusCode());
        assertEquals("application/json", response.headers().firstValue("Content-Type").orElse(""));
        assertEquals("{\"status\":\"ok\",\"version\":\"2.2.0-test\",\"storage\":\"memory\",\"books\":0,\"openLoans\":0,\"overdueLoans\":0}",
                response.body());
    }

    @Test
    void postBooksCreatesAMediumAndAnswers201WithLocation() throws Exception {
        HttpResponse<String> response = send("POST", "/books",
                "{\"title\":\"Clean Code\",\"author\":\"Robert C. Martin\",\"isbn\":\"9780132350884\"}");

        assertEquals(201, response.statusCode());
        assertEquals("/books/1", response.headers().firstValue("Location").orElse(""));
        assertEquals("{\"id\":1,\"kind\":\"BOOK\",\"title\":\"Clean Code\",\"author\":\"Robert C. Martin\","
                + "\"isbn\":\"9780132350884\",\"durationMinutes\":0,\"available\":true}", response.body());
        assertEquals(1, library.media().size());
    }

    @Test
    void postBooksValidatesTheRequest() throws Exception {
        assertEquals(400, send("POST", "/books", "{\"author\":\"x\",\"isbn\":\"1\"}").statusCode());
        assertEquals(400, send("POST", "/books", "{\"title\":\"x\",\"isbn\":\"1\"}").statusCode());
        assertEquals(400, send("POST", "/books", "{\"title\":\"x\",\"author\":\"y\",\"isbn\":\"1\",\"kind\":\"VINYL\"}").statusCode());
        assertEquals(400, send("POST", "/books", "{\"title\":\"x\",\"author\":\"y\",\"isbn\":\"1\",\"kind\":\"AUDIOBOOK\"}").statusCode());
        assertEquals(400, send("POST", "/books", "{\"title\":\"" + "x".repeat(201) + "\",\"author\":\"y\",\"isbn\":\"1\"}").statusCode());
        HttpResponse<String> notJson = send("POST", "/books", "not json");
        assertEquals(400, notJson.statusCode());
        assertTrue(notJson.body().startsWith("{\"error\":\"invalid JSON"));

        send("POST", "/books", "{\"title\":\"x\",\"author\":\"y\",\"isbn\":\"1\"}");
        assertEquals(409, send("POST", "/books", "{\"title\":\"x\",\"author\":\"y\",\"isbn\":\"1\"}").statusCode());
    }

    @Test
    void getBooksListsAndFiltersByAvailability() throws Exception {
        library.addBook("Clean Code", "Robert C. Martin", "9780132350884");
        library.addAudiobook("The Pragmatic Programmer", "David Thomas", "9780135957059", 720);
        Member ada = library.addMember("Ada Lovelace");
        library.borrow(1, ada.id(), java.time.LocalDate.now(CLOCK));

        HttpResponse<String> all = send("GET", "/books", null);
        assertEquals(200, all.statusCode());
        assertTrue(all.body().contains("\"id\":1") && all.body().contains("\"id\":2"));

        HttpResponse<String> available = send("GET", "/books?available=true", null);
        assertTrue(available.body().contains("\"id\":2"));
        assertTrue(!available.body().contains("\"id\":1,"));

        HttpResponse<String> borrowed = send("GET", "/books?available=false", null);
        assertTrue(borrowed.body().contains("\"id\":1,") && !borrowed.body().contains("\"id\":2"));

        assertEquals(400, send("GET", "/books?available=maybe", null).statusCode());
    }

    @Test
    void loansCanBeCreatedAndReturned() throws Exception {
        library.addBook("Clean Code", "Robert C. Martin", "9780132350884");
        library.addMember("Ada Lovelace");

        HttpResponse<String> created = send("POST", "/loans", "{\"bookId\":1,\"memberId\":1}");
        assertEquals(201, created.statusCode());
        assertEquals("/loans/1", created.headers().firstValue("Location").orElse(""));
        assertEquals("{\"id\":1,\"bookId\":1,\"memberId\":1,\"borrowedOn\":\"2026-01-01\",\"due\":\"2026-01-15\",\"status\":\"OPEN\"}",
                created.body());

        assertEquals(409, send("POST", "/loans", "{\"bookId\":1,\"memberId\":1}").statusCode());
        assertEquals(404, send("POST", "/loans", "{\"bookId\":9,\"memberId\":1}").statusCode());
        assertEquals(404, send("POST", "/loans", "{\"bookId\":1,\"memberId\":9}").statusCode());
        assertEquals(400, send("POST", "/loans", "{\"bookId\":1}").statusCode());

        assertEquals(400, send("PATCH", "/loans/1", "{\"returned\":false}").statusCode());
        HttpResponse<String> returned = send("PATCH", "/loans/1", "{\"returned\":true}");
        assertEquals(200, returned.statusCode());
        assertEquals("{\"id\":1,\"bookId\":1,\"memberId\":1,\"borrowedOn\":\"2026-01-01\",\"due\":\"2026-01-15\","
                + "\"returnedOn\":\"2026-01-01\",\"status\":\"RETURNED\",\"daysOverdue\":0,\"lateFee\":0.0}", returned.body());
        assertEquals(409, send("PATCH", "/loans/1", "{\"returned\":true}").statusCode());
        assertEquals(404, send("PATCH", "/loans/77", "{\"returned\":true}").statusCode());
        assertEquals(404, send("PATCH", "/loans/abc", "{\"returned\":true}").statusCode());
    }

    @Test
    void unknownPathsAre404AndWrongMethodsAre405WithAllow() throws Exception {
        assertEquals(404, send("GET", "/nothing", null).statusCode());
        assertEquals(404, send("GET", "/books/1", null).statusCode());
        assertEquals("{\"error\":\"not found\"}", send("GET", "/nothing", null).body());

        HttpResponse<String> delete = send("DELETE", "/books", null);
        assertEquals(405, delete.statusCode());
        assertEquals("GET, POST", delete.headers().firstValue("Allow").orElse(""));
        assertEquals("POST", send("GET", "/loans", null).headers().firstValue("Allow").orElse(""));
        assertEquals("PATCH", send("GET", "/loans/1", null).headers().firstValue("Allow").orElse(""));
        assertEquals(405, send("POST", "/health", "{}").statusCode());
    }

    @Test
    void manyRequestsAtOnceAreAllAnswered() throws Exception {
        library.addBook("Clean Code", "Robert C. Martin", "9780132350884");
        Thread[] threads = new Thread[20];
        int[] codes = new int[threads.length];
        for (int i = 0; i < threads.length; i++) {
            int n = i;
            threads[i] = Thread.ofVirtual().start(() -> {
                try {
                    codes[n] = send("GET", "/books", null).statusCode();
                } catch (Exception e) {
                    codes[n] = -1;
                }
            });
        }
        for (Thread t : threads) {
            t.join();
        }
        for (int code : codes) {
            assertEquals(200, code);
        }
    }
}
