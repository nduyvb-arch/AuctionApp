package org.example.model.user;

public class Bidder extends User {
    protected Double balance;
    public Bidder(String id, String Username, String Password, Double balance){
        super(id, Username, Password);
        this.balance = balance;
    }

    @Override
    public void displayRole(){
        System.out.println("Role: Bidder - Tài khoản : " + this.getUsername());
    }

    public void placeBid(double amount) {
        System.out.println(this.getUsername()+ " đang đặt giá: " + amount);
    }
}
