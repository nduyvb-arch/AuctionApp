```mermaid
classDiagram
    %% Cụm Người dùng (User)
    class User {
        <<abstract>>
        #String id
        #String username
        #String password
        +getUsername() String
    }
    class Admin {
        +banUser(User user) void
    }
    class Seller {
        +createItem() void
    }
    class Bidder {
        +placeBid() void
    }

    %% Cụm Sản phẩm (Item)
    class Item {
        <<abstract>>
        #String name
        #double startingPrice
    }
    class Vehicle { -int yearOfManufacture }
    class Art { -String artistName }
    class Electronic { -int warrantyMonths }

    %% Cụm Quản lý Đấu giá (AuctionManager)
    class AuctionManager {
        <<Singleton>>
        -static AuctionManager instance
        -List activeAuctions
        +getInstance() AuctionManager
        +createAuction() void
    }

    %% Mối quan hệ kế thừa và liên kết
    Admin --|> User
    Seller --|> User
    Bidder --|> User
    
    Vehicle --|> Item
    Art --|> Item
    Electronic --|> Item

    AuctionManager ..> Item : Manages
    AuctionManager ..> User : Verifies
