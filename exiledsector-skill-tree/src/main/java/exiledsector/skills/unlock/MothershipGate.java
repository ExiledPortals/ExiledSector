package exiledsector.skills.unlock;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.EngagementResultForFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import java.util.List;

public final class MothershipGate {

    static final String DEFEATED_FLAG = "$exiledSector_defeatedMothership";
    static final String MOTHERSHIP_HULL_ID = "station_derelict_survey_mothership";

    private MothershipGate() {
    }

    public static boolean isDefeated(SectorAPI sector) {
        MemoryAPI playerMemory = sector == null ? null : sector.getPlayerMemoryWithoutUpdate();
        return playerMemory != null && playerMemory.getBoolean(DEFEATED_FLAG);
    }

    public static void recordBattle(SectorAPI sector, EngagementResultAPI result) {
        if (sector == null || result == null || !result.didPlayerWin()
                || !GateTriggers.awaits(sector, UnlockConditionType.MOTHERSHIP_DEFEATED, isDefeated(sector))) {
            return;
        }
        if (defeatedMothership(result.getLoserResult())) {
            GateTriggers.record(sector, UnlockConditionType.MOTHERSHIP_DEFEATED, sector.getPlayerMemoryWithoutUpdate(), DEFEATED_FLAG,
                    "Opened the Domain Drones gate: the player beat a Domain-era survey mothership");
        }
    }

    private static boolean defeatedMothership(EngagementResultForFleetAPI losers) {
        if (losers == null) {
            return false;
        }
        CampaignFleetAPI losingFleet = losers.getFleet();
        List<FleetMemberAPI> losingMembers = losingFleet == null || losingFleet.getFleetData() == null
                ? null : losingFleet.getFleetData().getMembersListCopy();
        return containsMothership(losers.getDestroyed()) || containsMothership(losers.getDisabled()) || containsMothership(losingMembers);
    }

    static boolean containsMothership(List<FleetMemberAPI> members) {
        if (members == null) {
            return false;
        }
        for (FleetMemberAPI member : members) {
            if (member != null && isMothershipHull(member.getHullSpec())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isMothershipHull(ShipHullSpecAPI hullSpec) {
        return hullSpec != null && (MOTHERSHIP_HULL_ID.equals(hullSpec.getHullId()) || MOTHERSHIP_HULL_ID.equals(hullSpec.getBaseHullId()));
    }

    public static final class BattleListener extends BaseCampaignEventListener {

        public BattleListener() {
            super(false);
        }

        @Override
        public void reportPlayerEngagement(EngagementResultAPI result) {
            recordBattle(Global.getSector(), result);
        }
    }
}
