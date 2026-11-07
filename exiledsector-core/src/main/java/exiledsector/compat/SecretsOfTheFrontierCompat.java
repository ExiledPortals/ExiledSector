package exiledsector.compat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorAPI;

import java.util.ArrayList;
import java.util.List;

public final class SecretsOfTheFrontierCompat {

    public static final String MOD_ID = "secretsofthefrontier";
    public static final List<String> TESTED_VERSIONS = List.of("0.15.1");
    public static final CompatTarget TARGET = new CompatTarget("Secrets of the Frontier", List.of(MOD_ID), TESTED_VERSIONS,
            SecretsOfTheFrontierCompat::missingFeatures);
    static final String FLASHBACK_ENCOUNTER_CLASS = "data.scripts.campaign.plugins.amemory.SotfAMemoryFEContext";

    private SecretsOfTheFrontierCompat() {
    }

    public static boolean isFlashbackEncounter(Object encounterContext) {
        for (Class<?> type = encounterContext == null ? null : encounterContext.getClass(); type != null; type = type.getSuperclass()) {
            if (FLASHBACK_ENCOUNTER_CLASS.equals(type.getName())) {
                return true;
            }
        }
        return false;
    }

    public static boolean inFlashbackEncounter() {
        SectorAPI sector = Global.getSector();
        CampaignUIAPI campaignUI = sector == null ? null : sector.getCampaignUI();
        InteractionDialogAPI dialog = campaignUI == null ? null : campaignUI.getCurrentInteractionDialog();
        return dialog != null && dialog.getPlugin() != null && isFlashbackEncounter(dialog.getPlugin().getContext());
    }

    static List<String> missingFeatures() {
        List<String> missing = new ArrayList<>();
        try {
            Class.forName(FLASHBACK_ENCOUNTER_CLASS, false, Global.getSettings().getScriptClassLoader());
        } catch (ClassNotFoundException | LinkageError e) {
            missing.add(FLASHBACK_ENCOUNTER_CLASS + ", whose scripted story fights drop none of Exiled Sector's loot");
        }
        return missing;
    }
}
