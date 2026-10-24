package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.WeaponGroupSpec;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.util.Misc;
import exiledlib.i18n.I18n;
import exiledlib.i18n.Translation;
import exiledsector.ui.CampaignMessages;

import java.util.ArrayList;
import java.util.List;

public final class UnfitWeapons {

    private UnfitWeapons() {
    }

    public static List<String> returnUnfitWeapons(ShipVariantAPI variant, CargoAPI cargo) {
        List<String> returnedWeaponIds = new ArrayList<>();
        if (variant == null || cargo == null || variant.getHullSpec() == null) {
            return returnedWeaponIds;
        }
        for (String slotId : variant.getNonBuiltInWeaponSlots()) {
            WeaponSlotAPI slot = variant.getSlot(slotId);
            WeaponSpecAPI weapon = variant.getWeaponSpec(slotId);
            if (slot == null || weapon == null || slot.isBuiltIn() || slot.getWeaponType() != WeaponType.BALLISTIC || slot.weaponFits(weapon)) {
                continue;
            }
            String weaponId = variant.getWeaponId(slotId);
            variant.clearSlot(slotId);
            for (WeaponGroupSpec group : variant.getWeaponGroups()) {
                group.removeSlot(slotId);
            }
            cargo.addWeapons(weaponId, 1);
            returnedWeaponIds.add(weaponId);
        }
        return returnedWeaponIds;
    }

    public static void returnUnfitWeaponsAndAnnounce(FleetMemberAPI member, ShipVariantAPI variant, CargoAPI cargo) {
        List<String> returnedWeaponIds = returnUnfitWeapons(variant, cargo);
        if (returnedWeaponIds.isEmpty()) {
            return;
        }
        member.setStatUpdateNeeded(true);
        member.updateStats();
        if (!CampaignMessages.available()) {
            return;
        }
        for (String weaponId : returnedWeaponIds) {
            WeaponSpecAPI weapon = Global.getSettings().getWeaponSpec(weaponId);
            String name = weapon == null ? weaponId : weapon.getWeaponName();
            CampaignMessages.plain(I18n.forGameText(() -> Translation.msg("weaponSlot.returned").arg("weapon", name).text()), Misc.getTextColor());
        }
    }
}
