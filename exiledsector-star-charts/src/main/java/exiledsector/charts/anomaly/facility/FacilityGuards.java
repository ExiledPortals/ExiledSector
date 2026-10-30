package exiledsector.charts.anomaly.facility;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import exiledsector.i18n.Translation;
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
        MemoryAPI memory = planet.getMemoryWithoutUpdate();
        CampaignFleetAPI guards = memory.get(GUARDS_KEY) instanceof CampaignFleetAPI stored && !stored.isEmpty() ? stored : null;
        if (guards == null) {
            guards = create(run, PreCollapseFacility.seed(run, planet.getId(), GUARD_SALT));
            if (guards == null) {
                return null;
            }
            memory.set(GUARDS_KEY, guards);
        }
        guards.setContainingLocation(planet.getContainingLocation());
        guards.setLocation(planet.getLocation().x, planet.getLocation().y);
        return guards;
    }

    public static void forget(SectorEntityToken planet) {
        planet.getMemoryWithoutUpdate().unset(GUARDS_KEY);
    }

    static CampaignFleetAPI create(ChartRun run, long seed) {
        Random random = new Random(seed);
        ChartGenContext context = ChartRunEffects.contextOf(run);
        ChartTier tier = ChartAnomalyFleets.tier(run.tier(), context);
        ChartAnomalyFleets.Spec scaled = ChartAnomalyFleets.combat(Factions.REMNANTS, FleetTypes.PATROL_LARGE, tier,
                tier.fleetSize() * FacilityRules.GUARD_SIZE_SHARE);
        CampaignFleetAPI guards = ChartAnomalyFleets.create(atLeast(scaled, VaultsOfKnowledgeCompat.defenceFleetPoints(random)), random);
        if (guards == null) {
            return null;
        }
        guards.setName(Translation.gameText("chart.facility.guards.name"));
        guards.setNoFactionInName(true);
        ChartAnomalyFleets.prepare(guards, run.runId(), run.tier(), random.nextLong(), false, context);
        guards.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
        return guards;
    }

    static ChartAnomalyFleets.Spec atLeast(ChartAnomalyFleets.Spec spec, float combatPoints) {
        if (spec.combatPoints() >= combatPoints) {
            return spec;
        }
        return new ChartAnomalyFleets.Spec(spec.factionId(), spec.fleetType(), combatPoints, spec.freighterPoints(), spec.tankerPoints(),
                spec.quality(), spec.officerLevelBonus(), spec.officerCountBonus(), spec.averageSMods());
    }
}
