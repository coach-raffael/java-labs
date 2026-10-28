package shelfy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

// The Factory of Module 12.3 and the Config of Module 14.5.
class StorageFactoryTest {

    @TempDir
    Path dir;

    @Test
    void createsEachKnownKind() {
        Config config = Config.from(Map.of("DATA_FILE", dir.resolve("data.txt").toString(),
                "DATABASE_URL", "jdbc:postgresql://localhost:5432/shelfy"));

        assertInstanceOf(InMemoryStorage.class, StorageFactory.create("memory", config));
        assertInstanceOf(FileStorage.class, StorageFactory.create("file", config));
        assertInstanceOf(PostgresStorage.class, StorageFactory.create("postgres", config));
    }

    @Test
    void anUnknownKindNamesTheAllowedValues() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> StorageFactory.create("cloud", Config.from(Map.of())));

        assertEquals("Unknown storage: cloud (use one of [memory, file, postgres])", e.getMessage());
    }

    @Test
    void postgresWithoutAUrlIsAnError() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> StorageFactory.create("postgres", Config.from(Map.of())));

        assertTrue(e.getMessage().contains("DATABASE_URL"));
    }

    @Test
    void configHasDefaultsAndReadsTheEnvironment() {
        Config defaults = Config.from(Map.of());
        assertEquals(8080, defaults.port());
        assertEquals("shelfy.txt", defaults.dataFile());
        assertEquals("INFO", defaults.logLevel());
        assertEquals("text", defaults.logFormat());
        assertEquals(14, defaults.loanDays());
        assertEquals("file", defaults.defaultStorage());

        Config custom = Config.from(Map.of("PORT", "9090", "LOAN_DAYS", "7", "LOG_FORMAT", "JSON", "LOG_LEVEL", "fine",
                "DATABASE_URL", "jdbc:postgresql://db:5432/shelfy", "DATABASE_USER", "shelfy", "DATABASE_PASSWORD", "x"));
        assertEquals(9090, custom.port());
        assertEquals(7, custom.loanDays());
        assertEquals("json", custom.logFormat());
        assertEquals("FINE", custom.logLevel());
        assertEquals("postgres", custom.defaultStorage());
    }

    @Test
    void configRejectsNonsense() {
        assertThrows(IllegalArgumentException.class, () -> Config.from(Map.of("PORT", "eighty")));
        assertThrows(IllegalArgumentException.class, () -> Config.from(Map.of("PORT", "70000")));
        assertThrows(IllegalArgumentException.class, () -> Config.from(Map.of("LOAN_DAYS", "0")));
        assertThrows(IllegalArgumentException.class, () -> Config.from(Map.of("LOG_FORMAT", "xml")));
        assertThrows(IllegalArgumentException.class, () -> Config.from(Map.of("LOG_LEVEL", "loud")));
    }
}
