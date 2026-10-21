package exiledsector.ui.cargo;

import exiledsector.compat.TaniaCompat;

public record AnchorLook(String crestFactionId) {

    public enum Style { LOCK_ON, PLATE_SHUTTERS }

    public Style style() {
        return TaniaCompat.FACTION_ID.equals(crestFactionId) ? Style.PLATE_SHUTTERS : Style.LOCK_ON;
    }
}
