package exiledlib.skills.skilleffect.fighter;

import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FighterFocusFireMarksTest {

    private static ShipAPI ship(String id, int owner, Map<String, Float> magnitudes) {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getId()).thenReturn(id);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.getMutableStats()).thenReturn(stats);
        magnitudes.forEach((key, value) -> when(stats.getDynamic().getValue(key, 0f)).thenReturn(value));
        when(ship.isAlive()).thenReturn(true);
        when(ship.getLocation()).thenReturn(new Vector2f());
        return ship;
    }

    @Test
    void hardlightMarksAreSharedByEveryFighterOfOneCarrierAndCapped() {
        ShipAPI carrier = ship("carrier", 0, Map.of(FighterFocusFireMarks.PERCENT_PER_HIT_KEY, 2f, FighterFocusFireMarks.MAX_PERCENT_KEY, 30f));
        ShipAPI enemy = ship("enemy", 1, Map.of());
        FighterFocusFireMarks marks = new FighterFocusFireMarks(carrier);
        FighterFocusFireMarks.FighterHits firstFighter = new FighterFocusFireMarks.FighterHits(marks);
        FighterFocusFireMarks.FighterHits secondFighter = new FighterFocusFireMarks.FighterHits(marks);
        DamageAPI damage = mock(DamageAPI.class);
        MutableStat modifier = mock(MutableStat.class);
        when(damage.getModifier()).thenReturn(modifier);

        assertNull(firstFighter.modifyDamageDealt(null, enemy, damage, new Vector2f(), false));
        assertEquals("exiledSector_fighterFocusFire_carrier", secondFighter.modifyDamageDealt(null, enemy, damage, new Vector2f(), false));
        verify(modifier).modifyPercent("exiledSector_fighterFocusFire_carrier", 2f);

        for (int hit = 0; hit < 20; hit++) {
            firstFighter.modifyDamageDealt(null, enemy, damage, new Vector2f(), false);
        }
        assertEquals(30f, marks.markPercent(enemy), 0.0001f);
        assertEquals(0f, marks.markPercent(ship("other", 1, Map.of())), 0.0001f);
    }

    @Test
    void hardlightCountsASecondOfBeamContactAsOneHit() {
        ShipAPI carrier = ship("carrier", 0, Map.of(FighterFocusFireMarks.PERCENT_PER_HIT_KEY, 2f, FighterFocusFireMarks.MAX_PERCENT_KEY, 30f));
        ShipAPI enemy = ship("enemy", 1, Map.of());
        FighterFocusFireMarks marks = new FighterFocusFireMarks(carrier);
        FighterFocusFireMarks.FighterHits beamFighter = new FighterFocusFireMarks.FighterHits(marks);
        DamageAPI beamTick = mock(DamageAPI.class);
        when(beamTick.isDps()).thenReturn(true);
        when(beamTick.getDpsDuration()).thenReturn(0.1f);
        when(beamTick.getModifier()).thenReturn(mock(MutableStat.class));

        for (int tick = 0; tick < 10; tick++) {
            beamFighter.modifyDamageDealt(null, enemy, beamTick, new Vector2f(), false);
        }

        assertEquals(2f, marks.markPercent(enemy), 0.001f);
    }

    @Test
    void hardlightMarksIgnoreFriendlyShips() {
        ShipAPI carrier = ship("carrier", 0, Map.of(FighterFocusFireMarks.PERCENT_PER_HIT_KEY, 2f, FighterFocusFireMarks.MAX_PERCENT_KEY, 30f));
        FighterFocusFireMarks marks = new FighterFocusFireMarks(carrier);
        ShipAPI ally = ship("ally", 0, Map.of());

        new FighterFocusFireMarks.FighterHits(marks).modifyDamageDealt(null, ally, mock(DamageAPI.class), new Vector2f(), false);

        assertEquals(0f, marks.markPercent(ally), 0.0001f);
    }
}
