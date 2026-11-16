package exiledsector.charts.anomaly.excavation;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import exiledsector.i18n.Translation;
import exiledsector.charts.ChartIds;
import exiledsector.charts.anomaly.HeldGuards;
import exiledsector.charts.content.ChartAnomalyFleets;
import exiledsector.charts.content.ChartContentPlanner;
import exiledsector.charts.content.ChartFleetMarks;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartTier;
import exiledsector.charts.loot.ChartFleetLedger;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;

import java.util.Random;

public final class ExcavationFleets {

    public static final String FLEET_TAG = ChartIds.PREFIX + "excavation_fleet";
    static final String SITE_FLEET_KEY = "$exiledSector_excavationFleet";
    static final String SECOND_CAVERN_KEY = "$exiledSector_excavationSecondCavern";
    static final String CORRUPTED_KEY = "$exiledSector_excavationCorrupted";
    static final String ENGRAM_DROPPED_KEY = "$exiledSector_excavationEngramDropped";
    static final long FLEET_SALT = 0x5348414654464cL;
    static final long SECOND_CAVERN_SALT = 0x444545504552L;

    private ExcavationFleets() {
    }

    public static CampaignFleetAPI fleetFor(SectorEntityToken planet, ChartRun run, ExcavationSite site, ExcavationGuardians guardians) {
        return HeldGuards.held(planet, SITE_FLEET_KEY, () -> ExcavationDeliveries.withDeliveries(planet, create(run, site, guardians), guardians));
    }

    public static boolean isHeld(SectorEntityToken planet) {
        return planet.getMemoryWithoutUpdate().get(SITE_FLEET_KEY) instanceof CampaignFleetAPI held && !held.isEmpty();
    }

    public static void forget(SectorEntityToken planet) {
        HeldGuards.forget(planet, SITE_FLEET_KEY);
        ExcavationDeliveries.dropWaiting(planet);
    }

    static CampaignFleetAPI create(ChartRun run, ExcavationSite site, ExcavationGuardians guardians) {
        Random random = new Random(site.seed() ^ (guardians.secondCavern() ? SECOND_CAVERN_SALT : FLEET_SALT));
        ChartGenContext context = ChartRunEffects.contextOf(run);
        ExcavationTuning tuning = ExcavationTuning.of(context);
        ChartTier tier = ChartAnomalyFleets.tier(run.tier(), context);
        float fleetSize = ExcavationRules.guardianFleetSize(tier, guardians, tuning);
        int maxShipSize = ExcavationRules.maxShipSize(tuning);
        boolean derelict = random.nextFloat() < ExcavationRules.DERELICT_CHANCE || run.contentFaction() == null;
        CampaignFleetAPI fleet = derelict ? null : build(run.contentFaction(), tier, fleetSize, maxShipSize, random, true);
        if (fleet == null) {
            derelict = true;
            fleet = build(Factions.DERELICT, tier, fleetSize, maxShipSize, random, false);
        }
        if (fleet == null) {
            return null;
        }
        fleet.setName(Translation.gameText(fleetNameKey(derelict, guardians)));
        ChartAnomalyFleets.prepare(fleet, run.runId(), run.tier(), random.nextLong(), false, context);
        ChartFleetLedger.recordStart(fleet);
        ChartFleetLedger.tagAnomaly(fleet, ExcavationSites.ANOMALY_ID);
        MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        ChartFleetMarks.setStrengthBonus(memory, ExcavationRules.strengthBonus(fleetSize, guardians.corrupted()));
        memory.set(SECOND_CAVERN_KEY, guardians.secondCavern());
        memory.set(CORRUPTED_KEY, guardians.corrupted());
        memory.set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
        fleet.addTag(FLEET_TAG);
        return fleet;
    }

    static String fleetNameKey(boolean derelict, ExcavationGuardians guardians) {
        if (guardians.corrupted()) {
            return "chart.anomaly.excavation_sites.fleet.corrupted";
        }
        return derelict ? "chart.anomaly.excavation_sites.fleet.derelict" : "chart.anomaly.excavation_sites.fleet.garrison";
    }

    private static CampaignFleetAPI build(String factionId, ChartTier tier, float fleetSize, int maxShipSize, Random random, boolean withOfficers) {
        ChartAnomalyFleets.Spec spec = ChartAnomalyFleets.combat(factionId, FleetTypes.PATROL_MEDIUM, tier, fleetSize, ChartContentPlanner.BOSS_SIZE_CAP);
        CampaignFleetAPI fleet = ChartAnomalyFleets.create(spec, random, maxShipSize, withOfficers);
        return fleet == null || dropLargeHulls(fleet, maxShipSize) ? null : fleet;
    }

    static boolean dropLargeHulls(CampaignFleetAPI fleet, int maxShipSize) {
        boolean dropped = false;
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            if (ExcavationRules.holdsAbove(member.getHullSpec().getHullSize(), maxShipSize)) {
                fleet.getFleetData().removeFleetMember(member);
                dropped = true;
            }
        }
        if (dropped && !fleet.isEmpty()) {
            fleet.getFleetData().ensureHasFlagship();
            fleet.getFleetData().sort();
            FleetMemberAPI flagship = fleet.getFlagship();
            if (flagship != null && fleet.getCommander() != null && (flagship.getCaptain() == null || flagship.getCaptain().isDefault())) {
                flagship.setCaptain(fleet.getCommander());
            }
        }
        return fleet.isEmpty();
    }

    public static boolean isExcavationFleet(Object entity) {
        return entity instanceof CampaignFleetAPI fleet && fleet.hasTag(FLEET_TAG);
    }

    public static ExcavationGuardians guardiansOf(CampaignFleetAPI fleet) {
        MemoryAPI memory = fleet == null ? null : fleet.getMemoryWithoutUpdate();
        return memory == null ? ExcavationGuardians.FIRST_CAVERN
                : new ExcavationGuardians(memory.getBoolean(SECOND_CAVERN_KEY), memory.getBoolean(CORRUPTED_KEY));
    }
}
