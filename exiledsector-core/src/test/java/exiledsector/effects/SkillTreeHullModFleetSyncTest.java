package exiledsector.effects;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import exiledsector.ExiledSector;
import exiledsector.i18n.HeadlessGameText;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SkillTreeHullModFleetSyncTest {

    private static final float EPSILON = 1e-4f;
    private static final String CORONA_CONTRIBUTION_KEY = "exiledSector_fleetCoronaResistanceContribution";

    private StatBonus sensorStrength;
    private StatBonus detectedRange;
    private StatBonus fleetBurn;
    private StatBonus moveSlowSpeed;
    private MutableStat navigationPenalty;

    @BeforeEach
    void setUp() {
        sensorStrength = new StatBonus();
        detectedRange = new StatBonus();
        fleetBurn = new StatBonus();
        moveSlowSpeed = new StatBonus();
        navigationPenalty = new MutableStat(1f);
    }

    private static FleetMemberAPI member(boolean carriesTree, float coronaContribution, MutableStat coronaEffect, MutableStat maxBurnLevel) {
        FleetMemberAPI member = mock(FleetMemberAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(member.getVariant().hasHullMod(SkillTreeHullMod.ID)).thenReturn(carriesTree);
        when(member.getStats().getDynamic().getValue(CORONA_CONTRIBUTION_KEY, 0f)).thenReturn(coronaContribution);
        when(member.getStats().getDynamic().getStat(Stats.CORONA_EFFECT_MULT)).thenReturn(coronaEffect);
        when(member.getStats().getMaxBurnLevel()).thenReturn(maxBurnLevel);
        return member;
    }

    private CampaignFleetAPI fleet(FleetMemberAPI... members) {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(fleet.isPlayerFleet()).thenReturn(false);
        when(fleet.getFleetData().getMembersListCopy()).thenReturn(List.of(members));
        when(fleet.getStats().getSensorStrengthMod()).thenReturn(sensorStrength);
        when(fleet.getStats().getDetectedRangeMod()).thenReturn(detectedRange);
        when(fleet.getStats().getFleetwideMaxBurnMod()).thenReturn(fleetBurn);
        when(fleet.getStats().getDynamic().getMod(Stats.MOVE_SLOW_SPEED_BONUS_MOD)).thenReturn(moveSlowSpeed);
        when(fleet.getCommanderStats().getDynamic().getStat(Stats.NAVIGATION_PENALTY_MULT)).thenReturn(navigationPenalty);
        return fleet;
    }

    @Test
    void aFleetWhoseLastTreeShipLeftLosesEveryFleetWideModifierWithoutReadingContributions() {
        MutableStat coronaEffect = new MutableStat(1f);
        coronaEffect.modifyMult(ExiledSector.key("fleetCoronaResistance"), 0.5f);
        MutableStat maxBurnLevel = new MutableStat(8f);
        maxBurnLevel.modifyFlat(ExiledSector.key("towLine"), 1f);
        FleetMemberAPI leftBehind = member(false, 0f, coronaEffect, maxBurnLevel);
        sensorStrength.modifyFlat(ExiledSector.key("sensorStrengthAlwaysCounts"), 5f);
        sensorStrength.modifyPercent(ExiledSector.key("swarmSensorStrength"), 10f);
        detectedRange.modifyPercent(ExiledSector.key("transponderOffSensorProfile"), -10f);
        fleetBurn.modifyFlat(ExiledSector.key("transponderOffBurnLevel"), 1f);
        moveSlowSpeed.modifyFlat(ExiledSector.key("moveSlowBurnLevel"), 1f);
        navigationPenalty.modifyFlat(ExiledSector.key("terrainMovementPenalty"), -0.2f);

        new SkillTreeHullMod().onFleetSync(fleet(leftBehind));

        assertNull(sensorStrength.getFlatBonus(ExiledSector.key("sensorStrengthAlwaysCounts")));
        assertNull(sensorStrength.getPercentBonus(ExiledSector.key("swarmSensorStrength")));
        assertNull(detectedRange.getPercentBonus(ExiledSector.key("transponderOffSensorProfile")));
        assertNull(fleetBurn.getFlatBonus(ExiledSector.key("transponderOffBurnLevel")));
        assertNull(moveSlowSpeed.getFlatBonus(ExiledSector.key("moveSlowBurnLevel")));
        assertNull(navigationPenalty.getFlatStatMod(ExiledSector.key("terrainMovementPenalty")));
        assertEquals(1f, coronaEffect.getModifiedValue(), EPSILON);
        assertEquals(8f, maxBurnLevel.getModifiedValue(), EPSILON);
        verify(leftBehind.getStats().getDynamic(), never()).getValue(anyString(), anyFloat());
    }

    @Test
    void aTreeShipsFleetWideEffectReachesEveryShipInItsFleet() throws Exception {
        MutableStat treeShipCorona = new MutableStat(1f);
        MutableStat escortCorona = new MutableStat(1f);
        FleetMemberAPI treeShip = member(true, 20f, treeShipCorona, new MutableStat(8f));
        FleetMemberAPI escort = member(false, 0f, escortCorona, new MutableStat(8f));

        try (HeadlessGameText ignored = HeadlessGameText.install(() -> false)) {
            new SkillTreeHullMod().onFleetSync(fleet(treeShip, escort));
        }

        assertEquals(0.8f, treeShipCorona.getModifiedValue(), EPSILON);
        assertEquals(0.8f, escortCorona.getModifiedValue(), EPSILON);
    }
}
