package exiledsector.charts.atlas;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasGeneratorTest {

    private static final long SEED = 0x5eedL;
    private static final int SCAN_RADIUS = 30;
    private static final String THREAT_FLAG = "$encounteredThreat";
    private static final String RAT = "assortment_of_things";

    private static AtlasRegion region(String id, float weight, int minSteps, String requiredMod, String unlock, Map<String, Float> factions,
                                      Map<String, Float> markers) {
        return new AtlasRegion(id, weight, minSteps, requiredMod, unlock, Map.of(), factions, markers);
    }

    private static final List<AtlasRegion> REGIONS = List.of(
            region(AtlasRegion.NORMAL, 10f, 0, "", "", Map.of(), Map.of()),
            region("graveyard", 10f, 0, "", "", Map.of("derelict", 4f, "pirates", 2f, "independent", 1f), Map.of()),
            region("badlands", 8f, 0, "", "", Map.of("pirates", 1f), Map.of("relay", 3f)),
            region("collapsed", 4f, 10, "", "", Map.of(), Map.of("black_hole", 5f)),
            region("rift", 3f, 12, RAT, "", Map.of(), Map.of()),
            region("expanse", 3f, 0, "", THREAT_FLAG, Map.of("remnant", 1f), Map.of()));

    private static final List<AtlasSpecial> SPECIALS = List.of(
            new AtlasSpecial("sensor", "sensor_array", AtlasSpecial.Placement.CHUNK, 0.5f, 4f, 6f, 1, 1f, "", ""),
            new AtlasSpecial("nexus", AtlasSpecial.INFESTATION, AtlasSpecial.Placement.CHUNK, 0.3f, 6f, 2.5f, 1, 1f, "", ""),
            new AtlasSpecial("bastion", "bastion", AtlasSpecial.Placement.FIXED, 0f, 1f, 0f, 3, 25f, "", ""));

    private static AtlasRules rules(List<AtlasRegion> regions) {
        return new AtlasRules(regions, Map.of("compact", 1f, "sprawling", 2f, "ringed", 1f),
                Map.of("pirates", 5f, "hegemony", 3f, "remnant", 2f, "derelict", 2f, "independent", 1f),
                new AtlasMarkerRule(0.5f, 0f, 0.5f), new AtlasMarkerRule(0.25f, 0f, 0.25f), new AtlasMarkerRule(0.15f, 0.01f, 0.5f),
                new AtlasMarkerRule(0.1f, 0.005f, 0.4f), SPECIALS,
                List.of(new AtlasMarkerCandidate("relay", 1f, ""), new AtlasMarkerCandidate("hunter", 1f, "")),
                List.of(new AtlasMarkerCandidate("black_hole", 1f, ""), new AtlasMarkerCandidate("flares", 1f, "")));
    }

    private static AtlasRules rules() {
        return rules(REGIONS);
    }

    private static AtlasGenerator generator(AtlasRules rules) {
        return new AtlasGenerator(SEED, rules, AtlasUnlocks.NONE);
    }

    private static List<AtlasNode> scan(AtlasGenerator generator) {
        List<AtlasNode> nodes = new ArrayList<>();
        for (AtlasCell cell : AtlasCell.ORIGIN.around(SCAN_RADIUS)) {
            nodes.add(generator.node(cell));
        }
        return nodes;
    }

    private static List<AtlasBiome> biomes(AtlasGenerator generator) {
        List<AtlasBiome> biomes = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        int chunks = SCAN_RADIUS / AtlasCell.CHUNK_SIZE + 1;
        for (int chunkX = -chunks; chunkX < chunks; chunkX++) {
            for (int chunkY = -chunks; chunkY < chunks; chunkY++) {
                for (AtlasBiome biome : generator.biomesAround(chunkX, chunkY)) {
                    if (seen.add(biome.siteKey())) {
                        biomes.add(biome);
                    }
                }
            }
        }
        return biomes;
    }

    private static boolean inside(AtlasCell cell, int radius) {
        return Math.abs(cell.gridX()) <= radius && Math.abs(cell.gridY()) <= radius;
    }

    @Test
    void theSameSeedBuildsTheSameNodesWhetherAskedByCellOrByChunk() {
        AtlasGenerator first = generator(rules());
        AtlasGenerator second = generator(rules());

        for (int chunkX = -2; chunkX <= 1; chunkX++) {
            for (int chunkY = -2; chunkY <= 1; chunkY++) {
                List<AtlasNode> chunk = first.chunk(chunkX, chunkY);
                assertEquals(AtlasCell.CHUNK_SIZE * AtlasCell.CHUNK_SIZE, chunk.size());
                for (AtlasNode node : chunk) {
                    assertEquals(node, second.node(node.cell()));
                }
            }
        }
    }

    @Test
    void anotherSeedBuildsAnotherAtlas() {
        assertNotEquals(scan(generator(rules())), scan(new AtlasGenerator(SEED + 1, rules(), AtlasUnlocks.NONE)));
    }

    @Test
    void theOriginIsAnOpenGatewayInNormalSpaceWithNoMarkers() {
        AtlasNode origin = generator(rules()).node(AtlasCell.ORIGIN);

        assertTrue(origin.isOrigin());
        assertFalse(origin.isVoid());
        assertEquals(0f, origin.mapX());
        assertEquals(0f, origin.mapY());
        assertEquals(AtlasRegion.NORMAL, origin.regionId());
        assertNull(origin.layoutId());
        assertFalse(origin.powerfulBoss());
        assertNull(origin.expansionId());
        assertNull(origin.specialId());
        assertFalse(origin.lanes().isEmpty());
    }

    @Test
    void lanesAreMutualShortAndReachEverySystemFromTheOrigin() {
        AtlasGenerator generator = generator(rules());
        Set<AtlasCell> systems = new HashSet<>();
        int laneEnds = 0;
        for (AtlasNode node : scan(generator)) {
            if (node.isVoid()) {
                assertTrue(node.lanes().isEmpty());
                continue;
            }
            systems.add(node.cell());
            for (AtlasCell other : node.lanes()) {
                laneEnds++;
                assertTrue(generator.lanes(other).contains(node.cell()), node.cell() + " and " + other + " disagree");
                float length = (float) Math.hypot(generator.mapX(other) - node.mapX(), generator.mapY(other) - node.mapY());
                assertTrue(length < AtlasGenerator.LONGEST_LANE);
            }
        }
        Set<AtlasCell> reached = new HashSet<>(List.of(AtlasCell.ORIGIN));
        ArrayDeque<AtlasCell> frontier = new ArrayDeque<>(reached);
        while (!frontier.isEmpty()) {
            for (AtlasCell next : generator.lanes(frontier.poll())) {
                if (systems.contains(next) && reached.add(next)) {
                    frontier.add(next);
                }
            }
        }
        float averageLanes = (float) laneEnds / systems.size();

        for (AtlasCell system : systems) {
            if (inside(system, SCAN_RADIUS - 6)) {
                assertTrue(reached.contains(system), system + " is cut off");
            }
        }
        assertTrue(averageLanes > 2.5f && averageLanes < 6f, "average lanes " + averageLanes);
    }

    @Test
    void lanesMatchAnUncappedGabrielGraphOverAWiderWindow() {
        AtlasGenerator generator = generator(rules());
        for (AtlasCell cell : AtlasCell.ORIGIN.around(12)) {
            if (generator.isVoid(cell)) {
                continue;
            }
            List<AtlasCell> others = new ArrayList<>();
            for (AtlasCell other : cell.around(9)) {
                if (!other.equals(cell) && !generator.isVoid(other)) {
                    others.add(other);
                }
            }
            Set<AtlasCell> expected = new HashSet<>();
            for (AtlasCell end : others) {
                double midX = (generator.mapX(cell) + generator.mapX(end)) / 2.0;
                double midY = (generator.mapY(cell) + generator.mapY(end)) / 2.0;
                double limit = Math.hypot(generator.mapX(end) - generator.mapX(cell), generator.mapY(end) - generator.mapY(cell)) / 2.0;
                boolean clear = others.stream().filter(other -> !other.equals(end))
                        .allMatch(other -> Math.hypot(generator.mapX(other) - midX, generator.mapY(other) - midY) >= limit - 1e-4);
                if (clear) {
                    expected.add(end);
                }
            }

            assertEquals(expected, new HashSet<>(generator.lanes(cell)), cell.toString());
        }
    }

    @Test
    void everyTwoByTwoBlockKeepsASystem() {
        AtlasGenerator generator = generator(rules());
        for (AtlasCell corner : AtlasCell.ORIGIN.around(SCAN_RADIUS - 1)) {
            boolean kept = false;
            for (int dx = 0; dx <= 1; dx++) {
                for (int dy = 0; dy <= 1; dy++) {
                    kept |= !generator.isVoid(new AtlasCell(corner.gridX() + dx, corner.gridY() + dy));
                }
            }
            assertTrue(kept, corner.toString());
        }
    }

    @Test
    void resultsDoNotDependOnQueryOrderOrCacheEvictions() {
        List<AtlasCell> area = AtlasCell.ORIGIN.around(10);
        AtlasGenerator ordered = generator(rules());
        Map<AtlasCell, AtlasNode> expected = new HashMap<>();
        area.forEach(cell -> expected.put(cell, ordered.node(cell)));
        AtlasGenerator shuffled = generator(rules());
        Random random = new Random(7);
        List<AtlasCell> order = new ArrayList<>(area);
        Collections.shuffle(order, random);
        for (int index = 0; index < order.size(); index++) {
            if (index % 2 == 0) {
                shuffled.node(new AtlasCell(random.nextInt(2000) - 1000, random.nextInt(2000) - 1000));
            }
            AtlasCell cell = order.get(index);
            assertEquals(expected.get(cell), shuffled.node(cell), cell.toString());
        }
    }

    @Test
    void anExpansionHookReadingTerritoryBordersStillOnlyMovesWithTheSitesANewRegionTakes() {
        AtlasMarkerCandidate borderLoving = new AtlasMarkerCandidate("relay", 1f, "", (traits, weight) -> traits.bordersOtherTerritory() ? weight * 4f : 0f);
        List<AtlasRegion> withExtra = new ArrayList<>(REGIONS);
        withExtra.add(region("furnace", 6f, 0, "", "", Map.of("hegemony", 1f), Map.of()));
        List<AtlasNode> before = scan(generator(rules().withExpansions(List.of(borderLoving))));
        List<AtlasNode> after = scan(generator(rules(withExtra).withExpansions(List.of(borderLoving))));
        int bordered = 0;
        for (int index = 0; index < before.size(); index++) {
            AtlasNode was = before.get(index);
            AtlasNode now = after.get(index);
            bordered += now.expansionId() != null ? 1 : 0;
            if (was.regionId().equals(now.regionId())) {
                assertEquals(was.expansionId(), now.expansionId(), was.cell().toString());
            }
        }

        assertTrue(bordered > 0);
    }

    @Test
    void systemsAreSparseAndSitAtVaryingDistances() {
        AtlasGenerator generator = generator(rules());
        List<AtlasNode> nodes = scan(generator);
        long empty = nodes.stream().filter(AtlasNode::isVoid).count();
        float shortest = Float.POSITIVE_INFINITY;
        float longest = 0f;
        for (AtlasNode node : nodes) {
            if (node.isVoid() || !inside(node.cell(), SCAN_RADIUS - 2)) {
                continue;
            }
            float nearest = Float.POSITIVE_INFINITY;
            for (AtlasCell other : node.lanes()) {
                nearest = Math.min(nearest, (float) Math.hypot(generator.mapX(other) - node.mapX(), generator.mapY(other) - node.mapY()));
            }
            shortest = Math.min(shortest, nearest);
            longest = Math.max(longest, nearest);
        }

        assertTrue(empty > nodes.size() / 6, empty + " empty cells");
        assertTrue(shortest < 0.6f, "shortest " + shortest);
        assertTrue(longest > 1.5f, "longest " + longest);
    }

    @Test
    void biomesClaimTheirSizeAndNeverOverlap() {
        AtlasGenerator generator = generator(rules());
        List<AtlasBiome> biomes = biomes(generator);
        Map<AtlasCell, AtlasBiome> claims = new HashMap<>();
        for (AtlasBiome biome : biomes) {
            assertTrue(biome.systems().size() >= 4 && biome.systems().size() <= 7, biome.regionId() + " holds " + biome.systems().size());
            for (AtlasCell system : biome.systems()) {
                assertNull(claims.put(system, biome), system + " is claimed twice");
                assertEquals(biome.regionId(), generator.node(system).regionId());
            }
        }
        for (AtlasNode node : scan(generator)) {
            if (!node.isVoid() && !claims.containsKey(node.cell())) {
                assertEquals(AtlasRegion.NORMAL, node.regionId(), node.cell().toString());
            }
        }

        assertFalse(biomes.isEmpty());
    }

    @Test
    void biomesAreLargeSparsePatchesWithNormalSpaceBetween() {
        AtlasGenerator generator = generator(rules());
        List<AtlasBiome> biomes = biomes(generator);
        for (AtlasBiome biome : biomes) {
            for (AtlasBiome other : biomes) {
                if (other != biome) {
                    double gap = Math.hypot(other.centreX() - biome.centreX(), other.centreY() - biome.centreY()) - biome.radius() - other.radius();
                    assertTrue(gap >= AtlasGenerator.BIOME_GAP, "biomes " + gap + " apart");
                }
            }
        }
        int covered = 0;
        int cells = 0;
        int biomeSystems = 0;
        int systems = 0;
        for (AtlasNode node : scan(generator)) {
            cells++;
            AtlasBiome cover = biomes.stream().filter(biome -> biome.covers(node.mapX(), node.mapY())).findFirst().orElse(null);
            covered += cover == null ? 0 : 1;
            if (node.isVoid()) {
                assertEquals(cover == null ? AtlasRegion.NORMAL : cover.regionId(), node.regionId());
                continue;
            }
            systems++;
            if (cover != null) {
                biomeSystems++;
                assertTrue(cover.systems().contains(node.cell()), node.cell() + " sits inside a biome it does not belong to");
            }
        }
        float coveredShare = (float) covered / cells;
        float systemShare = (float) biomeSystems / systems;

        assertTrue(coveredShare > 0.15f && coveredShare < 0.6f, "biomes cover " + coveredShare);
        assertTrue(systemShare > 0.02f && systemShare < coveredShare / 2f, "biomes hold " + systemShare + " of systems over " + coveredShare + " of space");
    }

    @Test
    void lockedBiomesStayNormalUnlessRevealedAfterTheUnlock() {
        AtlasGenerator locked = generator(rules());
        AtlasGenerator unlocked = new AtlasGenerator(SEED, rules(), new AtlasUnlocks(RAT::equals, Set.of(THREAT_FLAG)));
        int opened = 0;
        Map<Long, String> unlockedSites = new HashMap<>();
        for (AtlasBiome biome : biomes(unlocked)) {
            unlockedSites.put(biome.siteKey(), biome.regionId());
        }
        for (AtlasNode node : scan(locked)) {
            assertNotEquals("rift", node.regionId());
            assertNotEquals("expanse", node.regionId());
            assertEquals(node.isVoid(), unlocked.isVoid(node.cell()));
            assertEquals(node.lanes(), unlocked.lanes(node.cell()));
            String after = unlocked.node(node.cell()).regionId();
            if (!after.equals(node.regionId())) {
                opened++;
                assertEquals(AtlasRegion.NORMAL, node.regionId());
                assertTrue("rift".equals(after) || "expanse".equals(after), node.cell() + " became " + after);
            }
        }
        AtlasGenerator remembered = new AtlasGenerator(SEED, rules(), new AtlasUnlocks(RAT::equals, Set.of()), unlockedSites::get);
        AtlasGenerator modRemoved = new AtlasGenerator(SEED, rules(), AtlasUnlocks.NONE, unlockedSites::get);
        int expanses = 0;

        assertTrue(opened > 0);
        for (AtlasNode node : scan(unlocked)) {
            assertEquals(node.regionId(), remembered.node(node.cell()).regionId());
            String withoutMod = modRemoved.node(node.cell()).regionId();
            assertEquals("rift".equals(node.regionId()) ? AtlasRegion.NORMAL : node.regionId(), withoutMod);
            expanses += "expanse".equals(withoutMod) ? 1 : 0;
        }
        assertTrue(expanses > 0);
    }

    @Test
    void aBiomeIsNeverCentredCloserThanItsMinimumSteps() {
        int collapsed = 0;
        for (AtlasBiome biome : biomes(generator(rules()))) {
            if ("collapsed".equals(biome.regionId())) {
                collapsed++;
                assertTrue(Math.hypot(biome.centreX(), biome.centreY()) >= 10, biome.siteKey() + " is too close");
            }
        }

        assertTrue(collapsed > 0);
    }

    @Test
    void aRegionWithAFactionPoolKeepsItsSystemsToThatPoolInTerritories() {
        int pairs = 0;
        int split = 0;
        AtlasGenerator generator = generator(rules());
        for (AtlasNode node : scan(generator)) {
            if ("badlands".equals(node.regionId())) {
                assertEquals("pirates", node.factionId());
            }
            if (!"graveyard".equals(node.regionId())) {
                continue;
            }
            assertTrue(Set.of("derelict", "pirates", "independent").contains(node.factionId()), node.factionId());
            for (AtlasCell neighbour : node.lanes()) {
                AtlasNode other = generator.node(neighbour);
                if ("graveyard".equals(other.regionId())) {
                    pairs++;
                    split += node.factionId().equals(other.factionId()) ? 0 : 1;
                }
            }
        }

        assertTrue(pairs > 0);
        assertTrue(split < pairs * 0.25, split + " of " + pairs + " lanes cross a faction border");
    }

    @Test
    void addingARegionMovesOnlyTheSitesItTakes() {
        List<AtlasRegion> withExtra = new ArrayList<>(REGIONS);
        withExtra.add(region("furnace", 6f, 0, "", "", Map.of(), Map.of()));
        AtlasGenerator before = generator(rules());
        AtlasGenerator after = generator(rules(withExtra));
        Map<Long, String> afterSites = new HashMap<>();
        for (AtlasBiome biome : biomes(after)) {
            afterSites.put(biome.siteKey(), biome.regionId());
        }
        int taken = 0;
        for (AtlasCell cell : AtlasCell.ORIGIN.around(SCAN_RADIUS - 4)) {
            AtlasBiome was = before.biomeOf(cell);
            AtlasBiome now = after.biomeOf(cell);
            String wasRegion = was == null ? AtlasRegion.NORMAL : was.regionId();
            String nowRegion = now == null ? AtlasRegion.NORMAL : now.regionId();
            if ("furnace".equals(nowRegion)) {
                taken++;
            } else if (!wasRegion.equals(nowRegion)) {
                assertEquals("furnace", afterSites.get(was.siteKey()), cell + " changed from " + wasRegion + " to " + nowRegion);
            }
        }

        assertTrue(taken > 0);
    }

    @Test
    void addingAnExpansionOrPhenomenonMovesNoOtherRoll() {
        AtlasRules base = rules();
        List<AtlasMarkerCandidate> moreExpansions = new ArrayList<>(base.expansions());
        moreExpansions.add(new AtlasMarkerCandidate("convoy", 1f, ""));
        List<AtlasMarkerCandidate> morePhenomena = new ArrayList<>(base.phenomena());
        morePhenomena.add(new AtlasMarkerCandidate("nebula", 1f, ""));
        List<AtlasNode> before = scan(generator(base));
        List<AtlasNode> after = scan(generator(base.withExpansions(moreExpansions).withPhenomena(morePhenomena)));
        int convoys = 0;
        int nebulae = 0;
        for (int index = 0; index < before.size(); index++) {
            AtlasNode was = before.get(index);
            AtlasNode now = after.get(index);
            if ("convoy".equals(now.expansionId())) {
                convoys++;
            } else {
                assertEquals(was.expansionId(), now.expansionId(), was.cell().toString());
            }
            if ("nebula".equals(now.phenomenonId())) {
                nebulae++;
            } else {
                assertEquals(was.phenomenonId(), now.phenomenonId(), was.cell().toString());
            }
            assertEquals(was.regionId(), now.regionId());
            assertEquals(was.factionId(), now.factionId());
            assertEquals(was.layoutId(), now.layoutId());
            assertEquals(was.powerfulBoss(), now.powerfulBoss());
            assertEquals(was.specialId(), now.specialId());
            assertEquals(was.lanes(), now.lanes());
        }

        assertTrue(convoys > 0);
        assertTrue(nebulae > 0);
    }

    @Test
    void markersComeMoreOftenFurtherOutAndMissingModsNeverRoll() {
        int near = 0;
        int nearCount = 0;
        int far = 0;
        int farCount = 0;
        for (AtlasNode node : scan(generator(rules()))) {
            if (node.isVoid() || node.isOrigin()) {
                continue;
            }
            int marked = node.expansionId() != null ? 1 : 0;
            if (node.distance() <= 10f) {
                nearCount++;
                near += marked;
            } else if (node.distance() >= 25f) {
                farCount++;
                far += marked;
            }
        }
        for (AtlasNode node : scan(generator(rules().withExpansions(List.of(new AtlasMarkerCandidate("relay", 1f, RAT)))))) {
            assertNull(node.expansionId());
        }

        assertTrue((float) far / farCount > (float) near / nearCount);
    }

    @Test
    void theExpansionHookShapesTheRoll() {
        AtlasRules vetoed = rules().withExpansions(List.of(new AtlasMarkerCandidate("relay", 1f, "",
                (traits, weight) -> "badlands".equals(traits.regionId()) ? weight : 0f)));
        int inBadlands = 0;
        for (AtlasNode node : scan(generator(vetoed))) {
            if (node.expansionId() != null) {
                assertEquals("badlands", node.regionId());
                inBadlands++;
            }
        }

        assertTrue(inBadlands > 0);
    }

    @Test
    void bastionsSitAtFixedBearingsInEverySave() {
        for (long seed = 1; seed <= 3; seed++) {
            AtlasGenerator generator = new AtlasGenerator(seed, rules(), AtlasUnlocks.NONE);
            for (int index = 0; index < 3; index++) {
                double bearing = Math.PI / 2.0 + 2.0 * Math.PI * index / 3;
                AtlasNode bastion = generator.node(AtlasCell.nearest((float) (25 * Math.cos(bearing)), (float) (25 * Math.sin(bearing))));
                assertEquals("bastion", bastion.specialId());
                assertFalse(bastion.isVoid());
                assertEquals(25f, bastion.distance(), 1f);
            }
        }
    }

    @Test
    void chunkSitesRespectTheirDistanceAndInfestationsSurroundTheirNexus() {
        AtlasGenerator generator = generator(rules());
        List<AtlasNode> nodes = scan(generator);
        List<AtlasNode> nexuses = new ArrayList<>();
        int sensors = 0;
        for (AtlasNode node : nodes) {
            if ("sensor".equals(node.specialId())) {
                sensors++;
                assertTrue(node.distance() >= 4f);
            }
            if ("nexus".equals(node.specialId())) {
                nexuses.add(node);
                assertTrue(node.distance() >= 6f);
            }
            if (node.specialId() != null) {
                assertFalse(node.isVoid());
            }
        }
        int infested = 0;
        for (AtlasNode node : nodes) {
            if (node.isVoid() || !inside(node.cell(), SCAN_RADIUS - 3)) {
                continue;
            }
            boolean nearNexus = nexuses.stream().anyMatch(nexus -> Math.hypot(nexus.mapX() - node.mapX(), nexus.mapY() - node.mapY()) <= 2.5);
            assertEquals(nearNexus, node.infestationId() != null, node.cell().toString());
            infested += nearNexus ? 1 : 0;
        }

        assertTrue(sensors > 0);
        assertTrue(infested > 0);
    }

    @Test
    void powerfulBossesTurnUpAtAboutTheirChance() {
        int open = 0;
        int powerful = 0;
        for (AtlasNode node : scan(generator(rules()))) {
            if (!node.isVoid() && !node.isOrigin()) {
                open++;
                powerful += node.powerfulBoss() ? 1 : 0;
            }
        }

        assertEquals(0.25f, (float) powerful / open, 0.04f);
    }

    @Test
    void layoutsFollowTheirWeights() {
        int sprawling = 0;
        int compact = 0;
        for (AtlasNode node : scan(generator(rules()))) {
            if ("sprawling".equals(node.layoutId())) {
                sprawling++;
            } else if ("compact".equals(node.layoutId())) {
                compact++;
            }
        }

        assertEquals(2f, (float) sprawling / compact, 0.35f);
    }
}
