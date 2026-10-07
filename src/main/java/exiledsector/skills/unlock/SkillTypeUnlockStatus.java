package exiledsector.skills.unlock;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CharacterDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillType;
import org.apache.log4j.Logger;

import java.util.List;

public final class SkillTypeUnlockStatus {

    private SkillTypeUnlockStatus() {
    }

    public static boolean isLocked(SkillType skillType, ShipSkillData shipData) {
        List<UnlockCondition> conditions = skillType.getUnlockConditions();
        if (conditions.isEmpty()) return false;

        for (UnlockCondition condition : conditions) {
            if (isSatisfied(condition, shipData)) return false;
        }
        return true;
    }

    public static boolean isHidden(SkillType skillType, ShipSkillData shipData) {
        return isLocked(skillType, shipData) && !HiddenNodeDisplayConfig.showHiddenNodesByDefault();
    }

    private static boolean isSatisfied(UnlockCondition condition, ShipSkillData shipData) {
        if (UnlockConditionOverrides.isDisabled(condition.getType())) return true;

        try {
            return switch (condition.getType()) {
                case BLUEPRINT -> isBlueprintKnown(condition);
                case CHARACTER_STAT -> hasCharacterStat(condition);
                case MIN_SHIP_LEVEL -> hasMinShipLevel(condition, shipData);
                case MEMORY_FLAG -> hasMemoryFlag(condition);
                default -> false;
            };
        } catch (RuntimeException e) {
            Logger.getLogger(SkillTypeUnlockStatus.class).error("Failed to check unlock condition " + condition, e);
            return false;
        }
    }

    private static boolean isBlueprintKnown(UnlockCondition condition) {
        String hullModId = condition.getKey();
        if (hullModId == null) return false;

        CharacterDataAPI playerCharacter = Global.getSector().getCharacterData();
        return playerCharacter != null && playerCharacter.knowsHullMod(hullModId);
    }

    private static boolean hasCharacterStat(UnlockCondition condition) {
        String statId = condition.getKey();
        if (statId == null) return false;

        CharacterDataAPI playerCharacter = Global.getSector().getCharacterData();
        if (playerCharacter == null) return false;
        PersonAPI playerPerson = playerCharacter.getPerson();
        return playerPerson != null && playerPerson.getStats().getDynamic().getMod(statId).getFlatBonus() > 0f;
    }

    private static boolean hasMinShipLevel(UnlockCondition condition, ShipSkillData shipData) {
        return shipData != null && shipData.getLevel() >= condition.getMinLevel();
    }

    private static boolean hasMemoryFlag(UnlockCondition condition) {
        String flagKey = condition.getKey();
        if (flagKey == null) return false;
        return Global.getSector().getMemoryWithoutUpdate().getBoolean(flagKey);
    }
}
