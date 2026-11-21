package exiledsector.charts.anomaly.holdouts;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.content.ChartAnomalyFleets;
import exiledsector.charts.content.ChartContentGenerator;
import exiledsector.charts.content.ChartContentPlan.FleetRole;
import exiledsector.charts.content.ChartSpot;
import exiledsector.charts.generation.ChartTier;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

final class HoldoutFleets {

    static final int MIN_SMALL_FLEETS = 3;
    static final int MAX_SMALL_FLEETS = 5;
    static final int MIN_LARGE_FLEETS = 1;
    static final int MAX_LARGE_FLEETS = 2;
    static final float SMALL_FLEET_SHARE = 0.5f;
    static final float LARGE_FLEET_SHARE = 1.5f;
    static final long COUNT_SALT = 0x484f4c44434e54L;
    static final long FLEET_SALT = 0x484f4c44464c54L;
    static final float BOSS_TRUCE_DAYS = 100000f;
    private static final Logger LOG = Logger.getLogger(HoldoutFleets.class);

    record Planned(String fleetType, float sizePercent) {
    }

    private HoldoutFleets() {
    }

    static List<Planned> plan(ChartTier tier, Random random) {
        List<Planned> planned = new ArrayList<>();
        int small = MIN_SMALL_FLEETS + random.nextInt(MAX_SMALL_FLEETS - MIN_SMALL_FLEETS + 1);
        int large = MIN_LARGE_FLEETS + random.nextInt(MAX_LARGE_FLEETS - MIN_LARGE_FLEETS + 1);
        for (int i = 0; i < small; i++) {
            planned.add(new Planned(FleetTypes.PATROL_SMALL, tier.fleetSize() * SMALL_FLEET_SHARE));
        }
        for (int i = 0; i < large; i++) {
            planned.add(new Planned(FleetTypes.PATROL_LARGE, tier.fleetSize() * LARGE_FLEET_SHARE));
        }
        return planned;
    }

    static void spawn(AnomalyContentSite site, String holdoutFactionId) {
        ChartTier tier = ChartAnomalyFleets.tier(site.tier(), site.context());
        Random fleetRandom = site.random(FLEET_SALT);
        List<ChartSpot> spots = ChartAnomalyFleets.openSpots(site.system(), site.entry(), fleetRandom);
        Set<CampaignFleetAPI> bosses = bossesOf(site);
        Set<String> occupierFactionIds = turnOccupiersAgainst(site, holdoutFactionId, bosses);
        List<CampaignFleetAPI> holdouts = new ArrayList<>();
        for (Planned planned : plan(tier, site.random(COUNT_SALT))) {
            long fleetSeed = fleetRandom.nextLong();
            try {
                CampaignFleetAPI fleet = spawnOne(site, holdoutFactionId, tier, planned, fleetSeed, spot(spots, fleetRandom, site));
                if (fleet != null) {
                    occupierFactionIds.forEach(occupier -> ChartAnomalyFleets.makeHostileTo(fleet, occupier));
                    holdouts.add(fleet);
                }
            } catch (RuntimeException | LinkageError e) {
                LOG.error("[ExiledSector] A " + holdoutFactionId + " holdout fleet could not be spawned", e);
            }
        }
        bosses.forEach(boss -> keepOutOfTheFight(boss, holdoutFactionId, holdouts));
        if (!holdouts.isEmpty()) {
            site.addScript(new HoldoutRestraint(site.runId(), site.tier(), site.system(), holdoutFactionId,
                    holdouts.stream().map(CampaignFleetAPI::getId).toList()));
        }
    }

    private static Set<CampaignFleetAPI> bossesOf(AnomalyContentSite site) {
        Set<CampaignFleetAPI> bosses = new LinkedHashSet<>(site.forces().fleets(FleetRole.BOSS));
        if (site.forces().boss() != null) {
            bosses.add(site.forces().boss());
        }
        return bosses;
    }

    private static Set<String> turnOccupiersAgainst(AnomalyContentSite site, String holdoutFactionId, Set<CampaignFleetAPI> bosses) {
        Set<CampaignFleetAPI> occupiers = new LinkedHashSet<>(site.forces().fleets());
        occupiers.addAll(bosses);
        Set<String> occupierFactionIds = new LinkedHashSet<>(List.of(site.factionId()));
        for (CampaignFleetAPI occupier : occupiers) {
            String occupierFactionId = occupier.getFaction() == null || occupier.getFaction().getId() == null
                    ? site.factionId() : occupier.getFaction().getId();
            if (holdoutFactionId.equals(occupierFactionId)) {
                continue;
            }
            occupierFactionIds.add(occupierFactionId);
            if (!bosses.contains(occupier)) {
                ChartAnomalyFleets.makeHostileTo(occupier, holdoutFactionId);
            }
        }
        return occupierFactionIds;
    }

    private static void keepOutOfTheFight(CampaignFleetAPI boss, String holdoutFactionId, List<CampaignFleetAPI> holdouts) {
        boss.getMemoryWithoutUpdate().set(MemFlags.FLEET_IGNORED_BY_FACTION, holdoutFactionId);
        if (boss.getAI() != null) {
            holdouts.forEach(holdout -> boss.getAI().doNotAttack(holdout, BOSS_TRUCE_DAYS));
        }
    }

    private static CampaignFleetAPI spawnOne(AnomalyContentSite site, String holdoutFactionId, ChartTier tier, Planned planned, long fleetSeed,
                                             Vector2f at) {
        Random oneFleetRandom = new Random(fleetSeed);
        CampaignFleetAPI fleet = ChartAnomalyFleets.create(ChartAnomalyFleets.combat(holdoutFactionId, planned.fleetType(), tier, planned.sizePercent()),
                oneFleetRandom);
        if (fleet == null) {
            return null;
        }
        ChartAnomalyFleets.prepare(fleet, site.runId(), site.tier(), fleetSeed, true, site.context());
        site.addFleet(fleet, at);
        ChartContentGenerator.patrolSystem(fleet, site.system());
        return fleet;
    }

    private static Vector2f spot(List<ChartSpot> spots, Random random, AnomalyContentSite site) {
        return spots.isEmpty() ? ChartAnomalyFleets.farSideOf(site.entry()) : new Vector2f(spots.get(random.nextInt(spots.size())).position());
    }
}
