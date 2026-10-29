package exiledsector.ui.stats;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import exiledsector.ui.SkillTreePanelStyle;

public final class StatSheetPanelPlugin extends BaseCustomUIPanelPlugin {

    private static ShipStatSheet cachedSheet;
    private static float cachedWidth = Float.NaN;
    private static StatSheetLayout cachedLayout;

    private final StatSheetLayout layout;
    private PositionAPI position;

    private StatSheetPanelPlugin(StatSheetLayout layout) {
        this.layout = layout;
    }

    public static boolean addTo(TooltipMakerAPI tooltip, ShipStatSheet sheet, float width, float pad) {
        if (sheet.isEmpty() || SkillTreePanelStyle.font() == null) {
            return false;
        }
        StatSheetLayout layout = layoutFor(sheet, width);
        StatSheetPanelPlugin plugin = new StatSheetPanelPlugin(layout);
        CustomPanelAPI panel = Global.getSettings().createCustom(width, layout.height(), plugin);
        if (panel == null) {
            return false;
        }
        tooltip.addCustom(panel, pad);
        return true;
    }

    private static StatSheetLayout layoutFor(ShipStatSheet sheet, float width) {
        if (cachedLayout == null || !sheet.equals(cachedSheet) || width != cachedWidth) {
            if (cachedLayout != null) {
                cachedLayout.dispose();
            }
            cachedLayout = StatSheetLayout.build(sheet, width, 1);
            cachedSheet = sheet;
            cachedWidth = width;
        }
        return cachedLayout;
    }

    @Override
    public void positionChanged(PositionAPI position) {
        this.position = position;
    }

    @Override
    public void render(float alphaMult) {
        if (position != null) {
            layout.render(position.getX(), position.getY() + position.getHeight(), alphaMult, null);
        }
    }
}
