package cloud.dpgmedia;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class PeopleHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        System.out.println("handling people request");
        String method = exchange.getRequestMethod();

        if ("POST".equalsIgnoreCase(method)) {
            System.out.println("handling post request");
            // Parse the raw JSON request body manually
            String body = getRequestBody(exchange);

            // Manually parse the JSON payload (simple parsing, assumes well-formed input)
            Optional<RegisterPersonDto> optionalRegisterPersonDto = PeopleHandlerUtils.parseRegisterPersonDtoFrom(body);
            if (optionalRegisterPersonDto.isPresent()) {
                RegisterPersonDto registerPersonDto = optionalRegisterPersonDto.get();
                Person person = new Person(
                        Optional.empty(),// will not be used
                        registerPersonDto.firstName(),
                        registerPersonDto.lastName(),
                        registerPersonDto.dateOfBirth(),
                        LocalDateTime.now(),
                        new Location(
                                registerPersonDto.addressLocation().latitude(),
                                registerPersonDto.addressLocation().longitude()
                        ),
                        registerPersonDto.behavior(),
                        1
                );
                person = new PeopleStorePostgres().registerPerson(person);

                // Respond with a 201 Created and the created wish
                String response = generateJsonFromPerson(person);
                exchange.sendResponseHeaders(201, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                String errorResponse = "{\"error\":\"Invalid person data\"}";
                exchange.sendResponseHeaders(400, errorResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(errorResponse.getBytes());
                os.close();
            }
        } else if ("GET".equalsIgnoreCase(method)) {
            // Respond with the list of all people in JSON format
            PeopleStorePostgres postgres = new PeopleStorePostgres();
            String response = generateJsonFromPeople(postgres.getAllPeople());
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        } else if ("PUT".equalsIgnoreCase(method)) {
            System.out.println("handling put request");
            // Parse the raw JSON request body manually
            String body = getRequestBody(exchange);

            System.out.println("updating persondto");
            Optional<UpdatePersonDto> optionalUpdatePersonDto = PeopleHandlerUtils.parseUpdatePersonDto(body);
            System.out.println(optionalUpdatePersonDto.isPresent());
            if (optionalUpdatePersonDto.isPresent()) {
                System.out.println("updating person");
                UpdatePersonDto updatePersonDto = optionalUpdatePersonDto.get();
                Person person = new Person(
                        Optional.of(updatePersonDto.id()),
                        updatePersonDto.firstName(),
                        updatePersonDto.lastName(),
                        updatePersonDto.dateOfBirth(),
                        null,
                        new Location(
                                updatePersonDto.addressLocation().latitude(),
                                updatePersonDto.addressLocation().longitude()
                        ),
                        updatePersonDto.behavior(),
                        updatePersonDto.version()
                );

                person.increaseVersion();
                new PeopleStorePostgres().updatePerson(person);
                // Respond with a 200 ok to indicate success and the updated person
                exchange.sendResponseHeaders(200, -1);
                OutputStream os = exchange.getResponseBody();
                os.close();
            }

        } else{
            // Respond with a 405 Method Not Allowed for unsupported methods
            exchange.sendResponseHeaders(405, -1);
        }
    }



    private static String getRequestBody(HttpExchange exchange) throws IOException {
        InputStream requestBody = exchange.getRequestBody();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] temp = new byte[1024];
        int bytesRead;
        while ((bytesRead = requestBody.read(temp)) != -1) {
            buffer.write(temp, 0, bytesRead);
        }

        // Convert byte array to String using UTF-8 encoding
        String body = new String(buffer.toByteArray(), "UTF-8");
        return body;
    }

    private String generateJsonFromPeople(List<Person> allPeople) {
        StringBuilder jsonArray = new StringBuilder("[");
        for (int i = 0; i < allPeople.size(); i++) {
            Person person = allPeople.get(i);
            jsonArray.append(generateJsonFromPerson(person));
            if (i < allPeople.size() - 1) {
                jsonArray.append(", ");
            }
        }
        jsonArray.append("]");
        return jsonArray.toString();
    }

    private static String generateJsonFromPerson(Person person) {
        try {
            return String.format(
                    "{\"id\":\"%s\", \"firstName\":\"%s\", \"lastName\":\"%s\", \"dateOfBirth\":\"%s\", \"timeOfRegistration\":\"%s\", \"behavior\":\"%s\", \"addressLocation\":{\"latitude\":%f, \"longitude\":%f}, \"version\":%d}",
                    person.id.get(),
                    person.firstName,
                    person.lastName,
                    person.dateOfBirth.toString(),
                    person.timeOfRegistration.toString(),
                    person.behavior.toString(),
                    person.addressLocation.getLatitude(),
                    person.addressLocation.getLongitude(),
                    person.version
            );
        } catch (Exception e) {
            e.printStackTrace();
            return "{}"; // Return an empty JSON object in case of error
        }
    }


}
