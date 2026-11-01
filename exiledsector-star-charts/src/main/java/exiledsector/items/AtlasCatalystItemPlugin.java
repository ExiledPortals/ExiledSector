package exiledsector.items;

import com.fs.starfarer.api.campaign.CargoTransferHandlerAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import exiledsector.charts.atlas.AtlasCatalysts;
import exiledsector.ui.cargo.CargoIconLook;
import exiledsector.ui.cargo.CatalystCargo;

public class AtlasCatalystItemPlugin extends AnimatedCargoItemPlugin {

    private static final float TOOLTIP_PAD = 10f;

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, CargoTransferHandlerAPI transferHandler, Object stackSource) {
        CatalystCargo.writeTooltip(tooltip, itemId, TOOLTIP_PAD);
        addCostLabel(tooltip, TOOLTIP_PAD, transferHandler, stackSource);
    }

    @Override
    protected CargoIconLook cargoLook() {
        AtlasCatalysts.Catalyst catalyst = AtlasCatalysts.byItemId(itemId);
        return CatalystCargo.look(catalyst == null ? null : catalyst.family());
    }

    @Override
    protected String cargoIconPath() {
        AtlasCatalysts.Catalyst catalyst = AtlasCatalysts.byItemId(itemId);
        return catalyst == null || catalyst.iconPath().isEmpty() ? null : catalyst.iconPath();
    }
}
