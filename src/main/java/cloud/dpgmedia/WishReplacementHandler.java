package cloud.dpgmedia;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;

public class WishReplacementHandler implements HttpHandler {
    public void handle(HttpExchange exchange) throws IOException {
        System.out.println("handling wish replacement request");
        String method = exchange.getRequestMethod();

        if ("PUT".equalsIgnoreCase(method)) { // Handle PUT requests to create a wish
            System.out.println("handling put request");
            // Parse the raw JSON request body manually
            String body = getRequestBody(exchange);


            // Manually parse the JSON payload (simple parsing, assumes well-formed input)
            ReplacementWish replacementWish;
            try {
                replacementWish = parseReplacementWishFromJson(body);
            } catch (IllegalArgumentException e) {
                System.out.println("Validation error: " + e.getMessage());
                String errorResponse = String.format("{\"error\":\"%s\"}", e.getMessage());
                exchange.sendResponseHeaders(400, errorResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(errorResponse.getBytes());
                os.close();
                return;
            }

            if (replacementWish != null) {
                Person person = new PeopleStorePostgres().getPerson(replacementWish.beneficiaryId);
                if (person == null) {
                    System.out.println("No person found with id: " + replacementWish.beneficiaryId);
                    String errorResponse = String.format("{\"error\":\"No person found with id: %d\"}", replacementWish.beneficiaryId);
                    exchange.sendResponseHeaders(400, errorResponse.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(errorResponse.getBytes());
                    os.close();
                    return;
                }


                List<Wish> wishesForBeneficiary = new WishStorePostgres().getWishesForBeneficiary(replacementWish.beneficiaryId);
                // select wish from wisheslist where id  = replacementWish.idOfWishToBeReplaced
                Optional<Wish> optionalWish = wishesForBeneficiary.stream()
                        .filter(aWish -> aWish.id.equals(replacementWish.idOfWishToBeReplaced))
                        .findFirst();
                if (optionalWish.isEmpty()) {
                    System.out.println("No wish found with id: " + replacementWish.idOfWishToBeReplaced + " for beneficiary id: " + replacementWish.beneficiaryId);
                    String errorResponse = String.format("{\"error\":\"No wish found with id: %s for beneficiary id: %d\"}", replacementWish.idOfWishToBeReplaced, replacementWish.beneficiaryId);
                    exchange.sendResponseHeaders(400, errorResponse.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(errorResponse.getBytes());
                    os.close();
                    return;
                }else {
                    System.out.println("Found wish to be replaced: " + optionalWish.get().productName);
                }

                WishStorePostgres wishStore = new WishStorePostgres();
                System.out.println("Deleting wish from db: " + replacementWish.idOfWishToBeReplaced);
                wishStore.deleteWish(replacementWish.idOfWishToBeReplaced);
                System.out.println("Create new wish domain object with id: " + replacementWish.id);
                Wish newWish = new Wish(replacementWish.id, replacementWish.productName, replacementWish.quantity, replacementWish.beneficiaryId);
                System.out.println("Storing new wish in db with id: " + newWish.id);
                wishStore.storeWish(newWish);


                String response = generateJsonFromDeleteAndNewWish(optionalWish.get(), newWish);
                exchange.sendResponseHeaders(200, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                // Respond with a 400 Bad Request for invalid input
                String errorResponse = "{\"error\":\"Invalid replacement wish data\"}";
                exchange.sendResponseHeaders(400, errorResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(errorResponse.getBytes());
                os.close();
            }
        } else if ("GET".equalsIgnoreCase(method)) { // Handle GET requests to list all wishe
            // Respond with the list of all wishes in JSON format
            WishStorePostgres postgres = new WishStorePostgres();
            String response = generateJsonFromWish(postgres.getAllWishes());
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        } else {
            // Respond with a 405 Method Not Allowed for unsupported methods
            exchange.sendResponseHeaders(405, -1);
        }
    }

    private static String getRequestBody(HttpExchange exchange) throws IOException {
        InputStream requestBody = exchange.getRequestBody();
        //String body = new String(requestBody.readAllBytes());

        // Read all bytes using standard Java classes
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


    // Helper method: Parse JSON into an Wish object (manually)
    public static ReplacementWish parseReplacementWishFromJson(String json) {
        // Assumes a simple JSON structure like: {"id":"123", "productName":"Laptop", "quantity":2, "idOfWishToBeReplaced":456}
        try {
            String id, productName, quantityStr, idOfWishToBeReplaced;
            int beneficiaryId;
            System.out.println("Using jackson");
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(json);

            JsonNode idNode = rootNode.path("id");
            if (idNode.isMissingNode()) {
                throw new IllegalArgumentException("Missing required field: id");
            }
            id = idNode.asText();

            JsonNode productNameNode = rootNode.path("productName");
            if (productNameNode.isMissingNode()) {
                throw new IllegalArgumentException("Missing required field: productName");
            }
            productName = productNameNode.asText();

            JsonNode quantityNode = rootNode.path("quantity");
            if (quantityNode.isMissingNode()) {
                throw new IllegalArgumentException("Missing required field: quantity");
            }
            quantityStr = quantityNode.asText();

            JsonNode beneficiaryNode = rootNode.path("beneficiaryId");
            if (beneficiaryNode.isMissingNode()) {
                throw new IllegalArgumentException("Missing field: beneficiaryId");
            } else if (!beneficiaryNode.isInt()) {
                throw new IllegalArgumentException("beneficiaryId must be an integer");
            } else {
                beneficiaryId = beneficiaryNode.asInt();
            }
            JsonNode replacementNode = rootNode.path("idOfWishToBeReplaced");
            if (replacementNode.isMissingNode()) {
                throw new IllegalArgumentException("Missing field: idOfWishToBeReplaced");
            } else {
                idOfWishToBeReplaced = replacementNode.asText();
            }



            if (productName.isEmpty()) {
                throw new IllegalArgumentException("Wish ProductName cannot be Empty");
            }
            System.out.println("productname is " + productName);

            if (quantityStr.trim().isEmpty()) {
                System.out.println("json contains empty quantity");
                throw new IllegalArgumentException("Wish Quantity cannot be empty");
            }
            System.out.println("Quantity is:" + quantityStr);
            Double quantitydouble = Double.parseDouble(quantityStr);
            int quantity = quantitydouble.intValue();
            if (quantity < 0) {
                System.out.println("Throwing");
                throw new IllegalArgumentException("Wish Quantity cannot be negative");
            }

            return new ReplacementWish(id, productName, quantity, beneficiaryId, idOfWishToBeReplaced);
        } catch (Exception e) {
            System.out.println("caught :" + e.getMessage());
            if (e instanceof IllegalArgumentException) {
                System.out.println(" throwing illegal argument exception again");
                throw (IllegalArgumentException) e;
            } else {
                System.out.println("parsing error: " + e.getMessage());
            }
            // Return null if parsing fails (e.g., malformed JSON)
            return null;
        }
    }

    // Helper method: Generate JSON from an Wish object (manually)
    private static String generateJsonFromWish(Wish wish) {
        return String.format("{\"id\":\"%s\", \"productName\":\"%s\", \"quantity\":%d, \"beneficiaryId\":%d}", wish.id, wish.productName, wish.quantity, wish.beneficiaryId);
    }

    // generate JSON for a delete wish and a new wish
    private static String generateJsonFromDeleteAndNewWish(Wish deletedWish, Wish newWish) {
        return String.format("{\"deletedWish\":{\"id\":\"%s\", \"productName\":\"%s\", \"quantity\":%d, \"beneficiaryId\":%d}, \"newWish\":{\"id\":\"%s\", \"productName\":\"%s\", \"quantity\":%d, \"beneficiaryId\":%d}}",
                deletedWish.id, deletedWish.productName, deletedWish.quantity, deletedWish.beneficiaryId,
                newWish.id, newWish.productName, newWish.quantity, newWish.beneficiaryId);
    }

    // Helper method: Generate JSON from a list of Wish objects (manually)
    private static String generateJsonFromWish(List<Wish> wishes) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < wishes.size(); i++) {
            json.append(generateJsonFromWish(wishes.get(i)));
            if (i < wishes.size() - 1) {
                json.append(", ");
            }
        }
        json.append("]");
        return json.toString();
    }
}
