package exiledsector.skills.npc;

import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.layout.SkillNodeDecoration;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcLayoutValidatorTest {

    private static final String ROOT = "root";

    @BeforeEach
    void setUp() {
        RealSkillData.clear();
        node(ROOT, type("root_type", SkillTier.ROOT), List.of(), List.of("inner"));
    }

    @AfterEach
    void tearDown() {
        RealSkillData.clear();
    }

    private static SkillType type(String id, SkillTier tier, String... tags) {
        SkillType type = new SkillType.Builder(id, id + " name", "a.png", tier).tags(List.of(tags)).build();
        SkillTree.registerType(type);
        return type;
    }

    private static SkillType optionalType(String id, String... options) {
        SkillType type = new SkillType.Builder(id, id + " name", "a.png", SkillTier.SMALL).optionalOptionIds(List.of(options)).build();
        SkillTree.registerType(type);
        return type;
    }

    private static void node(String id, SkillType type, List<String> connectedTo, List<String> tags) {
        SkillTree.register(new SkillNode(id, type, connectedTo, 0f, 0f, SkillNodeDecoration.NONE, tags));
    }

    private static void innerNode(String id, String... connectedTo) {
        node(id, type(id + "_type", SkillTier.SMALL), List.of(connectedTo), List.of("inner"));
    }

    private static NpcLayout layout(List<String> requires, NpcLayoutEntry... entries) {
        return new NpcLayout("layout", "Layout", ROOT, requires, "", List.of(entries));
    }

    private static NpcLayoutEntry entry(String nodeId) {
        return new NpcLayoutEntry(nodeId, null);
    }

    private static List<String> problems(NpcLayout layout) {
        return NpcLayoutValidator.staticProblems(layout);
    }

    private static void assertSingleProblemContaining(NpcLayout layout, String fragment) {
        List<String> problems = problems(layout);
        assertEquals(1, problems.size(), problems.toString());
        assertTrue(problems.get(0).contains(fragment), problems.get(0));
    }

    @Test
    void aValidLayoutHasNoStaticProblems() {
        innerNode("a", ROOT);
        innerNode("b", "a");

        assertTrue(problems(layout(List.of("req_shields"), entry("a"), entry("b"))).isEmpty());
    }

    @Test
    void flagsAnUnknownOrNonRootRoot() {
        innerNode("a", ROOT);

        List<String> unknownRoot = problems(new NpcLayout("l", "L", "ghost", List.of(), "", List.of(entry("a"))));
        List<String> smallRoot = problems(new NpcLayout("l", "L", "a", List.of(), "", List.of()));

        assertTrue(unknownRoot.get(0).contains("unknown root node id ghost"), unknownRoot.toString());
        assertTrue(smallRoot.get(0).contains("not a ROOT node"), smallRoot.toString());
    }

    @Test
    void flagsAnUnknownNodeId() {
        assertSingleProblemContaining(layout(List.of(), entry("ghost")), "unknown node id");
    }

    @Test
    void flagsWormholeEntries() {
        node("wormhole_1", type("wormhole", SkillTier.WORMHOLE), List.of(ROOT), List.of("inner"));

        assertSingleProblemContaining(layout(List.of(), entry("wormhole_1")), "wormhole");
    }

    @Test
    void flagsEntriesOutsideTheInnerRegion() {
        node("pirate_1", type("pirate", SkillTier.SMALL), List.of(ROOT), List.of("pirate"));

        assertSingleProblemContaining(layout(List.of(), entry("pirate_1")), "not in region inner");
    }

    @Test
    void flagsCampaignOnlyAndPlayerOnlyNodes() {
        node("logistics_1", type("logistics", SkillTier.SMALL, "campaign_only"), List.of(ROOT), List.of("inner"));
        node("flagship_1", type("flagship", SkillTier.SMALL), List.of(ROOT), List.of("inner", "player_only"));

        List<String> problems = problems(layout(List.of(), entry("logistics_1"), entry("flagship_1")));

        assertEquals(2, problems.size(), problems.toString());
        assertTrue(problems.get(0).contains("campaign_only"));
        assertTrue(problems.get(1).contains("player_only"));
    }

    @Test
    void flagsCampaignOnlyOptions() {
        type("cargo", SkillTier.SMALL, "campaign_only");
        node("optional_1", optionalType("optional", "cargo"), List.of(ROOT), List.of("inner"));

        assertSingleProblemContaining(layout(List.of(), new NpcLayoutEntry("optional_1", "cargo")), "campaign_only");
    }

    @Test
    void flagsOptionalNodesWithoutAValidOptionAndOptionsOnNonOptionalNodes() {
        type("caps", SkillTier.SMALL);
        type("stranger", SkillTier.SMALL);
        node("optional_1", optionalType("optional", "caps"), List.of(ROOT), List.of("inner"));
        node("optional_2", optionalType("optional_b", "caps"), List.of(ROOT), List.of("inner"));
        innerNode("a", ROOT);

        List<String> problems = problems(layout(List.of(),
                entry("optional_1"), new NpcLayoutEntry("optional_2", "stranger"), new NpcLayoutEntry("a", "caps")));

        assertEquals(3, problems.size(), problems.toString());
        assertTrue(problems.get(0).contains("without an option"));
        assertTrue(problems.get(1).contains("stranger"));
        assertTrue(problems.get(2).contains("not an optional node"));
    }

    @Test
    void flagsEntriesNotConnectedToTheRootOrAnEarlierEntry() {
        innerNode("a", ROOT);
        innerNode("b", "a");

        assertSingleProblemContaining(layout(List.of(), entry("b"), entry("a")), "not connected");
    }

    @Test
    void flagsDuplicateEntriesAndTheRootAsAnEntry() {
        innerNode("a", ROOT);

        List<String> problems = problems(layout(List.of(), entry(ROOT), entry("a"), entry("a")));

        assertEquals(3, problems.size(), problems.toString());
        assertTrue(problems.get(0).contains("repeats the layout root"));
        assertTrue(problems.get(1).contains("root node"));
        assertTrue(problems.get(2).contains("duplicate"));
    }

    @Test
    void flagsNonRequirementTagsAndNpcNeverSatisfiedTagsInRequires() {
        innerNode("a", ROOT);

        List<String> problems = problems(layout(List.of("flux", "player_only", "req_shields"), entry("a")));

        assertEquals(2, problems.size(), problems.toString());
        assertTrue(problems.get(0).contains("flux"));
        assertTrue(problems.get(1).contains("player_only"));
    }

    @Test
    void flagsLayoutsWithoutNodes() {
        assertSingleProblemContaining(layout(List.of()), "no nodes");
    }

    @Test
    void fileProblemsReportMalformedLayoutsAndAMissingLayoutsObject() throws Exception {
        List<String> problems = NpcLayoutValidator.fileProblems(new JSONObject("{ \"layouts\": {"
                + "\"broken\": { \"nodes\": [] }, \"fine\": { \"root\": \"root\", \"nodes\": [] } } }"));

        assertEquals(1, problems.size(), problems.toString());
        assertTrue(problems.get(0).contains("broken"));
        assertFalse(NpcLayoutValidator.fileProblems(new JSONObject("{}")).isEmpty());
    }

    @Test
    void aReportFailsWhenAnEligibleArchetypeFallsShortOfTheTarget() {
        List<NpcLayoutEntry> entries = new ArrayList<>();
        String previous = ROOT;
        for (int i = 1; i <= NpcLayoutValidator.TARGET_NODE_COUNT; i++) {
            innerNode("n" + i, previous);
            entries.add(entry("n" + i));
            previous = "n" + i;
        }
        NpcLayout full = new NpcLayout("full", "Full", ROOT, List.of(), "", entries);
        NpcLayout shortOne = new NpcLayout("short", "Short", ROOT, List.of(), "", entries.subList(0, 10));

        assertTrue(NpcLayoutValidator.report(full).passed());
        NpcLayoutValidator.LayoutReport shortReport = NpcLayoutValidator.report(shortOne);
        assertFalse(shortReport.passed());
        assertEquals(NpcArchetypes.ALL.size(), shortReport.runProblems().size());
    }

    @Test
    void aReportListsIneligibleArchetypesWithTheirUnmetRequirement() {
        innerNode("a", ROOT);

        NpcLayoutValidator.LayoutReport report = NpcLayoutValidator.report(layout(List.of("req_phase"), entry("a")));

        assertEquals(List.of("frigate_phase", "frigate_phase_ballistic", "cruiser_phase"),
                report.eligibleRuns().stream().map(run -> run.archetype().name()).toList());
        assertTrue(report.ineligible().stream().allMatch(ineligible -> "req_phase".equals(ineligible.unmetRequirement())));
    }

    @Test
    void milestonesRecordTheNodeCountAtWhichNotablesAndKeystonesArrive() {
        innerNode("a", ROOT);
        node("notable_1", type("notable", SkillTier.NOTABLE), List.of("a"), List.of("inner"));
        innerNode("b", "notable_1");
        node("keystone_1", type("keystone", SkillTier.KEYSTONE), List.of("b"), List.of("inner"));

        NpcTreeBuild build = NpcSkillTreeBuilder.build(
                layout(List.of(), entry("a"), entry("notable_1"), entry("b"), entry("keystone_1")), 10,
                NpcArchetypes.ALL.get(0).profile(), NpcHullMods.NONE);

        assertEquals(List.of(
                new NpcLayoutValidator.Milestone(2, "notable_1", "notable name", SkillTier.NOTABLE),
                new NpcLayoutValidator.Milestone(4, "keystone_1", "keystone name", SkillTier.KEYSTONE)),
                NpcLayoutValidator.milestones(build));
    }
}
