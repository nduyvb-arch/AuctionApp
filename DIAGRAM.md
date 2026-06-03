```mermaid
classDiagram
namespace org_example_common_model {
        class Message { #String sender }
    }
    namespace org_example_common_model_user {
        class Entity { #String id }
        class User { #String username }
        class Admin { }
        class Seller { }
        class Bidder { }
    }
    namespace org_example_common_model_item {
        class Item { #String name }
        class Vehicle { }
        class Art { }
        class Electronic { }
        class ItemFactory { }
        class AuctionStatus { <<enumeration>> }
    }
    namespace org_example_server_data {
        class DatabaseManager { }
        class ItemDAO { }
    }
    namespace org_example_server_manager {
        class AuctionManager { }
        class UserManager { }
        class AntiSniper { }
        class AutoBid { }
    }
    namespace org_example_server_network {
        class Subject { <<interface>> }
        class Observer { <<interface>> }
        class AuctionServer { }
        class ClientHandler { }
        class AuctionSession { }
        class AuctionNotifier { }
    }
    namespace org_example_client_controllers {
        class LoginController { }
        class HomeController { }
        class SignUpController { }
        class BidObserver { <<interface>> }
    }
User --|> Entity
    Item --|> Entity
    Admin --|> User
    Seller --|> User
    Bidder --|> User
    Vehicle --|> Item
    Art --|> Item
    Electronic --|> Item
    AuctionNotifier ..|> Subject
    ClientHandler ..|> Observer
    ItemDAO o-- DatabaseManager
    LoginController ..> NetworkClient
    HomeController o-- NetworkClient
