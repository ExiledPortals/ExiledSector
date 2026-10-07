package exiledsector.effects;

import com.fs.starfarer.api.Global;
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
import exiledsector.socketables.SocketableUnlock;

import java.util.Random;

public class SocketableSalvageListener implements ShowLootListener {

    static final String ROLLED_KEY = "$exiledSector_socketablesRolled";
    private static final long SEED_SALT = 0x736F636B65746BL;

    @Override
    public void reportAboutToShowLootToPlayer(CargoAPI loot, InteractionDialogAPI dialog) {
        SectorEntityToken salvageEntity = dialog == null ? null : dialog.getInteractionTarget();
        if (loot == null || salvageEntity == null || salvageEntity instanceof CampaignFleetAPI) {
            return;
        }
        String siteId = siteId(salvageEntity);
        MemoryAPI entityMemory = salvageEntity.getMemoryWithoutUpdate();
        if (!SocketableDrops.hasRule(siteId) || entityMemory == null || entityMemory.getBoolean(ROLLED_KEY)) {
            return;
        }
        entityMemory.set(ROLLED_KEY, true);
        Random random = new Random(Misc.getSalvageSeed(salvageEntity) ^ SEED_SALT);
        for (SocketableItemData item : SocketableDrops.roll(siteId, random)) {
            loot.addSpecial(item.toSpecialItem(), 1f);
        }
        SocketableDrops.rollMaterials(siteId, random).forEach(loot::addCommodity);
        if (SocketableUnlock.frameworksOpen(Global.getSector())) {
            SocketableDrops.FrameworkLoot frameworkLoot = SocketableDrops.rollFrameworkLoot(siteId, random, 1f);
            frameworkLoot.items().forEach(item -> loot.addSpecial(item.toSpecialItem(), 1f));
            frameworkLoot.frameworks().forEach(framework -> loot.addSpecial(framework.toSpecialItem(), 1f));
        }
    }

    static String siteId(SectorEntityToken salvageEntity) {
        MemoryAPI entityMemory = salvageEntity.getMemoryWithoutUpdate();
        if (entityMemory != null && entityMemory.contains(MemFlags.SALVAGE_SPEC_ID_OVERRIDE)) {
            return entityMemory.getString(MemFlags.SALVAGE_SPEC_ID_OVERRIDE);
        }
        return salvageEntity.getCustomEntityType();
    }
}
