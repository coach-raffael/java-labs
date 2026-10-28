package shelfy;

import java.util.Map;
import java.util.logging.Level;

// Configuration from the environment (Module 14.5): every value has a default, so that the program
// starts with no variable set at all. The credentials are separate from the URL, so that the URL
// may appear in a log line and the password never does.
public record Config(int port, String databaseUrl, String databaseUser, String databasePassword, String dataFile,
                     String logLevel, String logFormat, int loanDays) {

    public static Config fromEnv() {
        return from(System.getenv());
    }

    // Reads from any map, which makes the parsing testable without touching the real environment.
    public static Config from(Map<String, String> env) {
        return new Config(
                intValue(env, "PORT", 8080),
                text(env, "DATABASE_URL", ""),
                text(env, "DATABASE_USER", ""),
                text(env, "DATABASE_PASSWORD", ""),
                text(env, "DATA_FILE", FileStorage.DEFAULT_FILE),
                text(env, "LOG_LEVEL", "INFO").toUpperCase(),
                text(env, "LOG_FORMAT", "text").toLowerCase(),
                intValue(env, "LOAN_DAYS", Library.LOAN_DAYS));
    }

    public Config {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("PORT must be between 0 and 65535, not " + port);
        }
        if (loanDays < 1) {
            throw new IllegalArgumentException("LOAN_DAYS must be at least 1, not " + loanDays);
        }
        if (!logFormat.equals("text") && !logFormat.equals("json")) {
            throw new IllegalArgumentException("LOG_FORMAT must be text or json, not " + logFormat);
        }
        try {
            Level.parse(logLevel);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("LOG_LEVEL must be a java.util.logging level such as FINE, INFO, WARNING or SEVERE, not " + logLevel);
        }
    }

    // With DATABASE_URL the data goes to PostgreSQL, without it to the text file.
    public String defaultStorage() {
        return databaseUrl.isEmpty() ? "file" : "postgres";
    }

    private static String text(Map<String, String> env, String name, String fallback) {
        String value = env.get(name);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static int intValue(Map<String, String> env, String name, int fallback) {
        String value = env.get(name);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " must be a number, not " + value);
        }
    }
}
