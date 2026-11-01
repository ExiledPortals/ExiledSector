package exiledsector.charts.objective;

import com.fs.starfarer.api.campaign.SpecialItemSpecAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.items.MaterialItems;
import exiledsector.ui.BaseDialogPlugin;
import exiledsector.ui.VanillaText;

import java.util.List;

final class GatewayUpgradeDialog extends BaseDialogPlugin {

    static final String CONTINUE = "exiledSector_gatewayUpgradeContinue";

    private final List<String> itemIds;

    GatewayUpgradeDialog(List<String> itemIds) {
        this.itemIds = List.copyOf(itemIds);
    }

    List<String> itemIds() {
        return itemIds;
    }

    @Override
    protected void open() {
        TextPanelAPI text = text();
        for (String itemId : itemIds) {
            SpecialItemSpecAPI spec = MaterialItems.spec(itemId);
            if (spec != null && spec.getIconName() != null) {
                text.addImage(spec.getIconName());
            }
            String itemName = MaterialItems.displayName(itemId);
            VanillaText.plainPara(text, I18n.forGameText(() -> Translation.msg("gatewayItem.upgrade.popup").arg("item", itemName).text()));
            AddRemoveCommodity.addItemGainText(MaterialItems.data(itemId), 1, text);
        }
        options().clearOptions();
        addLeaveOption(Translation.gameText("gatewayItem.upgrade.popup.continue"), CONTINUE);
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        if (CONTINUE.equals(optionData) && dialog() != null) {
            dialog().dismiss();
        }
    }
}
