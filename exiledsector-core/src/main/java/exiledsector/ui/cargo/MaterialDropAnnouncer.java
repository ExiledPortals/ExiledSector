package exiledsector.ui.cargo;

import com.fs.starfarer.api.Global;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.items.MaterialItems;
import exiledsector.items.MaterialTier;
import exiledsector.ui.CampaignMessages;

import java.util.Map;

public final class MaterialDropAnnouncer {

    private MaterialDropAnnouncer() {
    }

    public static void announce(Map<String, Integer> droppedMaterials) {
        MaterialTier bestTier = bestTier(droppedMaterials);
        if (bestTier != null && bestTier.dropSoundId() != null && Global.getSoundPlayer() != null) {
            Global.getSoundPlayer().playUISound(bestTier.dropSoundId(), 1f, 1f);
        }
        announceOnLootScreen(droppedMaterials);
    }

    public static void announceOnLootScreen(Map<String, Integer> droppedMaterials) {
        if (!CampaignMessages.available()) {
            return;
        }
        for (Map.Entry<String, Integer> dropped : droppedMaterials.entrySet()) {
            if (dropped.getValue() > 0 && MaterialItems.tier(dropped.getKey()).announced()) {
                String materialName = MaterialItems.displayName(dropped.getKey());
                String message = I18n.forGameText(() -> Translation.msg("material.dropped").arg("name", materialName).text());
                CampaignMessages.highlighted(message, MaterialItems.color(dropped.getKey()), materialName, null,
                        MaterialItems.color(dropped.getKey()), null);
            }
        }
    }

    static MaterialTier bestTier(Map<String, Integer> droppedMaterials) {
        MaterialTier bestTier = null;
        for (Map.Entry<String, Integer> dropped : droppedMaterials.entrySet()) {
            MaterialTier tier = MaterialItems.tier(dropped.getKey());
            if (dropped.getValue() > 0 && (bestTier == null || tier.compareTo(bestTier) > 0)) {
                bestTier = tier;
            }
        }
        return bestTier;
    }
}
