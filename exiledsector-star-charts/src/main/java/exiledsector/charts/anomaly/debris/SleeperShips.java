package exiledsector.charts.anomaly.debris;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

public final class SleeperShips {

    static final float WHOLE_SHARE = 100f;
    static final List<HullSize> SHIP_SIZES = List.of(HullSize.FRIGATE, HullSize.DESTROYER, HullSize.CRUISER, HullSize.CAPITAL_SHIP);

    public record Shares(float remnantPercent, float threatPercent, float dwellerPercent) {

        public boolean any() {
            return remnantPercent > 0f || threatPercent > 0f || dwellerPercent > 0f;
        }

        public String factionFor(float roll) {
            float rolledPercent = roll * WHOLE_SHARE;
            float upTo = 0f;
            for (Map.Entry<String, Float> share : List.of(Map.entry(Factions.REMNANTS, remnantPercent), Map.entry(Factions.THREAT, threatPercent),
                    Map.entry(Factions.DWELLER, dwellerPercent))) {
                upTo = Math.min(WHOLE_SHARE, upTo + Math.max(0f, share.getValue()));
                if (rolledPercent < upTo) {
                    return share.getKey();
                }
            }
            return null;
        }
    }

    record Candidate(String variantId, HullSize hullSize, float deploymentPoints) {
    }

    private SleeperShips() {
    }

    static Map<FleetMemberAPI, String> replace(CampaignFleetAPI fleet, Shares shares, Random random) {
        return replace(fleet, shares, random, SleeperShips::candidatesOf);
    }

    static Map<FleetMemberAPI, String> replace(CampaignFleetAPI fleet, Shares shares, Random random, Function<String, List<Candidate>> candidatesByFaction) {
        Map<FleetMemberAPI, String> replaced = new LinkedHashMap<>();
        if (shares == null || !shares.any()) {
            return replaced;
        }
        Map<String, List<Candidate>> candidates = new HashMap<>();
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            String factionId = shares.factionFor(random.nextFloat());
            ShipHullSpecAPI hull = member.getHullSpec();
            if (factionId == null || hull == null) {
                continue;
            }
            Candidate pick = equivalent(candidates.computeIfAbsent(factionId, candidatesByFaction), hull.getHullSize(), hull.getSuppliesToRecover(), random);
            FleetMemberAPI replacement = pick == null ? null : fleet.getFleetData().addFleetMember(pick.variantId());
            if (replacement != null) {
                fleet.getFleetData().removeFleetMember(member);
                replaced.put(replacement, factionId);
            }
        }
        return replaced;
    }

    static Candidate equivalent(List<Candidate> candidates, HullSize hullSize, float deploymentPoints, Random random) {
        List<Candidate> closest = new ArrayList<>();
        int closestSizeGap = Integer.MAX_VALUE;
        float closestPointGap = Float.MAX_VALUE;
        for (Candidate candidate : candidates) {
            int sizeGap = sizeGap(candidate.hullSize(), hullSize);
            float pointGap = Math.abs(candidate.deploymentPoints() - deploymentPoints);
            if (sizeGap < closestSizeGap || sizeGap == closestSizeGap && pointGap < closestPointGap) {
                closest.clear();
                closestSizeGap = sizeGap;
                closestPointGap = pointGap;
            }
            if (sizeGap == closestSizeGap && pointGap == closestPointGap) {
                closest.add(candidate);
            }
        }
        return closest.isEmpty() ? null : closest.get(random.nextInt(closest.size()));
    }

    private static int sizeGap(HullSize first, HullSize second) {
        int firstIndex = SHIP_SIZES.indexOf(first);
        int secondIndex = SHIP_SIZES.indexOf(second);
        return firstIndex < 0 || secondIndex < 0 ? SHIP_SIZES.size() : Math.abs(firstIndex - secondIndex);
    }

    static List<Candidate> candidatesOf(String factionId) {
        FactionAPI faction = Global.getSector().getFaction(factionId);
        return faction == null ? List.of() : candidatesOf(faction, Global.getSettings());
    }

    static List<Candidate> candidatesOf(FactionAPI faction, SettingsAPI settings) {
        Set<String> variantIds = new LinkedHashSet<>();
        for (Map.Entry<String, Float> role : DormantFleets.ROLES) {
            Set<String> roleVariants = faction.getVariantsForRole(role.getKey());
            if (roleVariants != null) {
                variantIds.addAll(roleVariants);
            }
        }
        List<Candidate> candidates = new ArrayList<>();
        for (String variantId : variantIds) {
            ShipVariantAPI variant = settings.doesVariantExist(variantId) ? settings.getVariant(variantId) : null;
            ShipHullSpecAPI hull = variant == null ? null : variant.getHullSpec();
            if (hull != null && SHIP_SIZES.contains(hull.getHullSize())) {
                candidates.add(new Candidate(variantId, hull.getHullSize(), hull.getSuppliesToRecover()));
            }
        }
        return candidates;
    }
}
