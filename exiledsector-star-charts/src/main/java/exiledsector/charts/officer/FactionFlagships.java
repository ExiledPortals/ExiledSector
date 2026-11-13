package exiledsector.charts.officer;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI.ShipTypeHints;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import exiledsector.ui.decoration.FleetHulls;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class FactionFlagships implements FlagshipCatalogue {

    public static final FactionFlagships INSTANCE = new FactionFlagships();
    static final Map<HullSize, Float> LOWEST_RANK_SIZE_WEIGHTS = Map.of(HullSize.FRIGATE, 0.5f, HullSize.DESTROYER, 1f,
            HullSize.CRUISER, 5f, HullSize.CAPITAL_SHIP, 3.5f);
    static final Map<HullSize, Float> HIGHEST_RANK_SIZE_WEIGHTS = Map.of(HullSize.FRIGATE, 0f, HullSize.DESTROYER, 0f,
            HullSize.CRUISER, 1f, HullSize.CAPITAL_SHIP, 4f);
    static final float SMALL_HULL_DP_SHARE = 0.5f;
    private static final List<HullSize> SIZES = List.of(HullSize.FRIGATE, HullSize.DESTROYER, HullSize.CRUISER, HullSize.CAPITAL_SHIP);
    private static final Set<ShipTypeHints> LOGISTICS_HINTS = Set.of(ShipTypeHints.CIVILIAN, ShipTypeHints.FREIGHTER, ShipTypeHints.TANKER,
            ShipTypeHints.LINER, ShipTypeHints.TRANSPORT);
    private static final Set<String> LOGISTICS_DESIGNATION_WORDS = Set.of("freighter", "superfreighter", "tanker", "transport", "tender",
            "shuttle", "liner", "tug", "hauler");
    private static final Logger LOG = Logger.getLogger(FactionFlagships.class);

    record Candidate(String hullId, HullSize hullSize, float deploymentPoints, List<String> variantIds) {
    }

    private FactionFlagships() {
    }

    @Override
    public Flagship pick(String factionId, int rankOrder, int rankCount, Random random) {
        Candidate picked = choose(candidates(factionId), rankCount <= 1 ? 1f : (float) rankOrder / (rankCount - 1), random);
        return picked == null ? null : new Flagship(picked.hullId(), picked.variantIds().get(random.nextInt(picked.variantIds().size())));
    }

    static Candidate choose(List<Candidate> candidates, float rankShare, Random random) {
        Map<HullSize, List<Candidate>> bySize = new EnumMap<>(HullSize.class);
        for (Candidate candidate : candidates) {
            bySize.computeIfAbsent(candidate.hullSize(), size -> new ArrayList<>()).add(candidate);
        }
        HullSize size = pickSize(bySize.keySet(), rankShare, random);
        if (size == null) {
            return null;
        }
        List<Candidate> pool = isSmall(size) ? highestDeploymentPoints(bySize.get(size)) : bySize.get(size);
        return pool.get(random.nextInt(pool.size()));
    }

    static float sizeWeight(HullSize hullSize, float rankShare) {
        float lowest = LOWEST_RANK_SIZE_WEIGHTS.get(hullSize);
        return lowest + (HIGHEST_RANK_SIZE_WEIGHTS.get(hullSize) - lowest) * rankShare;
    }

    private static HullSize pickSize(Set<HullSize> available, float rankShare, Random random) {
        List<HullSize> sizes = SIZES.stream().filter(available::contains).toList();
        float totalWeight = 0f;
        for (HullSize size : sizes) {
            totalWeight += sizeWeight(size, rankShare);
        }
        if (totalWeight <= 0f) {
            return sizes.isEmpty() ? null : sizes.get(sizes.size() - 1);
        }
        float roll = random.nextFloat() * totalWeight;
        for (HullSize size : sizes) {
            roll -= sizeWeight(size, rankShare);
            if (roll < 0f) {
                return size;
            }
        }
        return sizes.get(sizes.size() - 1);
    }

    private static boolean isSmall(HullSize size) {
        return size == HullSize.FRIGATE || size == HullSize.DESTROYER;
    }

    static List<Candidate> highestDeploymentPoints(List<Candidate> sameSize) {
        List<Candidate> sorted = sameSize.stream()
                .sorted(Comparator.comparingDouble(Candidate::deploymentPoints).reversed().thenComparing(Candidate::hullId)).toList();
        int kept = Math.max(1, (int) Math.ceil(sorted.size() * SMALL_HULL_DP_SHARE));
        return sorted.subList(0, kept);
    }

    static boolean isLogistics(ShipHullSpecAPI hullSpec) {
        Set<ShipTypeHints> hints = hullSpec.getHints();
        if (hints != null && hints.stream().anyMatch(LOGISTICS_HINTS::contains)) {
            return true;
        }
        String designation = hullSpec.getDesignation();
        if (designation == null) {
            return false;
        }
        for (String word : designation.toLowerCase(Locale.ROOT).split("[^a-z]+")) {
            if (LOGISTICS_DESIGNATION_WORDS.contains(word)) {
                return true;
            }
        }
        return false;
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
                List<String> variantIds = FleetHulls.roams(hullSpec) && !isLogistics(hullSpec) ? fittedVariants(hullId) : List.of();
                if (!variantIds.isEmpty()) {
                    candidates.add(new Candidate(hullId, hullSpec.getHullSize(), hullSpec.getSuppliesToRecover(), variantIds));
                }
            } catch (RuntimeException e) {
                LOG.debug("[ExiledSector] Faction " + factionId + " knows hull " + hullId + " but it has no usable spec: " + e.getMessage());
            }
        }
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
