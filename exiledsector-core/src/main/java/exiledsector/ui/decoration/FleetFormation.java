package exiledsector.ui.decoration;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.skills.skilleffect.render.SpriteCache;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

final class FleetFormation {

    static final Comparator<RoamingShip> LARGEST_FIRST = Comparator.comparingDouble(ship -> -ship.width() * ship.height());

    private final float fleetRadius;
    private final float largestSizeNum;
    private final int largestCount;

    private FleetFormation(float fleetRadius, float largestSizeNum, int largestCount) {
        this.fleetRadius = fleetRadius;
        this.largestSizeNum = largestSizeNum;
        this.largestCount = largestCount;
    }

    static FleetFormation of(List<ShipHullSpecAPI> hulls, float fleetRadius) {
        float largestSizeNum = 0f;
        int largestCount = 0;
        for (ShipHullSpecAPI hull : hulls) {
            float sizeNum = Misc.getSizeNum(hull.getHullSize());
            if (sizeNum > largestSizeNum) {
                largestSizeNum = sizeNum;
                largestCount = 1;
            } else if (Float.compare(sizeNum, largestSizeNum) == 0) {
                largestCount++;
            }
        }
        return new FleetFormation(fleetRadius, largestSizeNum, largestCount);
    }

    RoamingShip ship(ShipHullSpecAPI hull, SpriteCache spriteCache, float startFacingDeg, Random random) {
        return ship(hull, null, spriteCache, startFacingDeg, random);
    }

    RoamingShip ship(ShipHullSpecAPI hull, ShipVariantAPI variant, SpriteCache spriteCache, float startFacingDeg, Random random) {
        String spritePath = hull.getSpriteName();
        SpriteAPI texture = spritePath == null ? null : spriteCache.texture(spritePath);
        if (texture == null) {
            return null;
        }
        HullSize hullSize = hull.getHullSize();
        float sizeNum = Misc.getSizeNum(hullSize);
        boolean onlyLargest = Float.compare(sizeNum, largestSizeNum) == 0 && largestCount == 1;
        FleetShipDrift.HullMotion motion = FleetShipDrift.HullMotion.of(hullSize);
        float maxOffset = FleetShipDrift.maxOffset(fleetRadius, largestSizeNum, sizeNum, onlyLargest);
        FleetShipDrift drift = new FleetShipDrift(motion, maxOffset, startFacingDeg, random);
        HullModules modules = variant == null ? HullModules.of(hull, spriteCache) : HullModules.of(variant, spriteCache);
        return new RoamingShip(spritePath, texture.getWidth(), texture.getHeight(), hullSize, drift, HullEngines.of(FleetHullVisuals.of(hull)),
                modules);
    }

    List<RoamingShip> shipsLargestFirst(List<ShipHullSpecAPI> hulls, SpriteCache spriteCache, float startFacingDeg, Random random) {
        List<RoamingShip> ships = new ArrayList<>(hulls.size());
        for (ShipHullSpecAPI hull : hulls) {
            RoamingShip ship = ship(hull, spriteCache, startFacingDeg, random);
            if (ship != null) {
                ships.add(ship);
            }
        }
        ships.sort(LARGEST_FIRST);
        return ships;
    }
}
