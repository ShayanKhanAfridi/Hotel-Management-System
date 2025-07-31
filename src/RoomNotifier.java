import java.util.ArrayList;
import java.util.List;

public class RoomNotifier {
    private List<RoomObserver> observers = new ArrayList<>();

    public void addObserver(RoomObserver observer) {
        observers.add(observer);
    }

    public void notifyObservers(String action, String roomNo) {
        for (RoomObserver observer : observers) {
            observer.onRoomChange(action, roomNo);
        }
    }
}
