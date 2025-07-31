public class RoomLogger implements RoomObserver {
    @Override
    public void onRoomChange(String action, String roomNo) {
        System.out.println("📝 ROOM " + action.toUpperCase() + ": Room No " + roomNo);
    }
}
