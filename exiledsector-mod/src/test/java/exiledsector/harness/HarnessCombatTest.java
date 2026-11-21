package exiledsector.harness;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HarnessCombatTest {

    private static ShipAPI ship(String name, String hullId, int owner, boolean fighter) {
        ShipAPI ship = mock(ShipAPI.class);
        ShipHullSpecAPI hull = mock(ShipHullSpecAPI.class);
        when(hull.getHullId()).thenReturn(hullId);
        when(hull.getBaseHullId()).thenReturn(hullId);
        when(ship.getHullSpec()).thenReturn(hull);
        when(ship.getName()).thenReturn(name);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.isFighter()).thenReturn(fighter);
        when(ship.isAlive()).thenReturn(true);
        return ship;
    }

    private static ApplyDamageResultAPI damage(float hull, float armor, float shields, float emp) {
        ApplyDamageResultAPI result = mock(ApplyDamageResultAPI.class);
        when(result.getDamageToHull()).thenReturn(hull);
        when(result.getTotalDamageToArmor()).thenReturn(armor);
        when(result.getDamageToShields()).thenReturn(shields);
        when(result.getEmpDamage()).thenReturn(emp);
        return result;
    }

    @Test
    void shipSelectorsFindThePlayerTheFirstFightingEnemyOrAHull() {
        ShipAPI player = ship("ISS Hope", "eagle", CombatShips.PLAYER_SIDE, false);
        ShipAPI enemyFighter = ship("Talon", "talon", CombatShips.ENEMY_SIDE, true);
        ShipAPI wreck = ship("Wreck", "hound", CombatShips.ENEMY_SIDE, false);
        when(wreck.isHulk()).thenReturn(true);
        ShipAPI enemy = ship("Hound", "hound", CombatShips.ENEMY_SIDE, false);
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        when(engine.getPlayerShip()).thenReturn(player);
        when(engine.getShips()).thenReturn(List.of(player, enemyFighter, wreck, enemy));

        assertSame(player, CombatShips.find(engine, CombatShips.PLAYER));
        assertSame(enemy, CombatShips.find(engine, CombatShips.ENEMY));
        assertSame(enemy, CombatShips.find(engine, "hound"));
        assertSame(player, CombatShips.find(engine, "iss hope"));
        assertNull(CombatShips.find(engine, "onslaught"));
        assertTrue(CombatShips.anyEnemyFighting(engine));
        when(enemy.isAlive()).thenReturn(false);
        assertFalse(CombatShips.anyEnemyFighting(engine));
    }

    @Test
    void damageIsSummedPerSideAndPerShipWithFightersOnlyInTheSideTotals() throws Exception {
        DamageTally tally = new DamageTally();

        tally.record(CombatShips.ENEMY_SIDE, "Hound (hound)", damage(100f, 50f, 0f, 20f));
        tally.record(CombatShips.ENEMY_SIDE, "Hound (hound)", damage(0f, 0f, 200f, 0f));
        tally.record(CombatShips.ENEMY_SIDE, null, damage(30f, 0f, 0f, 0f));
        tally.record(CombatShips.PLAYER_SIDE, "ISS Hope (eagle)", damage(0f, 10f, 40f, 0f));

        assertEquals(380f, tally.enemySideTotal());
        assertEquals(50f, tally.playerSideTotal());
        JSONObject json = tally.toJson(10f);
        assertEquals(38.0, json.getJSONObject("enemySideTook").getDouble("totalPerSecond"), 1e-4);
        assertEquals(20.0, json.getJSONObject("enemySideTook").getDouble("emp"), 1e-4);
        assertEquals(350.0, json.getJSONObject("shipsTook").getJSONObject("Hound (hound)").getDouble("total"), 1e-4);
        assertEquals(2, json.getJSONObject("shipsTook").length());
    }

    @Test
    void aWreckStillCountsForTheSideItStartedOn() {
        ShipAPI enemy = ship("Lasher", "lasher", CombatShips.ENEMY_SIDE, false);
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        when(engine.getShips()).thenReturn(List.of(enemy));
        DamageTally tally = new DamageTally();
        tally.watch(engine);

        when(enemy.getOwner()).thenReturn(100);
        tally.reportDamageApplied(null, enemy, damage(500f, 0f, 0f, 0f));

        assertEquals(500f, tally.enemySideTotal());
        assertEquals(0f, tally.playerSideTotal());
    }

    @Test
    void statsAreWrittenWithTheirModsBySource() throws Exception {
        MutableStat speed = new MutableStat(100f);
        speed.modifyPercent("exiledSector_tree", 20f, "Engine tuning");
        speed.modifyMult("hullmod_safety", 1.5f);
        StatBonus range = new StatBonus();
        range.modifyFlat("exiledSector_tree", 100f);

        JSONObject speedJson = ShipStatsDump.statJson(speed);
        JSONObject rangeJson = ShipStatsDump.bonusJson(range);

        assertEquals("maxSpeed", ShipStatsDump.statName("getMaxSpeed"));
        assertEquals(180.0, speedJson.getDouble("modified"), 1e-4);
        assertEquals("Engine tuning", speedJson.getJSONObject("percent").getJSONObject("exiledSector_tree").getString("desc"));
        assertEquals(1.5, speedJson.getJSONObject("mult").getDouble("hullmod_safety"), 1e-4);
        assertEquals(100.0, rangeJson.getDouble("flatTotal"), 1e-4);
    }

    @Test
    void enemiesSpawnInARowAheadAndAlliesBeside() {
        Vector2f centre = new Vector2f(0f, -1500f);

        Vector2f lone = SpawnStep.spawnLocation(centre, true, 2500f, 0, 1);
        Vector2f leftOfTwo = SpawnStep.spawnLocation(centre, true, 2500f, 0, 2);
        Vector2f ally = SpawnStep.spawnLocation(centre, false, 800f, 0, 1);

        assertEquals(List.of(0f, 1000f), List.of(lone.x, lone.y));
        assertEquals(List.of(-SpawnStep.SPACING / 2f, 1000f), List.of(leftOfTwo.x, leftOfTwo.y));
        assertEquals(List.of(800f, -1500f), List.of(ally.x, ally.y));
    }

    @Test
    void combatStepsFailOutsideCombat() throws Throwable {
        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            global.when(Global::getCurrentState).thenReturn(GameState.CAMPAIGN);
            StepContext context = HarnessStepsTest.context(new HarnessLog(), 0, 0f);

            for (HarnessStep step : List.of(new SpawnStep("hound_Standard", SpawnStep.side("enemy"), 1000f, 1), new AutopilotStep(true),
                    new SpeedStep(2f), new RecordStatsStep(CombatShips.PLAYER, "stats"), new RecordDamageStep("damage", 5f, false, 0f),
                    new EndSimulationStep())) {
                StepOutcome outcome = step.advance(context);
                assertTrue(outcome.finished() && !outcome.passed(), step.name() + ": " + outcome.detail());
            }
        }
    }

    @Test
    void campaignSetupStepsFailOutsideTheCampaign() throws Throwable {
        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            global.when(Global::getCurrentState).thenReturn(GameState.COMBAT);
            StepContext context = HarnessStepsTest.context(new HarnessLog(), 0, 0f);

            for (HarnessStep step : List.of(new AddShipStep("eagle_Assault", 1), new SaveGameStep())) {
                StepOutcome outcome = step.advance(context);
                assertTrue(outcome.finished() && !outcome.passed(), step.name() + ": " + outcome.detail());
            }
        }
    }
}
