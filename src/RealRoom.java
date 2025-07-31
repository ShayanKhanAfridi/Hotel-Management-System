public class RealRoom implements Room1 {
    private int roomNo;
    private String roomType;
    private double price;
    private boolean available;

    public RealRoom(int roomNo, String roomType, double price, boolean available) {
        this.roomNo = roomNo;
        this.roomType = roomType;
        this.price = price;
        this.available = available;
    }

    public int getRoomNo() {
        return roomNo;
    }

    public String getRoomType() {
        return roomType;
    }

    public double getPrice() {
        return price;
    }

    public boolean isAvailable() {
        return available;
    }
}
