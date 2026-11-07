package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.listeners.ShowLootListener;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.util.Misc;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.WormholeAnchorDefinitions;
import exiledsector.skills.SkillTreeSetting;
import exiledsector.skills.tags.SkillTags;

import java.util.List;

public final class AlphaSiteOmegaCache implements ShowLootListener {

    static final String ANCHOR_ADDED_KEY = "$exiledSector_omegaAnchorAdded";
    private static final long ANCHOR_SEED_SALT = 0x6F6D656761L;

    static boolean isCache(SectorEntityToken entity) {
        return entity != null && Entities.ALPHA_SITE_WEAPONS_CACHE.equals(entity.getCustomEntityType());
    }

    @Override
    public void reportAboutToShowLootToPlayer(CargoAPI loot, InteractionDialogAPI dialog) {
        addAnchor(Global.getSector(), dialog == null ? null : dialog.getInteractionTarget(), loot);
    }

    static void addAnchor(SectorAPI sector, SectorEntityToken cache, CargoAPI loot) {
        if (loot == null || !isCache(cache) || !SkillTreeSetting.enabled()) {
            return;
        }
        MemoryAPI cacheMemory = cache.getMemoryWithoutUpdate();
        String definitionId = WormholeAnchorDefinitions.definitionIdFor(SkillTags.OMEGA_VOLUME);
        ItemDefinition definition = definitionId == null ? null : ItemDefinitions.get(definitionId);
        if (cacheMemory == null || cacheMemory.getBoolean(ANCHOR_ADDED_KEY) || definition == null
                || !WormholeAnchorDefinitions.volumeActive(definitionId)) {
            return;
        }
        cacheMemory.set(ANCHOR_ADDED_KEY, true);
        ItemData anchor = ItemData.rolled(definition, Misc.getSalvageSeed(cache) ^ ANCHOR_SEED_SALT);
        loot.addSpecial(anchor.toSpecialItem(), 1f);
        WormholeAnchorDefinitions.recordFound(List.of(anchor), sector, "the Alpha Site cache");
    }
}
