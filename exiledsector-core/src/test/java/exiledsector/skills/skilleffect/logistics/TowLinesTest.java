package exiledsector.skills.skilleffect.logistics;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.ExiledSector;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TowLinesTest {

    private static final float EPSILON = 1e-4f;

    private static FleetMemberAPI ship(HullSize hullSize, float burnLevel, float towBonus, boolean mothballed) {
        FleetMemberAPI member = mock(FleetMemberAPI.class, Answers.RETURNS_DEEP_STUBS);
        MutableStat maxBurnLevel = new MutableStat(burnLevel);
        when(member.isMothballed()).thenReturn(mothballed);
        when(member.isFighterWing()).thenReturn(false);
        when(member.getHullSpec().getHullSize()).thenReturn(hullSize);
        when(member.getStats().getMaxBurnLevel()).thenReturn(maxBurnLevel);
        when(member.getStats().getDynamic().getValue(TowLines.TOW_BURN_LEVEL_KEY, 0f)).thenReturn(towBonus);
        return member;
    }

    private static FleetMemberAPI tug(HullSize hullSize, float burnLevel) {
        return ship(hullSize, burnLevel, 1f, false);
    }

    private static FleetMemberAPI ship(HullSize hullSize, float burnLevel) {
        return ship(hullSize, burnLevel, 0f, false);
    }

    private static CampaignFleetAPI fleet(List<FleetMemberAPI> members) {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(fleet.getFleetData().getMembersListCopy()).thenReturn(members);
        return fleet;
    }

    private static float burnLevel(FleetMemberAPI member) {
        return member.getStats().getMaxBurnLevel().getModifiedValue();
    }

    @Test
    void towsTheSlowestShipItCanTow() {
        FleetMemberAPI tug = tug(HullSize.CRUISER, 9f);
        FleetMemberAPI slowFrigate = ship(HullSize.FRIGATE, 7f);
        FleetMemberAPI slowestCruiser = ship(HullSize.CRUISER, 6f);
        FleetMemberAPI slowerCapital = ship(HullSize.CAPITAL_SHIP, 5f);

        assertEquals(Map.of(slowestCruiser, 1f), TowLines.towBonuses(List.of(tug, slowFrigate, slowestCruiser, slowerCapital)));
    }

    @Test
    void towsOnlyShipsSlowerThanItselfAndNeverPastItsOwnBurnLevel() {
        FleetMemberAPI tug = tug(HullSize.CRUISER, 8f);

        assertEquals(Map.of(), TowLines.towBonuses(List.of(tug, ship(HullSize.FRIGATE, 8f))));
        FleetMemberAPI halfALevelSlower = ship(HullSize.FRIGATE, 7.5f);
        assertEquals(0.5f, TowLines.towBonuses(List.of(tug, halfALevelSlower)).get(halfALevelSlower), EPSILON);
    }

    @Test
    void severalTugsStackOnTheSlowestShipUpToTheFleetsBurnLevel() {
        FleetMemberAPI slowest = ship(HullSize.CRUISER, 6f);
        List<FleetMemberAPI> fleet = List.of(tug(HullSize.CRUISER, 8f), tug(HullSize.CRUISER, 8f), tug(HullSize.CRUISER, 8f),
                ship(HullSize.FRIGATE, 8f), slowest);

        assertEquals(Map.of(slowest, 2f), TowLines.towBonuses(fleet));
    }

    @Test
    void tugsSpreadAcrossShipsTiedForSlowest() {
        FleetMemberAPI firstSlow = ship(HullSize.FRIGATE, 6f);
        FleetMemberAPI secondSlow = ship(HullSize.FRIGATE, 6f);

        assertEquals(Map.of(firstSlow, 1f, secondSlow, 1f),
                TowLines.towBonuses(List.of(tug(HullSize.FRIGATE, 9f), tug(HullSize.FRIGATE, 9f), firstSlow, secondSlow)));
    }

    @Test
    void smallerTugsPickFirst() {
        FleetMemberAPI capitalTug = tug(HullSize.CAPITAL_SHIP, 9f);
        FleetMemberAPI frigateTug = tug(HullSize.FRIGATE, 9f);
        FleetMemberAPI slowFrigate = ship(HullSize.FRIGATE, 6f);
        FleetMemberAPI slowCapital = ship(HullSize.CAPITAL_SHIP, 6f);

        assertEquals(Map.of(slowFrigate, 1f, slowCapital, 1f), TowLines.towBonuses(List.of(capitalTug, slowFrigate, slowCapital, frigateTug)));
    }

    @Test
    void mothballedShipsDoNotTow() {
        FleetMemberAPI mothballedTug = ship(HullSize.CRUISER, 9f, 1f, true);

        assertEquals(Map.of(), TowLines.towBonuses(List.of(mothballedTug, ship(HullSize.FRIGATE, 6f))));
    }

    @Test
    void applyMovesTheTowWhenTheSlowestShipChanges() {
        FleetMemberAPI tug = tug(HullSize.CRUISER, 9f);
        FleetMemberAPI first = ship(HullSize.FRIGATE, 6f);
        FleetMemberAPI second = ship(HullSize.FRIGATE, 7f);
        CampaignFleetAPI fleet = fleet(List.of(tug, first, second));

        TowLines.apply(fleet);
        assertEquals(7f, burnLevel(first), EPSILON);

        first.getStats().getMaxBurnLevel().modifyFlat("refit", 2f);
        TowLines.apply(fleet);

        assertEquals(8f, burnLevel(first), EPSILON);
        assertEquals(8f, burnLevel(second), EPSILON);
        assertEquals(1f, second.getStats().getMaxBurnLevel().getFlatStatMod(ExiledSector.key("towLine")).getValue(), EPSILON);
    }
}
