package exiledsector.ui;

import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;
import exiledsector.ui.cargo.ItemAnimationSetting;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OverchargeBreathTest {

    @Test
    void theGlowRisesFromNothingToItsPeakAndBackOverOneBreath() {
        assertEquals(0f, OverchargeBreath.glow(0f, ItemAnimationSetting.Mode.FULL), 1e-5f);
        assertEquals(1f, OverchargeBreath.glow(OverchargeBreath.BREATH_SECONDS / 2f, ItemAnimationSetting.Mode.FULL), 1e-5f);
        assertEquals(0f, OverchargeBreath.glow(OverchargeBreath.BREATH_SECONDS, ItemAnimationSetting.Mode.FULL), 1e-5f);
    }

    @Test
    void calmAnimationsBreatheHalfAsDeep() {
        assertEquals(OverchargeBreath.CALM_DEPTH, OverchargeBreath.glow(OverchargeBreath.BREATH_SECONDS / 2f, ItemAnimationSetting.Mode.CALM), 1e-5f);
    }

    @Test
    void theBreathBlendsTheValueColourTowardsThePeak() {
        Color base = SkillTreePanelStyle.OVERCHARGED_TEXT_COLOR;

        assertEquals(base, OverchargeBreath.breathed(base, 0f));
        assertEquals(OverchargeBreath.GLOW_PEAK, OverchargeBreath.breathed(base, 1f));
    }

    @Test
    void onlyOverchargedSpansBreathe() {
        StyledText text = StyledText.styled("7%", Style.OVERCHARGED).append(StyledText.styled(" more hull points.", Style.IMMUTABLE));

        assertArrayEquals(new int[]{0}, OverchargeBreath.overchargedSpans(text.spans()));
        assertArrayEquals(new int[0], OverchargeBreath.overchargedSpans(List.of()));
    }
}
