package exiledsector.ui.socket;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.ui.decoration.RefitShipPortrait;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class PatternShipPortraits {

    private static final String PREVIEW_VARIANT_SUFFIX = "_exiledSector_patternPreview";

    private final Map<String, RefitShipPortrait> portraits = new HashMap<>();
    private final Set<String> unavailable = new HashSet<>();

    RefitShipPortrait of(String shipId, FleetMemberAPI member, String hullType) {
        RefitShipPortrait cached = portraits.get(shipId);
        if (cached != null || unavailable.contains(shipId)) {
            return cached;
        }
        RefitShipPortrait created = member != null ? RefitShipPortrait.of(member, member.getVariant()) : RefitShipPortrait.of(null, hullVariant(hullType));
        if (created == null) {
            unavailable.add(shipId);
        } else {
            portraits.put(shipId, created);
        }
        return created;
    }

    void retain(Set<String> shownShipIds) {
        portraits.entrySet().removeIf(entry -> {
            if (shownShipIds.contains(entry.getKey())) {
                return false;
            }
            entry.getValue().cleanup();
            return true;
        });
        unavailable.retainAll(shownShipIds);
    }

    public void cleanup() {
        portraits.values().forEach(RefitShipPortrait::cleanup);
        portraits.clear();
        unavailable.clear();
    }

    private static ShipVariantAPI hullVariant(String hullType) {
        if (hullType == null) {
            return null;
        }
        try {
            ShipHullSpecAPI hullSpec = Global.getSettings().getHullSpec(hullType);
            return hullSpec == null ? null : Global.getSettings().createEmptyVariant(hullType + PREVIEW_VARIANT_SUFFIX, hullSpec);
        } catch (RuntimeException missingHull) {
            return null;
        }
    }
}
