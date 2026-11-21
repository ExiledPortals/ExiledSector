package exiledsector.harness;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.charts.atlas.AtlasRuntime;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.run.diagnostics.ChartCensus;
import exiledsector.items.ItemStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class HarnessBaseline {

    private record MemberState(FleetMemberAPI member, float combatReadiness, float hullFraction) {
    }

    private final LocationAPI fleetLocation;
    private final float fleetX;
    private final float fleetY;
    private final boolean transponderOn;
    private final CargoAPI cargo;
    private final List<MemberState> members;
    private final List<PersistentSnapshot> persistentState;
    private final ChartCensus census;

    private HarnessBaseline(CampaignFleetAPI playerFleet, List<PersistentSnapshot> persistentState, ChartCensus census) {
        this.fleetLocation = playerFleet.getContainingLocation();
        this.fleetX = playerFleet.getLocation().x;
        this.fleetY = playerFleet.getLocation().y;
        this.transponderOn = playerFleet.isTransponderOn();
        this.cargo = playerFleet.getCargo().createCopy();
        this.members = new ArrayList<>();
        for (FleetMemberAPI member : playerFleet.getFleetData().getMembersListCopy()) {
            members.add(new MemberState(member, member.getRepairTracker().getCR(), member.getStatus().getHullFraction()));
        }
        this.persistentState = persistentState;
        this.census = census;
    }

    static List<String> persistentKeys() {
        return List.of(AtlasState.PERSISTENT_KEY, ItemStore.dataKey(), ItemStore.starChartDataKey());
    }

    static HarnessBaseline capture(SectorAPI sector) throws Throwable {
        List<PersistentSnapshot> persistentState = new ArrayList<>();
        for (String key : persistentKeys()) {
            persistentState.add(PersistentSnapshot.of(sector.getPersistentData(), key));
        }
        return new HarnessBaseline(sector.getPlayerFleet(), persistentState, ChartCensus.take(sector));
    }

    String describe() {
        return members.size() + " ships at " + fleetLocation.getName() + " (" + Math.round(fleetX) + ", " + Math.round(fleetY) + "), "
                + cargo.getStacksCopy().size() + " cargo stacks, persistent state " + persistentKeys();
    }

    void restore(SectorAPI sector) throws Throwable {
        CampaignFleetAPI playerFleet = sector.getPlayerFleet();
        HarnessCampaign.movePlayerFleetTo(sector, fleetLocation, fleetX, fleetY);
        playerFleet.setVelocity(0f, 0f);
        playerFleet.setTransponderOn(transponderOn);
        restoreMembers(playerFleet);
        playerFleet.getCargo().clear();
        playerFleet.getCargo().addAll(cargo.createCopy());
        Map<String, Object> persistentData = sector.getPersistentData();
        for (PersistentSnapshot snapshot : persistentState) {
            snapshot.restore(persistentData);
        }
        AtlasRuntime.invalidate();
    }

    private void restoreMembers(CampaignFleetAPI playerFleet) {
        List<FleetMemberAPI> baselineMembers = members.stream().map(MemberState::member).toList();
        for (FleetMemberAPI member : playerFleet.getFleetData().getMembersListCopy()) {
            if (!baselineMembers.contains(member)) {
                playerFleet.getFleetData().removeFleetMember(member);
            }
        }
        for (MemberState state : members) {
            if (!playerFleet.getFleetData().getMembersListCopy().contains(state.member())) {
                playerFleet.getFleetData().addFleetMember(state.member());
            }
            if (state.hullFraction() >= 1f) {
                state.member().getStatus().repairFully();
            } else {
                state.member().getStatus().setHullFraction(state.hullFraction());
            }
            state.member().getRepairTracker().setCR(state.combatReadiness());
        }
        playerFleet.getFleetData().setSyncNeeded();
        playerFleet.getFleetData().syncIfNeeded();
    }

    List<String> leaks(SectorAPI sector) {
        return ChartCensus.take(sector).changesSince(census);
    }
}
