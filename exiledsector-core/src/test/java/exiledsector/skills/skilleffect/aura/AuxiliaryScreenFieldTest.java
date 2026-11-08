package exiledsector.skills.skilleffect.aura;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuxiliaryScreenFieldTest {

    private static final float EPSILON = 1e-4f;

    private static AuxiliaryScreenListener source(HullSize hullSize, float reductionPercent) {
        return source(hullSize, reductionPercent, 0f);
    }

    private static AuxiliaryScreenListener source(HullSize hullSize, float reductionPercent, float rangeBonus) {
        ShipAPI ship = shipAt(new Vector2f(), 0, hullSize);
        DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class);
        when(ship.getMutableStats().getDynamic()).thenReturn(dynamic);
        when(dynamic.getValue(AuxiliaryScreenListener.DAMAGE_TAKEN_PERCENT_KEY, 0f)).thenReturn(reductionPercent);
        when(dynamic.getValue(AuxiliaryScreenListener.RANGE_BONUS_KEY, 0f)).thenReturn(rangeBonus);
        return new AuxiliaryScreenListener(ship);
    }

    private static ShipAPI shipAt(Vector2f location, int owner, HullSize hullSize) {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        when(stats.getHullDamageTakenMult()).thenReturn(new MutableStat(1f));
        when(stats.getArmorDamageTakenMult()).thenReturn(new MutableStat(1f));
        when(stats.getShieldDamageTakenMult()).thenReturn(new MutableStat(1f));
        when(ship.getMutableStats()).thenReturn(stats);
        when(ship.isAlive()).thenReturn(true);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.getLocation()).thenReturn(location);
        when(ship.getHullSize()).thenReturn(hullSize);
        return ship;
    }

    private static AuraField.ShipsNear among(List<ShipAPI> ships) {
        return (sourceShip, radius, filter) -> ships.stream().filter(filter).toList();
    }

    @Test
    void smallerAlliesInRangeGetTheStrongestReductionOnly() {
        ShipAPI frigate = shipAt(new Vector2f(300f, 0f), 0, HullSize.FRIGATE);
        ShipAPI destroyer = shipAt(new Vector2f(300f, 0f), 0, HullSize.DESTROYER);
        ShipAPI farFrigate = shipAt(new Vector2f(AuxiliaryScreenField.RANGE + 100f, 0f), 0, HullSize.FRIGATE);
        ShipAPI cruiserAlly = shipAt(new Vector2f(300f, 0f), 0, HullSize.CRUISER);
        ShipAPI enemyFrigate = shipAt(new Vector2f(300f, 0f), 1, HullSize.FRIGATE);
        ShipAPI fighter = shipAt(new Vector2f(300f, 0f), 0, HullSize.FIGHTER);
        when(fighter.isFighter()).thenReturn(true);

        Map<ShipAPI, Float> reductions = AuxiliaryScreenField.reductionPerAlly(
                List.of(source(HullSize.CRUISER, 10f), source(HullSize.CAPITAL_SHIP, 15f)),
                among(List.of(frigate, destroyer, farFrigate, cruiserAlly, enemyFrigate, fighter)));

        assertEquals(15f, reductions.get(frigate), EPSILON);
        assertEquals(15f, reductions.get(destroyer), EPSILON);
        assertFalse(reductions.containsKey(farFrigate));
        assertEquals(15f, reductions.get(cruiserAlly), EPSILON);
        assertFalse(reductions.containsKey(enemyFrigate));
        assertFalse(reductions.containsKey(fighter));
    }

    @Test
    void aRangeBonusReachesAlliesOnlyForThatSource() {
        ShipAPI farFrigate = shipAt(new Vector2f(AuxiliaryScreenField.RANGE + 100f, 0f), 0, HullSize.FRIGATE);

        Map<ShipAPI, Float> reductions = AuxiliaryScreenField.reductionPerAlly(
                List.of(source(HullSize.CRUISER, 10f, 400f), source(HullSize.CAPITAL_SHIP, 15f)), among(List.of(farFrigate)));

        assertEquals(10f, reductions.get(farFrigate), EPSILON);
    }

    @Test
    void anAllyOfTheSameSizeAsEverySourceIsNotScreened() {
        ShipAPI cruiser = shipAt(new Vector2f(300f, 0f), 0, HullSize.CRUISER);

        assertTrue(AuxiliaryScreenField.reductionPerAlly(List.of(source(HullSize.CRUISER, 10f)), among(List.of(cruiser))).isEmpty());
    }

    @Test
    void theFieldLowersDamageTakenAndRestoresItWhenTheAllyLeaves() {
        Vector2f allyLocation = new Vector2f(300f, 0f);
        ShipAPI frigate = shipAt(allyLocation, 0, HullSize.FRIGATE);
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        CollisionGridAPI shipGrid = mock(CollisionGridAPI.class);
        when(engine.getShipGrid()).thenReturn(shipGrid);
        when(shipGrid.getCheckIterator(any(), anyFloat(), anyFloat())).thenAnswer(invocation -> new ArrayList<Object>(List.of(frigate)).iterator());

        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            global.when(Global::getCombatEngine).thenReturn(engine);
            AuxiliaryScreenField field = new AuxiliaryScreenField();
            field.sources.add(source(HullSize.CRUISER, 10f));

            field.update(0.25f);
            MutableShipStatsAPI stats = frigate.getMutableStats();
            assertEquals(0.9f, stats.getHullDamageTakenMult().getModifiedValue(), EPSILON);
            assertEquals(0.9f, stats.getArmorDamageTakenMult().getModifiedValue(), EPSILON);
            assertEquals(0.9f, stats.getShieldDamageTakenMult().getModifiedValue(), EPSILON);

            allyLocation.set(AuxiliaryScreenField.RANGE + 100f, 0f);
            field.update(0.25f);
            assertEquals(1f, stats.getHullDamageTakenMult().getModifiedValue(), EPSILON);
            assertEquals(1f, stats.getShieldDamageTakenMult().getModifiedValue(), EPSILON);
        }
    }
}
