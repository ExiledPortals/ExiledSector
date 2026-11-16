package exiledsector.charts.anomaly.skirmish;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.charts.anomaly.AnomalyState;
import exiledsector.charts.atlas.AtlasRuntime;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.officer.OfficerDefeat;
import exiledsector.charts.officer.turn.OfficerTurns;
import exiledsector.charts.run.ChartRun;
import exiledsector.loot.Loot;
import org.apache.log4j.Logger;

import java.util.List;

public final class SkirmishObjective {

    private static final Logger LOG = Logger.getLogger(SkirmishObjective.class);

    private SkirmishObjective() {
    }

    public static boolean tracks(ChartRun run) {
        return SkirmishFlagships.of(run) != null;
    }

    public static SkirmishProgress fellInBattle(SectorAPI sector, ChartRun run, CampaignFleetAPI sourceFleet, List<FleetMemberAPI> lostMembers) {
        SkirmishFlagships flagships = SkirmishFlagships.of(run);
        if (flagships == null || sourceFleet == null || lostMembers == null) {
            return SkirmishProgress.NONE;
        }
        for (SkirmishFlagship flagship : flagships.standing()) {
            if (flagship.flies(sourceFleet) && lostMembers.stream().anyMatch(member -> flagship.flagshipId().equals(member.getId()))) {
                return fell(sector, run, flagship, FlagshipFall.BEATEN_BY_PLAYER);
            }
        }
        return SkirmishProgress.NONE;
    }

    public static SkirmishProgress fleetGone(SectorAPI sector, ChartRun run, CampaignFleetAPI fleet, boolean inPlayerBattle) {
        SkirmishFlagships flagships = SkirmishFlagships.of(run);
        if (flagships == null) {
            return SkirmishProgress.NONE;
        }
        for (SkirmishFlagship flagship : flagships.standing()) {
            if (flagship.flies(fleet)) {
                return fell(sector, run, flagship, inPlayerBattle ? FlagshipFall.BEATEN_BY_PLAYER : FlagshipFall.LOST_TO_RIVAL);
            }
        }
        return SkirmishProgress.NONE;
    }

    public static SkirmishProgress reconcile(SectorAPI sector, ChartRun run, StarSystemAPI system) {
        SkirmishFlagships flagships = SkirmishFlagships.of(run);
        if (flagships == null || system == null || inDialog(sector)) {
            return SkirmishProgress.NONE;
        }
        SkirmishProgress progress = SkirmishProgress.NONE;
        for (SkirmishFlagship flagship : flagships.standing()) {
            if (!stillFlies(system, flagship)) {
                progress = progress.then(fell(sector, run, flagship, FlagshipFall.LOST_TO_RIVAL));
            }
        }
        return progress;
    }

    private static SkirmishProgress fell(SectorAPI sector, ChartRun run, SkirmishFlagship flagship, FlagshipFall how) {
        SkirmishFlagships before = SkirmishFlagships.of(run);
        int fallOrder = before == null ? 1 : before.fallenCount() + 1;
        SkirmishFlagships.record(AnomalyState.of(run, FactionSkirmish.ANOMALY_ID), flagship.fallen(how, fallOrder));

        boolean beatenByPlayer = how == FlagshipFall.BEATEN_BY_PLAYER;
        Loot officerLoot = beatenByPlayer
                ? OfficerDefeat.officerBeaten(sector, run, flagship.officerRecordId(), flagship.flagshipId())
                : Loot.NONE;
        SkirmishFlagships after = SkirmishFlagships.of(run);
        boolean bothFallen = after == null || after.allFallen();
        if (beatenByPlayer && !bothFallen) {
            takeTurnAfterDefeat(sector, flagship.officerRecordId());
        }
        return new SkirmishProgress(true, officerLoot, bothFallen, beatenByPlayer ? flagship.officerRecordId() : null);
    }

    private static void takeTurnAfterDefeat(SectorAPI sector, int officerRecordId) {
        try {
            AtlasState state = AtlasState.getIfCreated(sector);
            if (state != null) {
                OfficerTurns.officerBeatenInOpenRun(AtlasRuntime.generator(sector), state, officerRecordId);
            }
        } catch (RuntimeException | LinkageError e) {
            LOG.error("[ExiledSector] Taking the Atlas officer turn after a Faction Skirmish officer was beaten failed", e);
        }
    }

    private static boolean stillFlies(StarSystemAPI system, SkirmishFlagship flagship) {
        for (CampaignFleetAPI fleet : system.getFleets()) {
            if (flagship.flies(fleet) && !fleet.isDespawning() && !fleet.isEmpty()) {
                return fleet.getFleetData() == null || fleet.getFleetData().getMembersListCopy().stream()
                        .anyMatch(member -> flagship.flagshipId().equals(member.getId()));
            }
        }
        return false;
    }

    private static boolean inDialog(SectorAPI sector) {
        return sector.getCampaignUI() != null && sector.getCampaignUI().getCurrentInteractionDialog() != null;
    }
}
