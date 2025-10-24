package cloud.dpgmedia;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

public class WishFulfillmentHandler implements HttpHandler {

    BigInteger MAX_HASH_VALUE = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", 16);

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        System.out.println("handling wish fulfillment request");
        String method = exchange.getRequestMethod();

        if ("POST".equalsIgnoreCase(method)) { // Handle POST requests to fulfill a wish
            System.out.println("handling post request for wish fulfillment");
            String body = getRequestBody(exchange);

            // Manually parse the JSON payload (simple parsing, assumes well-formed input)
            WishFulfillment wishFulfillment = parseWishFulfillmentFromJson(body);

            if (wishFulfillment != null) {
                Optional<Wish> wishToBeFulfilled = new WishStorePostgres().getWish(wishFulfillment.id);
                if (wishToBeFulfilled.isEmpty()) {
                    System.out.println("No wish found with id: " + wishFulfillment.id);
                    exchange.sendResponseHeaders(400, -1);
                    return;
                } else {
                    System.out.println("Found wish to be fulfilled: " + wishToBeFulfilled.get().productName);
                    // find beneficiary
                    Person person = new PeopleStorePostgres().getPerson(wishToBeFulfilled.get().beneficiaryId);
                    // throws runtime exception if person is not found
                    // calculate distance between person longitude and latitude and north pole
                    if (person.addressLocation == null) {
                        System.out.println("No address location found for person with id: " + person.id);
                        String response = "No address location found for person with id: " + person.id;
                        exchange.sendResponseHeaders(500, response.length());
                        exchange.getResponseBody().write(response.getBytes());
                        exchange.getResponseBody().close();
                        return;
                    }
                    double distanceToNorthPole = person.addressLocation.distanceToNorthPole();
                    System.out.println("Distance to North Pole: " + distanceToNorthPole + " km");
                    // distance to north pole and back
                    double deliveryDistanceBackAndForth = distanceToNorthPole * 2;
                    // calculate delivery cost based on distance



                    // per flown kilometer a short sha needs to be bruteforced
                    String blockHeader;
                    LocalDateTime localDateTime = LocalDateTime.now();
                    String localdatetimeString = LocalDateTime.now().toString();
                    System.out.println("ldt: " + localdatetimeString);

                    // max hash value divided by 16^(deliveryDistanceBackAndForth/8000)
                    // e.g. for 8000 km distance, max hash value divided by 16^1
                    // it has to be a power of 16
                    //
                    //BigInteger difficultyLevel = 16 to the power of (Math.ceil(deliveryDistanceBackAndForth / 8000));
                    System.out.println("pow" + Math.pow(16, deliveryDistanceBackAndForth/7500));
                    BigInteger difficultyLevel = BigInteger.valueOf((long) Math.pow(16, deliveryDistanceBackAndForth / 7500));
                    System.out.printf("difficultylevel: %064x%n", difficultyLevel);


                    BigInteger maxHashValue = MAX_HASH_VALUE.divide(difficultyLevel);
                    System.out.printf("maximum allowed hash value for this wish: %064x%n", maxHashValue);
                    // calculate the time of the following while loop
                    // get initial time
                    long startTime = System.currentTimeMillis();
                    for (int nonce = 0; nonce < Integer.MAX_VALUE; nonce++) {
                        String hexStringWithLeadingZeroes = String.format("%064x", maxHashValue);
                        blockHeader = localdatetimeString + hexStringWithLeadingZeroes + nonce + wishToBeFulfilled.get().productName;
                        String santaHash = HashCollision.getSantaHash(blockHeader);
                        //System.out.println(santaHash);
                        BigInteger santaHashValue = new BigInteger(santaHash, 16);

                        if (maxHashValue.compareTo(santaHashValue) > 0) {
                            long endTime = System.currentTimeMillis();
                            long duration = endTime - startTime;
                            // print block header and then print santa hash

                            String response = "Found valid santa hash: " + santaHash + " for block header: " + blockHeader + " in " + duration + " ms";
                            exchange.sendResponseHeaders(200, response.length());
                            exchange.getResponseBody().write(response.getBytes());
                            exchange.getResponseBody().close();
                            return;
                        }else{
                            if (nonce % 100_000 == 0) {
                                System.out.println("Tried " + (nonce + 1) + " nonces so far...");
                            }
                        }
                    }

                }
            } else {
                String response = "Invalid Json.";
                exchange.sendResponseHeaders(400, response.length());
                exchange.getResponseBody().write(response.getBytes());
                exchange.getResponseBody().close();
            }


        } else {
            // Method not allowed
            exchange.sendResponseHeaders(405, -1); // 405 Method Not Allowed
        }
    }

    public static String generateRandomString(int length) {
        // Characters to use in the random string
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder randomString = new StringBuilder(length);
        Random random = new Random();

        // Generate the random string
        for (int i = 0; i < length; i++) {
            randomString.append(characters.charAt(random.nextInt(characters.length())));
        }

        return randomString.toString();
    }

    private WishFulfillment parseWishFulfillmentFromJson(String json) {
        try {
            String id;
            System.out.println("Using jackson");
            ObjectMapper mapper = new ObjectMapper();

            JsonNode rootNode = mapper.readTree(json);
            JsonNode idNode = rootNode.path("id");
            if (idNode.isMissingNode()) {
                throw new IllegalArgumentException("Missing required field: id");
            }
            id = idNode.asText();

            return new WishFulfillment(id);
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

    private String getRequestBody(HttpExchange exchange) throws IOException {
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
