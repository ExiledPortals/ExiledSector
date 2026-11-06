package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.AdditionalMatchers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HullLossTest {

    private static MutableStat stat(float value) {
        MutableStat stat = mock(MutableStat.class);
        when(stat.getModifiedValue()).thenReturn(value);
        return stat;
    }

    private static ShipAPI damagedShip(float hitpoints) {
        ShipAPI ship = mock(ShipAPI.class);
        float[] hull = {hitpoints};
        when(ship.getHitpoints()).thenAnswer(invocation -> hull[0]);
        doAnswer(invocation -> {
            hull[0] = invocation.getArgument(0);
            return null;
        }).when(ship).setHitpoints(anyFloat());
        when(ship.getLocation()).thenReturn(new Vector2f());
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat unchanged = stat(1f);
        when(stats.getHullDamageTakenMult()).thenReturn(unchanged);
        when(stats.getEnergyDamageTakenMult()).thenReturn(unchanged);
        when(stats.getProjectileDamageTakenMult()).thenReturn(unchanged);
        when(stats.getArmorDamageTakenMult()).thenReturn(unchanged);
        when(ship.getMutableStats()).thenReturn(stats);
        return ship;
    }

    @Test
    void hullLossTheShipSurvivesComesStraightOffItsHitpoints() {
        ShipAPI ship = damagedShip(100f);
        CombatEngineAPI engine = mock(CombatEngineAPI.class);

        HullLoss.take(ship, 30f, null, engine);

        assertEquals(70f, ship.getHitpoints(), 0.01f);
        verify(engine, never()).applyDamage(any(CombatEntityAPI.class), any(Vector2f.class), anyFloat(), any(DamageType.class), anyFloat(), anyBoolean(), anyBoolean(), any());
    }

    @Test
    void lethalHullLossLeavesOneHitpointAndLandsAKillingBlowFromTheAttacker() {
        ShipAPI ship = damagedShip(20f);
        ShipAPI attacker = mock(ShipAPI.class);
        CombatEngineAPI engine = mock(CombatEngineAPI.class);

        HullLoss.take(ship, 30f, attacker, engine);

        assertEquals(HullLoss.KILLING_BLOW_HULL_LEFT, ship.getHitpoints(), 0.01f);
        verify(engine).applyDamage(same(ship), any(Vector2f.class), AdditionalMatchers.eq(HullLoss.KILLING_BLOW_MULTIPLE * HullLoss.KILLING_BLOW_HULL_LEFT, 0.01f),
                eq(DamageType.ENERGY), eq(0f), eq(true), eq(false), same(attacker));
    }

    @Test
    void theKillingBlowGrowsToBeatTheShipsDamageResistance() {
        ShipAPI ship = damagedShip(40f);
        MutableStat halved = stat(0.5f);
        when(ship.getMutableStats().getHullDamageTakenMult()).thenReturn(halved);
        when(ship.getMutableStats().getEnergyDamageTakenMult()).thenReturn(halved);

        assertEquals(3f * 40f / 0.25f, HullLoss.killingBlowDamage(ship), 0.01f);
    }

    @Test
    void theKillingBlowGrowsToCutThroughArmourThatTakesLessDamage() {
        ShipAPI ship = damagedShip(10f);
        ArmorGridAPI armor = mock(ArmorGridAPI.class);
        when(armor.getArmorRating()).thenReturn(100f);
        when(ship.getArmorGrid()).thenReturn(armor);
        MutableStat halved = stat(0.5f);
        when(ship.getMutableStats().getArmorDamageTakenMult()).thenReturn(halved);

        assertEquals(3f * (10f + 100f / 0.5f), HullLoss.killingBlowDamage(ship), 0.01f);
    }

    @Test
    void aStatThatMakesTheShipImmuneDoesNotTurnTheKillingBlowIntoNothing() {
        ShipAPI ship = damagedShip(10f);
        MutableStat immune = stat(0f);
        when(ship.getMutableStats().getEnergyDamageTakenMult()).thenReturn(immune);

        assertEquals(30f, HullLoss.killingBlowDamage(ship), 0.01f);
    }
}
