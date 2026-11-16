package exiledsector.loot;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class DropOriginTest {

    @Test
    void aSiteDropNamesItsSiteAndIsShapedAsFoundAnywhereWhereThePlayerIs() {
        SectorEntityToken derelict = mock(SectorEntityToken.class);

        DropOrigin origin = DropOrigin.site(derelict);

        assertEquals(DropOrigin.Kind.SITE, origin.kind());
        assertSame(derelict, origin.site());
        assertSame(DropOrigin.ANYWHERE, origin.whereThePlayerIs());
    }

    @Test
    void aDropWithoutASiteCountsAsFoundAnywhereAndBattleDropsStayBattleDropsWhereThePlayerIs() {
        assertSame(DropOrigin.ANYWHERE, DropOrigin.site(null));
        assertSame(DropOrigin.BATTLE, DropOrigin.BATTLE.whereThePlayerIs());
        assertSame(DropOrigin.ANYWHERE, DropOrigin.ANYWHERE.whereThePlayerIs());
    }

    @Test
    void onlyASiteDropCanNameASite() {
        SectorEntityToken derelict = mock(SectorEntityToken.class);

        assertThrows(IllegalArgumentException.class, () -> new DropOrigin(DropOrigin.Kind.BATTLE, derelict));
        assertThrows(IllegalArgumentException.class, () -> new DropOrigin(DropOrigin.Kind.SITE, null));
    }
}
