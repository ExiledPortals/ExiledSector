package exiledsector.socketables;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.i18n.StyledText;
import exiledsector.skills.SkillTypeEffect;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static exiledsector.socketables.SocketableFixtures.row;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SocketableHullSizeValuesTest {

    private static final String SCALED_POOL = "ARMOR_FLAT:5/10/15/20:2; HULL_MULT:4:6";

    @AfterEach
    void tearDown() {
        SocketableDefinitions.clear();
    }

    @Test
    void aSlashSeparatedValueGivesEachHullSizeItsOwnFixedValue() throws Exception {
        SocketableDefinitions.register(new JSONArray().put(row("scaled", "subroutine", SCALED_POOL + "; SHIELD_ARC_FLAT:-1/-2.5/3/4")));

        SocketableDefinition definition = SocketableDefinitions.get("scaled");
        SocketableDefinition.PoolEntry armor = definition.rollRange("ARMOR_FLAT");
        assertEquals(2f, armor.weight());
        assertEquals(List.of(5f, 10f, 15f, 20f), armor.hullValues());
        assertEquals(armor.min(), armor.max());
        assertEquals(List.of(-1f, -2.5f, 3f, 4f), definition.rollRange("SHIELD_ARC_FLAT").hullValues());
        assertEquals(1f, definition.rollRange("SHIELD_ARC_FLAT").weight());
        assertFalse(definition.rollRange("HULL_MULT").scalesWithHullSize());
    }

    @Test
    void badHullValuesSkipTheDefinition() throws Exception {
        SocketableDefinitions.register(new JSONArray()
                .put(row("three_values", "subroutine", "ARMOR_FLAT:5/10/15"))
                .put(row("five_values", "subroutine", "ARMOR_FLAT:5/10/15/20/25"))
                .put(row("empty_value", "subroutine", "ARMOR_FLAT:5//15/20"))
                .put(row("not_a_number", "subroutine", "ARMOR_FLAT:5/ten/15/20"))
                .put(row("extra_field", "subroutine", "ARMOR_FLAT:5/10/15/20:1:2"))
                .put(row("zero_weight", "subroutine", "ARMOR_FLAT:5/10/15/20:0")));

        assertEquals(0, SocketableDefinitions.all().size());
    }

    @Test
    void eachHullSizeGetsItsOwnValueWhateverWasStored() throws Exception {
        SocketableDefinitions.register(new JSONArray().put(row("scaled", "subroutine", SCALED_POOL)));
        Socketable socketable = SocketableKind.SUBROUTINE.create("socketable_1", "scaled", 1L,
                List.of(new RolledEffect("ARMOR_FLAT", 3f), new RolledEffect("HULL_MULT", 5f)));

        assertEquals(List.of(5f, 5f), magnitudes(socketable.skillEffects(HullSize.FRIGATE)));
        assertEquals(List.of(10f, 5f), magnitudes(socketable.skillEffects(HullSize.DESTROYER)));
        assertEquals(List.of(15f, 5f), magnitudes(socketable.skillEffects(HullSize.CRUISER)));
        assertEquals(List.of(20f, 5f), magnitudes(socketable.skillEffects(HullSize.CAPITAL_SHIP)));
        assertEquals(List.of(3f, 5f), magnitudes(socketable.skillEffects(HullSize.FIGHTER)));
    }

    @Test
    void aNewRollStoresTheFrigateValue() throws Exception {
        SocketableDefinitions.register(new JSONArray().put(row("scaled", "subroutine", "ARMOR_FLAT:5/10/15/20")));

        assertEquals(List.of(new RolledEffect("ARMOR_FLAT", 5f)), SocketableRoller.rollCommon(SocketableDefinitions.get("scaled"), 7L));
    }

    @Test
    void theGenericTooltipListsEveryHullSizeValueLikeVanilla() throws Exception {
        SocketableDefinitions.register(new JSONArray().put(row("scaled", "subroutine", SCALED_POOL)));
        Socketable socketable = SocketableKind.SUBROUTINE.create("socketable_2", "scaled", 1L,
                List.of(new RolledEffect("ARMOR_FLAT", 5f), new RolledEffect("HULL_MULT", 5f)));

        List<StyledText> lines = socketable.effectLines(true);

        assertEquals(2, lines.size());
        assertTrue(lines.get(0).plain().contains("5/10/15/20"), lines.get(0).plain());
        StyledText.Span valueSpan = lines.get(0).spans().get(0);
        assertTrue(lines.get(0).plain().substring(valueSpan.start(), valueSpan.end()).contains("5/10/15/20"));
    }

    @Test
    void onAShipTheTooltipShowsOnlyThatHullSizesValueWithNoRange() throws Exception {
        SocketableDefinitions.register(new JSONArray().put(row("scaled", "subroutine", SCALED_POOL)));
        Socketable socketable = SocketableKind.SUBROUTINE.create("socketable_3", "scaled", 1L, List.of(new RolledEffect("ARMOR_FLAT", 5f)));

        String expanded = socketable.effectLines(true, HullSize.CAPITAL_SHIP).get(0).plain();

        assertTrue(expanded.contains("20"), expanded);
        assertFalse(expanded.contains("/") || expanded.contains("("), expanded);
    }

    @Test
    void sharedUnitsAreWrittenOnceAroundTheJoinedValues() {
        assertEquals("5/10/15/20%", RolledEffect.joinSharingAffixes(List.of("5%", "10%", "15%", "20%")));
        assertEquals("+5/10/15/20", RolledEffect.joinSharingAffixes(List.of("+5", "+10", "+15", "+20")));
        assertEquals("0.5x/1x/2/3", RolledEffect.joinSharingAffixes(List.of("0.5x", "1x", "2", "3")));
    }

    private static List<Float> magnitudes(List<SkillTypeEffect> effects) {
        return effects.stream().map(SkillTypeEffect::magnitude).toList();
    }
}
