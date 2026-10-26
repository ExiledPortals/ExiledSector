package exiledsector.charts.effect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.BattleObjectiveAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.CombatTaskManagerAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import exiledsector.charts.content.ChartContentGenerator;
import exiledsector.charts.ChartGenContext.AppliedEffect;
import exiledsector.charts.ChartGenContext;
import exiledsector.charts.ChartIds;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChartBattlePluginTest {

    private CombatEngineAPI engine;
    private CombatFleetManagerAPI playerManager;
    private CombatFleetManagerAPI enemyManager;

    @BeforeEach
    void setUp() {
        engine = mock(CombatEngineAPI.class);
        when(engine.isInCampaign()).thenReturn(true);
        playerManager = mock(CombatFleetManagerAPI.class);
        enemyManager = mock(CombatFleetManagerAPI.class);
        when(engine.getFleetManager(FleetSide.PLAYER)).thenReturn(playerManager);
        when(engine.getFleetManager(FleetSide.ENEMY)).thenReturn(enemyManager);
    }

    @AfterEach
    void tearDown() {
        ChartFleetConditions.clear(null);
    }

    private ChartBattlePlugin startedPlugin(Map<ChartFleetStat, Float> mults) {
        ChartFleetConditions.set(mults, null);
        ChartBattlePlugin plugin = new ChartBattlePlugin();
        plugin.init(engine);
        return plugin;
    }

    private static DynamicStatsAPI dynamicStatsOf(MutableShipStatsAPI stats) {
        DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class);
        when(stats.getDynamic()).thenReturn(dynamic);
        return dynamic;
    }

    @Test
    void battleSizeChangesAreSplitByEachSidesCapAndFollowCapturedObjectives() {
        when(playerManager.getMaxStrength()).thenReturn(180);
        when(enemyManager.getMaxStrength()).thenReturn(120);
        BattleObjectiveAPI objective = mock(BattleObjectiveAPI.class);
        when(objective.getOwner()).thenReturn(-1);
        when(objective.getBattleSizeFractionBonus()).thenReturn(0.1f);
        when(engine.getObjectives()).thenReturn(List.of(objective));
        ChartFleetConditions.setBattleSizeBonus(-50);
        ChartBattlePlugin plugin = startedPlugin(Map.of());

        try (MockedStatic<Global> globalMock = Mockito.mockStatic(Global.class)) {
            SettingsAPI settings = mock(SettingsAPI.class);
            when(settings.getBattleSize()).thenReturn(300);
            globalMock.when(Global::getSettings).thenReturn(settings);

            plugin.advance(0f, List.of());
            plugin.advance(ChartBattlePlugin.REFRESH_SECONDS, List.of());
            verify(engine, times(1)).setMaxFleetPoints(FleetSide.PLAYER, 150);
            verify(engine, times(1)).setMaxFleetPoints(FleetSide.ENEMY, 100);

            when(objective.getOwner()).thenReturn(ChartBattlePlugin.PLAYER_OWNER);
            plugin.advance(ChartBattlePlugin.REFRESH_SECONDS, List.of());
            verify(engine).setMaxFleetPoints(FleetSide.PLAYER, 180);
        }
    }

    @Test
    void battleSizeLinesShowTheirSign() {
        assertTrue(ChartEffect.CHART_SMALLER_BATTLES_FLAT.description(30f).plain().contains("-30"));
        assertTrue(ChartEffect.CHART_LARGER_BATTLES_FLAT.description(40f).plain().contains("+40"));
        ChartGenContext context = ChartGenContext.of(List.of(new AppliedEffect(ChartEffect.CHART_SMALLER_BATTLES_FLAT, 30f),
                new AppliedEffect(ChartEffect.CHART_LARGER_BATTLES_FLAT, 45f)));
        assertEquals(15, context.battleSizeBonus());
    }

    @Test
    void cheaperEnemyShipsRaiseTheEnemyCapFromTheFirstFrameWithoutTouchingTheirStats() {
        when(playerManager.getMaxStrength()).thenReturn(180);
        when(enemyManager.getMaxStrength()).thenReturn(120);
        ChartBattlePlugin plugin = startedPlugin(Map.of(ChartFleetStat.ENEMY_DEPLOYMENT_COST, 0.8f));

        plugin.advance(0f, List.of());

        verify(engine).setMaxFleetPoints(FleetSide.ENEMY, 150);
        verify(engine).setMaxFleetPoints(FleetSide.PLAYER, 180);
        verify(enemyManager, never()).getReservesCopy();
    }

    @Test
    void commandPointRecoverySlowsByTheMultiplierOfTheWholeRate() {
        CombatTaskManagerAPI taskManager = mock(CombatTaskManagerAPI.class);
        MutableStat rateModifier = new MutableStat(0f);
        when(playerManager.getTaskManager(false)).thenReturn(taskManager);
        when(taskManager.getCPRateModifier()).thenReturn(rateModifier);
        when(taskManager.getCPRateMult()).thenReturn(1.2f);
        ChartBattlePlugin plugin = startedPlugin(Map.of(ChartFleetStat.COMMAND_POINT_RATE, 0.75f));

        plugin.advance(0f, List.of());
        assertEquals(-0.3f, rateModifier.getFlatStatMod(ChartBattlePlugin.MOD_ID).getValue(), 0.0001f);

        when(taskManager.getCPRateMult()).thenReturn(0.9f);
        plugin.advance(ChartBattlePlugin.REFRESH_SECONDS, List.of());
        assertEquals(-0.3f, rateModifier.getFlatStatMod(ChartBattlePlugin.MOD_ID).getValue(), 0.0001f);
    }

    @Test
    void onlyEnemyShipsCaptureFaster() {
        MutableStat enemyCapture = new MutableStat(1f);
        MutableStat playerCapture = new MutableStat(1f);
        ShipAPI enemyShip = shipWithCaptureRate(ChartBattlePlugin.ENEMY_OWNER, enemyCapture);
        ShipAPI playerShip = shipWithCaptureRate(0, playerCapture);
        when(engine.getShips()).thenReturn(List.of(enemyShip, playerShip));
        ChartBattlePlugin plugin = startedPlugin(Map.of(ChartFleetStat.ENEMY_OBJECTIVE_CAPTURE_RATE, 1.5f));

        plugin.advance(0f, List.of());

        assertEquals(1.5f, enemyCapture.getModifiedValue(), 0.0001f);
        assertEquals(1f, playerCapture.getModifiedValue(), 0.0001f);
    }

    @Test
    void enemyShipsAndFightersGetTheDamageListenerOnce() {
        ShipAPI enemyShip = mock(ShipAPI.class);
        ShipAPI enemyFighter = mock(ShipAPI.class);
        ShipAPI playerShip = mock(ShipAPI.class);
        when(enemyShip.getOwner()).thenReturn(ChartBattlePlugin.ENEMY_OWNER);
        when(enemyFighter.getOwner()).thenReturn(ChartBattlePlugin.ENEMY_OWNER);
        when(enemyFighter.isFighter()).thenReturn(true);
        when(enemyFighter.hasListenerOfClass(ChartEnemyDamageListener.class)).thenReturn(true);
        when(playerShip.getOwner()).thenReturn(0);
        when(engine.getShips()).thenReturn(List.of(enemyShip, enemyFighter, playerShip));
        ChartBattlePlugin plugin = startedPlugin(Map.of(ChartFleetStat.ENEMY_KINETIC_DAMAGE_DEALT, 1.2f));

        plugin.advance(0f, List.of());

        verify(enemyShip).addListener(ChartEnemyDamageListener.INSTANCE);
        verify(enemyFighter, never()).addListener(ChartEnemyDamageListener.INSTANCE);
        verify(playerShip, never()).addListener(ChartEnemyDamageListener.INSTANCE);
    }

    @Test
    void theEnemyDamageListenerRaisesOnlyItsDamageType() {
        ChartFleetConditions.set(Map.of(ChartFleetStat.ENEMY_KINETIC_DAMAGE_DEALT, 1.2f), null);
        DamageAPI kinetic = damageOf(DamageType.KINETIC);
        DamageAPI energy = damageOf(DamageType.ENERGY);

        assertEquals(ChartFleetConditions.MOD_ID, ChartEnemyDamageListener.INSTANCE.modifyDamageDealt(null, null, kinetic, null, false));
        assertNull(ChartEnemyDamageListener.INSTANCE.modifyDamageDealt(null, null, energy, null, true));

        assertEquals(1.2f, kinetic.getModifier().getMultStatMod(ChartFleetConditions.MOD_ID).getValue(), 0.0001f);
        assertNull(energy.getModifier().getMultStatMod(ChartFleetConditions.MOD_ID));
    }

    private static DamageAPI damageOf(DamageType type) {
        DamageAPI damage = mock(DamageAPI.class);
        MutableStat modifier = new MutableStat(1f);
        when(damage.getType()).thenReturn(type);
        when(damage.getModifier()).thenReturn(modifier);
        return damage;
    }

    private static ShipAPI shipWithCaptureRate(int owner, MutableStat captureRate) {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.getMutableStats()).thenReturn(stats);
        when(dynamicStatsOf(stats).getStat(Stats.SHIP_OBJECTIVE_CAP_RATE_MULT)).thenReturn(captureRate);
        return ship;
    }

    @Test
    void thePluginStaysOffOutsideChartBattlesAndInSimulations() {
        ChartBattlePlugin onlyShipDrawbacks = startedPlugin(Map.of(ChartFleetStat.TOP_SPEED, 0.8f));
        onlyShipDrawbacks.advance(0f, List.of());

        ChartFleetConditions.set(Map.of(ChartFleetStat.ENEMY_DEPLOYMENT_COST, 0.8f), null);
        when(engine.isSimulation()).thenReturn(true);
        ChartBattlePlugin simulation = new ChartBattlePlugin();
        simulation.init(engine);
        simulation.advance(0f, List.of());

        verify(enemyManager, never()).getReservesCopy();
    }

    @Test
    void huntedGivesEveryChartFleetTheSensorBonusAtSpawn() {
        ChartGenContext context = ChartGenContext.of(List.of(new AppliedEffect(ChartEffect.CHART_ENEMY_SENSOR_RANGE_FLAT, 5000f)));
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        MutableFleetStatsAPI fleetStats = mock(MutableFleetStatsAPI.class);
        StatBonus sensorRange = new StatBonus();
        when(fleet.getStats()).thenReturn(fleetStats);
        when(fleetStats.getSensorRangeMod()).thenReturn(sensorRange);

        ChartContentGenerator.applyEnemySensorRange(fleet, context.enemySensorRangeBonus());
        assertEquals(5000f, sensorRange.getFlatBonus(ChartIds.ENEMY_SENSOR_MOD_ID).getValue(), 0.0001f);

        StatBonus untouched = new StatBonus();
        when(fleetStats.getSensorRangeMod()).thenReturn(untouched);
        ChartContentGenerator.applyEnemySensorRange(fleet, ChartGenContext.none().enemySensorRangeBonus());
        assertNull(untouched.getFlatBonus(ChartIds.ENEMY_SENSOR_MOD_ID));
    }
}
