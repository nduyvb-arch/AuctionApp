classDiagram
    %% Gói common.model (Shared Data Structures)
    namespace org_example_common_model {
        class Message {
            #String sender
            #String content
            #LocalDateTime timestamp
        }
    }
    namespace org_example_common_model_user {
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
        class Admin {
            +banUser(User user) void
        }
        class Seller {
            +createItem() void
        }
        class Bidder {
            +placeBid() void
        }
    }
    namespace org_example_common_model_item {
        class Item {
            <<abstract>>
            #String name
            #double startingPrice
        }
        class Vehicle { -int yearOfManufacture }
        class Art { -String artistName }
        class Electronic { -int warrantyMonths }
        class ItemFactory {
            <<Factory>>
            +createItem() Item
        }
        class AuctionStatus {
            <<enumeration>>
            OPEN
            RUNNING
            FINISHED
            PAID
            CANCELED
        }
    }
    namespace org_example_common_model_chat {
        class AuctionChatMessage {
            -String auctionId
        }
    }

    %% Gói server (Backend Logic)
    namespace org_example_server_data {
        class DatabaseManager {
            -Connection connection
            +connect() void
        }
        class ItemDAO {
            -DatabaseManager db
            +save(Item item) void
        }
    }
    namespace org_example_server_manager {
        class AuctionManager {
            <<Singleton>>
            -static AuctionManager instance
            +getInstance() AuctionManager
        }
        class UserManager {
            <<Singleton>>
            -Map users
        }
        class AntiSniper {
            +checkAndExtend() void
        }
        class AutoBid {
            +executeAutoBid() void
        }
    }
    namespace org_example_server_network {
        class Subject {
            <<interface>>
            +registerObserver() void
        }
        class Observer {
            <<interface>>
            +update() void
        }
        class AuctionServer { -int port }
        class ClientHandler { -Socket socket }
        class AuctionSession { -String sessionId }
        class AuctionNotifier { -List observers }
    }

    %% Gói client (Frontend UI & Network)
    namespace org_example_client {
        class ClientApp { +start() void }
        class Launcher { +main() void }
    }
    namespace org_example_client_network {
        class NetworkClient {
            <<Singleton>>
            -Socket socket
        }
    }
    namespace org_example_client_controllers {
        class LoginController { +handleLogin() void }
        class HomeController { +handleJoinAuction() void }
        class SignUpController {}
        class AccountViewController {}
        class MyItemsController {}
        class WatchlistController {}
        class SalesHistoryController {}
        class RoleSelectionController {}
        class AddItemDialog {}
        class AddItemViewController {}
        class BidDialog {}
        class BidHistoryController {}
        class BidObserver { <<interface>> }
    }
    namespace org_example_client_controllers_admin {
        class AdminChildController { <<interface>> }
        class AdminDashboardController {}
        class AdminAuctionsController {}
        class AdminItemsController {}
        class AdminOverviewController {}
        class AdminStatsController {}
        class AdminUsersController {}
    }

    %% Mối quan hệ giữa các lớp
    User --|> Entity
    Item --|> Entity
    Admin --|> User
    Seller --|> User
    Bidder --|> User
    Vehicle --|> Item
    Art --|> Item
    Electronic --|> Item
    AuctionChatMessage --|> Message
    AuctionNotifier ..|> Subject
    ClientHandler ..|> Observer
    AdminDashboardController o-- AdminChildController
    ItemDAO o-- DatabaseManager
    
    NetworkClient ..> ClientHandler : Socket Connection
    LoginController ..> NetworkClient
    HomeController o-- NetworkClient
