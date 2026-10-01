package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LowBaseArmorBonusTest {

    private static final DefenseSkillEffect EFFECT = DefenseSkillEffect.ARMOR_FLAT_FOR_LOW_BASE_ARMOR;

    private static ShipHullSpecAPI hull(HullSize hullSize, float baseArmor, boolean phase) {
        ShipHullSpecAPI hull = mock(ShipHullSpecAPI.class);
        when(hull.getHullSize()).thenReturn(hullSize);
        when(hull.getArmorRating()).thenReturn(baseArmor);
        when(hull.isPhase()).thenReturn(phase);
        return hull;
    }

    private static String blockReason(ShipHullSpecAPI hull) {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getHullSpec()).thenReturn(hull);
        return EFFECT.blockAllocationReason(member, 1f, List.of());
    }

    @Test
    void theBonusShrinksInAStraightLineFromNoBaseArmorToTheCutoff() {
        assertEquals(400f, LowBaseArmorBonus.bonus(HullSize.FRIGATE, 0f), 1e-3f);
        assertEquals(275f, LowBaseArmorBonus.bonus(HullSize.FRIGATE, 200f), 1e-3f);
        assertEquals(150f, LowBaseArmorBonus.bonus(HullSize.FRIGATE, 400f), 1e-3f);
        assertEquals(650f, LowBaseArmorBonus.bonus(HullSize.CRUISER, 450f), 1e-3f);
        assertEquals(500f, LowBaseArmorBonus.bonus(HullSize.CAPITAL_SHIP, 1200f), 1e-3f);
    }

    @Test
    void hullsAboveTheCutoffOrWithoutAShipHullSizeGetNothing() {
        assertEquals(0f, LowBaseArmorBonus.bonus(HullSize.DESTROYER, 701f));
        assertEquals(0f, LowBaseArmorBonus.bonus(HullSize.FIGHTER, 0f));
        assertEquals(0f, LowBaseArmorBonus.bonus(null, 0f));
    }

    @Test
    void onlyNonPhaseHullsAtOrBelowTheCutoffCanAllocateIt() {
        assertNull(blockReason(hull(HullSize.DESTROYER, 700f, false)));
        assertNotNull(blockReason(hull(HullSize.DESTROYER, 701f, false)));
        assertNotNull(blockReason(hull(HullSize.FRIGATE, 100f, true)));
    }

    private static float appliedArmor(ShipHullSpecAPI hull, float magnitude) {
        HullSize hullSize = hull.getHullSize();
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.getHullSize()).thenReturn(hullSize);
        when(variant.getHullSpec()).thenReturn(hull);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        StatBonus armor = new StatBonus();
        when(stats.getVariant()).thenReturn(variant);
        when(stats.getArmorBonus()).thenReturn(armor);
        EFFECT.apply(stats, "citadel", magnitude);
        return armor.getFlatBonus();
    }

    @Test
    void theEffectAddsTheScaledBonusAsFlatArmor() {
        assertEquals(325f, appliedArmor(hull(HullSize.CRUISER, 450f, false), 0.5f), 1e-3f);
    }

    @Test
    void aPhaseHullGetsNoArmorEvenIfTheNodeWasAllocatedSomehow() {
        assertEquals(0f, appliedArmor(hull(HullSize.FRIGATE, 100f, true), 1f));
    }

    @Test
    void theDescriptionListsTheScaledBonusesAndTheCutoffsByHullSize() {
        assertEquals("Increases armor by up to 200/350/450/600. Hulls with less base armor gain more: the bonus falls to "
                        + "75/150/200/250 at 400/700/900/1200 base armor (frigate/destroyer/cruiser/capital ship). "
                        + "Can't be allocated on phase ships or on hulls with more base armor than that.",
                EFFECT.description(0.5f).plain());
    }
}
