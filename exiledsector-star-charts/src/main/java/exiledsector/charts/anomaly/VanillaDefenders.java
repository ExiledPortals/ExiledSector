package exiledsector.charts.anomaly;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import exiledsector.charts.ChartIds;
import exiledsector.charts.content.ChartAnomalyFleets;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;

public final class VanillaDefenders {

    public static final String HAS_DEFENDERS_KEY = "$hasDefenders";
    public static final String DEFENDER_FLEET_KEY = ChartIds.VANILLA_DEFENDER_FLEET_MEMORY_KEY;
    public static final String DEFEATED_KEY = "$defenderFleetDefeated";
    static final float UNTIL_INTERACTION_ENDS = 0f;

    public interface Factory {

        CampaignFleetAPI create(int runTier, ChartGenContext context, long seed);
    }

    private VanillaDefenders() {
    }

    public static void arm(SectorEntityToken site, long seed) {
        site.getMemoryWithoutUpdate().set(MemFlags.SALVAGE_SEED, seed);
    }

    public static boolean guarded(SectorEntityToken site) {
        MemoryAPI memory = site == null ? null : site.getMemoryWithoutUpdate();
        return memory != null && !memory.getBoolean(DEFEATED_KEY);
    }

    public static CampaignFleetAPI muster(SectorEntityToken site, ChartRun run, long salt, Factory factory) {
        if (run == null || !guarded(site)) {
            return null;
        }
        MemoryAPI memory = site.getMemoryWithoutUpdate();
        long seed = seedOf(memory, run, salt);
        ChartGenContext context = ChartRunEffects.contextOf(run);
        CampaignFleetAPI guards = memory.get(DEFENDER_FLEET_KEY) instanceof CampaignFleetAPI stored && !stored.isEmpty() ? stored : null;
        if (guards == null) {
            guards = factory.create(run.tier(), context, seed);
            if (guards == null) {
                return null;
            }
            memory.set(HAS_DEFENDERS_KEY, true, UNTIL_INTERACTION_ENDS);
            memory.set(DEFENDER_FLEET_KEY, guards, UNTIL_INTERACTION_ENDS);
        }
        ChartAnomalyFleets.prepare(guards, run.runId(), run.tier(), seed, false, context);
        guards.setContainingLocation(site.getContainingLocation());
        if (site.getLocation() != null) {
            guards.setLocation(site.getLocation().x, site.getLocation().y);
        }
        return guards;
    }

    private static long seedOf(MemoryAPI memory, ChartRun run, long salt) {
        if (memory.get(MemFlags.SALVAGE_SEED) instanceof Number stored) {
            return stored.longValue();
        }
        long seed = run.runId().hashCode() ^ salt;
        memory.set(MemFlags.SALVAGE_SEED, seed);
        return seed;
    }
}
