package cloud.dpgmedia;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.time.LocalDateTime;

public class HashCollision {
    LocalDateTime localDateTime;
    int nonce;
    BigInteger maxHashValue;
    String productName;
    long calculationTimeInMillis;

    public HashCollision(LocalDateTime localDateTime, int nonce, BigInteger maxHashValue,String productName, long calculationTimeInMillis) {
        this.localDateTime = localDateTime;
        this.nonce = nonce;
        this.maxHashValue = maxHashValue;
        this.productName = productName;
        this.calculationTimeInMillis = calculationTimeInMillis;
    }

    public String getSantaHash() {
        String blockHeader = localDateTime.toString() + maxHashValue.toString(16) + nonce + productName;
        return getSantaHash(blockHeader);
    }

    public static String getSantaHash(String inputString) {
        byte[] bytes = doubleSha256(inputString);
        char[] hexArray = "0123456789abcdef".toCharArray();
        char[] hexChars = new char[bytes.length * 2];

        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hexChars[i * 2] = hexArray[v >>> 4];
            hexChars[i * 2 + 1] = hexArray[v & 0x0F];
        }

        return new String(hexChars);
    }

    private static byte[] doubleSha256(String input) {
        try {
            // Step 1: Get a MessageDigest instance for SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Step 2: Perform the first SHA-256 hash on the input (raw binary digest)
            byte[] firstHash = digest.digest(input.getBytes("UTF-8"));

            // Step 3: Perform the second SHA-256 hash on the result of the first hash
            byte[] doubleHash = digest.digest(firstHash);

            // Step 4: Return the double SHA-256 hash (raw binary digest)
            return doubleHash;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
