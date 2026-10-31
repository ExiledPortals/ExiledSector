package exiledsector.charts.wormhole;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CharacterDataAPI;
import com.fs.starfarer.api.campaign.PersistentUIDataAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.StarChartSettings;
import exiledsector.compat.VaultsOfKnowledgeCompat;
import exiledsector.effects.TransientScripts;

public final class ColonyFreeAtlas {

    public static final String ABILITY_ID = "exiledSector_outer_reach_atlas";
    static final int ABILITY_BARS = 5;

    private ColonyFreeAtlas() {
    }

    public static boolean active() {
        return StarChartSettings.enabled() && StarChartSettings.noColony();
    }

    public static void onGameLoad(SectorAPI sector) {
        syncAbility(sector);
        TransientScripts.nextFrame(sector, ColonyFreeAtlas::syncResearch);
    }

    public static void onSettingsChanged() {
        SectorAPI sector = Global.getSector();
        if (sector == null || Global.getCurrentState() != GameState.CAMPAIGN) {
            return;
        }
        syncAbility(sector);
        syncResearch();
    }

    static void syncResearch() {
        VaultsOfKnowledgeCompat.showGatewayResearch(!active());
    }

    public static void syncAbility(SectorAPI sector) {
        CharacterDataAPI character = sector.getCharacterData();
        if (character == null || sector.getPlayerFleet() == null) {
            return;
        }
        boolean granted = character.getAbilities().contains(ABILITY_ID);
        boolean wanted = active() && WormholeGateway.blueprintUsed();
        if (wanted && !granted) {
            character.addAbility(ABILITY_ID);
            assignSlot(sector.getUIData().getAbilitySlotsAPI());
        } else if (!wanted && granted) {
            character.removeAbility(ABILITY_ID);
            clearSlots(sector.getUIData().getAbilitySlotsAPI());
        }
    }

    static void assignSlot(PersistentUIDataAPI.AbilitySlotsAPI slots) {
        int shownBar = slots.getCurrBarIndex();
        try {
            for (int bar = 0; bar < ABILITY_BARS; bar++) {
                slots.setCurrBarIndex(bar);
                for (PersistentUIDataAPI.AbilitySlotAPI slot : slots.getCurrSlotsCopy()) {
                    if (ABILITY_ID.equals(slot.getAbilityId())) {
                        return;
                    }
                }
            }
            for (int bar = 0; bar < ABILITY_BARS; bar++) {
                slots.setCurrBarIndex(bar);
                for (PersistentUIDataAPI.AbilitySlotAPI slot : slots.getCurrSlotsCopy()) {
                    if (slot.getAbilityId() == null) {
                        slot.setAbilityId(ABILITY_ID);
                        return;
                    }
                }
            }
        } finally {
            slots.setCurrBarIndex(shownBar);
        }
    }

    static void clearSlots(PersistentUIDataAPI.AbilitySlotsAPI slots) {
        int shownBar = slots.getCurrBarIndex();
        for (int bar = 0; bar < ABILITY_BARS; bar++) {
            slots.setCurrBarIndex(bar);
            for (PersistentUIDataAPI.AbilitySlotAPI slot : slots.getCurrSlotsCopy()) {
                if (ABILITY_ID.equals(slot.getAbilityId())) {
                    slot.setAbilityId(null);
                }
                if (ABILITY_ID.equals(slot.getInHyperAbilityId())) {
                    slot.setInHyperAbilityId(null);
                }
            }
        }
        slots.setCurrBarIndex(shownBar);
    }
}
