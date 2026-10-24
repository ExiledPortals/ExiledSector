package exiledsector.skills;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledlib.MockSector;
import exiledlib.skills.HullSizeSkillEffect;
import exiledlib.skills.SkillTypeEffect;
import exiledlib.skills.skilleffect.SkillEffect;
import exiledlib.skills.skilleffect.defense.DefenseSkillEffect;
import exiledsector.persistence.ShipSkillDataManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AllocatedSkillEffectsTest {

    private MockSector mockSector;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();

        SkillTree.clearNodes();
        SkillTree.clearTypes();
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
        SkillTree.clearNodes();
        SkillTree.clearTypes();
    }

    private FleetMemberAPI memberWithHullSize(HullSize hullSize) {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn("ship-a");
        ShipHullSpecAPI hullSpec = mock(ShipHullSpecAPI.class);
        when(hullSpec.getHullSize()).thenReturn(hullSize);
        when(member.getHullSpec()).thenReturn(hullSpec);
        return member;
    }

    @Test
    void includesHullSizeScaledEffectsNotJustFlatEffects() {
        HullSizeSkillEffect hullSizeEffect = new HullSizeSkillEffect(DefenseSkillEffect.ARMOR_PERCENT, 5f, 10f, 15f, 20f);
        SkillType type = new SkillType.Builder("hull", "Hull", "a.png", SkillTier.SMALL)
                .effects(List.of(new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 10f)))
                .hullSizeEffects(List.of(hullSizeEffect))
                .build();
        SkillNode node = new SkillNode("hull_1", type, List.of(), 0f, 0f);
        SkillTree.register(node);

        FleetMemberAPI member = memberWithHullSize(HullSize.CRUISER);
        ShipTrees.allocate(ShipSkillDataManager.get("ship-a"), node, 1);

        List<SkillEffect> effects = AllocatedSkillEffects.forMember(member);

        assertTrue(effects.contains(DefenseSkillEffect.HULL_PERCENT));
        assertTrue(effects.contains(DefenseSkillEffect.ARMOR_PERCENT));
    }

    @Test
    void skipsNodesBackedByAVanillaHullMod() {
        SkillType type = new SkillType.Builder("hull", "Hull", "a.png", SkillTier.SMALL)
                .effects(List.of(new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 10f)))
                .vanillaHullModId("some_vanilla_hullmod")
                .build();
        SkillNode node = new SkillNode("hull_1", type, List.of(), 0f, 0f);
        SkillTree.register(node);

        FleetMemberAPI member = memberWithHullSize(HullSize.CRUISER);
        ShipTrees.allocate(ShipSkillDataManager.get("ship-a"), node, 1);

        List<SkillEffect> effects = AllocatedSkillEffects.forMember(member);

        assertFalse(effects.contains(DefenseSkillEffect.HULL_PERCENT));
    }

    @Test
    void returnsAnEmptyListWhenNothingIsAllocated() {
        FleetMemberAPI member = memberWithHullSize(HullSize.FRIGATE);

        List<SkillEffect> effects = AllocatedSkillEffects.forMember(member);

        assertEquals(0, effects.size());
    }
}
