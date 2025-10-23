package cloud.dpgmedia;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.util.Optional;

public class PeopleHandlerUtils {

    /**
     * Parses a Person object from a JSON string request body.
     *
     * @param body The raw JSON request body.
     * @return A Person object or null if the parsing fails.
     */
    public static Optional<RegisterPersonDto> parseRegisterPersonDtoFrom(String body) {
        try {
            // Create Jackson ObjectMapper
            ObjectMapper objectMapper = new ObjectMapper();

            // Parse the JSON string into a JsonNode tree
            JsonNode rootNode = objectMapper.readTree(body);

            // if requestbody contains id or version throw an illegal argument exception
            if (rootNode.has("id") || rootNode.has("version")) {
                throw new IllegalArgumentException("Request body should not contain id or version for registration");
            }
            // Extract necessary fields from the JSON payload
            String firstName = rootNode.get("firstName").asText();
            String lastName = rootNode.get("lastName").asText();
            LocalDate dateOfBirth = LocalDate.parse(rootNode.get("dateOfBirth").asText());

            // Parse nested addressLocation data if available
            JsonNode locationNode = rootNode.get("addressLocation");
            LocationDto addressLocationDto = null;
            if (locationNode != null) {
                double latitude = locationNode.get("latitude").asDouble();
                double longitude = locationNode.get("longitude").asDouble();

                // Create Location object with latitude and longitude
                addressLocationDto = new LocationDto(latitude, longitude);
            }
            Behavior behavior = Behavior.valueOf(rootNode.get("behavior").asText().toUpperCase());

            // Create and return the Person object
            return Optional.of(new RegisterPersonDto(firstName, lastName, dateOfBirth, addressLocationDto, behavior));
        } catch (Exception e) {
            // Handle parsing errors (e.g., malformed JSON)
            System.err.println("Failed to parse Person from JSON body: " + e.getMessage());
            return Optional.empty();
        }
    }

    public static Optional<UpdatePersonDto> parseUpdatePersonDto(String body) {
        try {
            // Create Jackson ObjectMapper
            ObjectMapper objectMapper = new ObjectMapper();

            // Parse the JSON string into a JsonNode tree
            JsonNode rootNode = objectMapper.readTree(body);

            // Extract necessary fields from the JSON payload
            int id = rootNode.get("id").asInt();
            String firstName = rootNode.get("firstName").asText();
            String lastName = rootNode.get("lastName").asText();
            LocalDate dateOfBirth = LocalDate.parse(rootNode.get("dateOfBirth").asText());

            // Parse nested addressLocation data if available
            JsonNode locationNode = rootNode.get("addressLocation");
            LocationDto addressLocationDto = null;
            if (locationNode != null) {
                double latitude = locationNode.get("latitude").asDouble();
                double longitude = locationNode.get("longitude").asDouble();

                // Create Location object with latitude and longitude
                addressLocationDto = new LocationDto(latitude, longitude);
            }
            Behavior behavior = Behavior.valueOf(rootNode.get("behavior").asText().toUpperCase());
            int version = rootNode.get("version").asInt();

            return Optional.of(new UpdatePersonDto(id, firstName, lastName, dateOfBirth, addressLocationDto, behavior, version));
        } catch (Exception e) {
            // Handle parsing errors (e.g., malformed JSON)
            System.err.println("Failed to parse Person from JSON body: " + e.getMessage());
            return Optional.empty();
        }
    }
}