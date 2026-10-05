package exiledsector.effects;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.listeners.ShowLootListener;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.util.Misc;
import exiledsector.socketables.SocketableDrops;
import exiledsector.socketables.SocketableItemData;

import java.util.Random;

public class SocketableSalvageListener implements ShowLootListener {

    static final String ROLLED_KEY = "$exiledSector_socketablesRolled";
    private static final long SEED_SALT = 0x736F636B65746BL;

    @Override
    public void reportAboutToShowLootToPlayer(CargoAPI loot, InteractionDialogAPI dialog) {
        SectorEntityToken target = dialog == null ? null : dialog.getInteractionTarget();
        if (loot == null || target == null || target instanceof CampaignFleetAPI) {
            return;
        }
        String site = siteId(target);
        MemoryAPI memory = target.getMemoryWithoutUpdate();
        if (!SocketableDrops.hasRule(site) || memory == null || memory.getBoolean(ROLLED_KEY)) {
            return;
        }
        memory.set(ROLLED_KEY, true);
        Random random = new Random(Misc.getSalvageSeed(target) ^ SEED_SALT);
        for (SocketableItemData item : SocketableDrops.roll(site, random)) {
            loot.addSpecial(item.toSpecialItem(), 1f);
        }
    }

    static String siteId(SectorEntityToken target) {
        MemoryAPI memory = target.getMemoryWithoutUpdate();
        if (memory != null && memory.contains(MemFlags.SALVAGE_SPEC_ID_OVERRIDE)) {
            return memory.getString(MemFlags.SALVAGE_SPEC_ID_OVERRIDE);
        }
        return target.getCustomEntityType();
    }
}
