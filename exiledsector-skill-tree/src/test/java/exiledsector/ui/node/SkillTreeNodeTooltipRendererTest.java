package exiledsector.ui.node;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledlib.i18n.Style;
import exiledsector.skills.AllocationGate;
import exiledsector.skills.ModuleShareExtent;
import exiledsector.skills.NodeEligibility;
import exiledsector.skills.SkillItemCost;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SkillTreeNodeTooltipRendererTest {

    private static final SkillType WITH_AUTOMATED_FLAVOUR = new SkillType.Builder("plating", "Plating", "", SkillTier.SMALL)
            .flavour("The crew welds spare plating everywhere.")
            .flavourAutomated("The refit crew welds spare plating everywhere.")
            .build();

    private static FleetMemberAPI member(boolean automated) {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        ShipHullSpecAPI hullSpec = mock(ShipHullSpecAPI.class);
        when(member.getVariant()).thenReturn(variant);
        when(variant.getHullSpec()).thenReturn(hullSpec);
        when(variant.hasHullMod("automated")).thenReturn(automated);
        return member;
    }

    @Test
    void anAutomatedShipShowsTheAutomatedFlavour() {
        assertEquals("The refit crew welds spare plating everywhere.", SkillTreeNodeTooltipRenderer.flavourText(WITH_AUTOMATED_FLAVOUR, member(true)));
    }

    @Test
    void aCrewedShipShowsTheCrewFlavour() {
        assertEquals("The crew welds spare plating everywhere.", SkillTreeNodeTooltipRenderer.flavourText(WITH_AUTOMATED_FLAVOUR, member(false)));
    }

    @Test
    void withNoShipTheCrewFlavourShows() {
        assertEquals("The crew welds spare plating everywhere.", SkillTreeNodeTooltipRenderer.flavourText(WITH_AUTOMATED_FLAVOUR, null));
    }

    @Test
    void anAutomatedShipFallsBackToTheCrewFlavourWhenTheAutomatedTextIsEmpty() {
        SkillType crewOnly = new SkillType.Builder("plating", "Plating", "", SkillTier.SMALL)
                .flavour("The crew welds spare plating everywhere.")
                .flavourAutomated("")
                .build();

        assertEquals("The crew welds spare plating everywhere.", SkillTreeNodeTooltipRenderer.flavourText(crewOnly, member(true)));
    }

    @Test
    void anUnpaidCostIsGreenWhenCargoCoversItAndOrangeWhenItDoesNot() {
        SkillItemCost cost = new SkillItemCost("alpha_core", 2f);

        assertEquals(Style.GOOD, SkillTreeNodeTooltipRenderer.unpaidCostLine(cost, 2f).style());
        assertEquals(Style.HIGHLIGHT, SkillTreeNodeTooltipRenderer.unpaidCostLine(cost, 1f).style());
        assertEquals("Costs 2 alpha_core to allocate, returned when unallocated.", SkillTreeNodeTooltipRenderer.unpaidCostLine(cost, 0f).text().plain());
    }

    @Test
    void aPaidCostKeepsTheBodyColourAndAFreeNodeShowsNoCost() {
        assertNull(SkillTreeNodeTooltipRenderer.paidCostLine(new SkillItemCost("alpha_core", 1f)).style());
        assertNull(SkillTreeNodeTooltipRenderer.paidCostLine(null));
    }

    @Test
    void theCostLineReplacesAnItemCostRefusalButNotOthers() {
        SkillTreeNodeTooltipRenderer.ItemCostLine costLine = SkillTreeNodeTooltipRenderer.unpaidCostLine(new SkillItemCost("alpha_core", 1f), 0f);
        AllocationGate.Verdict itemCost = new AllocationGate.Verdict(AllocationGate.Refusal.INELIGIBLE,
                new NodeEligibility.Block(NodeEligibility.Kind.ITEM_COST, "alpha_core", null, new SkillItemCost("alpha_core", 1f)));
        AllocationGate.Verdict outOfOp = new AllocationGate.Verdict(AllocationGate.Refusal.OUT_OF_OP, null);

        assertNull(SkillTreeNodeTooltipRenderer.withoutItemCostRefusal("Requires 1 alpha_core (have 0).", costLine, () -> itemCost));
        assertEquals("Not enough OP.", SkillTreeNodeTooltipRenderer.withoutItemCostRefusal("Not enough OP.", costLine, () -> outOfOp));
        assertEquals("Requires 1 alpha_core (have 0).", SkillTreeNodeTooltipRenderer.withoutItemCostRefusal("Requires 1 alpha_core (have 0).", null,
                () -> itemCost));
    }

    @Test
    void theModuleShareLineNamesTheShareOrSaysNothingIsShared() {
        assertEquals("Modules get 50% of these effects.", SkillTreeNodeTooltipRenderer.moduleShareLine(ModuleShareExtent.ALL, 50).plain());
        assertEquals("Modules get 100% of some of these effects.", SkillTreeNodeTooltipRenderer.moduleShareLine(ModuleShareExtent.SOME, 100).plain());
        assertEquals("Not shared with modules.", SkillTreeNodeTooltipRenderer.moduleShareLine(ModuleShareExtent.NONE, 50).plain());
        assertEquals("Not shared with modules.", SkillTreeNodeTooltipRenderer.moduleShareLine(ModuleShareExtent.ALL, 0).plain());
        assertNull(SkillTreeNodeTooltipRenderer.moduleShareLine(null, 50));
    }
}
