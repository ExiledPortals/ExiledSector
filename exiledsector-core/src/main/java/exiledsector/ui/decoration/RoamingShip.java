package exiledsector.ui.decoration;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.skilleffect.render.SpriteCache;

record RoamingShip(String spritePath, float width, float height, HullSize hullSize, FleetShipDrift drift, HullEngines engines) {

    float longestSide() {
        return Math.max(width, height) * drift.motion().scaleMult();
    }

    void drawHull(SpriteCache spriteCache, float screenX, float screenY, float screenScale, float alpha) {
        engines.drawHull(spriteCache, spritePath, screenX, screenY, width * screenScale, height * screenScale, screenScale, drift.facingDeg(), alpha);
    }
}
