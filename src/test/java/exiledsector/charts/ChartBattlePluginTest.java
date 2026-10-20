package exiledsector.charts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.CombatTaskManagerAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import exiledsector.charts.ChartGenContext.AppliedEffect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChartBattlePluginTest {

    private static final int BATTLE_SIZE = 300;

    private MockedStatic<Global> globalMock;
    private CombatEngineAPI engine;
    private CombatFleetManagerAPI playerManager;
    private CombatFleetManagerAPI enemyManager;
    private StatBonus playerFraction;
    private StatBonus enemyFraction;
    private PersonAPI playerCommander;

    @BeforeEach
    void setUp() {
        SettingsAPI settings = mock(SettingsAPI.class);
        when(settings.getBattleSize()).thenReturn(BATTLE_SIZE);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSettings).thenReturn(settings);
        engine = mock(CombatEngineAPI.class);
        when(engine.isInCampaign()).thenReturn(true);
        playerManager = mock(CombatFleetManagerAPI.class);
        enemyManager = mock(CombatFleetManagerAPI.class);
        when(engine.getFleetManager(FleetSide.PLAYER)).thenReturn(playerManager);
        when(engine.getFleetManager(FleetSide.ENEMY)).thenReturn(enemyManager);
        playerFraction = new StatBonus();
        enemyFraction = new StatBonus();
        playerCommander = commanderWith(playerFraction);
        when(playerManager.getFleetCommanderPreferPlayer()).thenReturn(playerCommander);
        PersonAPI enemyCommander = commanderWith(enemyFraction);
        when(enemyManager.getFleetCommanderPreferPlayer()).thenReturn(enemyCommander);
    }

    @AfterEach
    void tearDown() {
        ChartBattlePlugin.liftCommanderBonuses();
        ChartFleetConditions.clear(null);
        globalMock.close();
    }

    private static PersonAPI commanderWith(StatBonus fractionBonus) {
        PersonAPI commander = mock(PersonAPI.class);
        MutableCharacterStatsAPI stats = mock(MutableCharacterStatsAPI.class);
        DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class);
        when(commander.getStats()).thenReturn(stats);
        when(stats.getDynamic()).thenReturn(dynamic);
        when(dynamic.getMod(Stats.DEPLOYMENT_POINTS_FRACTION_OF_BATTLE_SIZE_BONUS_MOD)).thenReturn(fractionBonus);
        return commander;
    }

    private ChartBattlePlugin startedPlugin(Map<ChartFleetStat, Float> mults) {
        ChartFleetConditions.set(mults, null);
        ChartBattlePlugin plugin = new ChartBattlePlugin();
        plugin.init(engine);
        return plugin;
    }

    @Test
    void thePlayersDeploymentCapShrinksAtOnceAndStaysStableAfterTheEngineRecomputesIt() {
        ChartBattlePlugin plugin = startedPlugin(Map.of(ChartFleetStat.DEPLOYMENT_POINTS, 0.8f));
        when(playerManager.getMaxStrength()).thenReturn(120);

        plugin.advance(0f, List.of());

        assertEquals(-24f / BATTLE_SIZE, playerFraction.getFlatBonus(ChartBattlePlugin.MOD_ID).getValue(), 0.0001f);
        verify(playerManager).setMaxStrength(96);
        assertNull(enemyFraction.getFlatBonus(ChartBattlePlugin.MOD_ID));

        when(playerManager.getMaxStrength()).thenReturn(96);
        plugin.advance(ChartBattlePlugin.REFRESH_SECONDS, List.of());
        assertEquals(-24f / BATTLE_SIZE, playerFraction.getFlatBonus(ChartBattlePlugin.MOD_ID).getValue(), 0.0001f);

        when(playerManager.getMaxStrength()).thenReturn(126);
        plugin.advance(ChartBattlePlugin.REFRESH_SECONDS, List.of());
        assertEquals(-30f / BATTLE_SIZE, playerFraction.getFlatBonus(ChartBattlePlugin.MOD_ID).getValue(), 0.0001f,
                "a captured objective raises the base, and the penalty follows it");
    }

    @Test
    void theEnemyCapGrowsAndCommanderBonusesAreLiftedAfterTheBattle() {
        ChartBattlePlugin plugin = startedPlugin(Map.of(ChartFleetStat.ENEMY_DEPLOYMENT_POINTS, 1.25f));
        when(enemyManager.getMaxStrength()).thenReturn(160);

        plugin.advance(0f, List.of());

        assertEquals(40f / BATTLE_SIZE, enemyFraction.getFlatBonus(ChartBattlePlugin.MOD_ID).getValue(), 0.0001f);
        verify(enemyManager).setMaxStrength(200);
        assertTrue(ChartBattlePlugin.hasCommanderBonuses());

        ChartBattlePlugin.liftCommanderBonuses();
        assertNull(enemyFraction.getFlatBonus(ChartBattlePlugin.MOD_ID));
        assertFalse(ChartBattlePlugin.hasCommanderBonuses());
    }

    @Test
    void endingTheBattleLiftsTheBonusesAndStopsThePlugin() {
        ChartBattlePlugin plugin = startedPlugin(Map.of(ChartFleetStat.DEPLOYMENT_POINTS, 0.8f));
        when(playerManager.getMaxStrength()).thenReturn(120);
        plugin.advance(0f, List.of());

        when(engine.isCombatOver()).thenReturn(true);
        plugin.advance(ChartBattlePlugin.REFRESH_SECONDS, List.of());

        assertNull(playerFraction.getFlatBonus(ChartBattlePlugin.MOD_ID));
    }

    @Test
    void aSavedPlayerCommanderBonusIsLiftedOnLoad() {
        playerFraction.modifyFlat(ChartBattlePlugin.MOD_ID, -0.1f);

        ChartBattlePlugin.liftFrom(playerCommander);

        assertNull(playerFraction.getFlatBonus(ChartBattlePlugin.MOD_ID));
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

    private static ShipAPI shipWithCaptureRate(int owner, MutableStat captureRate) {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.getMutableStats()).thenReturn(stats);
        when(stats.getDynamic()).thenReturn(dynamic);
        when(dynamic.getStat(Stats.SHIP_OBJECTIVE_CAP_RATE_MULT)).thenReturn(captureRate);
        return ship;
    }

    @Test
    void thePluginStaysOffOutsideChartBattlesAndInSimulations() {
        ChartBattlePlugin onlyShipDrawbacks = startedPlugin(Map.of(ChartFleetStat.TOP_SPEED, 0.8f));
        onlyShipDrawbacks.advance(0f, List.of());

        ChartFleetConditions.set(Map.of(ChartFleetStat.DEPLOYMENT_POINTS, 0.8f), null);
        when(engine.isSimulation()).thenReturn(true);
        ChartBattlePlugin simulation = new ChartBattlePlugin();
        simulation.init(engine);
        simulation.advance(0f, List.of());

        verify(playerManager, never()).setMaxStrength(anyInt());
        verify(playerManager, never()).getMaxStrength();
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
