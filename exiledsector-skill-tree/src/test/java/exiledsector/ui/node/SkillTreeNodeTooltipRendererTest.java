package exiledsector.ui.node;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
