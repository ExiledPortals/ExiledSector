package exiledsector.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import org.lwjgl.input.Keyboard;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseDialogPlugin implements InteractionDialogPlugin {

    private final Map<String, MemoryAPI> memoryMap = new HashMap<>();
    private InteractionDialogAPI dialog;

    @Override
    public final void init(InteractionDialogAPI interactionDialog) {
        dialog = interactionDialog;
        fillMemoryMap(interactionDialog.getInteractionTarget());
        open();
    }

    private void fillMemoryMap(SectorEntityToken target) {
        memoryMap.clear();
        if (target == null) {
            return;
        }
        SectorAPI sector = Global.getSector();
        memoryMap.put(MemKeys.LOCAL, target.getMemoryWithoutUpdate());
        FactionAPI faction = target.getFaction();
        memoryMap.put(MemKeys.FACTION, faction != null ? faction.getMemoryWithoutUpdate() : Global.getFactory().createMemory());
        memoryMap.put(MemKeys.GLOBAL, sector.getMemoryWithoutUpdate());
        memoryMap.put(MemKeys.PLAYER, sector.getCharacterData().getMemoryWithoutUpdate());
        MarketAPI market = target.getMarket();
        if (market != null) {
            memoryMap.put(MemKeys.MARKET, market.getMemoryWithoutUpdate());
        }
    }

    protected abstract void open();

    protected final InteractionDialogAPI dialog() {
        return dialog;
    }

    protected final TextPanelAPI text() {
        return dialog.getTextPanel();
    }

    protected final OptionPanelAPI options() {
        return dialog.getOptionPanel();
    }

    protected final void addLeaveOption(String label, Object leaveOption) {
        OptionPanelAPI options = options();
        options.addOption(label, leaveOption);
        options.setShortcut(leaveOption, Keyboard.KEY_ESCAPE, false, false, false, true);
    }

    protected final void showEntityVisual(SectorEntityToken entity) {
        if (entity != null && entity.getCustomInteractionDialogImageVisual() != null) {
            dialog.getVisualPanel().showImageVisual(entity.getCustomInteractionDialogImageVisual());
        }
    }

    protected final void handOff(InteractionDialogPlugin next) {
        dialog.setPlugin(next);
        next.init(dialog);
    }

    protected final void returnHere(InteractionDialogAPI leaving, SectorEntityToken site) {
        leaving.setPlugin(this);
        leaving.setInteractionTarget(site);
        fillMemoryMap(site);
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
