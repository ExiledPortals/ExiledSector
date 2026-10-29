package exiledsector.ui;

import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ShipCardTooltipTest {

    private static final ScreenRect HOST = new ScreenRect(100f, 50f, 1600f, 900f);
    private static final String SHIP_NAME = "ISS Resolute";
    private static final float NAME_HEIGHT = 16f;
    private static final float CONTENTS_WIDTH = 460f;
    private static final float CONTENTS_HEIGHT = 300f;

    private VanillaPanel contents;
    private VanillaPosition titlePosition;
    private PositionAPI contentsPosition;

    @Test
    void theTooltipOpensRightOfTheCardWithMatchingTops() {
        ScreenRect card = new ScreenRect(120f, 600f, 128f, 128f);

        ShipCardTooltip.Placement placement = ShipCardTooltip.place(card, 450f, 300f, HOST);

        assertEquals(120f + 128f + ShipCardTooltip.GAP - HOST.left(), placement.left(), 0.001f);
        assertEquals(600f + 128f - 300f - HOST.bottom(), placement.bottom(), 0.001f);
    }

    @Test
    void aCardNearTheBottomLiftsTheTooltipOntoTheScreen() {
        ScreenRect card = new ScreenRect(120f, 60f, 128f, 128f);

        ShipCardTooltip.Placement placement = ShipCardTooltip.place(card, 450f, 500f, HOST);

        assertEquals(0f, placement.bottom(), 0.001f);
    }

    @Test
    void aCardNearTheRightEdgeOpensTheTooltipOnItsLeft() {
        ScreenRect card = new ScreenRect(1500f, 400f, 128f, 128f);

        ShipCardTooltip.Placement placement = ShipCardTooltip.place(card, 450f, 300f, HOST);

        assertEquals(1500f - ShipCardTooltip.GAP - 450f - HOST.left(), placement.left(), 0.001f);
    }

    @Test
    void aTooltipTallerThanTheScreenStartsAtTheScreenBottom() {
        ScreenRect card = new ScreenRect(120f, 600f, 128f, 128f);

        ShipCardTooltip.Placement placement = ShipCardTooltip.place(card, 450f, 1200f, HOST);

        assertEquals(0f, placement.bottom(), 0.001f);
    }

    @Test
    void theEmptyTooltipNeverShows() {
        ShipCardTooltip.NONE.show(null);

        assertFalse(ShipCardTooltip.NONE.isShown());
    }

    @Test
    void theShipNameGoesAboveTheVanillaTitleAndGrowsTheTooltip() throws Exception {
        VanillaTooltip tooltip = vanillaTooltip(-2f);

        ShipCardTooltip.addShipName(tooltip, SHIP_NAME);

        float nameLineHeight = NAME_HEIGHT + ShipCardTooltip.SHIP_NAME_GAP;
        InOrder order = inOrder(tooltip, titlePosition, contentsPosition);
        order.verify(tooltip).addTitle(SHIP_NAME);
        order.verify(titlePosition).setYAlignOffset(-2f - nameLineHeight);
        order.verify(contentsPosition).setSize(CONTENTS_WIDTH, CONTENTS_HEIGHT + nameLineHeight);
        order.verify(tooltip).updateSize();
    }

    @Test
    void aShipWithoutANameLeavesTheTooltipAlone() throws Exception {
        VanillaTooltip tooltip = vanillaTooltip(0f);

        ShipCardTooltip.addShipName(tooltip, " ");
        ShipCardTooltip.addShipName(tooltip, null);

        verifyNoInteractions(tooltip);
    }

    @Test
    void aTooltipWithNothingInItGetsNoName() throws Exception {
        VanillaTooltip tooltip = vanillaTooltip(0f);
        when(contents.getChildrenNonCopy()).thenReturn(List.of());

        ShipCardTooltip.addShipName(tooltip, SHIP_NAME);

        verify(tooltip, never()).addTitle(anyString());
        verify(tooltip, never()).updateSize();
    }

    @Test
    void aTooltipThatIsNotATooltipMakerGetsNoName() {
        UIComponentAPI tooltip = mock(UIComponentAPI.class);

        assertDoesNotThrow(() -> ShipCardTooltip.addShipName(tooltip, SHIP_NAME));
        verifyNoInteractions(tooltip);
    }

    @Test
    void aFailingTooltipStillShowsWithoutTheName() throws Exception {
        VanillaTooltip tooltip = vanillaTooltip(0f);
        when(tooltip.getContents()).thenThrow(new IllegalStateException("changed game build"));

        assertDoesNotThrow(() -> ShipCardTooltip.addShipName(tooltip, SHIP_NAME));
        verify(tooltip, never()).addTitle(anyString());
    }

    private VanillaTooltip vanillaTooltip(float titleOffset) {
        VanillaTooltip tooltip = mock(VanillaTooltip.class);
        contents = mock(VanillaPanel.class);
        contentsPosition = mock(PositionAPI.class);
        when(contentsPosition.getWidth()).thenReturn(CONTENTS_WIDTH);
        when(contentsPosition.getHeight()).thenReturn(CONTENTS_HEIGHT);
        when(contents.getPosition()).thenReturn(contentsPosition);
        UIComponentAPI title = mock(UIComponentAPI.class);
        titlePosition = mock(VanillaPosition.class);
        when(titlePosition.getYAlignOffset()).thenReturn(titleOffset);
        when(title.getPosition()).thenReturn(titlePosition);
        doReturn(List.of(title)).when(contents).getChildrenNonCopy();
        when(tooltip.getContents()).thenReturn(contents);
        LabelAPI nameLabel = mock(LabelAPI.class);
        PositionAPI namePosition = mock(PositionAPI.class);
        when(namePosition.getHeight()).thenReturn(NAME_HEIGHT);
        when(nameLabel.getPosition()).thenReturn(namePosition);
        when(tooltip.addTitle(SHIP_NAME)).thenReturn(nameLabel);
        return tooltip;
    }

    public abstract static class VanillaTooltip implements TooltipMakerAPI {
        public abstract UIComponentAPI getContents();

        public abstract void updateSize();
    }

    public abstract static class VanillaPanel implements UIComponentAPI {
        public abstract List<?> getChildrenNonCopy();
    }

    public abstract static class VanillaPosition implements PositionAPI {
        public abstract float getYAlignOffset();
    }
}
