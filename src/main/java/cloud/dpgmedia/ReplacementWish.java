package cloud.dpgmedia;

public class ReplacementWish {

    // Define the ReplacementWish class
    public String id;
    public String productName;
    public int quantity;
    public int beneficiaryId;
    public String idOfWishToBeReplaced; // id of wish that needs to be replaced

    public ReplacementWish(String id, String productName, int quantity, int beneficiaryId, String idOfWishToBeReplaced) {
        this.id = id;
        this.productName = productName;
        this.quantity = quantity;
        this.beneficiaryId = beneficiaryId;
        this.idOfWishToBeReplaced = idOfWishToBeReplaced;
    }

}
