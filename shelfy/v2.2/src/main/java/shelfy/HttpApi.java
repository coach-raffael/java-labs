package shelfy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

// The HTTP side of shelfy (Modules 13 and 14), built on the JDK's own server (module jdk.httpserver).
// Five endpoints: GET /health, GET /books, POST /books, POST /loans, PATCH /loans/<id>.
// Every handler returns a Response; the wrapper writes it, logs one line per request and turns
// exceptions into status codes. Each request runs on its own virtual thread (Module 13.6).
public class HttpApi {

    private static final Logger LOG = Logger.getLogger("shelfy.http");
    private static final String JSON = "application/json";

    private final Library library;
    private final Json json;
    private final String version;
    private final Clock clock;
    private HttpServer server;

    public HttpApi(Library library, Json json, String version, Clock clock) {
        this.library = library;
        this.json = json;
        this.version = version;
        this.clock = clock;
    }

    // Port 0 means: any free port; port() tells you which one (the tests use that).
    public void start(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.createContext("/health", wrap(this::health));
        server.createContext("/books", wrap(this::books));
        server.createContext("/loans", wrap(this::loans));
        server.createContext("/", wrap(exchange -> notFound()));
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
    }

    public int port() {
        return server.getAddress().getPort();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    // What a handler produces: status, JSON body, extra headers (Location, Allow).
    record Response(int status, String body, Map<String, String> headers) {

        static Response of(int status, String body) {
            return new Response(status, body, Map.of());
        }

        Response withHeader(String name, String value) {
            Map<String, String> all = new HashMap<>(headers);
            all.put(name, value);
            return new Response(status, body, all);
        }
    }

    @FunctionalInterface
    interface Handler {
        Response handle(HttpExchange exchange) throws IOException;
    }

    private HttpHandler wrap(Handler handler) {
        return exchange -> {
            long started = System.nanoTime();
            Response response;
            try {
                response = handler.handle(exchange);
            } catch (BadRequestException | IllegalArgumentException e) {
                response = error(400, e.getMessage());
            } catch (NoSuchMediumException | NoSuchMemberException | NoSuchLoanException e) {
                response = error(404, e.getMessage());
            } catch (BookNotAvailableException | AlreadyReturnedException | DuplicateIsbnException e) {
                response = error(409, e.getMessage());
            } catch (StorageException e) {
                LOG.log(Level.SEVERE, "storage failed: " + e.getMessage(), e);
                response = error(500, "storage failed: " + e.getMessage());
            } catch (RuntimeException e) {
                LOG.log(Level.SEVERE, "unexpected error", e);
                response = error(500, "unexpected error: " + e);
            }
            send(exchange, response);
            long millis = (System.nanoTime() - started) / 1_000_000;
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            LOG.log(Level.INFO, method + " " + path + " " + response.status() + " " + millis + "ms",
                    new Object[] {new Logging.RequestLog(method, path, response.status(), millis)});
        };
    }

    // Writes the response and closes the body: a forgotten close keeps the client waiting (Module 13.2).
    private static void send(HttpExchange exchange, Response response) throws IOException {
        byte[] bytes = response.body().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", JSON);
        response.headers().forEach((name, value) -> exchange.getResponseHeaders().set(name, value));
        exchange.sendResponseHeaders(response.status(), bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private Response error(int status, String message) {
        return Response.of(status, json.write(new Views.ErrorView(message)));
    }

    private Response notFound() {
        return error(404, "not found");
    }

    private Response methodNotAllowed(String allow) {
        return error(405, "method not allowed").withHeader("Allow", allow);
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    // GET /health: 200 with the counters, 503 when the storage does not answer.
    Response health(HttpExchange exchange) {
        if (!exchange.getRequestURI().getPath().equals("/health")) {
            return notFound();
        }
        if (!exchange.getRequestMethod().equals("GET")) {
            return methodNotAllowed("GET");
        }
        String storage = library.storage().name();
        try {
            Library.Stats stats = library.stats(today());
            return Response.of(200, json.write(new Views.Health("ok", version, storage, stats.media(),
                    stats.openLoans(), stats.overdueLoans())));
        } catch (StorageException e) {
            LOG.log(Level.WARNING, "health check failed: " + e.getMessage());
            return Response.of(503, json.write(new Views.HealthError("error", version, storage, "database unreachable")));
        }
    }

    // GET /books[?available=true|false], POST /books
    Response books(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestURI().getPath().equals("/books")) {
            return notFound();
        }
        return switch (exchange.getRequestMethod()) {
            case "GET" -> listBooks(queryParams(exchange).get("available"));
            case "POST" -> createBook(body(exchange));
            default -> methodNotAllowed("GET, POST");
        };
    }

    private Response listBooks(String available) {
        Boolean onlyAvailable = null;
        if (available != null) {
            if (!available.equals("true") && !available.equals("false")) {
                throw new BadRequestException("available must be true or false, not " + available);
            }
            onlyAvailable = Boolean.valueOf(available);
        }
        Set<Integer> borrowed = library.borrowedIds();
        List<Views.MediumView> views = library.media().stream()
                .map(m -> Views.MediumView.of(m, !borrowed.contains(m.id())))
                .toList();
        if (onlyAvailable != null) {
            boolean wanted = onlyAvailable;
            views = views.stream().filter(v -> v.available() == wanted).toList();
        }
        return Response.of(200, json.write(views));
    }

    private Response createBook(String body) {
        Views.NewBook request = json.read(body, Views.NewBook.class);
        if (request.title() == null || request.title().isBlank()) {
            throw new BadRequestException("title is required");
        }
        if (request.title().trim().length() > 200) {
            throw new BadRequestException("title must have at most 200 characters");
        }
        if (request.author() == null || request.author().isBlank()) {
            throw new BadRequestException("author is required");
        }
        if (request.isbn() == null || request.isbn().isBlank()) {
            throw new BadRequestException("isbn is required");
        }
        String kind = request.kind() == null || request.kind().isBlank() ? "BOOK" : request.kind().trim().toUpperCase();
        Medium stored = switch (kind) {
            case "BOOK" -> library.addBook(request.title(), request.author(), request.isbn());
            case "AUDIOBOOK" -> {
                if (request.durationMinutes() == null || request.durationMinutes() <= 0) {
                    throw new BadRequestException("durationMinutes is required for an AUDIOBOOK");
                }
                yield library.addAudiobook(request.title(), request.author(), request.isbn(), request.durationMinutes());
            }
            default -> throw new BadRequestException("kind must be BOOK or AUDIOBOOK, not " + request.kind());
        };
        return Response.of(201, json.write(Views.MediumView.of(stored, true)))
                .withHeader("Location", "/books/" + stored.id());
    }

    // POST /loans, PATCH /loans/<id>
    Response loans(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        if (path.equals("/loans")) {
            return method.equals("POST") ? createLoan(body(exchange)) : methodNotAllowed("POST");
        }
        if (path.startsWith("/loans/") && path.indexOf('/', "/loans/".length()) < 0) {
            String rest = path.substring("/loans/".length());
            int loanId;
            try {
                loanId = Integer.parseInt(rest);
            } catch (NumberFormatException e) {
                return notFound();
            }
            return method.equals("PATCH") ? returnLoan(loanId, body(exchange)) : methodNotAllowed("PATCH");
        }
        return notFound();
    }

    private Response createLoan(String body) {
        Views.NewLoan request = json.read(body, Views.NewLoan.class);
        if (request.bookId() == null) {
            throw new BadRequestException("bookId is required");
        }
        if (request.memberId() == null) {
            throw new BadRequestException("memberId is required");
        }
        LocalDate today = today();
        Loan loan = library.borrow(request.bookId(), request.memberId(), today);
        return Response.of(201, json.write(Views.LoanView.of(loan, today)))
                .withHeader("Location", "/loans/" + loan.id());
    }

    private Response returnLoan(int loanId, String body) {
        Views.ReturnRequest request = json.read(body, Views.ReturnRequest.class);
        if (!Boolean.TRUE.equals(request.returned())) {
            throw new BadRequestException("body must be {\"returned\": true}");
        }
        LocalDate today = today();
        Loan returned = library.returnLoan(loanId, today);
        return Response.of(200, json.write(Views.ReturnedLoanView.of(returned,
                library.daysOverdue(returned, today), library.lateFee(returned, today))));
    }

    private static String body(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    // ?available=true&x=y  ->  {available=true, x=y}
    static Map<String, String> queryParams(HttpExchange exchange) {
        Map<String, String> params = new HashMap<>();
        String query = exchange.getRequestURI().getQuery();
        if (query == null || query.isEmpty()) {
            return params;
        }
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq < 0) {
                params.put(pair, "");
            } else {
                params.put(pair.substring(0, eq), pair.substring(eq + 1));
            }
        }
        return params;
    }
}
