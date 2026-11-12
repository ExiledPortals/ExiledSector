package exiledsector.charts.officer;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import exiledsector.ui.decoration.FleetHulls;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public final class FactionFlagships implements FlagshipCatalogue {

    public static final FactionFlagships INSTANCE = new FactionFlagships();
    private static final List<HullSize> SIZES_BY_RANK = List.of(HullSize.FRIGATE, HullSize.DESTROYER, HullSize.CRUISER, HullSize.CAPITAL_SHIP);
    private static final Logger LOG = Logger.getLogger(FactionFlagships.class);

    private record Candidate(String hullId, HullSize hullSize, List<String> variantIds) {
    }

    private FactionFlagships() {
    }

    @Override
    public Flagship pick(String factionId, int rankOrder, int rankCount, Random random) {
        List<Candidate> candidates = candidates(factionId);
        if (candidates.isEmpty()) {
            return null;
        }
        float leaning = rankCount <= 1 ? 0f : (float) rankOrder / (rankCount - 1) * (SIZES_BY_RANK.size() - 1);
        float totalWeight = 0f;
        float[] weights = new float[candidates.size()];
        for (int index = 0; index < candidates.size(); index++) {
            weights[index] = sizeWeight(candidates.get(index).hullSize(), leaning);
            totalWeight += weights[index];
        }
        float roll = random.nextFloat() * totalWeight;
        Candidate picked = candidates.get(candidates.size() - 1);
        for (int index = 0; index < candidates.size(); index++) {
            roll -= weights[index];
            if (roll < 0f) {
                picked = candidates.get(index);
                break;
            }
        }
        return new Flagship(picked.hullId(), picked.variantIds().get(random.nextInt(picked.variantIds().size())));
    }

    static float sizeWeight(HullSize hullSize, float leaning) {
        int sizeOrder = SIZES_BY_RANK.indexOf(hullSize);
        return 1f / (1f + 2f * Math.abs(sizeOrder - leaning));
    }

    private static List<Candidate> candidates(String factionId) {
        FactionAPI faction = Global.getSector() == null ? null : Global.getSector().getFaction(factionId);
        if (faction == null || faction.getKnownShips() == null) {
            return List.of();
        }
        List<Candidate> candidates = new ArrayList<>();
        for (String hullId : faction.getKnownShips().stream().sorted().toList()) {
            try {
                ShipHullSpecAPI hullSpec = Global.getSettings().getHullSpec(hullId);
                List<String> variantIds = FleetHulls.roams(hullSpec) ? fittedVariants(hullId) : List.of();
                if (!variantIds.isEmpty()) {
                    candidates.add(new Candidate(hullId, hullSpec.getHullSize(), variantIds));
                }
            } catch (RuntimeException e) {
                LOG.debug("[ExiledSector] Faction " + factionId + " knows hull " + hullId + " but it has no usable spec: " + e.getMessage());
            }
        }
        candidates.sort(Comparator.comparing(Candidate::hullId));
        return candidates;
    }

    private static List<String> fittedVariants(String hullId) {
        List<String> variantIds = Global.getSettings().getHullIdToVariantListMap().get(hullId);
        if (variantIds == null) {
            return List.of();
        }
        List<String> fitted = new ArrayList<>();
        for (String variantId : variantIds.stream().sorted().toList()) {
            ShipVariantAPI variant = Global.getSettings().getVariant(variantId);
            if (variant != null && !variant.isEmptyHullVariant()) {
                fitted.add(variantId);
            }
        }
        return fitted;
    }
}
