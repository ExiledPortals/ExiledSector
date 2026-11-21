package exiledsector.charts.atlas;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.FactionFlagships;
import exiledsector.charts.officer.FlagshipCatalogue;
import exiledsector.charts.officer.OfficerFleets;
import exiledsector.charts.officer.OfficerScavenging;
import exiledsector.charts.officer.OfficerStartingItems;
import exiledsector.charts.officer.ScavengeRoll;
import exiledsector.charts.officer.StartingItemRoll;
import exiledsector.charts.officer.OfficerTrophyRefits;
import exiledsector.charts.officer.TrophyRefit;
import exiledsector.charts.officer.turn.OfficerBoons;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import exiledsector.charts.tree.AtlasTree;

import java.util.Set;

public record TerritoryContext(String excludedFactionId, AtlasCell openRunCell, FlagshipCatalogue catalogue, StartingItemRoll startingItems,
                               ScavengeRoll scavenges, Set<Integer> openRunOfficerIds, OfficerBoons boons, TrophyRefit refits) {

    public static final TerritoryContext NONE = new TerritoryContext(null, null, FlagshipCatalogue.NONE);

    public TerritoryContext {
        openRunOfficerIds = openRunOfficerIds == null ? Set.of() : Set.copyOf(openRunOfficerIds);
        boons = boons == null ? OfficerBoons.NONE : boons;
        refits = refits == null ? TrophyRefit.NONE : refits;
    }

    public TerritoryContext(String excludedFactionId, AtlasCell openRunCell, FlagshipCatalogue catalogue) {
        this(excludedFactionId, openRunCell, catalogue, StartingItemRoll.NONE, ScavengeRoll.NONE, Set.of());
    }

    public TerritoryContext(String excludedFactionId, AtlasCell openRunCell, FlagshipCatalogue catalogue, StartingItemRoll startingItems,
                            ScavengeRoll scavenges, Set<Integer> openRunOfficerIds) {
        this(excludedFactionId, openRunCell, catalogue, startingItems, scavenges, openRunOfficerIds, OfficerBoons.NONE, TrophyRefit.NONE);
    }

    public static TerritoryContext current() {
        SectorAPI sector = Global.getSector();
        OfficerBoons boons = boonsOrNone(sector);
        return new TerritoryContext(commissionFactionId(), openRunCellOrNull(), FactionFlagships.INSTANCE, OfficerStartingItems.forSector(sector),
                OfficerScavenging.forSector(sector, boons.luckyScavenging()), openRunOfficerIdsOrNone(), boons,
                boons.trophyRefit() ? OfficerTrophyRefits.forSector(sector) : TrophyRefit.NONE);
    }

    public TerritoryContext withoutOpenRun() {
        return new TerritoryContext(excludedFactionId, null, catalogue, startingItems, scavenges, Set.of(), boons, refits);
    }

    public TerritoryContext withOpenRun(AtlasCell cell, Set<Integer> officerRecordIds) {
        return new TerritoryContext(excludedFactionId, cell, catalogue, startingItems, scavenges, officerRecordIds, boons, refits);
    }

    public boolean takesPartInOpenRun(AtlasState state, AtlasOfficer officer) {
        if (openRunOfficerIds.contains(officer.recordId())) {
            return true;
        }
        return openRunCell != null && officer.isActive()
                && (officer.commandCentre().equals(openRunCell) || officer.commandCentre().equals(state.commandCentreOf(openRunCell)));
    }

    private static OfficerBoons boonsOrNone(SectorAPI sector) {
        try {
            AtlasState state = AtlasState.getIfCreated(sector);
            return state == null ? OfficerBoons.NONE : OfficerBoons.of(AtlasTree.atlasEffects(state));
        } catch (RuntimeException e) {
            return OfficerBoons.NONE;
        }
    }

    private static String commissionFactionId() {
        try {
            return Misc.getCommissionFactionId();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static Set<Integer> openRunOfficerIdsOrNone() {
        try {
            ChartRun run = ChartRuns.currentOrNull();
            return run == null ? Set.of() : OfficerFleets.fightingRecordIdsOf(run.atlasNode());
        } catch (RuntimeException e) {
            return Set.of();
        }
    }

    private static AtlasCell openRunCellOrNull() {
        try {
            return AtlasAnomalies.openRunCell();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
