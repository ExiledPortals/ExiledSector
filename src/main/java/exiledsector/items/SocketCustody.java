package exiledsector.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import exiledsector.PlayerFleet;
import exiledsector.persistence.SaveData;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.persistence.SkillTreeTemplateStore;
import exiledsector.skills.ShipSkillData;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class SocketCustody {

    static final String LOST_KEY = "exiledSector_socketShipsLostInCombat";

    public record Installation(String shipId, String nodeId, String sectionType) {

        public Installation(String shipId, String nodeId) {
            this(shipId, nodeId, null);
        }

        public static Installation inSection(String shipId, String sectionType) {
            return new Installation(shipId, null, sectionType);
        }

        public boolean isSection() {
            return sectionType != null;
        }
    }

    private SocketCustody() {
    }

    public static Map<String, Installation> installations(Map<String, ShipSkillData> shipDataById) {
        Map<String, Installation> installations = new HashMap<>();
        shipDataById.forEach((shipId, shipData) -> {
            shipData.getSocketedItems().forEach((nodeId, itemId) -> installations.put(itemId, new Installation(shipId, nodeId)));
            shipData.getSectionItems().forEach((socketTypeId, itemId) ->
                    installations.put(itemId, Installation.inSection(shipId, socketTypeId)));
        });
        return installations;
    }

    static Set<String> reconcile(Map<String, ShipSkillData> shipDataById, Set<String> ownedShipIds, Set<String> lostInCombat, ItemStore store) {
        lostInCombat.removeIf(ownedShipIds::contains);
        Set<String> lostForGood = new HashSet<>(lostInCombat);
        lostForGood.retainAll(shipDataById.keySet());
        shipDataById.forEach((shipId, shipData) -> {
            if (ownedShipIds.contains(shipId) || !holdsSocketItems(shipData)) {
                return;
            }
            boolean destroyed = lostInCombat.contains(shipId);
            for (String nodeId : Set.copyOf(shipData.getSocketedItems().keySet())) {
                String itemId = shipData.unsocketItem(nodeId);
                Item item = store.find(itemId);
                if (destroyed && item != null) {
                    store.remove(item);
                }
            }
            for (String socketTypeId : Set.copyOf(shipData.getSectionItems().keySet())) {
                Item item = store.find(shipData.removeFromSection(socketTypeId));
                if (destroyed && item != null) {
                    store.remove(item);
                }
            }
        });
        lostInCombat.removeIf(shipId -> !shipDataById.containsKey(shipId) || !holdsSocketItems(shipDataById.get(shipId)));
        return lostForGood;
    }

    static boolean holdsSocketItems(ShipSkillData shipData) {
        return !shipData.getSocketedItems().isEmpty() || !shipData.getSectionItems().isEmpty();
    }

    public static Map<String, FleetMemberAPI> reconcile() {
        if (PlayerFleet.orNull() == null) {
            return Map.of();
        }
        Map<String, FleetMemberAPI> ownedShipsById = ownedShips();
        Set<String> lostForGood = reconcile(ShipSkillDataManager.all(), ownedShipsById.keySet(), lostInCombat(), ItemStore.get());
        if (!lostForGood.isEmpty()) {
            forget(lostForGood);
        }
        return ownedShipsById;
    }

    private static void forget(Set<String> lostShipIds) {
        lostShipIds.forEach(ShipSkillDataManager::remove);
        lostShipIds.forEach(SkillTreeTemplateStore::clearAssignment);
        Global.getLogger(SocketCustody.class).info("[ExiledSector] Forgot the skill trees of ships lost in combat: " + lostShipIds);
    }

    public static Set<String> ownedShipIds() {
        if (PlayerFleet.orNull() == null) {
            return Set.of();
        }
        return ownedShips().keySet();
    }

    public static boolean isInstalled(Item item) {
        return isInstalled(ShipSkillDataManager.all(), item);
    }

    static boolean isInstalled(Map<String, ShipSkillData> shipDataById, Item item) {
        String itemId = item.id();
        for (ShipSkillData shipData : shipDataById.values()) {
            if (shipData.holdsItem(itemId)) {
                return true;
            }
        }
        return false;
    }

    public static void markHostsChanged(Item item) {
        String itemId = item.id();
        for (ShipSkillData shipData : ShipSkillDataManager.all().values()) {
            if (shipData.holdsItem(itemId)) {
                shipData.markChanged();
            }
        }
    }

    static boolean fitsTreeSocket(Item item) {
        return item instanceof Subroutine || item instanceof WormholeAnchor;
    }

    static boolean fitsSection(Item item, String socketTypeId) {
        return item.canSocketInto(ShipSection.byIdOrNull(socketTypeId));
    }

    public static boolean unsocketIfMisfit(Item item) {
        return unsocketIfMisfit(ShipSkillDataManager.all(), item);
    }

    static boolean unsocketIfMisfit(Map<String, ShipSkillData> shipDataById, Item item) {
        String itemId = item.id();
        for (ShipSkillData shipData : shipDataById.values()) {
            for (Map.Entry<String, String> socketedEntry : List.copyOf(shipData.getSocketedItems().entrySet())) {
                if (socketedEntry.getValue().equals(itemId) && (!fitsTreeSocket(item)
                        || SubroutineSocketLimits.limitReached(item, socketedEntry.getKey(), shipData.getSocketedItems()))) {
                    shipData.unsocketItem(socketedEntry.getKey());
                    return true;
                }
            }
            for (Map.Entry<String, String> slotEntry : List.copyOf(shipData.getSectionItems().entrySet())) {
                if (slotEntry.getValue().equals(itemId) && !fitsSection(item, slotEntry.getKey())) {
                    shipData.removeFromSection(slotEntry.getKey());
                    return true;
                }
            }
        }
        return false;
    }

    public static void recordLostInCombat(Collection<FleetMemberAPI> members) {
        Set<String> lostShipIds = lostInCombat();
        members.forEach(member -> lostShipIds.add(member.getId()));
    }

    public static void recordRecovered(Collection<FleetMemberAPI> members) {
        Set<String> lostShipIds = lostInCombat();
        members.forEach(member -> lostShipIds.remove(member.getId()));
    }

    public static Function<Item, String> shipNames(Map<String, FleetMemberAPI> ownedShipsById) {
        Map<String, Installation> installations = installations(ShipSkillDataManager.all());
        return item -> {
            Installation installation = installations.get(item.id());
            if (installation == null) {
                return null;
            }
            FleetMemberAPI ownerMember = ownedShipsById.get(installation.shipId());
            return ownerMember == null ? installation.shipId() : ownerMember.getShipName();
        };
    }

    private static Set<String> lostInCombat() {
        return SaveData.getOrCreate(LOST_KEY, HashSet::new);
    }

    private static Map<String, FleetMemberAPI> ownedShips() {
        Map<String, FleetMemberAPI> ownedShipsById = new LinkedHashMap<>();
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (playerFleet != null) {
            playerFleet.getFleetData().getMembersListCopy().forEach(member -> ownedShipsById.put(member.getId(), member));
        }
        Set<MarketAPI> visitedMarkets = new HashSet<>();
        for (LocationAPI location : Global.getSector().getAllLocations()) {
            for (CampaignFleetAPI locationFleet : location.getFleets()) {
                if (locationFleet.getFaction() != null && locationFleet.getFaction().isPlayerFaction()) {
                    locationFleet.getFleetData().getMembersListCopy().forEach(member -> ownedShipsById.put(member.getId(), member));
                }
            }
            for (SectorEntityToken entity : location.getAllEntities()) {
                MarketAPI market = entity.getMarket();
                if (market != null && visitedMarkets.add(market)) {
                    addStoredShips(market, ownedShipsById);
                }
            }
        }
        return ownedShipsById;
    }

    private static void addStoredShips(MarketAPI market, Map<String, FleetMemberAPI> ownedShipsById) {
        for (SubmarketAPI submarket : market.getSubmarketsCopy()) {
            boolean isStorage = Submarkets.SUBMARKET_STORAGE.equals(submarket.getSpecId())
                    || (submarket.getPlugin() != null && submarket.getPlugin().isFreeTransfer());
            CargoAPI storageCargo = isStorage ? submarket.getCargoNullOk() : null;
            if (storageCargo != null && storageCargo.getMothballedShips() != null) {
                storageCargo.getMothballedShips().getMembersListCopy().forEach(member -> ownedShipsById.put(member.getId(), member));
            }
        }
    }
}
