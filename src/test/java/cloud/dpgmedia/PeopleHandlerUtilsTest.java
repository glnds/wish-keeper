package cloud.dpgmedia;

import junit.framework.TestCase;
import java.util.Optional;

public class PeopleHandlerUtilsTest extends TestCase {

    public void testParseRegisterPersonDtoMissingFirstName() {
        String json = "{\"lastName\":\"Doe\",\"dateOfBirth\":\"1990-01-01\",\"behavior\":\"NICE\"}";
        Optional<RegisterPersonDto> result = PeopleHandlerUtils.parseRegisterPersonDtoFrom(json);

        // When a required field is missing, parseRegisterPersonDtoFrom catches the exception and returns Optional.empty()
        assertTrue("Should return empty Optional when firstName is missing", result.isEmpty());
    }

    public void testParseRegisterPersonDtoMissingLastName() {
        String json = "{\"firstName\":\"John\",\"dateOfBirth\":\"1990-01-01\",\"behavior\":\"NICE\"}";
        Optional<RegisterPersonDto> result = PeopleHandlerUtils.parseRegisterPersonDtoFrom(json);

        assertTrue("Should return empty Optional when lastName is missing", result.isEmpty());
    }

    public void testParseRegisterPersonDtoMissingDateOfBirth() {
        String json = "{\"firstName\":\"John\",\"lastName\":\"Doe\",\"behavior\":\"NICE\"}";
        Optional<RegisterPersonDto> result = PeopleHandlerUtils.parseRegisterPersonDtoFrom(json);

        assertTrue("Should return empty Optional when dateOfBirth is missing", result.isEmpty());
    }

    public void testParseRegisterPersonDtoMissingBehavior() {
        String json = "{\"firstName\":\"John\",\"lastName\":\"Doe\",\"dateOfBirth\":\"1990-01-01\"}";
        Optional<RegisterPersonDto> result = PeopleHandlerUtils.parseRegisterPersonDtoFrom(json);

        assertTrue("Should return empty Optional when behavior is missing", result.isEmpty());
    }

    public void testParseRegisterPersonDtoWithIdShouldFail() {
        String json = "{\"id\":1,\"firstName\":\"John\",\"lastName\":\"Doe\",\"dateOfBirth\":\"1990-01-01\",\"behavior\":\"NICE\"}";
        Optional<RegisterPersonDto> result = PeopleHandlerUtils.parseRegisterPersonDtoFrom(json);

        assertTrue("Should return empty Optional when id is present in registration", result.isEmpty());
    }

    public void testParseRegisterPersonDtoWithVersionShouldFail() {
        String json = "{\"version\":1,\"firstName\":\"John\",\"lastName\":\"Doe\",\"dateOfBirth\":\"1990-01-01\",\"behavior\":\"NICE\"}";
        Optional<RegisterPersonDto> result = PeopleHandlerUtils.parseRegisterPersonDtoFrom(json);

        assertTrue("Should return empty Optional when version is present in registration", result.isEmpty());
    }

    public void testParseRegisterPersonDtoValidData() {
        String json = "{\"firstName\":\"John\",\"lastName\":\"Doe\",\"dateOfBirth\":\"1990-01-01\",\"behavior\":\"NICE\",\"addressLocation\":{\"latitude\":50.0,\"longitude\":4.0}}";
        Optional<RegisterPersonDto> result = PeopleHandlerUtils.parseRegisterPersonDtoFrom(json);

        assertTrue("Should return present Optional for valid data", result.isPresent());
        assertEquals("John", result.get().firstName());
        assertEquals("Doe", result.get().lastName());
        assertEquals(Behavior.NICE, result.get().behavior());
    }

    public void testParseUpdatePersonDtoMissingId() {
        String json = "{\"firstName\":\"John\",\"lastName\":\"Doe\",\"dateOfBirth\":\"1990-01-01\",\"behavior\":\"NICE\",\"version\":1}";
        Optional<UpdatePersonDto> result = PeopleHandlerUtils.parseUpdatePersonDto(json);

        assertTrue("Should return empty Optional when id is missing", result.isEmpty());
    }

    public void testParseUpdatePersonDtoMissingVersion() {
        String json = "{\"id\":1,\"firstName\":\"John\",\"lastName\":\"Doe\",\"dateOfBirth\":\"1990-01-01\",\"behavior\":\"NICE\"}";
        Optional<UpdatePersonDto> result = PeopleHandlerUtils.parseUpdatePersonDto(json);

        assertTrue("Should return empty Optional when version is missing", result.isEmpty());
    }

    public void testParseUpdatePersonDtoValidData() {
        String json = "{\"id\":1,\"firstName\":\"John\",\"lastName\":\"Doe\",\"dateOfBirth\":\"1990-01-01\",\"behavior\":\"NICE\",\"version\":1,\"addressLocation\":{\"latitude\":50.0,\"longitude\":4.0}}";
        Optional<UpdatePersonDto> result = PeopleHandlerUtils.parseUpdatePersonDto(json);

        assertTrue("Should return present Optional for valid data", result.isPresent());
        assertEquals(1, result.get().id());
        assertEquals("John", result.get().firstName());
        assertEquals("Doe", result.get().lastName());
        assertEquals(1, result.get().version());
    }
}
