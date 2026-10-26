package exiledsector.effects;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.StatBonus;
import exiledlib.skills.CombatGate;
import exiledlib.skills.skilleffect.defense.DefenseSkillEffect;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShipCombatPlanTest {

    @Test
    void aGatedNodeAppliesOnlyWhileItsGateHoldsAndOnlyTouchesTheStatWhenThatChanges() {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        StatBonus hull = mock(StatBonus.class);
        when(ship.getMutableStats()).thenReturn(stats);
        when(stats.getHullBonus()).thenReturn(hull);
        ShipCombatPlan plan = new ShipCombatPlan();
        plan.addGatedNode(CombatGate.LOW_HULL, List.of(new ResolvedTree.EffectEntry(DefenseSkillEffect.HULL_PERCENT, "gated_hull", 30f)));

        when(ship.getHullLevel()).thenReturn(0.9f);
        plan.advance(ship, 0.1f);
        verify(hull, never()).modifyPercent(anyString(), anyFloat());

        when(ship.getHullLevel()).thenReturn(0.4f);
        plan.advance(ship, 0.1f);
        plan.advance(ship, 0.1f);
        verify(hull, times(1)).modifyPercent("gated_hull", 30f);

        when(ship.getHullLevel()).thenReturn(0.6f);
        plan.advance(ship, 0.1f);
        verify(hull, times(1)).modifyPercent("gated_hull", 0f);
    }

    @Test
    void gatedDamageTakenReductionsAreScaledBackToStayWithinTheCap() {
        ResolvedTree.GatedNode gated = new ResolvedTree.GatedNode(CombatGate.LOW_HULL,
                List.of(new ResolvedTree.EffectEntry(DefenseSkillEffect.KINETIC_DAMAGE_TAKEN_PERCENT, "gated_kinetic", -30f),
                        new ResolvedTree.EffectEntry(DefenseSkillEffect.HULL_PERCENT, "gated_hull", 30f)));

        List<ResolvedTree.GatedNode> limited = ResolvedTree.withinDamageTakenCaps(List.of(gated),
                Map.of(DefenseSkillEffect.KINETIC_DAMAGE_TAKEN_PERCENT, -70f));

        assertEquals(-10f, limited.get(0).effects().get(0).magnitude(), 0.01f);
        assertEquals(30f, limited.get(0).effects().get(1).magnitude(), 1e-4f);
        assertEquals(List.of(gated), ResolvedTree.withinDamageTakenCaps(List.of(gated), Map.of()));
    }
}
