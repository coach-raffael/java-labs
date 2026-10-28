package shelfy;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

// JSON with Jackson 3 (Module 13.4). One JsonMapper for the whole program, built in Main and handed
// to whoever needs it. Records go in and out without annotations: the component names are the
// JSON field names. Bad input turns into a BadRequestException, which the API answers with 400.
public class Json {

    private final JsonMapper mapper = JsonMapper.builder().build();

    public String write(Object value) {
        return mapper.writeValueAsString(value);
    }

    public <T> T read(String json, Class<T> type) {
        if (json == null || json.isBlank()) {
            throw new BadRequestException("request body must be JSON");
        }
        try {
            T value = mapper.readValue(json, type);
            if (value == null) {
                throw new BadRequestException("request body must be a JSON object");
            }
            return value;
        } catch (JacksonException e) {
            throw new BadRequestException("invalid JSON: " + firstLine(e.getMessage()));
        }
    }

    private static String firstLine(String message) {
        if (message == null) {
            return "cannot parse";
        }
        int end = message.indexOf('\n');
        return end < 0 ? message : message.substring(0, end);
    }
}
