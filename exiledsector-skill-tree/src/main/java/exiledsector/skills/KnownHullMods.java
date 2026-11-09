package exiledsector.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CharacterDataAPI;
import com.fs.starfarer.api.campaign.SectorAPI;

public final class KnownHullMods {

    private KnownHullMods() {
    }

    public static boolean knownByPlayer(String hullModId) {
        SectorAPI sector = Global.getSector();
        CharacterDataAPI playerCharacter = sector == null ? null : sector.getCharacterData();
        return playerCharacter == null || playerCharacter.knowsHullMod(hullModId);
    }
}
