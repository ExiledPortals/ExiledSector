package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.listeners.EconomyTickListener;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.Seeds;
import exiledsector.compat.RandomAssortmentCompat;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.items.Item;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemUnlock;
import exiledsector.items.WormholeAnchorDefinitions;
import exiledsector.skills.tags.SkillTags;
import exiledsector.ui.CampaignMessages;
import org.apache.log4j.Logger;

public class HyperNavBeaconGrant implements EconomyTickListener {

    static final String GRANTED_KEY = "$exiledSector_hyperNavBeaconGranted";
    private static final Logger LOG = Logger.getLogger(HyperNavBeaconGrant.class);

    @Override
    public void reportEconomyTick(int iterIndex) {
        grantOnce(Global.getSector());
    }

    @Override
    public void reportEconomyMonthEnd() {
    }

    public static boolean grantOnce(SectorAPI sector) {
        MemoryAPI sectorMemory = sector == null ? null : sector.getMemoryWithoutUpdate();
        CampaignFleetAPI playerFleet = sector == null ? null : sector.getPlayerFleet();
        if (sectorMemory == null || playerFleet == null || sectorMemory.getBoolean(GRANTED_KEY) || !RandomAssortmentCompat.isModEnabled()) {
            return false;
        }
        String beaconId = WormholeAnchorDefinitions.definitionIdFor(SkillTags.EXOTECH_REGION);
        ItemDefinition beacon = beaconId == null ? null : ItemDefinitions.get(beaconId);
        if (beacon == null || !WormholeAnchorDefinitions.areaActive(beacon.id()) || !ItemUnlock.unlockConditionMet(beacon, sector)) {
            return false;
        }
        ItemData beaconData = ItemData.rolled(beacon, Seeds.random(sector.getSeedString() + "|" + GRANTED_KEY).nextLong());
        playerFleet.getCargo().addSpecial(beaconData.toSpecialItem(), 1f);
        sectorMemory.set(GRANTED_KEY, true);
        LOG.info("[ExiledSector] Gave the player a " + beacon.id() + " for recovering the Exoship");
        Item preview = beaconData.preview();
        if (preview != null && CampaignMessages.available()) {
            String message = I18n.forGameText(() -> Translation.msg("socketable.anchor.beaconGranted").arg("name", preview.name()).text());
            CampaignMessages.plain(message, Misc.getPositiveHighlightColor());
        }
        return true;
    }
}
