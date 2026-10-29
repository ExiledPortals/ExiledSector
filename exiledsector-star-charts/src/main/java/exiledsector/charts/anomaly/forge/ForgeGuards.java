package exiledsector.charts.anomaly.forge;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageGenFromSeed;
import exiledlib.i18n.Translation;
import exiledsector.charts.ChartIds;
import exiledsector.charts.content.ChartAnomalyFleets;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartTier;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;

import java.util.Random;

public final class ForgeGuards {

    static final String GUARDIAN_VARIANT = "guardian_Standard";
    static final String HAS_DEFENDERS_KEY = "$hasDefenders";
    static final String DEFENDER_FLEET_KEY = ChartIds.VANILLA_DEFENDER_FLEET_MEMORY_KEY;
    static final String DEFEATED_KEY = "$defenderFleetDefeated";
    static final String DERELICT_TALLY_KEY = SalvageGenFromSeed.DEFEATED_DERELICT_STR;
    static final long GUARD_SALT = 0x4755415244L;
    static final float UNTIL_INTERACTION_ENDS = 0f;

    public record DerelictTally(Object value) {
    }

    private ForgeGuards() {
    }

    static void arm(SectorEntityToken forge, long seed) {
        MemoryAPI memory = forge.getMemoryWithoutUpdate();
        memory.set(MemFlags.SALVAGE_SEED, seed);
        memory.set(HAS_DEFENDERS_KEY, true);
    }

    public static boolean guarded(SectorEntityToken forge) {
        MemoryAPI memory = forge == null ? null : forge.getMemoryWithoutUpdate();
        return memory != null && !memory.getBoolean(DEFEATED_KEY);
    }

    static CampaignFleetAPI muster(SectorEntityToken forge, ChartRun run) {
        if (run == null || !guarded(forge)) {
            return null;
        }
        MemoryAPI memory = forge.getMemoryWithoutUpdate();
        long seed = seedOf(memory, run);
        ChartGenContext context = ChartRunEffects.contextOf(run);
        CampaignFleetAPI guards = memory.get(DEFENDER_FLEET_KEY) instanceof CampaignFleetAPI stored && !stored.isEmpty() ? stored : null;
        if (guards == null) {
            guards = create(run.tier(), context, seed);
            if (guards == null) {
                return null;
            }
            memory.set(HAS_DEFENDERS_KEY, true, UNTIL_INTERACTION_ENDS);
            memory.set(DEFENDER_FLEET_KEY, guards, UNTIL_INTERACTION_ENDS);
        }
        ChartAnomalyFleets.prepare(guards, run.runId(), run.tier(), seed, false, context);
        guards.setContainingLocation(forge.getContainingLocation());
        if (forge.getLocation() != null) {
            guards.setLocation(forge.getLocation().x, forge.getLocation().y);
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
        ChartAnomalyFleets.Spec escort = ChartAnomalyFleets.combat(Factions.DERELICT, FleetTypes.PATROL_MEDIUM, tier,
                tier.fleetSize() * ForgeRules.ESCORT_SIZE_SHARE);
        CampaignFleetAPI guards = ChartAnomalyFleets.create(escort, random, ForgeRules.ESCORT_MAX_SHIP_SIZE, false);
        if (guards == null) {
            guards = ChartAnomalyFleets.emptyFleet(Factions.DERELICT, FleetTypes.PATROL_MEDIUM);
        }
        if (guards == null) {
            return null;
        }
        boolean withGuardian = ChartAnomalyFleets.addCoreFlagship(guards, GUARDIAN_VARIANT, Commodities.ALPHA_CORE, random);
        if (!withGuardian && guards.isEmpty()) {
            return null;
        }
        guards.setName(Translation.gameText("chart.forge.guards.name"));
        return guards;
    }

    static DerelictTally derelictTally() {
        return new DerelictTally(Global.getSector().getMemoryWithoutUpdate().get(DERELICT_TALLY_KEY));
    }

    static void restore(DerelictTally tally) {
        MemoryAPI global = Global.getSector().getMemoryWithoutUpdate();
        if (tally.value() == null) {
            global.unset(DERELICT_TALLY_KEY);
        } else {
            global.set(DERELICT_TALLY_KEY, tally.value());
        }
    }
}
