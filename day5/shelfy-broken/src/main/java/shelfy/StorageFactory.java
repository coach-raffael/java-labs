package shelfy;

import java.nio.file.Path;
import java.util.List;

// Factory (Module 12.3): one place that knows how to build each kind of storage. Main asks for
// a name, the Library never learns which one it got. Module 14 added "postgres" here and
// nowhere else.
public final class StorageFactory {

    public static final List<String> KINDS = List.of("memory", "file", "postgres");

    private StorageFactory() {
    }

    public static Storage create(String kind, Config config) {
        return switch (kind) {
            case "memory" -> new InMemoryStorage();
            case "file" -> new FileStorage(Path.of(config.dataFile()));
            case "postgres" -> {
                if (config.databaseUrl().isEmpty()) {
                    throw new IllegalArgumentException("storage postgres needs DATABASE_URL (jdbc:postgresql://host:5432/shelfy)");
                }
                yield new PostgresStorage(config.databaseUrl(), config.databaseUser(), config.databasePassword());
            }
            default -> throw new IllegalArgumentException("Unknown storage: " + kind + " (use one of " + KINDS + ")");
        };
    }
}
