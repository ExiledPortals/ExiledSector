package exiledsector.skills.skilleffect.defense;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BattleObjectiveAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.StatBonus;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class DefensiveCompactTest {

    private static final float EPSILON = 1e-4f;
    private static final int PLAYER_SIDE = 0;
    private static final int ENEMY_SIDE = 1;

    private static BattleObjectiveAPI objective(int owner, float x) {
        BattleObjectiveAPI objective = mock(BattleObjectiveAPI.class);
        when(objective.getOwner()).thenReturn(owner);
        when(objective.getLocation()).thenReturn(new Vector2f(x, 0f));
        return objective;
    }

    private static CombatEngineAPI engine(BattleObjectiveAPI... objectives) {
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        when(engine.getObjectives()).thenReturn(List.of(objectives));
        return engine;
    }

    private static ShipAPI ship(MutableStat hullDamageTaken, MutableStat armorDamageTaken, MutableStat shieldDamageTaken) {
        ShipAPI ship = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getOwner()).thenReturn(PLAYER_SIDE);
        when(ship.getLocation()).thenReturn(new Vector2f(0f, 0f));
        when(ship.getMutableStats().getHullDamageTakenMult()).thenReturn(hullDamageTaken);
        when(ship.getMutableStats().getArmorDamageTakenMult()).thenReturn(armorDamageTaken);
        when(ship.getMutableStats().getShieldDamageTakenMult()).thenReturn(shieldDamageTaken);
        return ship;
    }

    private static ShipAPI ship() {
        return ship(new MutableStat(1f), new MutableStat(1f), new MutableStat(1f));
    }

    @Test
    void onlyAnObjectiveHeldByTheShipsOwnSideWithinRangeCounts() {
        ShipAPI ship = ship();

        assertTrue(HeldObjective.isNearby(ship, engine(objective(PLAYER_SIDE, HeldObjective.RANGE))));
        assertFalse(HeldObjective.isNearby(ship, engine(objective(PLAYER_SIDE, HeldObjective.RANGE + 1f))));
        assertFalse(HeldObjective.isNearby(ship, engine(objective(ENEMY_SIDE, 100f))));
        assertTrue(HeldObjective.isNearby(ship, engine(objective(ENEMY_SIDE, 100f), objective(PLAYER_SIDE, 900f))));
        assertFalse(HeldObjective.isNearby(ship, engine()));
        assertFalse(HeldObjective.isNearby(ship, null));
    }

    @Test
    void theRangeBonusEffectExtendsTheRangeOfAHeldObjective() {
        ShipAPI ship = ship();
        StatBonus rangeBonus = new StatBonus();
        when(ship.getMutableStats().getDynamic().getMod(HeldObjective.RANGE_BONUS_KEY)).thenReturn(rangeBonus);
        when(ship.getMutableStats().getDynamic().getValue(HeldObjective.RANGE_BONUS_KEY, 0f))
                .thenAnswer(invocation -> rangeBonus.computeEffective(0f));
        CombatEngineAPI farObjective = engine(objective(PLAYER_SIDE, 2500f));

        assertFalse(HeldObjective.isNearby(ship, farObjective));

        DefenseSkillEffect.DAMAGE_TAKEN_NEAR_HELD_OBJECTIVE_RANGE_FLAT.apply(ship.getMutableStats(), "mod_id", 1000f);

        assertEquals(2500f, HeldObjective.range(ship), EPSILON);
        assertTrue(HeldObjective.isNearby(ship, farObjective));
        assertFalse(HeldObjective.isNearby(ship, engine(objective(PLAYER_SIDE, 2501f))));
    }

    @Test
    void hullArmorAndShieldDamageTakenAreReducedOnlyNearAHeldObjective() {
        MutableStat hull = new MutableStat(1f);
        MutableStat armor = new MutableStat(1f);
        MutableStat shield = new MutableStat(1f);
        ShipAPI ship = ship(hull, armor, shield);
        DefenseSkillEffect effect = DefenseSkillEffect.DAMAGE_TAKEN_NEAR_HELD_OBJECTIVE_PERCENT;

        assertTrue(effect.advancesInCombat());
        assertTrue(effect.lowerIsBetter());
        assertFalse(effect.invertsDescriptionColours());
        try (MockedStatic<Global> global = mockStatic(Global.class)) {
            CombatEngineAPI held = engine(objective(PLAYER_SIDE, 500f));
            global.when(Global::getCombatEngine).thenReturn(held);
            effect.advanceInCombat(ship, "mod_id", -20f);
            assertEquals(0.8f, hull.getModifiedValue(), EPSILON);
            assertEquals(0.8f, armor.getModifiedValue(), EPSILON);
            assertEquals(0.8f, shield.getModifiedValue(), EPSILON);

            CombatEngineAPI lost = engine(objective(ENEMY_SIDE, 500f));
            global.when(Global::getCombatEngine).thenReturn(lost);
            effect.advanceInCombat(ship, "mod_id", -20f);
            assertEquals(1f, hull.getModifiedValue(), EPSILON);
            assertEquals(1f, armor.getModifiedValue(), EPSILON);
            assertEquals(1f, shield.getModifiedValue(), EPSILON);
        }
    }
}
