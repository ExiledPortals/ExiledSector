package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.persistence.ShipRecords;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ModuleParents {

    static final String COMBAT_REGISTRY_KEY = "exiledSector_moduleParents";

    private ModuleParents() {
    }

    public static ModuleParent find(FleetMemberAPI moduleMember, ShipVariantAPI moduleVariant) {
        String parentMemberId = ModuleLinks.parentId(moduleVariant);
        if (parentMemberId == null || moduleMember == null) {
            return null;
        }
        FleetDataAPI fleetData = moduleMember.getFleetData();
        if (fleetData != null) {
            List<FleetMemberAPI> fleetMembers = fleetData.getMembersListCopy();
            return fleetMembers.contains(moduleMember) ? null : refitParent(fleetMembers, parentMemberId);
        }
        CombatEngineAPI engine = Global.getCombatEngine();
        return engine == null ? null : combatRegistry(engine).get(parentMemberId);
    }

    static void register(ShipEffectContext parentContext, ResolvedTree parentTree) {
        CombatEngineAPI engine = Global.getCombatEngine();
        ShipVariantAPI parentVariant = parentContext.variant();
        if (engine != null && parentContext.member() != null && ModuleLinks.hasModules(parentVariant)) {
            combatRegistry(engine).put(parentContext.member().getId(), new ModuleParent(parentContext, parentTree));
        }
    }

    private static ModuleParent refitParent(List<FleetMemberAPI> fleetMembers, String parentMemberId) {
        for (FleetMemberAPI member : fleetMembers) {
            if (parentMemberId.equals(member.getId())) {
                ShipVariantAPI variant = member.getVariant();
                ShipEffectContext context = new ShipEffectContext(ShipRecords.resolve(member, variant), member, variant, variant.getHullSize());
                ResolvedTree resolvedTree = SkillTreeHullMod.resolve(context);
                return resolvedTree == null ? null : new ModuleParent(context, resolvedTree);
            }
        }
        return null;
    }

    // the registry is only ever stored under this key by this class
    @SuppressWarnings("unchecked")
    private static Map<String, ModuleParent> combatRegistry(CombatEngineAPI engine) {
        Map<String, Object> customData = engine.getCustomData();
        return (Map<String, ModuleParent>) customData.computeIfAbsent(COMBAT_REGISTRY_KEY, key -> new HashMap<String, ModuleParent>());
    }
}
