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
import exiledsector.persistence.ShipSkillDataManager;
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

    public static Map<String, Installation> installations(Map<String, ShipSkillData> ships) {
        Map<String, Installation> installations = new HashMap<>();
        ships.forEach((shipId, data) -> data.getSocketedItems().forEach((nodeId, socketableId) ->
                installations.put(socketableId, new Installation(shipId, nodeId))));
        return installations;
    }

    static void reconcile(Map<String, ShipSkillData> ships, Set<String> ownedShipIds, Set<String> lostInCombat, SocketableStore store) {
        lostInCombat.removeIf(ownedShipIds::contains);
        ships.forEach((shipId, data) -> {
            if (ownedShipIds.contains(shipId) || data.getSocketedItems().isEmpty()) {
                return;
            }
            boolean destroyed = lostInCombat.contains(shipId);
            for (String nodeId : Set.copyOf(data.getSocketedItems().keySet())) {
                String socketableId = data.unsocketItem(nodeId);
                Socketable socketable = store.find(socketableId);
                if (destroyed && socketable != null) {
                    store.remove(socketable);
                }
            }
        });
        lostInCombat.removeIf(shipId -> !ships.containsKey(shipId) || ships.get(shipId).getSocketedItems().isEmpty());
    }

    public static Map<String, FleetMemberAPI> reconcile() {
        SectorAPI sector = Global.getSector();
        if (sector == null || sector.getPlayerFleet() == null) {
            return Map.of();
        }
        Map<String, FleetMemberAPI> owned = ownedShips();
        reconcile(ShipSkillDataManager.all(), owned.keySet(), lostInCombat(), SocketableStore.get());
        return owned;
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

    static boolean isInstalled(Map<String, ShipSkillData> ships, Socketable socketable) {
        String id = socketable.id();
        for (ShipSkillData data : ships.values()) {
            if (data.getSocketedItems().containsValue(id)) {
                return true;
            }
        }
        return false;
    }

    public static void recordLostInCombat(Collection<FleetMemberAPI> members) {
        Set<String> lost = lostInCombat();
        members.forEach(member -> lost.add(member.getId()));
    }

    public static void recordRecovered(Collection<FleetMemberAPI> members) {
        Set<String> lost = lostInCombat();
        members.forEach(member -> lost.remove(member.getId()));
    }

    public static Function<Socketable, String> shipNames(Map<String, FleetMemberAPI> owned) {
        Map<String, Installation> installations = installations(ShipSkillDataManager.all());
        return socketable -> {
            Installation installation = installations.get(socketable.id());
            if (installation == null) {
                return null;
            }
            FleetMemberAPI member = owned.get(installation.shipId());
            return member == null ? installation.shipId() : member.getShipName();
        };
    }

    // persistentData is a raw Object map; this key is only ever written as Set<String>
    @SuppressWarnings("unchecked")
    private static Set<String> lostInCombat() {
        return (Set<String>) Global.getSector().getPersistentData().computeIfAbsent(LOST_KEY, key -> new HashSet<String>());
    }

    private static Map<String, FleetMemberAPI> ownedShips() {
        Map<String, FleetMemberAPI> owned = new LinkedHashMap<>();
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet != null) {
            fleet.getFleetData().getMembersListCopy().forEach(member -> owned.put(member.getId(), member));
        }
        Set<MarketAPI> markets = new HashSet<>();
        for (LocationAPI location : Global.getSector().getAllLocations()) {
            for (CampaignFleetAPI other : location.getFleets()) {
                if (other.getFaction() != null && other.getFaction().isPlayerFaction()) {
                    other.getFleetData().getMembersListCopy().forEach(member -> owned.put(member.getId(), member));
                }
            }
            for (SectorEntityToken entity : location.getAllEntities()) {
                MarketAPI market = entity.getMarket();
                if (market != null && markets.add(market)) {
                    addStoredShips(market, owned);
                }
            }
        }
        return owned;
    }

    private static void addStoredShips(MarketAPI market, Map<String, FleetMemberAPI> owned) {
        for (SubmarketAPI submarket : market.getSubmarketsCopy()) {
            boolean storage = Submarkets.SUBMARKET_STORAGE.equals(submarket.getSpecId())
                    || (submarket.getPlugin() != null && submarket.getPlugin().isFreeTransfer());
            CargoAPI cargo = storage ? submarket.getCargoNullOk() : null;
            if (cargo != null && cargo.getMothballedShips() != null) {
                cargo.getMothballedShips().getMembersListCopy().forEach(member -> owned.put(member.getId(), member));
            }
        }
    }
}
