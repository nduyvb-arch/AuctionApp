package org.example.manager;
import org.example.model.item.AuctionStatus;
import org.example.model.item.Item;

import java.time.LocalDateTime;
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

        if (targetItem.getStatus() != AuctionStatus.ACTIVE)
        {
            return "Lỗi: Phiên đấu giá chưa bắt đầu hoặc đã kết thúc";
        }
        if (targetItem.getEndTime() != null && LocalDateTime.now().isAfter(targetItem.getEndTime()))
        {
            return "Lỗi: phiên đấu giá đã kết thúc";
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

    // Hàm mở phiên đấu giá mới cho 1 sản phẩm
    public synchronized String startAuction(String itemId, int durationInMinutes)
    {
        Item targetItem = null;
        for (Item item: auctionItems)
        {
            if (item.getId().equals(itemId))
            {
                targetItem = item;
                break;
            }
        }
        if (targetItem == null)
        {
            return "Lỗi: Sản phầm không hợp lệ";
        }
        if (targetItem.getStatus() != AuctionStatus.PENDING)
        {
            return "Lỗi: Sản phẩm đang mở ở phiên khác hoặc đã đóng";
        }
        targetItem.setStatus(AuctionStatus.ACTIVE);
        targetItem.setEndTime(LocalDateTime.now().plusMinutes(durationInMinutes));

        return "Đã bắt đầu 1 phiên đấu giá cho sản phầm: (" + targetItem.getItemName() + "). Thời gian đấu giá: " + durationInMinutes + " phút";
    }

    public synchronized List<String> checkAndCloseExpiredAuctions
    {
        List<String> notifications = new ArrayList<>();

        for (Item item : auctionItems)
        {
            if (item.getStatus() == AuctionStatus.ACTIVE && item.getEndTime() != null && LocalDateTime.now().isAfter(item.getEndTime()))
            {
                item.setStatus(AuctionStatus.CLOSED);

                String msg;

                if (item.getCurrentWinnerId() != null)
                {
                    msg = "ĐẤU GIÁ KẾT THÚC: Sản phẩm [" + item.getItemName() + "] đã thuộc về " + item.getCurrentWinnerId() + " với giá " + item.getCurrentPrice();
                }
                else {
                    msg = "ĐẤU GIÁ KẾT THÚC: Sản phẩm [" + item.getItemName() + "] không có ai đặt giá";
                }

                notifications.add(msg);
            }
        }

        return notifications;
    }
}
