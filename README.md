# 🛒 Hệ Thống Đấu Giá Trực Tuyến (Online Auction System)
Dự án Lập trình nâng cao - Nhóm 2.

## 🏗️ Sơ đồ kiến trúc lớp (Class Diagram)
Dưới đây là sơ đồ lớp thiết kế theo chuẩn Hướng đối tượng (OOP) và Design Patterns của hệ thống:

```mermaid
classDiagram
    class Entity {
        <<abstract>>
        #String id
        +getId() String
        +setId(String id) void
    }

    class User {
        <<abstract>>
        #String username
        #String password
        +getUsername() String
        +getPassword() String
        +displayRole()* void
    }

    class Bidder {
        +placeBid(Auction auction, double amount) void
        +displayRole() void
    }

    class Seller {
        +createItem(String type, String name, double price) Item
        +displayRole() void
    }

    class Admin {
        +banUser(User user) void
        +displayRole() void
    }

    class Item {
        <<abstract>>
        #String name
        #double startingPrice
        +getName() String
        +getStartingPrice() double
        +printInfo()* void
    }

    class Electronics {
        -int warrantyMonths
        +printInfo() void
    }

    class Art {
        -String artistName
        +printInfo() void
    }

    class Vehicle {
        -int yearOfManufacture
        +printInfo() void
    }

    class Auction {
        -Item item
        -Seller seller
        -double currentHighestBid
        -String status
        +processBid(BidTransaction bid) boolean
        +closeAuction() void
    }

    class BidTransaction {
        -Bidder bidder
        -double bidAmount
        -LocalDateTime timestamp
    }

    class AuctionManager {
        <<Singleton>>
        -static AuctionManager instance
        -List~Auction~ activeAuctions
        +getInstance()$ AuctionManager
    }

    class ItemFactory {
        <<Factory>>
        +createItem(String type, String id, String name, double price)$ Item
    }

    Entity <|-- User
    Entity <|-- Item
    User <|-- Bidder
    User <|-- Seller
    User <|-- Admin
    Item <|-- Electronics
    Item <|-- Art
    Item <|-- Vehicle
    Auction "1" *-- "1" Item
    Auction "1" o-- "1" Seller
    Auction "1" *-- "*" BidTransaction
    BidTransaction "*" o-- "1" Bidder
    AuctionManager "1" o-- "*" Auction
    ItemFactory ..> Item