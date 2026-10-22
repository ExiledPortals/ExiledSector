package exiledsector.ui.decoration;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;

record RoamingShip(String spritePath, float width, float height, HullSize hullSize, FleetShipDrift drift, HullEngines engines) {

    float longestSide() {
        return Math.max(width, height) * drift.motion().scaleMult();
    }
}
