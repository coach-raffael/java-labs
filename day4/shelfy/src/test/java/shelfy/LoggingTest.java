package shelfy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.junit.jupiter.api.Test;

// The two formatters of Module 14.6: one line each, JSON carries the request fields.
class LoggingTest {

    @Test
    void textFormatIsOneLineWithTimeLevelLoggerAndMessage() {
        LogRecord record = new LogRecord(Level.INFO, "GET /health 200 3ms");
        record.setLoggerName("shelfy.http");
        record.setInstant(Instant.EPOCH);

        String line = new Logging.TextFormatter().format(record);

        assertEquals("1970-01-01T00:00:00Z INFO shelfy.http GET /health 200 3ms" + System.lineSeparator(), line);
    }

    @Test
    void jsonFormatCarriesMethodPathStatusAndMillis() {
        LogRecord record = new LogRecord(Level.INFO, "GET /health 200 3ms");
        record.setLoggerName("shelfy.http");
        record.setInstant(Instant.EPOCH);
        record.setParameters(new Object[] {new Logging.RequestLog("GET", "/health", 200, 3)});

        String line = new Logging.JsonFormatter().format(record);

        assertEquals("{\"time\":\"1970-01-01T00:00:00Z\",\"level\":\"INFO\",\"logger\":\"shelfy.http\","
                + "\"message\":\"GET /health 200 3ms\",\"method\":\"GET\",\"path\":\"/health\",\"status\":200,\"ms\":3}"
                + System.lineSeparator(), line);
    }

    @Test
    void jsonFormatEscapesQuotesInTheMessage() {
        LogRecord record = new LogRecord(Level.WARNING, "said \"hello\"");
        record.setLoggerName("shelfy");

        String line = new Logging.JsonFormatter().format(record);

        assertTrue(line.contains("\"message\":\"said \\\"hello\\\"\""));
    }
}
