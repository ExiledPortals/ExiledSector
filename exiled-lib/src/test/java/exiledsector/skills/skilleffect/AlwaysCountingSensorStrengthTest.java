package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AlwaysCountingSensorStrengthTest {

    @Test
    void aShipInsideTheTopFiveAddsNothingExtra() {
        float extra = FleetWideEffects.strengthLeftOutOfTheTop(
                new float[]{100, 90, 80, 70, 60, 50}, new boolean[]{false, true, false, false, false, false}, 5);

        assertEquals(0f, extra);
    }

    @Test
    void aShipOutsideTheTopFiveAddsItsWholeStrength() {
        float extra = FleetWideEffects.strengthLeftOutOfTheTop(
                new float[]{100, 90, 80, 70, 60, 50, 40}, new boolean[]{false, false, false, false, false, false, true}, 5);

        assertEquals(40f, extra);
    }

    @Test
    void onATieTheOrdinaryShipTakesTheTopSlotSoTheMarkedShipStillAdds() {
        float extra = FleetWideEffects.strengthLeftOutOfTheTop(
                new float[]{60, 60, 60, 60, 60, 60}, new boolean[]{true, false, false, false, false, false}, 5);

        assertEquals(60f, extra);
    }

    @Test
    void severalMarkedShipsOutsideTheTopAllAdd() {
        float extra = FleetWideEffects.strengthLeftOutOfTheTop(
                new float[]{30, 100, 30, 90, 80, 70, 60}, new boolean[]{true, false, true, false, false, false, false}, 5);

        assertEquals(60f, extra);
    }

    @Test
    void theEffectMarksTheShipForTheFleetCalculation() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, Answers.RETURNS_DEEP_STUBS);
        StatBonus flag = new StatBonus();
        when(stats.getDynamic().getMod(FleetWideEffects.SENSOR_STRENGTH_ALWAYS_COUNTS_KEY)).thenReturn(flag);

        LogisticsSkillEffect.SENSOR_STRENGTH_ALWAYS_COUNTS.apply(stats, "mod_id", 0f);

        assertEquals(1f, flag.getFlatBonus());
    }

    private static FleetMemberAPI member(float sensorStrength, boolean alwaysCounts, boolean mothballed) {
        FleetMemberAPI member = mock(FleetMemberAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(member.isMothballed()).thenReturn(mothballed);
        when(member.getStats().getSensorStrength().getModifiedValue()).thenReturn(sensorStrength);
        DynamicStatsAPI dynamic = member.getStats().getDynamic();
        when(dynamic.getValue(FleetWideEffects.SENSOR_STRENGTH_ALWAYS_COUNTS_KEY, 0f)).thenReturn(alwaysCounts ? 1f : 0f);
        return member;
    }

    private static StatBonus syncFleet(List<FleetMemberAPI> members) {
        return syncFleet(members, new StatBonus());
    }

    private static StatBonus syncFleet(List<FleetMemberAPI> members, StatBonus sensorStrength) {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(fleet.getFleetData().getMembersListCopy()).thenReturn(members);
        MutableFleetStatsAPI fleetStats = mock(MutableFleetStatsAPI.class);
        when(fleet.getStats()).thenReturn(fleetStats);
        when(fleetStats.getSensorStrengthMod()).thenReturn(sensorStrength);
        SettingsAPI settings = mock(SettingsAPI.class);
        when(settings.getInt("maxSensorShips")).thenReturn(5);
        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            global.when(Global::getSettings).thenReturn(settings);
            FleetWideEffects.applyAlwaysCountingSensorStrength(fleet);
        }
        return sensorStrength;
    }

    @Test
    void theFleetGetsAFlatBonusForMarkedShipsLeftOutOfTheTopFive() {
        List<FleetMemberAPI> members = new ArrayList<>();
        for (float strength : new float[]{100, 90, 80, 70, 60}) {
            members.add(member(strength, false, false));
        }
        members.add(member(45, true, false));
        members.add(member(30, true, true));

        StatBonus sensorStrength = syncFleet(members);

        assertEquals(45f, sensorStrength.getFlatBonus());
    }

    @Test
    void theBonusIsRemovedOnceNoMarkedShipIsLeftOut() {
        StatBonus stale = new StatBonus();
        stale.modifyFlat("exiledSector_sensorStrengthAlwaysCounts", 45f);

        StatBonus sensorStrength = syncFleet(List.of(member(100, true, false), member(50, false, false)), stale);

        assertNull(sensorStrength.getFlatBonuses().get("exiledSector_sensorStrengthAlwaysCounts"));
    }
}
