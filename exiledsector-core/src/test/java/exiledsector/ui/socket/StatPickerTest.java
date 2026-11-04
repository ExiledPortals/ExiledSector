package exiledsector.ui.socket;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatPickerTest {

    private static final Map<String, String> LABELS = Map.of(
            "FLUX_DISSIPATION_PERCENT", "#% increased flux dissipation",
            "FLUX_CAPACITY_PERCENT", "#% increased flux capacity",
            "ARMOR_PERCENT", "#% increased armor",
            "MIN_CREW_PERCENT", "#% reduced minimum crew required");
    private static final List<String> STATS = List.of("ARMOR_PERCENT", "FLUX_CAPACITY_PERCENT", "FLUX_DISSIPATION_PERCENT", "MIN_CREW_PERCENT");

    private static List<String> matching(String typed) {
        return StatPicker.matching(STATS, LABELS::get, typed);
    }

    @Test
    void eachKeystrokeNarrowsTheListByWhatTheLabelContains() {
        assertEquals(STATS, matching(""));
        assertEquals(List.of("FLUX_CAPACITY_PERCENT", "FLUX_DISSIPATION_PERCENT"), matching("f"));
        assertEquals(List.of("FLUX_CAPACITY_PERCENT", "FLUX_DISSIPATION_PERCENT"), matching("FLUX "));
        assertEquals(List.of("FLUX_DISSIPATION_PERCENT"), matching("flux d"));
        assertEquals(List.of("MIN_CREW_PERCENT"), matching("crew"));
        assertEquals(List.of(), matching("shield"));
    }

    @Test
    void theListOpensBelowItsFieldWithTheRoomDownToThePaneEdge() {
        StatPicker.ListBounds bounds = StatPicker.place(40f, 124f, 200f, 260f, 500f, 600f);

        assertEquals(124f + HoloPanel.LINE_PAD, bounds.top(), 0.001f);
        assertEquals(600f - HoloPanel.PAD - bounds.top(), bounds.height(), 0.001f);
        assertEquals(260f, bounds.width(), 0.001f);
    }

    @Test
    void aFieldNearThePaneBottomStillOpensItsListBelowAndStaysInsideThePane() {
        StatPicker.ListBounds bounds = StatPicker.place(400f, 524f, 200f, 900f, 500f, 560f);

        assertEquals(524f + HoloPanel.LINE_PAD, bounds.top(), 0.001f);
        assertTrue(bounds.top() + bounds.height() <= 560f - HoloPanel.PAD + 0.001f);
        assertTrue(bounds.left() >= HoloPanel.PAD);
        assertTrue(bounds.left() + bounds.width() <= 500f - HoloPanel.PAD + 0.001f);
    }

    @Test
    void choicesStackWithALinePadBetweenThem() {
        assertEquals(20f + 34f + HoloPanel.LINE_PAD, StatPicker.stackHeight(List.of(20f, 34f)), 0.001f);
        assertEquals(0f, StatPicker.stackHeight(List.of()), 0.001f);
    }

    @Test
    void aLabelTooWideForTheFieldIsCutWithAnEllipsis() {
        ToDoubleFunction<String> tenPerChar = text -> text.length() * 10.0;

        assertEquals("#% increased armor", StatPicker.fitted("#% increased armor", 180f, tenPerChar));
        assertEquals("#% increased...", StatPicker.fitted("#% increased armor", 150f, tenPerChar));
        assertEquals("...", StatPicker.fitted("#% increased armor", 30f, tenPerChar));
    }
}
