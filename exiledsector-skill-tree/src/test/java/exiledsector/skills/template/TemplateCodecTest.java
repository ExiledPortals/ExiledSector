package exiledsector.skills.template;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TemplateCodecTest {

    private static final SkillTreeTemplate TEMPLATE = new SkillTreeTemplate("id-1", "Brawler \"v2\", =final= 装甲", "root_low_tech_1",
            HullSize.CRUISER, List.of(new TemplateStep("armor_1", null), new TemplateStep("slot_3", "hull"), new TemplateStep("flux_2", null)));

    @Test
    void aTemplateSurvivesARoundTripWithItsNameOrderAndOptions() {
        assertEquals(TEMPLATE, TemplateCodec.decode(TemplateCodec.encode(TEMPLATE)));
    }

    @Test
    void aTemplateWithNoHullSizeRoundTripsWithoutOne() {
        SkillTreeTemplate noHull = new SkillTreeTemplate("id-2", "Any", "root_midline_1", null, List.of());

        assertEquals(noHull, TemplateCodec.decode(TemplateCodec.encode(noHull)));
    }

    @Test
    void malformedTextOrMissingIdentityDecodesToNothing() {
        assertNull(TemplateCodec.decode(null));
        assertNull(TemplateCodec.decode("not json"));
        assertNull(TemplateCodec.decode("{\"v\":1,\"name\":\"x\",\"root\":\"r\",\"steps\":[]}"));
        assertNull(TemplateCodec.decode("{\"v\":1,\"id\":\"a\",\"root\":\"r\",\"steps\":[]}"));
        assertNull(TemplateCodec.decode("{\"v\":1,\"id\":\"a\",\"name\":\"  \",\"root\":\"r\"}"));
    }

    @Test
    void anUnknownHullSizeFutureFieldsAndBlankStepsAreTolerated() {
        SkillTreeTemplate decoded = TemplateCodec.decode("{\"v\":2,\"id\":\"a\",\"name\":\"n\",\"root\":\"r\",\"hull\":\"TITAN\","
                + "\"extra\":true,\"steps\":[\"unknown_node\",\" \",\"=orphan\",\"slot=\"]}");

        assertNull(decoded.hullSize());
        assertEquals(List.of(new TemplateStep("unknown_node", null), new TemplateStep("slot", null)), decoded.steps());
    }

    @Test
    void nodeIdsAreTheRootPlusEveryStep() {
        assertEquals(Set.of("root_low_tech_1", "armor_1", "slot_3", "flux_2"), TEMPLATE.nodeIds());
    }
}
