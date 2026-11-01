package exiledsector.charts.anomaly.facility;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import exiledsector.i18n.Translation;
import exiledsector.charts.anomaly.HeldGuards;
import exiledsector.charts.content.ChartAnomalyFleets;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartTier;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;
import exiledsector.compat.VaultsOfKnowledgeCompat;

import java.util.Random;

public final class FacilityGuards {

    static final String GUARDS_KEY = "$exiledSector_facilityGuards";
    static final long GUARD_SALT = 0x47554152445331L;

    private FacilityGuards() {
    }

    public static CampaignFleetAPI fleetFor(SectorEntityToken planet, ChartRun run) {
        return HeldGuards.held(planet, GUARDS_KEY, () -> create(run, PreCollapseFacility.seed(run, planet.getId(), GUARD_SALT)));
    }

    public static void forget(SectorEntityToken planet) {
        HeldGuards.forget(planet, GUARDS_KEY);
    }

    static CampaignFleetAPI create(ChartRun run, long seed) {
        Random random = new Random(seed);
        ChartGenContext context = ChartRunEffects.contextOf(run);
        ChartTier tier = ChartAnomalyFleets.tier(run.tier(), context);
        ChartAnomalyFleets.Spec scaled = ChartAnomalyFleets.combat(Factions.REMNANTS, FleetTypes.PATROL_LARGE, tier,
                tier.fleetSize() * FacilityRules.GUARD_SIZE_SHARE);
        CampaignFleetAPI guards = ChartAnomalyFleets.create(scaled.atLeast(VaultsOfKnowledgeCompat.defenceFleetPoints(random)), random);
        if (guards == null) {
            return null;
        }
        guards.setName(Translation.gameText("chart.facility.guards.name"));
        guards.setNoFactionInName(true);
        ChartAnomalyFleets.prepare(guards, run.runId(), run.tier(), random.nextLong(), false, context);
        guards.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
        return guards;
    }
}
