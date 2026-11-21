package exiledsector.items.pattern;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import exiledsector.SectorMarkets;

public final class HullPatternShips {

    public enum Where {FLEET, STORAGE, AWAY}

    public record Location(Where where, FleetMemberAPI member, String marketName) {
    }

    private HullPatternShips() {
    }

    public static Location locate(String shipId) {
        FleetMemberAPI inFleet = HullPatternActivity.fleetMember(shipId);
        if (inFleet != null) {
            return new Location(Where.FLEET, inFleet, null);
        }
        Location[] stored = new Location[1];
        if (shipId != null) {
            SectorMarkets.forEach(Global.getSector(), market -> {
                if (stored[0] == null) {
                    FleetMemberAPI storedMember = storedIn(market, shipId);
                    if (storedMember != null) {
                        stored[0] = new Location(Where.STORAGE, storedMember, market.getName());
                    }
                }
            });
        }
        return stored[0] != null ? stored[0] : new Location(Where.AWAY, null, null);
    }

    private static FleetMemberAPI storedIn(MarketAPI market, String shipId) {
        for (SubmarketAPI submarket : market.getSubmarketsCopy()) {
            boolean isStorage = Submarkets.SUBMARKET_STORAGE.equals(submarket.getSpecId())
                    || (submarket.getPlugin() != null && submarket.getPlugin().isFreeTransfer());
            CargoAPI storageCargo = isStorage ? submarket.getCargoNullOk() : null;
            if (storageCargo == null || storageCargo.getMothballedShips() == null) {
                continue;
            }
            for (FleetMemberAPI member : storageCargo.getMothballedShips().getMembersListCopy()) {
                if (shipId.equals(member.getId())) {
                    return member;
                }
            }
        }
        return null;
    }
}
