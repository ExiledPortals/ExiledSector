package exiledsector.i18n;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogueTest {

    private static final Map<String, String> ENGLISH = Map.of(
            "greeting", "Hello {name}",
            "nodes.one", "{count} node",
            "nodes.other", "{count} nodes",
            "only.english", "Only English",
            "format.list.sep", ", ");
    private static final Map<String, String> CHINESE = Map.of(
            "greeting", "你好{name}",
            "nodes.other", "{count}个节点",
            "format.list.sep", "、");

    @Test
    void higherLayersOverrideLowerOnesKeyByKey() {
        I18n.install(Catalogue.layered("zh_CN", List.of(ENGLISH, CHINESE)));

        assertEquals("你好舰长", Translation.msg("greeting").arg("name", "舰长").text());
        assertEquals("Only English", Translation.text("only.english"));
    }

    @Test
    void missingKeysRenderAsVisibleMarkers() {
        I18n.install(new Catalogue("en", ENGLISH));

        assertEquals("[[no.such.key]]", Translation.text("no.such.key"));
        assertFalse(Translation.has("no.such.key"));
        assertTrue(Translation.has("greeting"));
    }

    @Test
    void englishPicksOneOrOtherByCount() {
        I18n.install(new Catalogue("en", ENGLISH));

        assertEquals("1 node", Translation.msg("nodes").count(1).text());
        assertEquals("2 nodes", Translation.msg("nodes").count(2).text());
        assertEquals("0 nodes", Translation.msg("nodes").count(0).text());
    }

    @Test
    void chineseAlwaysUsesOtherEvenForOne() {
        I18n.install(Catalogue.layered("zh_CN", List.of(ENGLISH, CHINESE)));

        assertEquals("1个节点", Translation.msg("nodes").count(1).text());
    }

    @Test
    void listsUseTheLocaleSeparator() {
        I18n.install(Catalogue.layered("zh_CN", List.of(ENGLISH, CHINESE)));

        StyledText list = Translation.list(List.of(StyledText.of("甲"), StyledText.styled("乙", Style.NODE)));

        assertEquals("甲、<node>乙</node>", list.toMarkup());
    }

    @Test
    void dataTextFallsBackToTheEnglishSourceAndParsesItsMarkup() {
        I18n.install(Catalogue.layered("zh_CN", List.of(ENGLISH, Map.of("skillType.armor.name", "装甲"))));

        assertEquals("装甲", Translation.data("skillType.armor.name", "Armor"));
        assertEquals("Hull", Translation.data("skillType.hull.name", "Hull"));
        assertEquals("<good>More</good> armor", Translation.dataStyled("skillType.x.description", "<good>More</good> armor").toMarkup());
    }

    @Test
    void thePseudoLocaleWrapsTextButLeavesMetadataUntouched() {
        Catalogue pseudo = Catalogue.compose(PseudoLocale.LOCALE, locale -> locale.equals("en")
                ? Map.of("meta.font", "graphics/fonts/orbitron20aabold.fnt", "meta.fakeBold", "true", "a", "Text")
                : Map.of());

        assertEquals("graphics/fonts/orbitron20aabold.fnt", pseudo.raw("meta.font"));
        assertEquals("true", pseudo.raw("meta.fakeBold"));
        assertEquals("[Text~]", pseudo.raw("a"));
    }

    @Test
    void englishAndThePseudoLocaleLeaveTheFontSettingsToTheCodeDefaults() {
        for (Catalogue catalogue : List.of(RealCatalogue.english(), RealCatalogue.of(PseudoLocale.LOCALE))) {
            assertFalse(catalogue.has("meta.font"));
            assertFalse(catalogue.has("meta.fakeBold"));
        }
    }

    @Test
    void numbersFormatTheSameInEveryLocale() {
        I18n.install(Catalogue.layered("zh_CN", List.of(Map.of("value", "{value}"))));

        assertEquals("12.5", Translation.msg("value").arg("value", 12.5f).text());
        assertEquals("10", Translation.msg("value").arg("value", 10f).text());
    }
}
