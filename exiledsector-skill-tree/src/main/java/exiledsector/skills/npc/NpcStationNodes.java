package exiledsector.skills.npc;

import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.skilleffect.SkillEffect;
import exiledsector.skills.skilleffect.fighter.FighterSkillEffect;
import exiledsector.skills.skilleffect.movement.MovementSkillEffect;
import exiledsector.skills.skilleffect.phase.PhaseSkillEffect;
import exiledsector.skills.skilleffect.shipsystem.ShipSystemSkillEffect;
import exiledsector.skills.SkillType;
import exiledsector.skills.tags.ShipProfile;

import java.util.List;

final class NpcStationNodes {

    private NpcStationNodes() {
    }

    static boolean isWasted(SkillType skillType, ShipProfile profile) {
        if (!profile.station() || skillType == null) {
            return false;
        }
        List<SkillTypeEffect> effects = skillType.effectsFor(profile.hullSize());
        return !effects.isEmpty() && effects.stream().allMatch(effect -> isWasted(effect.effect(), profile));
    }

    private static boolean isWasted(SkillEffect effect, ShipProfile profile) {
        return effect instanceof MovementSkillEffect || effect instanceof PhaseSkillEffect || effect instanceof ShipSystemSkillEffect
                || (effect instanceof FighterSkillEffect && profile.fighterBays() <= 0);
    }
}
