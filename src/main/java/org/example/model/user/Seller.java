package org.example.model.user;

public class Seller extends User {
    public Seller(String id, String Username, String Password){
        super(id, Username, Password);
    }

    @Override
    public void displayRole(){
        System.out.println("Role: Seller - Tai khoan: " + this.getUsername());
    }

    public void putItem(String itemName){
        System.out.println(this.Username + " đã đăng sản phẩm: " + itemName);
    }
}
