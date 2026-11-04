package exiledsector.skills.skilleffect.shipsystem;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.combat.threat.RoilingSwarmEffect;
import com.fs.starfarer.api.impl.combat.threat.RoilingSwarmEffect.SwarmMember;
import com.fs.starfarer.api.util.FaderUtil;

public final class FragmentSwarms {

    public static final String HULL_MOD_ID = "fragment_swarm";

    private FragmentSwarms() {
    }

    public static boolean hasSwarmHullMod(ShipVariantAPI variant) {
        return variant != null && variant.hasHullMod(HULL_MOD_ID);
    }

    public static RoilingSwarmEffect swarmOf(ShipAPI ship) {
        return ship == null || Global.getCombatEngine() == null ? null : RoilingSwarmEffect.getSwarmFor(ship);
    }

    public static boolean isFullTo(RoilingSwarmEffect swarm, float fraction) {
        int maintained = swarm == null ? 0 : swarm.getNumMembersToMaintain();
        return maintained > 0 && swarm.getMembers().size() >= maintained * fraction;
    }

    public static boolean isFull(RoilingSwarmEffect swarm) {
        return isFullTo(swarm, 1f);
    }

    public static boolean hasFragments(RoilingSwarmEffect swarm, int needed) {
        if (needed <= 0) {
            return true;
        }
        if (swarm == null) {
            return false;
        }
        int found = 0;
        for (Object member : swarm.getMembers()) {
            if (member instanceof SwarmMember fragment && isActive(fragment.fader) && ++found >= needed) {
                return true;
            }
        }
        return false;
    }

    public static boolean spend(RoilingSwarmEffect swarm, int fragments) {
        if (fragments <= 0 || !hasFragments(swarm, fragments)) {
            return false;
        }
        swarm.despawnMembers(fragments);
        return true;
    }

    public static int regrow(RoilingSwarmEffect swarm, int fragments) {
        if (swarm == null || fragments <= 0) {
            return 0;
        }
        int added = Math.min(fragments, swarm.getNumMembersToMaintain() - swarm.getMembers().size());
        if (added <= 0) {
            return 0;
        }
        swarm.addMembers(added);
        return added;
    }

    private static boolean isActive(FaderUtil fader) {
        return fader == null || !fader.isFadingOut() && !fader.isFadedOut();
    }
}
