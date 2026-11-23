package exiledsector.skills;

import com.fs.starfarer.api.combat.ShipVariantAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(ClearedSkillTreeExtension.class)
class HullFormsTest {

    private SkillType adaptive;
    private SkillType abyssal;
    private SkillNode projectorNode;

    @BeforeEach
    void setUp() {
        adaptive = TestTrees.registered(TestTrees.type("adaptive_entropy_projector", SkillTier.NOTABLE).build());
        abyssal = TestTrees.registered(TestTrees.type("abyssal_entropy_projector", SkillTier.NOTABLE).build());
        projectorNode = TestTrees.node("adaptive_entropy_projector_1", adaptive);
    }

    private static ShipVariantAPI variantWith(String... hullModIds) {
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        for (String hullModId : hullModIds) {
            when(variant.hasHullMod(hullModId)).thenReturn(true);
        }
        return variant;
    }

    @Test
    void anOrdinaryHullGetsTheAdaptiveProjector() {
        ShipSkillData data = new ShipSkillData();
        HullForms.bindHull(data, variantWith());

        assertSame(adaptive, projectorNode.resolveEffectiveType(data));
    }

    @Test
    void anAbyssalGridOrRaphaelsGraceHullGetsTheAbyssalProjector() {
        ShipSkillData gridShip = new ShipSkillData();
        ShipSkillData graceShip = new ShipSkillData();

        assertEquals(Set.of("rat_abyssal_grid"), HullForms.bindHull(gridShip, variantWith("rat_abyssal_grid")));
        HullForms.bindHull(graceShip, variantWith("rat_raphaels_grace"));

        assertSame(abyssal, projectorNode.resolveEffectiveType(gridShip));
        assertSame(abyssal, projectorNode.resolveEffectiveType(graceShip));
    }

    @Test
    void losingTheAbyssalHullModFallsBackToTheAdaptiveProjector() {
        ShipSkillData data = new ShipSkillData();
        HullForms.bindHull(data, variantWith("rat_abyssal_grid"));
        HullForms.bindHull(data, variantWith());

        assertSame(adaptive, projectorNode.resolveEffectiveType(data));
    }

    @Test
    void aShipNeverBoundToAHullKeepsTheBaseType() {
        assertSame(adaptive, HullForms.formOf(adaptive, new ShipSkillData()));
        assertSame(adaptive, HullForms.formOf(adaptive, null));
    }

    @Test
    void everyFormOfATypeIsListedForNpcMatching() {
        SkillType other = TestTrees.type("other", SkillTier.SMALL).build();

        assertEquals(List.of(adaptive, abyssal), HullForms.withForms(adaptive));
        assertEquals(List.of(other), HullForms.withForms(other));
    }
}
