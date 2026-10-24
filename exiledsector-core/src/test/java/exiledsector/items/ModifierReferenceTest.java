package exiledsector.items;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModifierReferenceTest {

    @Test
    void aBareReferenceFitsEveryShape() {
        ModifierReference reference = ModifierReference.parse(" @ARMOR_PERCENT ");

        assertEquals("ARMOR_PERCENT", reference.id());
        assertNull(reference.weightText());
        assertNull(reference.tierText());
        assertTrue(reference.hasShape(false, false));
    }

    @Test
    void aWeightAndATierRangeAreReadInEitherCombination() {
        ModifierReference weighted = ModifierReference.parse("@ARMOR_PERCENT:5");
        ModifierReference gated = ModifierReference.parse("@ARMOR_PERCENT:t3-6");
        ModifierReference both = ModifierReference.parse("@ARMOR_PERCENT:5:t3+");

        assertEquals("5", weighted.weightText());
        assertTrue(weighted.hasShape(true, false));
        assertFalse(weighted.hasShape(false, true));
        assertEquals("t3-6", gated.tierText());
        assertNull(gated.weightText());
        assertTrue(gated.hasShape(false, true));
        assertFalse(gated.hasShape(true, false));
        assertEquals("5", both.weightText());
        assertEquals("t3+", both.tierText());
        assertTrue(both.hasShape(true, true));
    }

    @Test
    void extraFieldsOrAnEmptyWeightNeverFit() {
        assertFalse(ModifierReference.parse("@ARMOR_PERCENT:5:6").hasShape(true, true));
        assertFalse(ModifierReference.parse("@ARMOR_PERCENT:5:t3:t4").hasShape(true, true));
        assertFalse(ModifierReference.parse("@ARMOR_PERCENT:").hasShape(false, false));
    }
}
