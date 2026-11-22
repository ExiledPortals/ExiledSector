package exiledsector.skills.skilleffect.special;

import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import exiledsector.skills.EffectContribution;
import exiledsector.skills.PlacedNode;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShroudedCountTest {

    private record Placed(String nodeId, String skillTypeId) implements PlacedNode {
    }

    private static HullModSpecAPI spec(boolean shrouded) {
        HullModSpecAPI spec = mock(HullModSpecAPI.class);
        when(spec.hasTag(Tags.SHROUDED)).thenReturn(shrouded);
        return spec;
    }

    private static EffectContribution contribution(Object origin) {
        return EffectContribution.scaledPermanent(origin, "node_", List.of());
    }

    @Test
    void onlyHullModsTaggedShroudedCount() {
        Map<String, HullModSpecAPI> specs = Map.of("shrouded_lens", spec(true), "heavyarmor", spec(false));

        assertEquals(Set.of("shrouded_lens"), ShroudedCount.shroudedHullModIds(List.of("shrouded_lens", "heavyarmor", "unknown"), specs::get));
    }

    @Test
    void eachShroudedSmallNodeCountsAndOtherNodesDoNot() {
        List<EffectContribution> contributions = List.of(contribution(new Placed("first", "shrouded_lens_damage")),
                contribution(new Placed("second", "shrouded_lens_damage")), contribution(new Placed("third", "hungering_rift_healing")),
                contribution(new Placed("fourth", "heavy_armor")), contribution("not a node"));

        assertEquals(3, ShroudedCount.nodeSourcesOf(contributions).size());
    }

    @Test
    void aNotableAndTheHullModItGrantsCountOnce() {
        Set<String> sources = new HashSet<>(ShroudedCount.shroudedHullModIds(List.of("shrouded_mantle"), hullModId -> spec(true)));
        sources.addAll(ShroudedCount.nodeSourcesOf(List.of(contribution(new Placed("mantle", "shrouded_mantle")))));

        assertEquals(Set.of("shrouded_mantle"), sources);
    }
}
