package exiledlib.skills;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledlib.i18n.Message;

public record HullSizeTable(float fighter, float frigate, float destroyer, float cruiser, float capital, float otherwise) {

    public float of(HullSize hullSize) {
        if (hullSize == null) {
            return otherwise;
        }
        return switch (hullSize) {
            case FIGHTER -> fighter;
            case FRIGATE -> frigate;
            case DESTROYER -> destroyer;
            case CRUISER -> cruiser;
            case CAPITAL_SHIP -> capital;
            default -> otherwise;
        };
    }

    public Message withShipSizeArgs(Message message) {
        return message.arg("frigate", frigate).arg("destroyer", destroyer).arg("cruiser", cruiser).arg("capital", capital);
    }
}
