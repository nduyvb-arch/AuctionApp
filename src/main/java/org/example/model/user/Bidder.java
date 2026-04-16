package org.example.model.user;

public class Bidder extends User {
    private double balance;
    public Bidder(String id, String username, String password, double balance){
        super(id, username, password);
        this.balance = balance;
    }

    @Override
    public void displayRole(){
        System.out.println("Role: Bidder - Tài khoản : " + this.getUsername());
    }

    public void placeBid(double amount) {
        System.out.println(this.getUsername()+ " đang đặt giá: " + amount);
    }

    public void setBalance(double newBalance)
    {
        this.balance = newBalance;
    }

    public double getBalance()
    {
        return this.balance;
    }
}
