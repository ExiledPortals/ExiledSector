package exiledsector.ui.decoration;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI.ShipTypeHints;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;

final class FleetHulls {

    static final int MIN_SHIPS_PER_FLEET = 3;
    static final int MAX_SHIPS_PER_FLEET = 5;
    private static final Logger LOG = Logger.getLogger(FleetHulls.class);

    private FleetHulls() {
    }

    static boolean roams(ShipHullSpecAPI hullSpec) {
        if (hullSpec == null || hullSpec.getSpriteName() == null || hullSpec.isCivilianNonCarrier()) {
            return false;
        }
        HullSize hullSize = hullSpec.getHullSize();
        if (hullSize != HullSize.FRIGATE && hullSize != HullSize.DESTROYER && hullSize != HullSize.CRUISER && hullSize != HullSize.CAPITAL_SHIP) {
            return false;
        }
        Set<ShipTypeHints> hints = hullSpec.getHints();
        return hints == null || !(hints.contains(ShipTypeHints.STATION) || hints.contains(ShipTypeHints.MODULE)
                || hints.contains(ShipTypeHints.HIDE_IN_CODEX));
    }

    static List<ShipHullSpecAPI> pickHulls(List<ShipHullSpecAPI> hulls, Random random) {
        List<ShipHullSpecAPI> flagships = hulls.stream().filter(hull -> hull.getHullSize() != HullSize.FRIGATE).toList();
        List<ShipHullSpecAPI> escorts = hulls.stream()
                .filter(hull -> hull.getHullSize() == HullSize.FRIGATE || hull.getHullSize() == HullSize.DESTROYER).toList();
        int shipCount = MIN_SHIPS_PER_FLEET + random.nextInt(MAX_SHIPS_PER_FLEET - MIN_SHIPS_PER_FLEET + 1);
        List<ShipHullSpecAPI> picked = new ArrayList<>(shipCount);
        picked.add(randomHull(flagships.isEmpty() ? hulls : flagships, random));
        for (int i = 1; i < shipCount; i++) {
            picked.add(randomHull(escorts.isEmpty() ? hulls : escorts, random));
        }
        return picked;
    }

    private static ShipHullSpecAPI randomHull(List<ShipHullSpecAPI> hulls, Random random) {
        return hulls.get(random.nextInt(hulls.size()));
    }

    static List<ShipHullSpecAPI> roamingHulls(String factionId) {
        FactionAPI faction = Global.getSector().getFaction(factionId);
        if (faction == null || faction.getKnownShips() == null) {
            return List.of();
        }
        List<ShipHullSpecAPI> hulls = new ArrayList<>();
        for (String hullId : faction.getKnownShips()) {
            try {
                ShipHullSpecAPI hullSpec = Global.getSettings().getHullSpec(hullId);
                if (roams(hullSpec)) {
                    hulls.add(hullSpec);
                }
            } catch (RuntimeException e) {
                LOG.debug("Faction " + factionId + " knows hull " + hullId + " but it has no spec: " + e.getMessage());
            }
        }
        hulls.sort(Comparator.comparing(ShipHullSpecAPI::getHullId));
        return hulls;
    }
}
