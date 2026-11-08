package exiledsector.skills;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.MockSector;
import exiledsector.skills.skilleffect.defense.DefenseSkillEffect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntegratedNodesTest {

    private static final SkillTypeEffect NORMAL_HULL = new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 10f);
    private static final SkillTypeEffect INTEGRATED_HULL = new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 20f);

    private MockSector mockSector;
    private SkillNode notable;
    private SkillNode plain;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        SkillTree.clearNodes();
        SkillTree.clearTypes();
        SkillType integrable = new SkillType.Builder("bulkheads", "Bulkheads", "a.png", SkillTier.NOTABLE)
                .effects(List.of(NORMAL_HULL))
                .integratedEffects(new IntegratedEffects(List.of(INTEGRATED_HULL),
                        List.of(new HullSizeSkillEffect(DefenseSkillEffect.ARMOR_FLAT, 0f, 0f, 50f, 100f))))
                .build();
        SkillType ordinary = new SkillType.Builder("hull", "Hull", "a.png", SkillTier.SMALL).effects(List.of(NORMAL_HULL)).build();
        notable = new SkillNode("bulkheads_1", integrable, List.of(), 0f, 0f);
        plain = new SkillNode("hull_1", ordinary, List.of(), 0f, 0f);
        SkillTree.register(notable);
        SkillTree.register(plain);
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
        SkillTree.clearNodes();
        SkillTree.clearTypes();
    }

    private ShipSkillData withNotable() {
        ShipSkillData data = new ShipSkillData();
        ShipTrees.allocate(data, notable, 1);
        ShipTrees.allocate(data, plain, 1);
        return data;
    }

    @Test
    void anIntegratedNodeUsesItsIntegratedEffectsInsteadOfItsNormalOnes() {
        ShipSkillData data = withNotable();
        AllocatedNode allocated = new AllocatedNode(notable, notable.getType());
        assertEquals(List.of(NORMAL_HULL), AllocatedSkillEffects.treeEffects(data, allocated, HullSize.CRUISER));

        data.integrate(notable.getId());

        assertEquals(List.of(INTEGRATED_HULL, new SkillTypeEffect(DefenseSkillEffect.ARMOR_FLAT, 50f)),
                AllocatedSkillEffects.treeEffects(data, allocated, HullSize.CRUISER));
        assertEquals(List.of(INTEGRATED_HULL), AllocatedSkillEffects.treeEffects(data, allocated, HullSize.FRIGATE));
    }

    @Test
    void onlyAnAllocatedIntegrableNodeThatIsNotYetIntegratedCanBeIntegrated() {
        ShipSkillData data = new ShipSkillData();
        assertFalse(IntegratedNodes.canIntegrate(data, notable));

        ShipTrees.allocate(data, notable, 1);
        ShipTrees.allocate(data, plain, 1);
        assertTrue(IntegratedNodes.canIntegrate(data, notable));
        assertFalse(IntegratedNodes.canIntegrate(data, plain));

        data.integrate(notable.getId());
        assertFalse(IntegratedNodes.canIntegrate(data, notable));
    }

    @Test
    void eachIntegrationCostsOneMoreStoryPointAndUnallocatingKeepsIt() {
        ShipSkillData data = withNotable();
        assertEquals(1, IntegratedNodes.nextCost(data));

        data.integrate(notable.getId());
        ShipTrees.deallocate(data, notable);

        assertTrue(data.isIntegrated(notable.getId()));
        assertEquals(2, IntegratedNodes.nextCost(data));
        assertFalse(data.isBlank());
    }

    @Test
    void refundsTheMostExpensiveIntegrationsFirst() {
        assertEquals(0, IntegratedNodes.costOfLast(3, 0));
        assertEquals(3, IntegratedNodes.costOfLast(3, 1));
        assertEquals(5, IntegratedNodes.costOfLast(3, 2));
        assertEquals(6, IntegratedNodes.costOfLast(3, 3));
    }

    @Test
    void forgetsIntegrationsWhoseNodeIsGoneOrNoLongerIntegrable() {
        ShipSkillData data = withNotable();
        data.integrate(notable.getId());
        data.integrate(plain.getId());
        data.integrate("removed_1");

        int refund = IntegratedNodes.forgetInapplicable(data, Map.of(notable.getId(), notable, plain.getId(), plain));

        assertEquals(5, refund);
        assertEquals(Set.of(notable.getId()), data.getIntegratedNodeIds());
    }

    @Test
    void renamingANodeKeepsItIntegrated() {
        ShipSkillData data = withNotable();
        data.integrate(notable.getId());

        assertTrue(ShipTrees.renameNode(data, notable.getId(), "bulkheads_2"));

        assertEquals(Set.of("bulkheads_2"), data.getIntegratedNodeIds());
    }

    @Test
    void replacingANodeKeepsItIntegrated() {
        ShipSkillData data = withNotable();
        data.integrate(notable.getId());

        assertTrue(ShipTrees.replaceNode(data, notable.getId(), "bulkheads_2"));

        assertEquals(Set.of("bulkheads_2"), data.getIntegratedNodeIds());
    }

    @Test
    void aPlanningCopyShowsTheShipsIntegrations() {
        ShipSkillData data = withNotable();
        data.integrate(notable.getId());

        ShipSkillData plan = ShipTrees.planningCopy(data);
        plan.integrate(plain.getId());

        assertTrue(plan.isIntegrated(notable.getId()));
        assertEquals(Set.of(notable.getId()), data.getIntegratedNodeIds());
    }
}
