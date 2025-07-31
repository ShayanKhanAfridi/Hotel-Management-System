public class RoomFactory {
    public static Room createRoom(String type) {
        if (type == null) {
            throw new IllegalArgumentException("Room type cannot be null.");
        }

        String normalizedType = type.trim().toLowerCase();

        if (normalizedType.contains("single")) {
            return new SingleRoom();
        } else if (normalizedType.contains("double")) {
            return new DoubleRoom();
        } else if (normalizedType.contains("deluxe")) {
            return new DeluxeRoom();
        } else if (normalizedType.contains("suite")) {
            return new SuiteRoom();
        } else {
            throw new IllegalArgumentException("Invalid room type: " + type);
        }
    }
}
