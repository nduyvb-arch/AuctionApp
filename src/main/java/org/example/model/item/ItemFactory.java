package org.example.model.item;

public class ItemFactory {
    public static Item createItem(String category, String name, String type, String description, double startingPrice, double bidIncrement)
    {
        if (category == null)
        {
            throw new IllegalArgumentException("Category can't be null");
        }

        switch (category.toLowerCase().trim())
        {
            case "electronic":
                return new Electronic(name, type, description, startingPrice, bidIncrement);

            case "vehicle":
                return new Vehicle(name, type, description, startingPrice, bidIncrement);

            case "art":
                return new Art(name, type, description, startingPrice, bidIncrement);

            default:
                throw new IllegalArgumentException("Loại sản phẩm không hợp lệ" + category);
        }
    }
}
