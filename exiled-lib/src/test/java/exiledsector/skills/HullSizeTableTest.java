package exiledsector.skills;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.i18n.Message;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class HullSizeTableTest {

    private static final HullSizeTable TABLE = new HullSizeTable(1f, 2f, 3f, 4f, 5f, 9f);

    @Test
    void eachHullSizeReadsItsOwnColumnAndAnythingElseTheFallback() {
        assertEquals(1f, TABLE.of(HullSize.FIGHTER));
        assertEquals(2f, TABLE.of(HullSize.FRIGATE));
        assertEquals(3f, TABLE.of(HullSize.DESTROYER));
        assertEquals(4f, TABLE.of(HullSize.CRUISER));
        assertEquals(5f, TABLE.of(HullSize.CAPITAL_SHIP));
        assertEquals(9f, TABLE.of(HullSize.DEFAULT));
        assertEquals(9f, TABLE.of(null));
    }

    @Test
    void descriptionsGetTheFourShipSizesButNotFighters() {
        Message message = mock(Message.class, RETURNS_SELF);

        TABLE.withShipSizeArgs(message);

        verify(message).arg("frigate", 2f);
        verify(message).arg("destroyer", 3f);
        verify(message).arg("cruiser", 4f);
        verify(message).arg("capital", 5f);
        verifyNoMoreInteractions(message);
    }
}
