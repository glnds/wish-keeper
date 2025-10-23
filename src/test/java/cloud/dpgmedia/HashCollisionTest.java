package cloud.dpgmedia;

import junit.framework.TestCase;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static cloud.dpgmedia.HashCollision.getSantaHash;

public class HashCollisionTest extends TestCase {

    MessageDigest digest = MessageDigest.getInstance("SHA-256");

    public HashCollisionTest() throws NoSuchAlgorithmException {
    }

    public void testSantaHash() {
        System.out.println(getSantaHash("2025-09-23T16:04:51.686506301549pony"));
        if (getSantaHash("2025-09-23T16:04:51.686506301549pony").startsWith("00000")) {
//            System.out.println("starts with 5 zeros");
        } else {
            System.out.println("does not start with 5 zeros");
            fail("hash does not start with 5 zeros and thus does not meet difficulty level");
        }
    }

    public void testSantaHashesLocal() {
        System.out.println(getSantaHash("2025-09-23T16:04:51.686506301549pony"));
        if (getSantaHash("2025-09-23T16:04:51.686506301549pony").startsWith("00000")) {
//            System.out.println("starts with 5 zeros");
        } else {
            System.out.println("does not start with 5 zeros");
            fail("hash does not start with 5 zeros and thus does not meet difficulty level");
        }
    }

    public void testSantaHashesLocal2() {
        System.out.println(getSantaHash("2025-09-23T16:04:51.686506301549pony"));
        if (getSantaHash("2025-09-23T16:04:51.686506301549pony").startsWith("00000")) {
//            System.out.println("starts with 5 zeros");
        } else {
            System.out.println("does not start with 5 zeros");
            fail("hash does not start with 5 zeros and thus does not meet difficulty level");
        }
    }

    public void test5ZeroHash() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        // get current time in millis
        long startTime = System.currentTimeMillis();
        int i=0;
        double amount = 4.0;
        // calculate average time of 10 santahashes with 5 zeroes
        for (int j = 0; j < amount; j++) {
            String random = "randomstartstring" + j;
            while(true){
                String santaHash = getSantaHash(random + "2025-09-23T16:04:51.686506301549pony" + i);
                if (santaHash.startsWith("00000")) {
                    System.out.println("Found hash with 5 leading zeros: " + santaHash + " for nonce " + i);
                    break;
                }
                i++;
            }
        }
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        double avgDuration = duration / amount;
        System.out.println(avgDuration);


    }





}



