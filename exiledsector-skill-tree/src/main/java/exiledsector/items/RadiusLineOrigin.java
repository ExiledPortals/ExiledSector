package exiledsector.items;

import exiledsector.skills.SocketedItem;
import exiledsector.skills.AllocatedNode;

public record RadiusLineOrigin(AllocatedNode node, String sourceSocketNodeId) implements SocketedItem {

    @Override
    public String category() {
        return SUBROUTINE;
    }

    @Override
    public String slotId() {
        return sourceSocketNodeId;
    }
}
