package exiledsector.ui;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TextPanelAPI;
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
        open();
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
