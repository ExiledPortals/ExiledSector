package exiledsector.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;
import exiledsector.ui.cargo.IconFxClock;
import exiledsector.ui.cargo.ItemAnimationSetting;

import java.awt.Color;
import java.util.List;
import java.util.stream.IntStream;

public final class OverchargeBreath extends BaseCustomUIPanelPlugin {

    static final float BREATH_SECONDS = 2.4f;
    static final float CALM_DEPTH = 0.5f;
    static final Color GLOW_PEAK = new Color(220, 255, 250);
    private static final float ANCHOR_SIZE = 1f;

    private final LabelAPI label;
    private final Color[] baseColors;
    private final Color[] shownColors;
    private final int[] breathingSpans;

    private OverchargeBreath(LabelAPI label, Color[] colors, int[] breathingSpans) {
        this.label = label;
        this.baseColors = colors.clone();
        this.shownColors = colors.clone();
        this.breathingSpans = breathingSpans;
    }

    static void attach(TooltipMakerAPI tooltip, LabelAPI label, StyledText text, Color[] colors) {
        int[] breathingSpans = overchargedSpans(text.spans());
        if (label == null || breathingSpans.length == 0 || ItemAnimationSetting.mode() == ItemAnimationSetting.Mode.OFF) {
            return;
        }
        UIComponentAPI labelComponent = tooltip.getPrev();
        if (labelComponent == null) {
            return;
        }
        CustomPanelAPI anchor = Global.getSettings().createCustom(ANCHOR_SIZE, ANCHOR_SIZE, new OverchargeBreath(label, colors, breathingSpans));
        if (anchor != null) {
            tooltip.addCustomDoNotSetPosition(anchor).getPosition().rightOfTop(labelComponent, -ANCHOR_SIZE);
        }
    }

    static int[] overchargedSpans(List<StyledText.Span> spans) {
        return IntStream.range(0, spans.size()).filter(index -> spans.get(index).style() == Style.OVERCHARGED).toArray();
    }

    static float glow(float seconds, ItemAnimationSetting.Mode mode) {
        float depth = mode == ItemAnimationSetting.Mode.CALM ? CALM_DEPTH : 1f;
        return (1f - (float) Math.cos(2.0 * Math.PI * seconds / BREATH_SECONDS)) * 0.5f * depth;
    }

    static Color breathed(Color base, float glow) {
        return new Color(channel(base.getRed(), GLOW_PEAK.getRed(), glow), channel(base.getGreen(), GLOW_PEAK.getGreen(), glow),
                channel(base.getBlue(), GLOW_PEAK.getBlue(), glow), base.getAlpha());
    }

    private static int channel(int from, int to, float fraction) {
        return Math.round(from + (to - from) * fraction);
    }

    @Override
    public void render(float alphaMult) {
        float glow = glow(IconFxClock.seconds(), ItemAnimationSetting.mode());
        for (int spanIndex : breathingSpans) {
            if (spanIndex < shownColors.length) {
                shownColors[spanIndex] = breathed(baseColors[spanIndex], glow);
            }
        }
        label.setHighlightColors(shownColors);
    }
}
