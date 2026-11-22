package exiledsector.skills.skilleffect.special;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import exiledsector.effects.ResolvedTree;
import exiledsector.effects.SkillTreeHullMod;
import exiledsector.skills.EffectContribution;
import exiledsector.skills.PlacedNode;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public final class ShroudedCount {

    static final Set<String> NOTABLE_HULL_MOD_IDS = Set.of("shrouded_lens", "shrouded_mantle", "shrouded_thunderhead");
    static final Set<String> SMALL_NODE_TYPE_IDS = Set.of("shrouded_lens_damage", "rift_lightning_damage", "hungering_rift_healing");
    private static final String SMALL_NODE_SOURCE_PREFIX = "node:";
    private static final Object NODE_SOURCES_KEY = new Object();

    private ShroudedCount() {
    }

    public static int of(FleetMemberAPI member, ShipVariantAPI variant) {
        if (variant == null) {
            return 0;
        }
        Set<String> sources = shroudedHullModIds(variant.getHullMods(), hullModId -> Global.getSettings().getHullModSpec(hullModId));
        ResolvedTree tree = SkillTreeHullMod.resolvedTree(member, variant);
        if (tree != null) {
            sources.addAll(tree.derived(NODE_SOURCES_KEY, () -> nodeSourcesOf(tree.contributions())));
        }
        return sources.size();
    }

    static Set<String> shroudedHullModIds(Collection<String> hullModIds, Function<String, HullModSpecAPI> specs) {
        Set<String> shrouded = new HashSet<>();
        for (String hullModId : hullModIds) {
            HullModSpecAPI spec = specs.apply(hullModId);
            if (spec != null && spec.hasTag(Tags.SHROUDED)) {
                shrouded.add(hullModId);
            }
        }
        return shrouded;
    }

    static Set<String> nodeSourcesOf(List<EffectContribution> contributions) {
        Set<String> sources = new HashSet<>();
        for (EffectContribution contribution : contributions) {
            if (contribution.origin() instanceof PlacedNode node) {
                if (NOTABLE_HULL_MOD_IDS.contains(node.skillTypeId())) {
                    sources.add(node.skillTypeId());
                } else if (SMALL_NODE_TYPE_IDS.contains(node.skillTypeId())) {
                    sources.add(SMALL_NODE_SOURCE_PREFIX + node.nodeId());
                }
            }
        }
        return Set.copyOf(sources);
    }
}
