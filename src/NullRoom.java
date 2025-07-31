public class NullRoom implements Room1 {
    public int getRoomNo() {
        return -1;
    }

    public String getRoomType() {
        return "No Room";
    }

    public double getPrice() {
        return 0.0;
    }

    public boolean isAvailable() {
        return false;
    }
}
