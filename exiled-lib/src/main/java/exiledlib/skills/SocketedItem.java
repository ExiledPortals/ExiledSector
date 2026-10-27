package exiledlib.skills;

public interface SocketedItem {

    String HULL_ITEM = "hull_item";
    String SUBROUTINE = "subroutine";

    String category();

    String slotId();

    default String targetSlotId() {
        return null;
    }
}
