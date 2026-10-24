package exiledlib.skills.skilleffect.fighter;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WingMemberCacheTest {

    @Test
    void theWingIsCopiedOncePerRefreshWindowAndAgainWhenTheFighterChangesWing() {
        ShipAPI fighter = mock(ShipAPI.class);
        FighterWingAPI wing = mock(FighterWingAPI.class);
        FighterWingAPI otherWing = mock(FighterWingAPI.class);
        when(wing.getWingMembers()).thenReturn(List.of(fighter));
        when(otherWing.getWingMembers()).thenReturn(List.of());
        when(fighter.getWing()).thenReturn(wing);
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        WingMemberCache cache = new WingMemberCache();

        when(engine.getTotalElapsedTime(false)).thenReturn(1f);
        cache.membersOf(fighter, engine);
        when(engine.getTotalElapsedTime(false)).thenReturn(1f + WingMemberCache.REFRESH_SECONDS / 2f);
        assertEquals(List.of(fighter), cache.membersOf(fighter, engine));
        verify(wing, times(1)).getWingMembers();

        when(engine.getTotalElapsedTime(false)).thenReturn(1f + WingMemberCache.REFRESH_SECONDS);
        cache.membersOf(fighter, engine);
        verify(wing, times(2)).getWingMembers();

        when(fighter.getWing()).thenReturn(otherWing);
        assertEquals(List.of(), cache.membersOf(fighter, engine));
    }
}
