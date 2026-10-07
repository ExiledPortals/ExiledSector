package exiledsector.skills.skilleffect;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EffectAliasesTest {

    @AfterEach
    void tearDown() {
        EffectAliases.clear();
    }

    private static JSONObject row(String alias, String effect) throws Exception {
        return new JSONObject().put("alias", alias).put("effect", effect);
    }

    @Test
    void anAliasResolvesToItsCurrentEffect() throws Exception {
        EffectAliases.register(new JSONArray().put(row("OLD_HULL_MULT", "HULL_MULT")));

        assertSame(SkillEffect.byName("HULL_MULT"), SkillEffect.byName("OLD_HULL_MULT"));
    }

    @Test
    void anAliasToANameThatIsNotAnEffectIsSkipped() throws Exception {
        EffectAliases.register(new JSONArray().put(row("OLD_THING", "NOT_AN_EFFECT")));

        assertNull(EffectAliases.currentName("OLD_THING"));
        assertThrows(IllegalArgumentException.class, () -> SkillEffect.byName("OLD_THING"));
    }

    @Test
    void anAliasThatIsStillAnEffectOfItsOwnIsSkipped() throws Exception {
        EffectAliases.register(new JSONArray().put(row("ARMOR_PERCENT", "HULL_MULT")));

        assertEquals("ARMOR_PERCENT", SkillEffect.byName("ARMOR_PERCENT").name());
        assertNull(EffectAliases.currentName("ARMOR_PERCENT"));
    }

    @Test
    void blankRowsAndAMissingFileLeaveNoAliases() throws Exception {
        assertDoesNotThrow(() -> EffectAliases.register(null));
        EffectAliases.register(new JSONArray().put(row("", "HULL_MULT")).put(row("OLD", "")));

        assertNull(EffectAliases.currentName(""));
        assertNull(EffectAliases.currentName("OLD"));
    }
}
