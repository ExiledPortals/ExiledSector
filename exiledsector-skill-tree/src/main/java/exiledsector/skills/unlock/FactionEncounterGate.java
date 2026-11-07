package exiledsector.skills.unlock;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import exiledsector.skills.npc.NpcFactionVolumes;
import exiledsector.skills.tags.SkillTags;

import java.util.List;
import java.util.function.Predicate;

public record FactionEncounterGate(String gateName, UnlockConditionType conditionType, String flagKey, boolean ownsFlag,
                                   boolean winNeeded, Predicate<String> fleetFactionMatches) {

    public static final FactionEncounterGate THREAT = new FactionEncounterGate("Threat", UnlockConditionType.THREAT_FOUGHT,
            "$encounteredThreat", false, false, Factions.THREAT::equals);
    public static final FactionEncounterGate REMNANT = new FactionEncounterGate("Remnant", UnlockConditionType.REMNANT_FOUGHT,
            "$exiledSector_foughtRemnant", true, false, factionId -> SkillTags.REDACTED_VOLUME.equals(NpcFactionVolumes.volumeFor(factionId)));
    public static final FactionEncounterGate DWELLER = new FactionEncounterGate("Dweller", UnlockConditionType.DWELLER_FOUGHT,
            "$encounteredDweller", false, false, Factions.DWELLER::equals);
    public static final FactionEncounterGate OMEGA = new FactionEncounterGate("Omega", UnlockConditionType.OMEGA_FOUND,
            "$exiledSector_foundOmega", true, true, Factions.OMEGA::equals);
    public static final List<FactionEncounterGate> ALL = List.of(THREAT, REMNANT, DWELLER, OMEGA);

    public boolean isMet(SectorAPI sector) {
        MemoryAPI playerMemory = sector == null ? null : sector.getPlayerMemoryWithoutUpdate();
        return playerMemory != null && playerMemory.getBoolean(flagKey);
    }

    public boolean awaits(SectorAPI sector) {
        return sector != null && GateTriggers.awaits(sector, conditionType, isMet(sector));
    }

    public void recordBattle(SectorAPI sector, BattleAPI battle, boolean playerWon) {
        if ((playerWon || !winNeeded) && awaits(sector) && GateTriggers.foughtFaction(battle, fleetFactionMatches)) {
            open(sector, winNeeded ? "the player beat one of its fleets" : "the player fought one of its fleets");
        }
    }

    public void open(SectorAPI sector, String reason) {
        MemoryAPI flagMemory = ownsFlag ? sector.getPlayerMemoryWithoutUpdate() : null;
        GateTriggers.record(sector, conditionType, flagMemory, flagKey, "Opened the " + gateName + " gate: " + reason);
    }

    public static final class BattleListener extends BaseCampaignEventListener {

        public BattleListener() {
            super(false);
        }

        @Override
        public void reportPlayerEngagement(EngagementResultAPI result) {
            if (result == null) {
                return;
            }
            SectorAPI sector = Global.getSector();
            for (FactionEncounterGate gate : ALL) {
                gate.recordBattle(sector, result.getBattle(), result.didPlayerWin());
            }
        }
    }
}
