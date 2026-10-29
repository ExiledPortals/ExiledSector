package exiledsector.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ShipCardNameTest {

    private static final float CHAR_WIDTH = 10f;

    private static double width(String text) {
        return text.codePointCount(0, text.length()) * CHAR_WIDTH;
    }

    @Test
    void aNameThatFitsIsShownWhole() {
        assertEquals("ISS Hound", ShipCardName.fit("ISS Hound", 90f, ShipCardNameTest::width));
    }

    @Test
    void aLongNameIsCutWithAnEllipsisInsideTheWidth() {
        String shown = ShipCardName.fit("ISS Unreasonably Long Name", 100f, ShipCardNameTest::width);

        assertEquals("ISS Unr...", shown);
        assertTrue(width(shown) <= 100f);
    }

    @Test
    void theCutDropsSpacesBeforeTheEllipsis() {
        assertEquals("ISS...", ShipCardName.fit("ISS  Hound", 70f, ShipCardNameTest::width));
    }

    @Test
    void theCutNeverSplitsASurrogatePair() {
        String name = "AB🚀CDEF";

        assertEquals("AB🚀...", ShipCardName.fit(name, 60f, ShipCardNameTest::width));
    }

    @Test
    void aWidthTooNarrowForAnyLetterLeavesOnlyTheEllipsis() {
        assertEquals(ShipCardName.ELLIPSIS, ShipCardName.fit("ISS Hound", 5f, ShipCardNameTest::width));
    }

    @Test
    void theNameIsCentredAlongTheTopOfTheCardInTheVanillaFleetListFont() {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getShipName()).thenReturn("ISS Hound");
        TooltipMakerAPI card = mock(TooltipMakerAPI.class);
        LabelAPI label = mock(LabelAPI.class);
        PositionAPI labelPosition = mock(PositionAPI.class);
        when(card.addPara(anyString(), anyFloat(), any(Color.class), any(Color.class), any(String[].class))).thenReturn(label);
        when(label.autoSizeToWidth(anyFloat())).thenReturn(labelPosition);
        SettingsAPI settings = mock(SettingsAPI.class);
        when(settings.computeStringWidth(anyString(), eq(ShipCardName.FONT))).thenReturn(40f);

        try (MockedStatic<Global> globalMock = Mockito.mockStatic(Global.class)) {
            globalMock.when(Global::getSettings).thenReturn(settings);
            ShipCardName.addTo(card, member, 128f);
        }

        InOrder order = inOrder(card);
        order.verify(card).setParaFont(ShipCardName.FONT);
        order.verify(card).addPara("%s", 0f, ShipCardName.COLOR, ShipCardName.COLOR, "ISS Hound");
        order.verify(card).setParaFontDefault();
        verify(label).setAlignment(Alignment.MID);
        verify(label).autoSizeToWidth(128f);
        verify(labelPosition).inTL(0f, ShipCardName.TOP_INSET);
    }

    @Test
    void aShipWithoutANameAddsNoLabel() {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getShipName()).thenReturn("  ");
        TooltipMakerAPI card = mock(TooltipMakerAPI.class);

        ShipCardName.addTo(card, member, 128f);

        verifyNoInteractions(card);
    }
}
