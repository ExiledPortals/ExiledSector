package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FighterBaysMultTest {

    private static MutableShipStatsAPI carrier(float bays, int builtInWings, MutableStat bayStat) {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        ShipHullSpecAPI hull = mock(ShipHullSpecAPI.class);
        when(stats.getVariant()).thenReturn(variant);
        when(variant.getHullSpec()).thenReturn(hull);
        when(hull.getBuiltInWings()).thenReturn(Collections.nCopies(builtInWings, "wing"));
        when(stats.getNumFighterBays()).thenReturn(bayStat);
        bayStat.setBaseValue(bays);
        return stats;
    }

    @Test
    void halvesTheBaysWithoutTouchingBuiltInWings() {
        MutableStat bays = new MutableStat(0f);
        FighterSkillEffect.FIGHTER_BAYS_MULT.apply(carrier(4f, 0, bays), "mod_id", -50f);
        assertEquals(2f, bays.getModifiedValue(), 0.001f);

        MutableStat withBuiltIns = new MutableStat(0f);
        FighterSkillEffect.FIGHTER_BAYS_MULT.apply(carrier(4f, 2, withBuiltIns), "mod_id", -50f);
        assertEquals(3f, withBuiltIns.getModifiedValue(), 0.001f);
    }

    @Test
    void aZeroMagnitudeLeavesTheBaysAlone() {
        MutableStat bays = new MutableStat(0f);
        FighterSkillEffect.FIGHTER_BAYS_MULT.apply(carrier(4f, 2, bays), "mod_id", 0f);
        assertEquals(4f, bays.getModifiedValue(), 0.001f);
    }

    @Test
    void describesTheBayLossAsLess() {
        assertEquals("50% less strikecraft bays, not counting bays with built-in wings.",
                FighterSkillEffect.FIGHTER_BAYS_MULT.description(-50f).plain());
    }
}
