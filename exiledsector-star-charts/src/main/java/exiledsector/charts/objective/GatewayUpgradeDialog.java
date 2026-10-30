package exiledsector.charts.objective;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.SpecialItemSpecAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.items.MaterialItems;
import exiledsector.items.WormholeTechItems;
import exiledsector.ui.VanillaText;
import org.lwjgl.input.Keyboard;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class GatewayUpgradeDialog implements InteractionDialogPlugin {

    static final String CONTINUE = "exiledSector_gatewayUpgradeContinue";

    private final List<String> itemIds;
    private final Map<String, MemoryAPI> memoryMap = new HashMap<>();
    private InteractionDialogAPI dialog;

    GatewayUpgradeDialog(List<String> itemIds) {
        this.itemIds = List.copyOf(itemIds);
    }

    List<String> itemIds() {
        return itemIds;
    }

    @Override
    public void init(InteractionDialogAPI interactionDialog) {
        dialog = interactionDialog;
        TextPanelAPI text = interactionDialog.getTextPanel();
        for (String itemId : itemIds) {
            SpecialItemSpecAPI spec = MaterialItems.spec(itemId);
            if (spec != null && spec.getIconName() != null) {
                text.addImage(spec.getIconName());
            }
            String itemName = WormholeTechItems.displayName(itemId);
            VanillaText.plainPara(text, I18n.forGameText(() -> Translation.msg("gatewayItem.upgrade.popup").arg("item", itemName).text()));
            AddRemoveCommodity.addItemGainText(WormholeTechItems.data(itemId), 1, text);
        }
        OptionPanelAPI options = interactionDialog.getOptionPanel();
        options.clearOptions();
        options.addOption(Translation.gameText("gatewayItem.upgrade.popup.continue"), CONTINUE);
        options.setShortcut(CONTINUE, Keyboard.KEY_ESCAPE, false, false, false, true);
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        if (CONTINUE.equals(optionData) && dialog != null) {
            dialog.dismiss();
        }
    }

    @Override
    public void optionMousedOver(String optionText, Object optionData) {
    }

    @Override
    public void advance(float amount) {
    }

    @Override
    public void backFromEngagement(EngagementResultAPI battleResult) {
    }

    @Override
    public Object getContext() {
        return null;
    }

    @Override
    public Map<String, MemoryAPI> getMemoryMap() {
        return memoryMap;
    }
}
