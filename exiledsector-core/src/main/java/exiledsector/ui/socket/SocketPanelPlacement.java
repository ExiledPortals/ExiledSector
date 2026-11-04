package exiledsector.ui.socket;

public record SocketPanelPlacement(float panelLeft, float panelTop, float panelWidth, float panelHeight, boolean instantly,
                                   FreeArea advancedSearchArea) {

    public SocketPanelPlacement(float panelLeft, float panelTop, float panelWidth, float panelHeight, boolean instantly) {
        this(panelLeft, panelTop, panelWidth, panelHeight, instantly, null);
    }

    public SocketPanelPlacement withAdvancedSearchArea(FreeArea searchArea) {
        return new SocketPanelPlacement(panelLeft, panelTop, panelWidth, panelHeight, instantly, searchArea);
    }
}
