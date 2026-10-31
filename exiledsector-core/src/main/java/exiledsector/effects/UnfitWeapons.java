package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.WeaponGroupSpec;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;

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
            if (slot == null || weapon == null || slot.isBuiltIn() || slot.weaponFits(weapon)) {
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
        ReturnedToCargo.announce(member, returnUnfitWeapons(variant, cargo), UnfitWeapons::weaponName, "weaponSlot.returned", "weapon");
    }

    private static String weaponName(String weaponId) {
        WeaponSpecAPI weapon = Global.getSettings().getWeaponSpec(weaponId);
        return weapon == null ? weaponId : weapon.getWeaponName();
    }
}
