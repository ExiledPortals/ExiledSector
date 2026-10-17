package exiledsector.hullitems;

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

    public record Installation(String shipId, String nodeId, String frameworkSocketType) {

        public Installation(String shipId, String nodeId) {
            this(shipId, nodeId, null);
        }

        public static Installation inFramework(String shipId, String frameworkSocketType) {
            return new Installation(shipId, null, frameworkSocketType);
        }

        public boolean isFrameworkSocket() {
            return frameworkSocketType != null;
        }
    }

    private SocketCustody() {
    }

    public static Map<String, Installation> installations(Map<String, ShipSkillData> shipDataById) {
        Map<String, Installation> installations = new HashMap<>();
        shipDataById.forEach((shipId, shipData) -> {
            shipData.getSocketedItems().forEach((nodeId, hullItemId) -> installations.put(hullItemId, new Installation(shipId, nodeId)));
            shipData.getFrameworkSocketedItems().forEach((socketTypeId, hullItemId) ->
                    installations.put(hullItemId, Installation.inFramework(shipId, socketTypeId)));
        });
        return installations;
    }

    static Set<String> reconcile(Map<String, ShipSkillData> shipDataById, Set<String> ownedShipIds, Set<String> lostInCombat, HullItemStore store) {
        lostInCombat.removeIf(ownedShipIds::contains);
        Set<String> lostForGood = new HashSet<>(lostInCombat);
        lostForGood.retainAll(shipDataById.keySet());
        shipDataById.forEach((shipId, shipData) -> {
            if (ownedShipIds.contains(shipId) || !holdsSocketItems(shipData)) {
                return;
            }
            boolean destroyed = lostInCombat.contains(shipId);
            for (String nodeId : Set.copyOf(shipData.getSocketedItems().keySet())) {
                String hullItemId = shipData.unsocketItem(nodeId);
                HullItem hullItem = store.find(hullItemId);
                if (destroyed && hullItem != null) {
                    store.remove(hullItem);
                }
            }
            for (String socketTypeId : Set.copyOf(shipData.getFrameworkSocketedItems().keySet())) {
                HullItem hullItem = store.find(shipData.unsocketFrameworkItem(socketTypeId));
                if (destroyed && hullItem != null) {
                    store.remove(hullItem);
                }
            }
        });
        lostInCombat.removeIf(shipId -> !shipDataById.containsKey(shipId) || !holdsSocketItems(shipDataById.get(shipId)));
        return lostForGood;
    }

    static boolean holdsSocketItems(ShipSkillData shipData) {
        return !shipData.getSocketedItems().isEmpty() || !shipData.getFrameworkSocketedItems().isEmpty();
    }

    public static Map<String, FleetMemberAPI> reconcile() {
        if (PlayerFleet.orNull() == null) {
            return Map.of();
        }
        Map<String, FleetMemberAPI> ownedShipsById = ownedShips();
        Set<String> lostForGood = reconcile(ShipSkillDataManager.all(), ownedShipsById.keySet(), lostInCombat(), HullItemStore.get());
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

    public static boolean isInstalled(HullItem hullItem) {
        return isInstalled(ShipSkillDataManager.all(), hullItem);
    }

    static boolean isInstalled(Map<String, ShipSkillData> shipDataById, HullItem hullItem) {
        String hullItemId = hullItem.id();
        for (ShipSkillData shipData : shipDataById.values()) {
            if (shipData.holdsHullItem(hullItemId)) {
                return true;
            }
        }
        return false;
    }

    public static void markHostsChanged(HullItem hullItem) {
        String hullItemId = hullItem.id();
        for (ShipSkillData shipData : ShipSkillDataManager.all().values()) {
            if (shipData.holdsHullItem(hullItemId)) {
                shipData.markChanged();
            }
        }
    }

    static boolean fitsTreeSocket(HullItem hullItem) {
        return hullItem.kind() == SocketType.SUBROUTINE;
    }

    static boolean fitsFrameworkSocket(HullItem hullItem, String socketTypeId) {
        return hullItem.canSocketInto(SocketType.byIdOrNull(socketTypeId));
    }

    public static boolean unsocketIfMisfit(HullItem hullItem) {
        return unsocketIfMisfit(ShipSkillDataManager.all(), hullItem);
    }

    static boolean unsocketIfMisfit(Map<String, ShipSkillData> shipDataById, HullItem hullItem) {
        String hullItemId = hullItem.id();
        for (ShipSkillData shipData : shipDataById.values()) {
            for (Map.Entry<String, String> socketedEntry : List.copyOf(shipData.getSocketedItems().entrySet())) {
                if (socketedEntry.getValue().equals(hullItemId) && !fitsTreeSocket(hullItem)) {
                    shipData.unsocketItem(socketedEntry.getKey());
                    return true;
                }
            }
            for (Map.Entry<String, String> slotEntry : List.copyOf(shipData.getFrameworkSocketedItems().entrySet())) {
                if (slotEntry.getValue().equals(hullItemId) && !fitsFrameworkSocket(hullItem, slotEntry.getKey())) {
                    shipData.unsocketFrameworkItem(slotEntry.getKey());
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

    public static Function<HullItem, String> shipNames(Map<String, FleetMemberAPI> ownedShipsById) {
        Map<String, Installation> installations = installations(ShipSkillDataManager.all());
        return hullItem -> {
            Installation installation = installations.get(hullItem.id());
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
