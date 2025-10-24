package cloud.dpgmedia;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

public class BasicApi {


    public static void main(String[] args) throws IOException {
        // Step 1: Create an HTTP server running on port 8000
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);

        // Step 2: Define an endpoint: "/api/hello"
        server.createContext("/api/hello", new HelloHandler());
        server.createContext("/api/wish", new WishHandler());
        server.createContext("/api/wishreplace", new WishReplacementHandler());
        server.createContext("/api/people", new PeopleHandler());
        server.createContext("/api/wishfulfill", new WishFulfillmentHandler() );

        // Step 3: Start the server
        server.setExecutor(null); // Single-threaded executor
        server.start();
        System.out.println("Server is running on http://localhost:8000");
    }

    // Step 4: Define the response logic for the endpoint
    static class HelloHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "Hello, World!";
            // Send a 200 OK response with the "Hello, World!" message
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }



    static class WishHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            System.out.println("handling wish request");
            String method = exchange.getRequestMethod();

            if ("POST".equalsIgnoreCase(method)) { // Handle POST requests to create a wish
                System.out.println("handling post request");
                // Parse the raw JSON request body manually
                String body = getRequestBody(exchange);


                // Manually parse the JSON payload (simple parsing, assumes well-formed input)
                Wish newWish;
                try {
                    newWish = parseWishFromJson(body);
                } catch (IllegalArgumentException e) {
                    System.out.println("Validation error: " + e.getMessage());
                    String errorResponse = String.format("{\"error\":\"%s\"}", e.getMessage());
                    exchange.sendResponseHeaders(400, errorResponse.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(errorResponse.getBytes());
                    os.close();
                    return;
                }

                if (newWish != null) {
                    Person person = new PeopleStorePostgres().getPerson(newWish.beneficiaryId);
                    if (person == null) {
                        System.out.println("No person found with id: " + newWish.beneficiaryId);
                        String errorResponse = String.format("{\"error\":\"No person found with id: %d\"}", newWish.beneficiaryId);
                        exchange.sendResponseHeaders(400, errorResponse.length());
                        OutputStream os = exchange.getResponseBody();
                        os.write(errorResponse.getBytes());
                        os.close();
                        return;
                    }


                    List<Wish> wishesForBeneficiary = new WishStorePostgres().getWishesForBeneficiary(newWish.beneficiaryId);
                    if (wishesForBeneficiary.size() < 3) {
                        System.out.println("Beneficiary " + newWish.beneficiaryId + " has less than 3 wishes, allowing new wish.");
                        new WishStorePostgres().storeWish(newWish);
                    } else {
                        System.out.println("Beneficiary " + newWish.beneficiaryId + " already has 3 wishes, cannot add more.");
                        String errorResponse = String.format("{\"error\":\"Beneficiary %d already has 3 wishes, cannot add more\"}", newWish.beneficiaryId);
                        exchange.sendResponseHeaders(400, errorResponse.length());
                        OutputStream os = exchange.getResponseBody();
                        os.write(errorResponse.getBytes());
                        os.close();
                        return;
                    }


                    // Respond with a 201 Created and the created wish
                    String response = generateJsonFromWish(newWish);
                    exchange.sendResponseHeaders(201, response.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                } else {
                    // Respond with a 400 Bad Request for invalid input
                    String errorResponse = "{\"error\":\"Invalid wish data\"}";
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
    }

    // Helper method: Parse JSON into an Wish object (manually)
    public static Wish parseWishFromJson(String json) {
        // Assumes a simple JSON structure like: {"productName":"Laptop", "quantity":2, "beneficiaryId":5}
        // ID is auto-generated and should not be provided in the request
        try {
            String productName, quantityStr;
            int beneficiaryId;
            System.out.println("Using jackson");
            ObjectMapper mapper = new ObjectMapper();

            // Auto-generate ID using UUID
            String id = UUID.randomUUID().toString();

            // Parse and validate productName
            JsonNode productNameNode = mapper.readTree(json).path("productName");
            if (productNameNode.isMissingNode()) {
                throw new IllegalArgumentException("Missing field: productName");
            }
            productName = productNameNode.asText();

            // Parse and validate quantity
            JsonNode quantityNode = mapper.readTree(json).path("quantity");
            if (quantityNode.isMissingNode()) {
                throw new IllegalArgumentException("Missing field: quantity");
            }
            quantityStr = quantityNode.asText();

            // Parse and validate beneficiaryId
            JsonNode beneficiaryNode = mapper.readTree(json).path("beneficiaryId");
            if (beneficiaryNode.isMissingNode()) {
                throw new IllegalArgumentException("Missing field: beneficiaryId");
            }else if (!beneficiaryNode.isInt()) {
                throw new IllegalArgumentException("beneficiaryId must be an integer");
            } else {
                beneficiaryId = beneficiaryNode.asInt();
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

            return new Wish(id, productName, quantity, beneficiaryId);
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