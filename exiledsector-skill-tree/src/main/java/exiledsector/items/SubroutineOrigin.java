package exiledsector.items;

import exiledlib.skills.SocketedItem;
import exiledsector.skills.AllocatedNode;

public record SubroutineOrigin(AllocatedNode socket) implements SocketedItem {

    @Override
    public String category() {
        return SUBROUTINE;
    }

    @Override
    public String slotId() {
        return socket.node().getId();
    }
}
