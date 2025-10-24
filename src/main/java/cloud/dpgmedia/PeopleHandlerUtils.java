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
            JsonNode firstNameNode = rootNode.path("firstName");
            if (firstNameNode.isMissingNode()) {
                throw new IllegalArgumentException("Missing required field: firstName");
            }
            String firstName = firstNameNode.asText();

            JsonNode lastNameNode = rootNode.path("lastName");
            if (lastNameNode.isMissingNode()) {
                throw new IllegalArgumentException("Missing required field: lastName");
            }
            String lastName = lastNameNode.asText();

            JsonNode dateOfBirthNode = rootNode.path("dateOfBirth");
            if (dateOfBirthNode.isMissingNode()) {
                throw new IllegalArgumentException("Missing required field: dateOfBirth");
            }
            LocalDate dateOfBirth = LocalDate.parse(dateOfBirthNode.asText());

            // Parse nested addressLocation data if available
            JsonNode locationNode = rootNode.get("addressLocation");
            LocationDto addressLocationDto = null;
            if (locationNode != null) {
                double latitude = locationNode.get("latitude").asDouble();
                double longitude = locationNode.get("longitude").asDouble();

                // Create Location object with latitude and longitude
                addressLocationDto = new LocationDto(latitude, longitude);
            }

            JsonNode behaviorNode = rootNode.path("behavior");
            if (behaviorNode.isMissingNode()) {
                throw new IllegalArgumentException("Missing required field: behavior");
            }
            Behavior behavior = Behavior.valueOf(behaviorNode.asText().toUpperCase());

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
            JsonNode idNode = rootNode.path("id");
            if (idNode.isMissingNode()) {
                throw new IllegalArgumentException("Missing required field: id");
            }
            int id = idNode.asInt();

            JsonNode firstNameNode = rootNode.path("firstName");
            if (firstNameNode.isMissingNode()) {
                throw new IllegalArgumentException("Missing required field: firstName");
            }
            String firstName = firstNameNode.asText();

            JsonNode lastNameNode = rootNode.path("lastName");
            if (lastNameNode.isMissingNode()) {
                throw new IllegalArgumentException("Missing required field: lastName");
            }
            String lastName = lastNameNode.asText();

            JsonNode dateOfBirthNode = rootNode.path("dateOfBirth");
            if (dateOfBirthNode.isMissingNode()) {
                throw new IllegalArgumentException("Missing required field: dateOfBirth");
            }
            LocalDate dateOfBirth = LocalDate.parse(dateOfBirthNode.asText());

            // Parse nested addressLocation data if available
            JsonNode locationNode = rootNode.get("addressLocation");
            LocationDto addressLocationDto = null;
            if (locationNode != null) {
                double latitude = locationNode.get("latitude").asDouble();
                double longitude = locationNode.get("longitude").asDouble();

                // Create Location object with latitude and longitude
                addressLocationDto = new LocationDto(latitude, longitude);
            }

            JsonNode behaviorNode = rootNode.path("behavior");
            if (behaviorNode.isMissingNode()) {
                throw new IllegalArgumentException("Missing required field: behavior");
            }
            Behavior behavior = Behavior.valueOf(behaviorNode.asText().toUpperCase());

            JsonNode versionNode = rootNode.path("version");
            if (versionNode.isMissingNode()) {
                throw new IllegalArgumentException("Missing required field: version");
            }
            int version = versionNode.asInt();

            return Optional.of(new UpdatePersonDto(id, firstName, lastName, dateOfBirth, addressLocationDto, behavior, version));
        } catch (Exception e) {
            // Handle parsing errors (e.g., malformed JSON)
            System.err.println("Failed to parse Person from JSON body: " + e.getMessage());
            return Optional.empty();
        }
    }
}