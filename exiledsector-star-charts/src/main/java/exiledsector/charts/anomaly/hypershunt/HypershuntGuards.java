package exiledsector.charts.anomaly.hypershunt;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.procgen.SalvageEntityGenDataSpec.DropData;
import exiledsector.i18n.Translation;
import exiledsector.charts.ChartIds;
import exiledsector.charts.content.ChartAnomalyFleets;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartTier;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;

import java.util.List;
import java.util.Random;

public final class HypershuntGuards {

    static final String HAS_DEFENDERS_KEY = "$hasDefenders";
    static final String DEFENDER_FLEET_KEY = ChartIds.VANILLA_DEFENDER_FLEET_MEMORY_KEY;
    static final String DEFEATED_KEY = "$defenderFleetDefeated";
    static final long GUARD_SALT = 0x4f4d454741L;
    static final float UNTIL_INTERACTION_ENDS = 0f;
    static final float GUARD_SIZE_SHARE = 1f;
    static final float VANILLA_DEFENDER_POINTS = 80f;

    private HypershuntGuards() {
    }

    static List<DropData> omegaDrops() {
        return List.of(drop("omega_weapons_small", 5, 7), drop("omega_weapons_medium", 3, 4));
    }

    private static DropData drop(String group, int chances, int maxChances) {
        DropData drop = new DropData();
        drop.group = group;
        drop.chances = chances;
        drop.maxChances = maxChances;
        return drop;
    }

    static void arm(SectorEntityToken hypershunt, long seed) {
        MemoryAPI memory = hypershunt.getMemoryWithoutUpdate();
        memory.set(MemFlags.SALVAGE_SEED, seed);
        memory.set(HAS_DEFENDERS_KEY, true);
    }

    public static boolean guarded(SectorEntityToken hypershunt) {
        MemoryAPI memory = hypershunt == null ? null : hypershunt.getMemoryWithoutUpdate();
        return memory != null && !memory.getBoolean(DEFEATED_KEY);
    }

    static CampaignFleetAPI muster(SectorEntityToken hypershunt, ChartRun run) {
        if (run == null || !guarded(hypershunt)) {
            return null;
        }
        MemoryAPI memory = hypershunt.getMemoryWithoutUpdate();
        long seed = seedOf(memory, run);
        ChartGenContext context = ChartRunEffects.contextOf(run);
        CampaignFleetAPI guards = memory.get(DEFENDER_FLEET_KEY) instanceof CampaignFleetAPI stored && !stored.isEmpty() ? stored : null;
        if (guards == null) {
            guards = create(run.tier(), context, seed);
            if (guards == null) {
                return null;
            }
            ChartAnomalyFleets.prepare(guards, run.runId(), run.tier(), seed, true, context);
            memory.set(HAS_DEFENDERS_KEY, true, UNTIL_INTERACTION_ENDS);
            memory.set(DEFENDER_FLEET_KEY, guards, UNTIL_INTERACTION_ENDS);
        }
        guards.setContainingLocation(hypershunt.getContainingLocation());
        if (hypershunt.getLocation() != null) {
            guards.setLocation(hypershunt.getLocation().x, hypershunt.getLocation().y);
        }
        return guards;
    }

    private static long seedOf(MemoryAPI memory, ChartRun run) {
        if (memory.get(MemFlags.SALVAGE_SEED) instanceof Number stored) {
            return stored.longValue();
        }
        long seed = run.runId().hashCode() ^ GUARD_SALT;
        memory.set(MemFlags.SALVAGE_SEED, seed);
        return seed;
    }

    static CampaignFleetAPI create(int runTier, ChartGenContext context, long seed) {
        Random random = new Random(seed);
        ChartTier tier = ChartAnomalyFleets.tier(runTier, context);
        ChartAnomalyFleets.Spec scaled = ChartAnomalyFleets.combat(Factions.OMEGA, FleetTypes.PATROL_LARGE, tier, tier.fleetSize() * GUARD_SIZE_SHARE);
        CampaignFleetAPI guards = ChartAnomalyFleets.create(scaled.atLeast(VANILLA_DEFENDER_POINTS), random);
        if (guards == null) {
            return null;
        }
        guards.setName(Translation.gameText("chart.hypershunt.guards.name"));
        guards.setNoFactionInName(true);
        hideDetails(guards);
        omegaDrops().forEach(guards::addDropRandom);
        return guards;
    }

    static void hideDetails(CampaignFleetAPI guards) {
        FleetDataAPI fleetData = guards.getFleetData();
        if (fleetData == null) {
            return;
        }
        for (FleetMemberAPI member : fleetData.getMembersListCopy()) {
            ShipVariantAPI variant = member.getVariant().clone();
            member.setVariant(variant, false, false);
            variant.addTag(Tags.SHIP_LIMITED_TOOLTIP);
        }
    }
}
