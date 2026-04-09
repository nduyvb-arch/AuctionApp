package org.example.manager;
import org.example.model.item.Item;
import java.util.ArrayList;
import java.util.List;

public class AuctionManager {
    private static volatile AuctionManager instance;
    // ds sản phẩm được đấu giá
    private List<Item> auctionItems;

    private AuctionManager()
    {
        auctionItems = new ArrayList<>();
    }

    public static synchronized AuctionManager getInstance()
    {
        if (instance == null)
        {
            instance = new AuctionManager();
        }

        return instance;
    }

    public void addItem(Item item)
    {
        auctionItems.add(item);
    }

    public List<Item> getAllItems()
    {
        return this.auctionItems;
    }

    public synchronized String placeBid(String itemId, double bidAmount, String bidderId)
    {
        Item targetItem = null;
        // Tìm sản phầm trong danh sách
        for (Item item : auctionItems)
        {
            if (item.getId().equals(itemId))
            {
                targetItem = item;
                break;
            }
        }

        // Lỗi nếu ID không hợp lệ
        if (targetItem == null)
        {
            return "Lỗi, sản phẩm cần tìm không tồn tại";
        }

        // Kiểm tra tính hợp lệ của giá được đấu
        double minRequiredBid = targetItem.getStartingPrice();

        if (targetItem.getCurrentPrice() > 0)
        {
            minRequiredBid = targetItem.getCurrentPrice() + targetItem.getBidIncrement();
        }

        if (bidAmount < minRequiredBid)
        {
            return "Lỗi, giá thấp nhất có thể là: " + minRequiredBid;
        }

        // Cập nhật phiên đấu giá nếu hợp lệ
        targetItem.setCurrentWinnerId(bidderId);
        targetItem.setCurrentPrice(bidAmount);

        return "Đặt giá thành công, giá của bạn đang cao nhất với mức giá " + bidAmount + " cho sản phẩm " + itemId;
    }
}
