package exiledsector.charts.anomaly.excavation;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import exiledlib.i18n.Translation;
import exiledsector.charts.ChartIds;
import exiledsector.charts.content.ChartAnomalyFleets;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartTier;
import exiledsector.charts.loot.ChartFleetLoot;
import exiledsector.charts.loot.DropType;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;

import java.util.Random;

public final class ShaftFleets {

    public static final String FLEET_TAG = ChartIds.PREFIX + "excavation_fleet";
    static final String LEAN_KEY = "$exiledSector_excavationLean";
    static final String SITE_FLEET_KEY = "$exiledSector_excavationFleet";
    static final long FLEET_SALT = 0x5348414654464cL;

    private ShaftFleets() {
    }

    public static CampaignFleetAPI fleetFor(SectorEntityToken planet, ChartRun run, ExcavationSite site) {
        MemoryAPI memory = planet.getMemoryWithoutUpdate();
        CampaignFleetAPI fleet = memory.get(SITE_FLEET_KEY) instanceof CampaignFleetAPI stored && !stored.isEmpty() ? stored : null;
        if (fleet == null) {
            fleet = create(run, site);
            if (fleet == null) {
                return null;
            }
            memory.set(SITE_FLEET_KEY, fleet);
        }
        fleet.setLocation(planet.getLocation().x, planet.getLocation().y);
        return fleet;
    }

    public static void forget(SectorEntityToken planet) {
        planet.getMemoryWithoutUpdate().unset(SITE_FLEET_KEY);
    }

    static CampaignFleetAPI create(ChartRun run, ExcavationSite site) {
        Random random = new Random(site.seed() ^ FLEET_SALT);
        ChartGenContext context = ChartRunEffects.contextOf(run);
        ChartTier tier = ChartAnomalyFleets.tier(run.tier(), context);
        boolean derelict = random.nextFloat() < ExcavationRules.DERELICT_CHANCE || run.contentFaction() == null;
        CampaignFleetAPI fleet = derelict ? null : build(run.contentFaction(), tier, random, true);
        if (fleet == null) {
            derelict = true;
            fleet = build(Factions.DERELICT, tier, random, false);
        }
        if (fleet == null) {
            return null;
        }
        fleet.setName(Translation.gameText(derelict ? "chart.excavation.fleet.derelict" : "chart.excavation.fleet.garrison"));
        ChartAnomalyFleets.prepare(fleet, run.runId(), run.tier(), random.nextLong(), false, context);
        ChartFleetLoot.recordStart(fleet);
        MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        memory.set(LEAN_KEY, site.lean().name());
        memory.set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
        fleet.addTag(FLEET_TAG);
        ExcavationLoot.addToFleet(fleet, site.lean(), context, run.tier());
        return fleet;
    }

    private static CampaignFleetAPI build(String factionId, ChartTier tier, Random random, boolean withOfficers) {
        ChartAnomalyFleets.Spec spec = ChartAnomalyFleets.combat(factionId, FleetTypes.PATROL_MEDIUM, tier,
                tier.fleetSize() * ExcavationRules.FLEET_SIZE_SHARE);
        CampaignFleetAPI fleet = ChartAnomalyFleets.create(spec, random, ExcavationRules.MAX_SHIP_SIZE, withOfficers);
        return fleet == null || dropLargeHulls(fleet) ? null : fleet;
    }

    static boolean dropLargeHulls(CampaignFleetAPI fleet) {
        boolean dropped = false;
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            if (ExcavationRules.holdsAbove(member.getHullSpec().getHullSize())) {
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

    public static boolean isShaftFleet(Object entity) {
        return entity instanceof CampaignFleetAPI fleet && fleet.hasTag(FLEET_TAG);
    }

    public static DropType leanOf(CampaignFleetAPI fleet) {
        if (!isShaftFleet(fleet)) {
            return null;
        }
        try {
            return DropType.valueOf(fleet.getMemoryWithoutUpdate().getString(LEAN_KEY));
        } catch (RuntimeException e) {
            return DropType.RESOURCE;
        }
    }
}
