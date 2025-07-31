public class RoomTableRefresher implements RoomObserver {
    private ManageRooms manager;

    public RoomTableRefresher(ManageRooms manager) {
        this.manager = manager;
    }

    @Override
    public void onRoomChange(String action, String roomNo) {
        manager.loadRoomData(); // refresh table method
        System.out.println("✅ Table refreshed after: " + action + " (Room No: " + roomNo + ")");
    }
}
