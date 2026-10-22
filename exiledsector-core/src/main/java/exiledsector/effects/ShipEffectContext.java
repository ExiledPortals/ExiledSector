package exiledsector.effects;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.skills.ShipSkillData;

public record ShipEffectContext(ShipSkillData shipData, FleetMemberAPI member, ShipVariantAPI variant, HullSize hullSize) {
}
