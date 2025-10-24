package cloud.dpgmedia;

import junit.framework.TestCase;

public class WishTest extends TestCase {
    public void testParseWishFromJson() {
        try {
            Wish wish = BasicApi.parseWishFromJson("{\"productName\":\"Laptop\", \"quantity\":2}");
            fail("a wish without beneficiaryId should throw an exception");
        }catch (Exception e) {

        }
    }

    public void testWishQuantityCannotBeNegative() {
        try {
            Wish wish = BasicApi.parseWishFromJson("{\"productName\":\"Laptop\", \"quantity\":-1, \"beneficiaryId\":5}");
            fail();
        } catch (IllegalArgumentException e) {
            assert e.getMessage().equals("Wish Quantity cannot be negative");
            System.out.println("Caught expected exception: " + e.getMessage());
        }
    }

    public void testWishProductNameCannotBeEmpty() {
        try {
            Wish wish = BasicApi.parseWishFromJson("{\"productName\":\"\", \"quantity\":2, \"beneficiaryId\":5}");
            fail();
        } catch (IllegalArgumentException e) {
            assert e.getMessage().equals("Wish ProductName cannot be Empty");
            System.out.println("Caught expected exception: " + e.getMessage());
        }
    }

    public void testWishQuantityCannotBeEmpty() {
        try {
            Wish wish = BasicApi.parseWishFromJson("{\"productName\":\"Laptop\", \"quantity\":\" \", \"beneficiaryId\":5}");
            fail();
        } catch (IllegalArgumentException e) {
            System.out.println("Caught expected exception: " + e.getMessage());
            assert e.getMessage().equals("Wish Quantity cannot be empty");
            System.out.println("Caught expected exception: " + e.getMessage());
        }
    }

    public void testWishWithSpacesBetweenColons() {
        Wish wish = BasicApi.parseWishFromJson("{ \"productName\" : \"Laptop\" , \"quantity\" : 2 , \"beneficiaryId\":5 }");
        assert wish.id != null && !wish.id.isEmpty();
        assert wish.productName.equals("Laptop");
        assert wish.beneficiaryId == 5;

        System.out.println("Parsed Wish id: " + wish.id + " productName: " + wish.productName + " quantity: " + wish.quantity + " beneficiaryId: " + wish.beneficiaryId);
    }

    public void testWishWithDecimalQuantity() {
        Wish wish = BasicApi.parseWishFromJson("{\"productName\":\"Laptop\", \"quantity\":2.0, \"beneficiaryId\":5}");
        assert wish.id != null && !wish.id.isEmpty();
        assert wish.productName.equals("Laptop");
        assert wish.beneficiaryId == 5;

        System.out.println("Parsed Wish id: " + wish.id + " productName: " + wish.productName + " quantity: " + wish.quantity + " beneficiaryId: " + wish.beneficiaryId);
    }

    public void testWishKeysInDifferentOrder() {
        Wish wish = BasicApi.parseWishFromJson("{\"quantity\":2, \"productName\":\"lawnmower\", \"beneficiaryId\":5}");
        assert wish.id != null && !wish.id.isEmpty();
        assert wish.productName.equals("lawnmower");
        assert wish.quantity == 2;
        assert wish.beneficiaryId == 5;

        System.out.println("Parsed Wish id: " + wish.id + " productName: " + wish.productName + " quantity: " + wish.quantity + " beneficiaryId: " + wish.beneficiaryId);
    }

    public void testWishWithExtraKey() {
        Wish wish = BasicApi.parseWishFromJson("{\"productName\":\"Laptop\", \"quantity\":2, \"extraKey\":\"extraValue\", \"beneficiaryId\":5}");
        assert wish.id != null && !wish.id.isEmpty();
        assert wish.productName.equals("Laptop");
        assert wish.quantity == 2;
        assert wish.beneficiaryId == 5;

        System.out.println("Parsed Wish id: " + wish.id + " productName: " + wish.productName + " quantity: " + wish.quantity + " beneficiaryId: " + wish.beneficiaryId);
    }

    public void testWishMissingProductName() {
        try {
            Wish wish = BasicApi.parseWishFromJson("{\"quantity\":2, \"beneficiaryId\":5}");
            fail("Should throw exception when productName is missing");
        } catch (IllegalArgumentException e) {
            assert e.getMessage().equals("Missing field: productName");
            System.out.println("Caught expected exception: " + e.getMessage());
        }
    }

    public void testWishMissingQuantity() {
        try {
            Wish wish = BasicApi.parseWishFromJson("{\"productName\":\"Laptop\", \"beneficiaryId\":5}");
            fail("Should throw exception when quantity is missing");
        } catch (IllegalArgumentException e) {
            assert e.getMessage().equals("Missing field: quantity");
            System.out.println("Caught expected exception: " + e.getMessage());
        }
    }

    public void testWishMissingBeneficiaryId() {
        try {
            Wish wish = BasicApi.parseWishFromJson("{\"productName\":\"Laptop\", \"quantity\":2}");
            fail("Should throw exception when beneficiaryId is missing");
        } catch (IllegalArgumentException e) {
            assert e.getMessage().equals("Missing field: beneficiaryId");
            System.out.println("Caught expected exception: " + e.getMessage());
        }
    }

}
