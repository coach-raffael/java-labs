package shelfy;

import java.io.PrintStream;
import java.time.Instant;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

// java.util.logging set up for a server (Module 14.6): everything goes to stdout, one line per
// record, as plain text or as JSON. LOG_LEVEL and LOG_FORMAT come from Config.
public final class Logging {

    private Logging() {
    }

    public static void configure(String level, String format) {
        Logger root = Logger.getLogger("");
        for (Handler handler : root.getHandlers()) {
            root.removeHandler(handler);
        }
        Formatter formatter = format.equals("json") ? new JsonFormatter() : new TextFormatter();
        Handler handler = new StdoutHandler(System.out, formatter);
        Level parsed = Level.parse(level);
        handler.setLevel(parsed);
        root.addHandler(handler);
        root.setLevel(parsed);
    }

    // The JDK's ConsoleHandler writes to stderr; a server writes its log to stdout and flushes every line.
    static class StdoutHandler extends StreamHandler {

        StdoutHandler(PrintStream out, Formatter formatter) {
            super(out, formatter);
        }

        @Override
        public synchronized void publish(LogRecord record) {
            super.publish(record);
            flush();
        }
    }

    // 2026-10-28T09:15:02Z INFO shelfy.http GET /health 200 3ms
    static class TextFormatter extends Formatter {

        @Override
        public String format(LogRecord record) {
            String thrown = record.getThrown() == null ? "" : " " + record.getThrown();
            return Instant.ofEpochMilli(record.getMillis()) + " " + record.getLevel() + " " + record.getLoggerName()
                    + " " + formatMessage(record) + thrown + System.lineSeparator();
        }
    }

    // {"time":"2026-10-28T09:15:02Z","level":"INFO","logger":"shelfy.http","message":"GET /health 200 3ms"}
    // plus the fields that a RequestRecord carries (method, path, status, ms), see HttpApi.
    static class JsonFormatter extends Formatter {

        @Override
        public String format(LogRecord record) {
            StringBuilder json = new StringBuilder("{");
            field(json, "time", Instant.ofEpochMilli(record.getMillis()).toString());
            json.append(',');
            field(json, "level", record.getLevel().getName());
            json.append(',');
            field(json, "logger", record.getLoggerName());
            json.append(',');
            field(json, "message", formatMessage(record));
            if (record.getParameters() != null) {
                for (Object parameter : record.getParameters()) {
                    if (parameter instanceof RequestLog request) {
                        json.append(',');
                        field(json, "method", request.method());
                        json.append(',');
                        field(json, "path", request.path());
                        json.append(",\"status\":").append(request.status());
                        json.append(",\"ms\":").append(request.millis());
                    }
                }
            }
            if (record.getThrown() != null) {
                json.append(',');
                field(json, "exception", record.getThrown().toString());
            }
            return json.append('}').append(System.lineSeparator()).toString();
        }

        private static void field(StringBuilder json, String name, String value) {
            json.append('"').append(name).append("\":\"").append(escape(value)).append('"');
        }

        private static String escape(String text) {
            StringBuilder out = new StringBuilder();
            for (char c : text.toCharArray()) {
                switch (c) {
                    case '"' -> out.append("\\\"");
                    case '\\' -> out.append("\\\\");
                    case '\n' -> out.append("\\n");
                    case '\r' -> out.append("\\r");
                    case '\t' -> out.append("\\t");
                    default -> {
                        if (c < 0x20) {
                            out.append(String.format("\\u%04x", (int) c));
                        } else {
                            out.append(c);
                        }
                    }
                }
            }
            return out.toString();
        }
    }

    // One HTTP request, attached to its log record so that the JSON formatter can write the fields.
    public record RequestLog(String method, String path, int status, long millis) {
    }
}
