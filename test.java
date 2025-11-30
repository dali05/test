import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.HashMap;
import java.util.Map;

@Converter
public class JsonMapConverter implements AttributeConverter<Map<String, Object>, String> {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, Object> map) {
        try {
            return mapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String json) {
        try {
            return mapper.readValue(json, HashMap.class);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}