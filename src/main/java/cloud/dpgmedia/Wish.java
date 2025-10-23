package cloud.dpgmedia;

public class Wish {
    // Define the Wish class
    public String id;
    public String productName;
    public int quantity;
    public int beneficiaryId;

    public Wish(String id, String productName, int quantity, int beneficiaryId) {
        this.id = id;
        this.productName = productName;
        this.quantity = quantity;
        this.beneficiaryId = beneficiaryId;
    }

}
