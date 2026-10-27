package exiledlib.skills;

public interface SocketedItem {

    String slotId();

    default String targetSlotId() {
        return null;
    }
}
