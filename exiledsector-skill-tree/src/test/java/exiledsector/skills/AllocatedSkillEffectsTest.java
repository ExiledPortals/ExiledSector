package exiledsector.skills;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.MockSector;
import exiledsector.MockShips;
import exiledsector.skills.skilleffect.SkillEffect;
import exiledsector.skills.skilleffect.defense.DefenseSkillEffect;
import exiledsector.persistence.ShipSkillDataManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static exiledsector.skills.TestTrees.node;
import static exiledsector.skills.TestTrees.type;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ClearedSkillTreeExtension.class)
class AllocatedSkillEffectsTest {

    private MockSector mockSector;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
    }

    private FleetMemberAPI memberWithHullSize(HullSize hullSize) {
        return MockShips.member("ship-a").hullSize(hullSize).build();
    }

    @Test
    void includesHullSizeScaledEffectsNotJustFlatEffects() {
        HullSizeSkillEffect hullSizeEffect = new HullSizeSkillEffect(DefenseSkillEffect.ARMOR_PERCENT, 5f, 10f, 15f, 20f);
        SkillNode node = node("hull_1", type("hull", SkillTier.SMALL)
                .effects(List.of(new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 10f)))
                .hullSizeEffects(List.of(hullSizeEffect))
                .build());

        FleetMemberAPI member = memberWithHullSize(HullSize.CRUISER);
        ShipTrees.allocate(ShipSkillDataManager.get("ship-a"), node, 1);

        List<SkillEffect> effects = AllocatedSkillEffects.forMember(member);

        assertTrue(effects.contains(DefenseSkillEffect.HULL_PERCENT));
        assertTrue(effects.contains(DefenseSkillEffect.ARMOR_PERCENT));
    }

    @Test
    void appliesTheOwnEffectsOfANodeBackedByAVanillaHullMod() {
        SkillNode node = node("hull_1", type("hull", SkillTier.SMALL)
                .effects(List.of(new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 10f)))
                .vanillaHullModId("some_vanilla_hullmod")
                .build());

        FleetMemberAPI member = memberWithHullSize(HullSize.CRUISER);
        ShipTrees.allocate(ShipSkillDataManager.get("ship-a"), node, 1);

        List<SkillEffect> effects = AllocatedSkillEffects.forMember(member);

        assertTrue(effects.contains(DefenseSkillEffect.HULL_PERCENT));
    }

    @Test
    void returnsAnEmptyListWhenNothingIsAllocated() {
        FleetMemberAPI member = memberWithHullSize(HullSize.FRIGATE);

        List<SkillEffect> effects = AllocatedSkillEffects.forMember(member);

        assertEquals(0, effects.size());
    }
}
