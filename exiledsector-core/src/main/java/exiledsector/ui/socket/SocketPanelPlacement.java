package exiledsector.ui.socket;

public record SocketPanelPlacement(float panelLeft, float panelTop, float panelWidth, float panelHeight, boolean instantly,
                                   AdvancedSearchRoom advancedSearchRoom) {

    public SocketPanelPlacement(float panelLeft, float panelTop, float panelWidth, float panelHeight, boolean instantly) {
        this(panelLeft, panelTop, panelWidth, panelHeight, instantly, null);
    }

    public SocketPanelPlacement withAdvancedSearchArea(FreeArea searchArea) {
        return withAdvancedSearchRoom(AdvancedSearchRoom.fixed(searchArea));
    }

    public SocketPanelPlacement withAdvancedSearchRoom(AdvancedSearchRoom searchRoom) {
        return new SocketPanelPlacement(panelLeft, panelTop, panelWidth, panelHeight, instantly, searchRoom);
    }
}
