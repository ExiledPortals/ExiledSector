package exiledsector.items.subroutine.doctrine;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.items.Item;
import exiledsector.items.ItemStore;
import exiledsector.items.subroutine.NodeGrantStatus;
import exiledsector.skills.ShipSkillData;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

public final class ConvertedAnchorRelease {

    static final String DONE_KEY = "exiledSector_infestedAnchorReleaseDone";
    private static final Logger LOG = Logger.getLogger(ConvertedAnchorRelease.class);

    private ConvertedAnchorRelease() {
    }

    public static void releaseOnce(SectorAPI sector, Map<String, ShipSkillData> shipDataById) {
        Map<String, Object> persistentData = sector == null ? null : sector.getPersistentData();
        if (persistentData == null || Boolean.TRUE.equals(persistentData.get(DONE_KEY))
                || DoctrineArchives.all().stream().noneMatch(DoctrineArchive::infested)) {
            return;
        }
        List<String> releasedItemIds = release(shipDataById, ItemStore::lookup);
        persistentData.put(DONE_KEY, Boolean.TRUE);
        if (!releasedItemIds.isEmpty()) {
            LOG.info("[ExiledSector] Returned " + releasedItemIds.size()
                    + " Threat and Shrouded Wormhole Anchors to storage as Infested Doctrine Archives: " + releasedItemIds);
        }
    }

    static List<String> release(Map<String, ShipSkillData> shipDataById, Function<String, Item> itemLookup) {
        List<String> releasedItemIds = new ArrayList<>();
        for (ShipSkillData shipData : shipDataById.values()) {
            for (Map.Entry<String, String> socketed : new TreeMap<>(shipData.getSocketedItems()).entrySet()) {
                Item item = itemLookup.apply(socketed.getValue());
                if (item != null && DoctrineArchives.infested(item.definitionId())) {
                    shipData.unsocketItem(socketed.getKey());
                    NodeGrantStatus.forget(item.id());
                    releasedItemIds.add(item.id());
                }
            }
        }
        return releasedItemIds;
    }
}
