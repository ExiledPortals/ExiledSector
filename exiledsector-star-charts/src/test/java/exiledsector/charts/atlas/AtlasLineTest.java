package exiledsector.charts.atlas;

import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AtlasLineTest {

    @Test
    void tooltipTonesBecomeTheSkillTreeHighlightStyles() {
        assertEquals(StyledText.styled("x", Style.HIGHLIGHT), new AtlasLine("x", AtlasLine.Tone.MARKER).styled());
        assertEquals(StyledText.styled("x", Style.GOOD), new AtlasLine("x", AtlasLine.Tone.GOOD).styled());
        assertEquals(StyledText.styled("x", Style.BAD), new AtlasLine("x", AtlasLine.Tone.BAD).styled());
        assertEquals(StyledText.styled("x", Style.MUTED), new AtlasLine("x", AtlasLine.Tone.HINT).styled());
        assertEquals(StyledText.of("x"), new AtlasLine("x", AtlasLine.Tone.PLAIN).styled());
    }

    @Test
    void styledLinesReadAsPlainTextWithoutTheirMarkup() {
        assertEquals("40% x", new AtlasLine("<bad>40%</bad> x", AtlasLine.Tone.STYLED).plain());
        assertEquals("<x>", new AtlasLine("<x>", AtlasLine.Tone.PLAIN).plain());
    }
}
