package exiledsector.charts.officer;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class OfficerSightings {

    private OfficerSightings() {
    }

    public static boolean identifyInView(AtlasState state, Set<AtlasCell> visibleCells) {
        List<Integer> sighted = new ArrayList<>();
        for (AtlasOfficer officer : state.officers()) {
            if (!officer.identified() && officer.isActive() && visibleCells.contains(officer.commandCentre())) {
                sighted.add(officer.recordId());
            }
        }
        return identify(state, sighted);
    }

    public static boolean identify(AtlasState state, Collection<Integer> recordIds) {
        boolean changed = false;
        for (Integer recordId : recordIds) {
            AtlasOfficer officer = recordId == null ? null : state.officer(recordId);
            if (officer != null && !officer.identified()) {
                state.putOfficer(officer.identifiedNow());
                changed = true;
            }
        }
        return changed;
    }

    public static void battleOccurred(BattleAPI battle) {
        if (battle == null || !battle.isPlayerInvolved()) {
            return;
        }
        AtlasState state = AtlasState.getIfCreated(Global.getSector());
        if (state != null) {
            identify(state, officersIn(battle.getNonPlayerSideSnapshot()));
        }
    }

    static List<Integer> officersIn(List<CampaignFleetAPI> fleets) {
        List<Integer> recordIds = new ArrayList<>();
        if (fleets == null) {
            return recordIds;
        }
        for (CampaignFleetAPI fleet : fleets) {
            Object recordId = fleet.getMemoryWithoutUpdate().get(OfficerFleets.RECORD_MEMORY_KEY);
            if (recordId instanceof Number number) {
                recordIds.add(number.intValue());
            }
        }
        return recordIds;
    }

    public static Set<AtlasCell> shownSystems(AtlasState state) {
        Set<AtlasCell> systems = new LinkedHashSet<>();
        for (AtlasOfficer officer : state.officers()) {
            if (officer.identified() && officer.isActive()) {
                systems.add(officer.commandCentre());
            }
        }
        return systems;
    }
}
