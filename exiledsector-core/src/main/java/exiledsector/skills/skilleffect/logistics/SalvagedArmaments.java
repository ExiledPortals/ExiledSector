package exiledsector.skills.skilleffect.logistics;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.data.RandomRounding;

import java.util.List;
import java.util.Random;

public final class SalvagedArmaments {

    static final String CONTRIBUTION_KEY = "exiledSector_postBattleWeaponSalvageContribution";

    private SalvagedArmaments() {
    }

    public static void addExtra(CampaignFleetAPI playerFleet, CargoAPI loot, Random random) {
        if (playerFleet == null || playerFleet.getFleetData() == null || loot == null) {
            return;
        }
        float bonusPercent = bonusPercent(playerFleet.getFleetData().getMembersListCopy());
        if (bonusPercent <= 0f) {
            return;
        }
        for (CargoStackAPI stack : loot.getStacksCopy()) {
            boolean weaponStack = stack.isWeaponStack();
            if (!weaponStack && !stack.isFighterWingStack()) {
                continue;
            }
            int extra = extraCount(stack.getSize(), bonusPercent, random);
            if (extra <= 0) {
                continue;
            }
            if (weaponStack) {
                loot.addWeapons(stack.getWeaponSpecIfWeapon().getWeaponId(), extra);
            } else {
                loot.addFighters(stack.getFighterWingSpecIfWing().getId(), extra);
            }
        }
    }

    static float bonusPercent(List<FleetMemberAPI> members) {
        return FleetContributions.sum(members, CONTRIBUTION_KEY);
    }

    static int extraCount(float quantity, float bonusPercent, Random random) {
        return RandomRounding.roundWithOneDraw(quantity * bonusPercent / 100f, random);
    }
}
