package exiledsector.charts.anomaly.skirmish;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.ChartIds;
import exiledsector.charts.atlas.AtlasRunNode;
import exiledsector.charts.anomaly.ChartAnomaly;
import exiledsector.charts.atlas.AtlasAnomalies;
import exiledsector.charts.atlas.AtlasBorders;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasOpening;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.atlas.AtlasTraits;
import exiledsector.charts.atlas.RedeploymentCatalystUse;
import exiledsector.items.UseVerdict;

public final class FactionSkirmish implements ChartAnomaly {

    public static final String ANOMALY_ID = "faction_skirmish";
    public static final FactionSkirmish INSTANCE = new FactionSkirmish();
    public static final String NO_SIDE_KEY = "catalyst.refused.skirmish.no_side";
    public static final String NO_ATTACKER_KEY = "catalyst.refused.skirmish.no_attacker";

    private FactionSkirmish() {
    }

    @Override
    public String id() {
        return ANOMALY_ID;
    }

    @Override
    public void loadData() {
        SkirmishFactions.load();
    }

    @Override
    public void checkRun(SectorAPI sector) {
        ChartSkirmishes.checkFinalPush(sector);
    }

    @Override
    public void fleetGone(SectorAPI sector, CampaignFleetAPI fleet) {
        if (fleet != null && SkirmishSide.ATTACKER.key().equals(fleet.getMemoryWithoutUpdate().getString(ChartIds.SKIRMISH_SIDE_MEMORY_KEY))) {
            ChartSkirmishes.checkFinalPush(sector);
        }
    }

    @Override
    public boolean fitsAt(AtlasTraits traits) {
        if (traits.specialId() != null || traits.infestationId() != null || !SkirmishFactions.takesSides(traits.factionId())) {
            return false;
        }
        return traits.neighbourFactionIds().stream().anyMatch(neighbour -> !neighbour.equals(traits.factionId()) && SkirmishFactions.takesSides(neighbour));
    }

    @Override
    public boolean holdsAt(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        return SkirmishFactions.takesSides(defender(generator, state, cell)) && attacker(generator, state, cell) != null;
    }

    @Override
    public UseVerdict insertVerdict(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        String defender = defender(generator, state, cell);
        if (defender == null || generator.node(cell).specialId() != null) {
            return UseVerdict.refused(RedeploymentCatalystUse.SPECIAL_KEY);
        }
        if (!defender.equals(AtlasOpening.territoryOwnerOf(generator, state, cell))) {
            return UseVerdict.refused(RedeploymentCatalystUse.INFESTED_KEY).with("faction", RedeploymentCatalystUse.factionName(defender));
        }
        if (!SkirmishFactions.takesSides(defender)) {
            return UseVerdict.refused(NO_SIDE_KEY).with("faction", RedeploymentCatalystUse.factionName(defender));
        }
        return attacker(generator, state, cell) == null ? UseVerdict.refused(NO_ATTACKER_KEY) : UseVerdict.ALLOW;
    }

    public static String defender(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        return AtlasOpening.ownerOf(generator, state, cell);
    }

    public static String attacker(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        return AtlasBorders.attacker(generator, state, cell, SkirmishFactions::takesSides);
    }

    public static boolean isIn(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        return AtlasAnomalies.effective(generator, state, cell).contains(ANOMALY_ID);
    }

    public static AtlasRunNode join(AtlasRunNode node, AtlasGenerator generator, AtlasState state, AtlasCell cell, SkirmishSide side) {
        if (!node.anomalyIds().contains(ANOMALY_ID)) {
            return node;
        }
        return node.withSkirmish(attacker(generator, state, cell), side == null ? SkirmishSide.NEITHER : side);
    }
}
