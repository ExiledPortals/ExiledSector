package exiledsector.ui.socket;

import com.fs.starfarer.api.ui.TooltipMakerAPI;
import exiledsector.i18n.I18n;

import java.util.function.Consumer;

record GameTextTooltip(float tooltipWidth, Consumer<TooltipMakerAPI> writer) implements TooltipMakerAPI.TooltipCreator {

    @Override
    public boolean isTooltipExpandable(Object tooltipParam) {
        return false;
    }

    @Override
    public float getTooltipWidth(Object tooltipParam) {
        return tooltipWidth;
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
        I18n.forGameText(() -> writer.accept(tooltip));
    }
}
