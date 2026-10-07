package exiledsector.socketables;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import exiledsector.persistence.OpSpentSlotManager;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.persistence.SkillTreeTemplateStore;
import exiledsector.skills.ShipSkillData;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class SocketCustody {

    static final String LOST_KEY = "exiledSector_socketShipsLostInCombat";

    public record Installation(String shipId, String nodeId) {
    }

    private SocketCustody() {
    }

    public static Map<String, Installation> installations(Map<String, ShipSkillData> shipDataById) {
        Map<String, Installation> installations = new HashMap<>();
        shipDataById.forEach((shipId, shipData) -> shipData.getSocketedItems().forEach((nodeId, socketableId) ->
                installations.put(socketableId, new Installation(shipId, nodeId))));
        return installations;
    }

    static Set<String> reconcile(Map<String, ShipSkillData> shipDataById, Set<String> ownedShipIds, Set<String> lostInCombat, SocketableStore store) {
        lostInCombat.removeIf(ownedShipIds::contains);
        Set<String> lostForGood = new HashSet<>(lostInCombat);
        lostForGood.retainAll(shipDataById.keySet());
        shipDataById.forEach((shipId, shipData) -> {
            if (ownedShipIds.contains(shipId) || shipData.getSocketedItems().isEmpty()) {
                return;
            }
            boolean destroyed = lostInCombat.contains(shipId);
            for (String nodeId : Set.copyOf(shipData.getSocketedItems().keySet())) {
                String socketableId = shipData.unsocketItem(nodeId);
                Socketable socketable = store.find(socketableId);
                if (destroyed && socketable != null) {
                    store.remove(socketable);
                }
            }
        });
        lostInCombat.removeIf(shipId -> !shipDataById.containsKey(shipId) || shipDataById.get(shipId).getSocketedItems().isEmpty());
        return lostForGood;
    }

    public static Map<String, FleetMemberAPI> reconcile() {
        SectorAPI sector = Global.getSector();
        if (sector == null || sector.getPlayerFleet() == null) {
            return Map.of();
        }
        Map<String, FleetMemberAPI> ownedShipsById = ownedShips();
        Set<String> lostForGood = reconcile(ShipSkillDataManager.all(), ownedShipsById.keySet(), lostInCombat(), SocketableStore.get());
        if (!lostForGood.isEmpty()) {
            forget(lostForGood);
        }
        return ownedShipsById;
    }

    private static void forget(Set<String> lostShipIds) {
        lostShipIds.forEach(ShipSkillDataManager::remove);
        lostShipIds.forEach(SkillTreeTemplateStore::clearAssignment);
        OpSpentSlotManager.releaseUnless(shipId -> !lostShipIds.contains(shipId));
        Global.getLogger(SocketCustody.class).info("[ExiledSector] Forgot the skill trees of ships lost in combat: " + lostShipIds);
    }

    public static Set<String> ownedShipIds() {
        SectorAPI sector = Global.getSector();
        if (sector == null || sector.getPlayerFleet() == null) {
            return Set.of();
        }
        return ownedShips().keySet();
    }

    public static boolean isInstalled(Socketable socketable) {
        return isInstalled(ShipSkillDataManager.all(), socketable);
    }

    static boolean isInstalled(Map<String, ShipSkillData> shipDataById, Socketable socketable) {
        String socketableId = socketable.id();
        for (ShipSkillData shipData : shipDataById.values()) {
            if (shipData.getSocketedItems().containsValue(socketableId)) {
                return true;
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

    public static Function<Socketable, String> shipNames(Map<String, FleetMemberAPI> ownedShipsById) {
        Map<String, Installation> installations = installations(ShipSkillDataManager.all());
        return socketable -> {
            Installation installation = installations.get(socketable.id());
            if (installation == null) {
                return null;
            }
            FleetMemberAPI ownerMember = ownedShipsById.get(installation.shipId());
            return ownerMember == null ? installation.shipId() : ownerMember.getShipName();
        };
    }

    // persistentData is a raw Object map; this key is only ever written as Set<String>
    @SuppressWarnings("unchecked")
    private static Set<String> lostInCombat() {
        return (Set<String>) Global.getSector().getPersistentData().computeIfAbsent(LOST_KEY, key -> new HashSet<String>());
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
