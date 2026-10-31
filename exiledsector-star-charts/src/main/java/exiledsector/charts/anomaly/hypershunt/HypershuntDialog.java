package exiledsector.charts.anomaly.hypershunt;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageDefenderInteraction;
import exiledsector.i18n.Translation;
import exiledsector.ui.VanillaText;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class HypershuntDialog implements InteractionDialogPlugin {

    static final String LEAVE = "exiledSector_hypershuntLeave";
    static final String BEAT_DEFENDERS_CONTINUE = "salBeatDefendersContinue";

    private final Map<String, MemoryAPI> memoryMap = new HashMap<>();
    private InteractionDialogAPI interactionDialog;
    private SectorEntityToken hypershunt;

    @Override
    public void init(InteractionDialogAPI interactionDialog) {
        this.interactionDialog = interactionDialog;
        this.hypershunt = interactionDialog.getInteractionTarget();
        showVisual();
        VanillaText.plainPara(interactionDialog.getTextPanel(), Translation.gameText("chart.hypershunt.description"));
        if (!engageGuards()) {
            showState();
        }
    }

    private void showVisual() {
        if (hypershunt != null && hypershunt.getCustomInteractionDialogImageVisual() != null) {
            interactionDialog.getVisualPanel().showImageVisual(hypershunt.getCustomInteractionDialogImageVisual());
        }
    }

    private boolean engageGuards() {
        CampaignFleetAPI guards = HypershuntGuards.muster(hypershunt, CoronalHypershunt.liveRunFor(hypershunt));
        if (guards == null) {
            return false;
        }
        fillMemoryMap();
        VanillaText.plainPara(interactionDialog.getTextPanel(), Translation.gameText("chart.hypershunt.guarded"));
        return new SalvageDefenderInteraction().execute(null, interactionDialog, new ArrayList<>(), memoryMap);
    }

    private void fillMemoryMap() {
        SectorAPI sector = Global.getSector();
        memoryMap.put(MemKeys.LOCAL, hypershunt.getMemoryWithoutUpdate());
        FactionAPI faction = hypershunt.getFaction();
        memoryMap.put(MemKeys.FACTION, faction != null ? faction.getMemoryWithoutUpdate() : Global.getFactory().createMemory());
        memoryMap.put(MemKeys.GLOBAL, sector.getMemoryWithoutUpdate());
        memoryMap.put(MemKeys.PLAYER, sector.getCharacterData().getMemoryWithoutUpdate());
    }

    private void showState() {
        OptionPanelAPI options = interactionDialog.getOptionPanel();
        options.clearOptions();
        VanillaText.plainPara(interactionDialog.getTextPanel(), Translation.gameText("chart.hypershunt.beyondRepair"));
        options.addOption(Translation.gameText("chart.dialog.option.leave"), LEAVE);
        options.setShortcut(LEAVE, Keyboard.KEY_ESCAPE, false, false, false, true);
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        if (BEAT_DEFENDERS_CONTINUE.equals(optionData)) {
            showVisual();
            showState();
        } else {
            interactionDialog.dismiss();
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
