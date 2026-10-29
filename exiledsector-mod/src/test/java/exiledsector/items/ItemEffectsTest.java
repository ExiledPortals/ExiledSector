package exiledsector.items;

import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;
import exiledsector.modifiers.ModifierEffect;
import exiledsector.skills.skilleffect.SkillEffect;
import exiledsector.items.ItemDataIntegrity.Severity;
import exiledsector.items.ItemDataIntegrity.Violation;
import exiledsector.modifiers.ModifierEffects;
import exiledsector.modifiers.ModifierLine;
import org.json.JSONArray;
import org.json.JSONException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemEffectsTest {

    private static final String PLUNDER = "PLUNDER_PERCENT";
    private static final String OLD_PLUNDER = "LOOT_PERCENT";

    private record PlunderEffect(String name) implements ModifierEffect {

        @Override
        public StyledText description(float magnitude) {
            return StyledText.of("Plunder ").append(StyledText.styled("+" + Math.round(magnitude) + "%", Style.GOOD));
        }
    }

    private static final class PlunderEffects implements ModifierEffects {

        private static final Map<String, String> CURRENT_NAMES = Map.of(PLUNDER, PLUNDER, OLD_PLUNDER, PLUNDER);

        @Override
        public ModifierEffect byName(String effectName) {
            String currentName = CURRENT_NAMES.get(effectName);
            return currentName == null ? null : new PlunderEffect(currentName);
        }
    }

    private final ModifierEffects plunderEffects = new PlunderEffects();
    private final ItemStore store = new ItemStore();

    @AfterEach
    void restore() {
        ItemEffects.register(ItemType.HULL_ITEM, ItemEffects.SKILL_EFFECTS);
        ItemDefinitions.clear();
        ItemModifiers.clear();
    }

    @Test
    void shipItemTypesResolveTheirEffectsThroughTheSkillEffects() {
        assertSame(ItemEffects.SKILL_EFFECTS, ItemEffects.forType(ItemType.SUBROUTINE));
        assertSame(ItemEffects.SKILL_EFFECTS, ItemEffects.forType(ItemType.HULL_ITEM));
        assertSame(ItemEffects.SKILL_EFFECTS, ItemEffects.forType(null));
        assertSame(ItemEffects.SKILL_EFFECTS, ItemEffects.forTypes(Set.of(ItemType.SUBROUTINE, ItemType.HULL_ITEM)));
        assertSame(ItemEffects.SKILL_EFFECTS, ItemEffects.forTypes(Set.of()));
        assertSame(SkillEffect.byName("ARMOR_PERCENT"), ItemEffects.SKILL_EFFECTS.byName("ARMOR_PERCENT"));
        assertEquals("ARMOR_PERCENT", ItemEffects.SKILL_EFFECTS.currentName("ARMOR_PERCENT"));
        assertNull(ItemEffects.SKILL_EFFECTS.byName("NOT_AN_EFFECT"));
        assertNull(ItemEffects.SKILL_EFFECTS.byName(null));
        assertEquals("NOT_AN_EFFECT", new RolledEffect("NOT_AN_EFFECT", 1f).effectName());
    }

    @Test
    void aTypeWithItsOwnResolverLoadsItsModifiersThroughIt() throws Exception {
        ItemEffects.register(ItemType.HULL_ITEM, plunderEffects);
        ItemModifiers.register(new JSONArray()
                .put(HullItemRows.modifierRow(PLUNDER, "prefix", "hull_item", OLD_PLUNDER + ":10:20", "Plundering"))
                .put(HullItemRows.modifierRow("ARMOR_PERCENT", "prefix", "hull_item", "ARMOR_PERCENT:5:10", "Plated"))
                .put(HullItemRows.modifierRow("HULL_MULT", "prefix", "subroutine", "HULL_MULT:4:6", "Reinforced"))
                .put(HullItemRows.modifierRow("HULL_PERCENT", "prefix", HullItemRows.EVERY_ITEM_TYPE, "HULL_PERCENT:4:6", "Sturdy")));

        assertEquals(PLUNDER, ItemModifiers.get(PLUNDER).line().effectName());
        assertNull(ItemModifiers.get("ARMOR_PERCENT"), "a skill effect is not an effect of the hull item resolver");
        assertNotNull(ItemModifiers.get("HULL_MULT"), "subroutines still resolve skill effects");
        assertNull(ItemModifiers.get("HULL_PERCENT"), "one modifier cannot span two resolvers");
        assertThrows(IllegalArgumentException.class, () -> ItemEffects.forTypes(Set.of(ItemType.SUBROUTINE, ItemType.HULL_ITEM)));
    }

    @Test
    void anItemDescribesAndAppliesItsEffectsThroughItsTypesResolver() throws Exception {
        ItemEffects.register(ItemType.HULL_ITEM, plunderEffects);
        ItemDefinitions.register(new JSONArray().put(HullItemRows.row("map", "reactor", OLD_PLUNDER + ":10:20")));
        Item item = store.add(HullItemRows.data("map", 1L, List.of(new RolledEffect(OLD_PLUNDER, 15f)), null));

        assertSame(plunderEffects, item.modifierEffects());
        assertEquals(PLUNDER, item.effects().get(0).effectName());
        assertEquals(List.of("Plunder +15%"), item.effectLines().stream().map(StyledText::plain).toList());
        assertTrue(item.effectLines(true).get(0).plain().startsWith("Plunder +15%"));
        assertTrue(item.skillEffects().isEmpty(), "an effect that is not a ship skill effect never reaches a ship");
        assertEquals("Plunder +10/20%", new RolledEffect(PLUNDER, 10f)
                .rangeDescription(plunderEffects, ModifierLine.perTierBand(PLUNDER, List.of(1, 6), List.of(10f, 20f))).plain());
    }

    @Test
    void theDataCheckValidatesNamesAgainstTheModifiersOwnResolver() throws JSONException {
        JSONArray modifiers = new JSONArray()
                .put(HullItemRows.modifierRow(PLUNDER, "prefix", "hull_item", PLUNDER + ":10:20", "Plundering"))
                .put(HullItemRows.modifierRow("PLUNDER_PERCENT_OLD", "prefix", "hull_item", OLD_PLUNDER + ":10:20", "Raiding"));
        JSONArray hullItems = new JSONArray().put(HullItemRows.row("map", "reactor", "@" + PLUNDER + "; @PLUNDER_PERCENT_OLD"));

        List<String> skillErrors = errors(modifiers, hullItems);
        ItemEffects.register(ItemType.HULL_ITEM, plunderEffects);
        List<String> plunderErrors = errors(modifiers, hullItems);

        assertTrue(skillErrors.contains(PLUNDER + " uses " + PLUNDER + ", which is not a skill effect"), skillErrors.toString());
        assertTrue(plunderErrors.stream().noneMatch(error -> error.contains("which is not a skill effect")), plunderErrors.toString());
        assertTrue(plunderErrors.contains("PLUNDER_PERCENT_OLD uses the old effect name " + OLD_PLUNDER + "; use " + PLUNDER),
                plunderErrors.toString());
    }

    @Test
    void registeringAResolverReturnsTheOneItReplaced() {
        assertSame(ItemEffects.SKILL_EFFECTS, ItemEffects.register(ItemType.HULL_ITEM, plunderEffects));
        assertSame(plunderEffects, ItemEffects.register(ItemType.HULL_ITEM, ItemEffects.SKILL_EFFECTS));
        assertThrows(IllegalArgumentException.class, () -> ItemEffects.register(null, plunderEffects));
        assertThrows(IllegalArgumentException.class, () -> ItemEffects.register(ItemType.HULL_ITEM, null));
    }

    private static List<String> errors(JSONArray modifiers, JSONArray hullItems) throws JSONException {
        return ItemDataChecks.check(modifiers, new JSONArray(), hullItems, new JSONArray()).stream()
                .filter(violation -> violation.severity() == Severity.ERROR).map(Violation::message).toList();
    }
}
