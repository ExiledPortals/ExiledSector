package exiledsector.charts;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import exiledsector.charts.ChartGenContext.AppliedEffect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChartFleetConditionsTest {

    private CampaignFleetAPI playerFleet;
    private FleetMemberAPI playerMember;

    @BeforeEach
    void setUp() {
        playerFleet = mock(CampaignFleetAPI.class);
        when(playerFleet.isPlayerFleet()).thenReturn(true);
        FleetDataAPI fleetData = mock(FleetDataAPI.class);
        when(playerFleet.getFleetData()).thenReturn(fleetData);
        when(fleetData.getFleet()).thenReturn(playerFleet);
        playerMember = mock(FleetMemberAPI.class);
        when(playerMember.getFleetData()).thenReturn(fleetData);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(playerMember));
    }

    @AfterEach
    void tearDown() {
        ChartFleetConditions.clear(null);
    }

    @Test
    void rollsOfOneStatAddUpIntoOneClampedMultiplier() {
        ChartGenContext context = ChartGenContext.of(List.of(
                new AppliedEffect(ChartEffect.CHART_TOP_SPEED_PERCENT, -10f),
                new AppliedEffect(ChartEffect.CHART_TOP_SPEED_PERCENT, -15f),
                new AppliedEffect(ChartEffect.CHART_KINETIC_DAMAGE_TAKEN_PERCENT, 20f),
                new AppliedEffect(ChartEffect.CHART_SHIELD_ARC_PERCENT, -500f)));

        Map<ChartFleetStat, Float> mults = context.fleetStatMults();

        assertEquals(0.75f, mults.get(ChartFleetStat.TOP_SPEED), 0.0001f);
        assertEquals(1.2f, mults.get(ChartFleetStat.KINETIC_DAMAGE_TAKEN), 0.0001f);
        assertEquals(ChartFleetStat.MIN_MULT, mults.get(ChartFleetStat.SHIELD_ARC), 0.0001f);
        assertEquals(3, mults.size());
        assertTrue(ChartGenContext.none().fleetStatMults().isEmpty());
    }

    @Test
    void theDescriptionShowsTheSizeOfAPenaltyWithoutItsSign() {
        String description = ChartEffect.CHART_TOP_SPEED_PERCENT.description(-15f).plain();
        assertTrue(description.contains("15%"));
        assertFalse(description.contains("-15"));
    }

    @Test
    void settingTheConditionsMarksThePlayerFleetForAStatUpdateOnlyWhenTheyChange() {
        Map<ChartFleetStat, Float> mults = Map.of(ChartFleetStat.TOP_SPEED, 0.8f);

        assertTrue(ChartFleetConditions.set(mults, playerFleet));
        assertFalse(ChartFleetConditions.set(mults, playerFleet));
        assertFalse(ChartFleetConditions.set(Map.of(ChartFleetStat.TOP_SPEED, 0.8f), playerFleet));
        verify(playerMember, times(1)).setStatUpdateNeeded(true);

        assertTrue(ChartFleetConditions.clear(playerFleet));
        assertFalse(ChartFleetConditions.isActive());
        verify(playerMember, times(2)).setStatUpdateNeeded(true);
    }

    @Test
    void onlyShipsAndFightersOfThePlayerFleetAreAffected() {
        ChartFleetConditions.set(Map.of(ChartFleetStat.TOP_SPEED, 0.8f, ChartFleetStat.MISSILE_WEAPON_RANGE, 0.9f,
                ChartFleetStat.ENERGY_DAMAGE_TAKEN, 1.25f), playerFleet);
        ShipStats playerShip = new ShipStats(playerMember);
        FleetMemberAPI otherMember = mock(FleetMemberAPI.class);
        ShipStats otherShip = new ShipStats(otherMember);

        ChartFleetConditions.applyToShip(playerShip.stats);
        ChartFleetConditions.applyToShip(otherShip.stats);

        assertEquals(0.8f, playerShip.maxSpeed.getMultStatMod(ChartFleetConditions.MOD_ID).getValue(), 0.0001f);
        assertEquals(0.9f, playerShip.missileRange.getMultBonus(ChartFleetConditions.MOD_ID).getValue(), 0.0001f);
        assertEquals(1.25f, playerShip.energyTaken.getMultStatMod(ChartFleetConditions.MOD_ID).getValue(), 0.0001f);
        assertEquals(1.25f, playerShip.energyShieldTaken.getMultStatMod(ChartFleetConditions.MOD_ID).getValue(), 0.0001f);
        assertNull(otherShip.maxSpeed.getMultStatMod(ChartFleetConditions.MOD_ID));

        ShipAPI carrier = mock(ShipAPI.class);
        when(carrier.getMutableStats()).thenReturn(playerShip.stats);
        ShipStats fighterStats = new ShipStats(null);
        ShipAPI fighter = mock(ShipAPI.class);
        when(fighter.getMutableStats()).thenReturn(fighterStats.stats);
        ChartFleetConditions.applyToFighter(fighter, carrier);
        assertEquals(0.8f, fighterStats.maxSpeed.getMultStatMod(ChartFleetConditions.MOD_ID).getValue(), 0.0001f);
    }

    @Test
    void crampedRaisesTheDeploymentCostOfPlayerShipsAndBattleOnlyStatsLeaveShipsAlone() {
        ChartFleetConditions.set(Map.of(ChartFleetStat.DEPLOYMENT_COST, 1.25f, ChartFleetStat.ENEMY_DEPLOYMENT_COST, 0.8f,
                ChartFleetStat.COMMAND_POINT_RATE, 0.7f), playerFleet);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class);
        StatBonus deploymentCost = new StatBonus();
        when(stats.getFleetMember()).thenReturn(playerMember);
        when(stats.getDynamic()).thenReturn(dynamic);
        when(dynamic.getMod(Stats.DEPLOYMENT_POINTS_MOD)).thenReturn(deploymentCost);

        ChartFleetConditions.applyToShip(stats);

        assertEquals(25f, deploymentCost.computeEffective(20f), 0.0001f);
        assertFalse(ChartFleetStat.ENEMY_DEPLOYMENT_COST.appliesToShips());
        assertFalse(ChartFleetStat.COMMAND_POINT_RATE.appliesToShips());
    }

    @Test
    void shieldAndArmorDrawbacksReachTheirShipStatsAndEnemyDamageStaysOffShips() {
        ChartFleetConditions.set(Map.of(ChartFleetStat.SHIELD_UPKEEP, 1.5f, ChartFleetStat.SHIELD_RAISE_RATE, 0.6f,
                ChartFleetStat.SHIELD_DAMAGE_TAKEN, 1.2f, ChartFleetStat.ARMOR_DAMAGE_TAKEN, 1.15f,
                ChartFleetStat.ENEMY_ENERGY_DAMAGE_DEALT, 1.2f), playerFleet);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat upkeep = new MutableStat(1f);
        MutableStat raiseRate = new MutableStat(1f);
        MutableStat shieldTaken = new MutableStat(1f);
        MutableStat armorTaken = new MutableStat(1f);
        when(stats.getFleetMember()).thenReturn(playerMember);
        when(stats.getShieldUpkeepMult()).thenReturn(upkeep);
        when(stats.getShieldUnfoldRateMult()).thenReturn(raiseRate);
        when(stats.getShieldDamageTakenMult()).thenReturn(shieldTaken);
        when(stats.getArmorDamageTakenMult()).thenReturn(armorTaken);

        ChartFleetConditions.applyToShip(stats);

        assertEquals(1.5f, upkeep.getModifiedValue(), 0.0001f);
        assertEquals(0.6f, raiseRate.getModifiedValue(), 0.0001f);
        assertEquals(1.2f, shieldTaken.getModifiedValue(), 0.0001f);
        assertEquals(1.15f, armorTaken.getModifiedValue(), 0.0001f);
        assertFalse(ChartFleetStat.ENEMY_ENERGY_DAMAGE_DEALT.appliesToShips());
    }

    @Test
    void nothingIsTouchedWhileNoConditionsAreActive() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        when(stats.getFleetMember()).thenReturn(playerMember);

        ChartFleetConditions.applyToShip(stats);

        verify(stats, never()).getMaxSpeed();
    }

    private static final class ShipStats {
        final MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        final MutableStat maxSpeed = new MutableStat(100f);
        final MutableStat energyTaken = new MutableStat(1f);
        final MutableStat energyShieldTaken = new MutableStat(1f);
        final StatBonus missileRange = new StatBonus();

        ShipStats(FleetMemberAPI member) {
            when(stats.getFleetMember()).thenReturn(member);
            when(stats.getMaxSpeed()).thenReturn(maxSpeed);
            when(stats.getEnergyDamageTakenMult()).thenReturn(energyTaken);
            when(stats.getEnergyShieldDamageTakenMult()).thenReturn(energyShieldTaken);
            when(stats.getBallisticWeaponRangeBonus()).thenReturn(new StatBonus());
            when(stats.getEnergyWeaponRangeBonus()).thenReturn(new StatBonus());
            when(stats.getMissileWeaponRangeBonus()).thenReturn(missileRange);
        }
    }
}
