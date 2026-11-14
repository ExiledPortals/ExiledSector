package exiledsector.compat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;

import java.util.ArrayList;
import java.util.List;

public final class SecretsOfTheFrontierCompat {

    public static final String MOD_ID = "secretsofthefrontier";
    public static final List<String> TESTED_VERSIONS = List.of("0.15.1");
    public static final CompatTarget TARGET = new CompatTarget("Secrets of the Frontier", List.of(MOD_ID), TESTED_VERSIONS,
            SecretsOfTheFrontierCompat::missingFeatures);
    static final String FLASHBACK_ENCOUNTER_CLASS = "data.scripts.campaign.plugins.amemory.SotfAMemoryFEContext";
    static final String CONCORD_SHIFT_SYSTEM_ID = "sotf_concordshift";

    private SecretsOfTheFrontierCompat() {
    }

    public static boolean hasConcordShift(ShipHullSpecAPI hullSpec) {
        return hullSpec != null && CONCORD_SHIFT_SYSTEM_ID.equals(hullSpec.getShipSystemId());
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
        if (!CompatTarget.classExists(FLASHBACK_ENCOUNTER_CLASS)) {
            missing.add(FLASHBACK_ENCOUNTER_CLASS + ", whose scripted story fights drop none of Exiled Sector's loot");
        }
        if (!CompatTarget.specExists(() -> Global.getSettings().getShipSystemSpec(CONCORD_SHIFT_SYSTEM_ID))) {
            missing.add("the ship system " + CONCORD_SHIFT_SYSTEM_ID + ", whose ships can take phase nodes");
        }
        return missing;
    }
}
