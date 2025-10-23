package cloud.dpgmedia;

import junit.framework.TestCase;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class WishStorePostgresTest extends TestCase {

    public void testStoreAndRetrieveWishForExistingBeneficiary() {
        WishStorePostgres store = new WishStorePostgres();
        Wish wish = new Wish("test-id", "Testy Product", 5, 1);
        store.storeWish(wish);

        // Retrieve all wishes and check if the stored wish is present
        boolean found = store.getAllWishes().stream()
                .anyMatch(w -> w.id.equals("test-id") && w.productName.equals("Testy Product") && w.quantity == 5);

        assert found : "Stored wish not found in database";
        System.out.println("Test passed: Stored wish found in database");
    }

    public void testStoreAndRetrieveWishForNonExistingBeneficiary() {
        WishStorePostgres store = new WishStorePostgres();
        Wish wish = new Wish("test-id", "Testy Product", 5, 99999999); // assuming 99999999 is a non-existing beneficiaryId
        try {
            store.storeWish(wish);
            fail("if a beneficiary does not exist then the wish should be rejected");
        }catch (Exception e) {
            System.out.println("Caught expected exception: " + e.getMessage());
        }
    }

    public void testPrintAllWishesInWishStore() {
        WishStorePostgres store = new WishStorePostgres();
        System.out.println("All wishes in WishStorePostgres:");
        for (Wish wish : store.getAllWishes()) {
            System.out.println("Wish id: " + wish.id + ", productName: " + wish.productName + ", quantity: " + wish.quantity + ", beneficiaryId: " + wish.beneficiaryId);
        }
    }

    public void testSqlInjectionDropTable() {
        WishStorePostgres store = new WishStorePostgres();
        Wish maliciousWish = new Wish("malicious-id", "Malicious Product', 1); DROP TABLE wishes; --", 1, 3);
        store.storeWish(maliciousWish);
        // check if wishes table still exists by trying to retrieve all wishes
        try {
            store.getAllWishes();
            System.out.println("Test passed: wishes table still exists after SQL injection attempt");
        } catch (Exception e) {
            fail("Test failed: wishes table was dropped due to SQL injection");
        }

    }


    protected void setUp() throws Exception {
        super.setUp();
        System.out.println("Setting up WishStorePostgres, cleaning up test data if any");
        // Database connection details
        String url = "jdbc:postgresql://localhost:5432/webapp_db";
        String user = "geert";
        String password = "gman";
        String sql = "DELETE FROM wishes WHERE id = 'test-id' or id = 'malicious-id'";
        String people = "INSERT INTO people (\n" +
                "    firstName, \n" +
                "    lastName, \n" +
                "    dateOfBirth, \n" +
                "    timeOfRegistration, \n" +
                "    latitude, \n" +
                "    longitude, \n" +
                "    version\n" +
                ")\n" +
                "VALUES (\n" +
                "    'Jane', \n" +
                "    'Smith', \n" +
                "    '1990-07-20', \n" +
                "    '2023-10-17 16:00:00', \n" +
                "    51.507351, \n" +
                "    -0.127758, \n" +
                "    1\n" +
                ");\n";
        try (Connection connection = DriverManager.getConnection(url, user, password);
             Statement statement = connection.createStatement()) {

            // Execute the raw SQL DELETE query
            System.out.println("Executing SQL: " + sql);
            int rowsAffected = statement.executeUpdate(sql);
            statement.executeQuery(people);
            System.out.println("Rows deleted: " + rowsAffected);

        } catch (Exception e) {
            e.printStackTrace();

        }
    }

}



