package exiledsector.charts.atlas;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
            new AtlasSpecial("sensor", AtlasSpecial.Kind.SENSOR_ARRAY, AtlasSpecial.Placement.CHUNK, 0.5f, 4f, 6f, 1, 1f, "", "", ""),
            new AtlasSpecial("nexus", AtlasSpecial.Kind.INFESTATION, AtlasSpecial.Placement.CHUNK, 0.3f, 6f, 2.5f, 1, 1f, "", "", ""),
            new AtlasSpecial("bastion", AtlasSpecial.Kind.BASTION, AtlasSpecial.Placement.FIXED, 0f, 1f, 0f, 3, 25f, "", "", ""));

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
        return new AtlasGenerator(SEED, rules, AtlasUnlocks.NONE, AtlasMemory.NONE);
    }

    private static AtlasRules withExpansions(AtlasRules rules, List<AtlasMarkerCandidate> expansions) {
        return new AtlasRules(rules.regions(), rules.layoutWeights(), rules.factionWeights(), rules.voidRule(), rules.powerfulBossRule(),
                rules.expansionRule(), rules.phenomenonRule(), rules.specials(), expansions, rules.phenomena());
    }

    private static AtlasRules withPhenomena(AtlasRules rules, List<AtlasMarkerCandidate> phenomena) {
        return new AtlasRules(rules.regions(), rules.layoutWeights(), rules.factionWeights(), rules.voidRule(), rules.powerfulBossRule(),
                rules.expansionRule(), rules.phenomenonRule(), rules.specials(), rules.expansions(), phenomena);
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

    private static AtlasRegion sized(String id, float size, float density) {
        return new AtlasRegion(id, 1f, 0, "", "", Map.of(), Map.of(), Map.of(), AtlasRegion.NO_MAX_STEPS, 0f, size, density, Map.of());
    }

    private static AtlasRegion clouded(String id, float clouds, float walls) {
        return new AtlasRegion(id, 1f, 0, "", "", Map.of(), Map.of(), Map.of(), AtlasRegion.NO_MAX_STEPS, 0f, 1f, 1f, Map.of(), clouds, walls);
    }

    private static final List<AtlasRegion> CLOUDED = List.of(clouded(AtlasRegion.NORMAL, 0f, 0.3f), clouded("storm", 0.6f, 0.9f),
            clouded("calm", 0.1f, 0.5f));

    private static int cloudTiles(AtlasGenerator generator, int radius) {
        int clouds = 0;
        for (int tileX = -radius * 4; tileX < radius * 4; tileX++) {
            for (int tileY = -radius * 4; tileY < radius * 4; tileY++) {
                clouds += generator.cloudAt((tileX + 0.5f) / 4f, (tileY + 0.5f) / 4f) ? 1 : 0;
            }
        }
        return clouds;
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
        assertNotEquals(scan(generator(rules())), scan(new AtlasGenerator(SEED + 1, rules(), AtlasUnlocks.NONE, AtlasMemory.NONE)));
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
        assertTrue(averageLanes > 2.2f && averageLanes < 3.5f, "average lanes " + averageLanes);
    }

    private static double distance(AtlasGenerator generator, AtlasCell from, AtlasCell to) {
        return Math.hypot(generator.mapX(to) - generator.mapX(from), generator.mapY(to) - generator.mapY(from));
    }

    @Test
    void lanesMatchAnUncappedRelativeNeighbourhoodGraphOverAWiderWindow() {
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
                double limit = distance(generator, cell, end);
                boolean clear = others.stream().filter(other -> !other.equals(end))
                        .allMatch(other -> Math.max(distance(generator, other, cell), distance(generator, other, end)) >= limit - 1e-4);
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
        List<AtlasNode> before = scan(generator(withExpansions(rules(), List.of(borderLoving))));
        List<AtlasNode> after = scan(generator(withExpansions(rules(withExtra), List.of(borderLoving))));
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
    void biomesTileTheWholeMapWithEveryCellInItsNearestSite() {
        AtlasGenerator generator = generator(rules());
        List<AtlasBiome> sites = biomes(generator);
        Map<String, Integer> siteRegions = new HashMap<>();
        sites.forEach(site -> siteRegions.merge(site.regionId(), 1, Integer::sum));
        for (AtlasNode node : scan(generator)) {
            if (node.isOrigin() || !inside(node.cell(), SCAN_RADIUS - 8)) {
                continue;
            }
            AtlasBiome owner = generator.biomeOf(node.cell());
            AtlasBiome nearest = sites.stream().min(Comparator.comparingDouble(
                    site -> Math.hypot(site.centreX() - node.mapX(), site.centreY() - node.mapY()))).orElseThrow();

            assertEquals(nearest.siteKey(), owner.siteKey(), node.cell().toString());
            assertEquals(owner.regionId(), node.regionId(), node.cell().toString());
        }
        float normalShare = (float) siteRegions.getOrDefault(AtlasRegion.NORMAL, 0) / sites.size();

        assertTrue(siteRegions.keySet().containsAll(Set.of(AtlasRegion.NORMAL, "graveyard", "badlands", "collapsed")), siteRegions.toString());
        assertTrue(normalShare > 0.2f && normalShare < 0.8f, "normal space holds " + normalShare + " of sites");
    }

    @Test
    void aLargerRegionClaimsMoreSpaceForEachSite() {
        List<AtlasRegion> regions = List.of(sized("vast", 2f, 1f), sized("tiny", 0.5f, 1f));
        AtlasGenerator generator = generator(rules(regions));
        Map<String, Set<Long>> sites = new HashMap<>();
        Map<String, Integer> cells = new HashMap<>();
        for (AtlasCell cell : AtlasCell.ORIGIN.around(SCAN_RADIUS)) {
            AtlasBiome biome = generator.biomeOf(cell);
            sites.computeIfAbsent(biome.regionId(), id -> new HashSet<>()).add(biome.siteKey());
            cells.merge(biome.regionId(), 1, Integer::sum);
        }
        float vastPerSite = (float) cells.get("vast") / sites.get("vast").size();
        float tinyPerSite = (float) cells.get("tiny") / sites.get("tiny").size();

        assertTrue(vastPerSite > tinyPerSite * 2f, vastPerSite + " against " + tinyPerSite);
    }

    @Test
    void aRegionsDensitySetsHowManyOfItsCellsHoldASystem() {
        List<AtlasRegion> regions = List.of(sized("sparse", 1f, 0f), sized("full", 1f, 1f));
        AtlasGenerator generator = generator(rules(regions));
        int sparseCells = 0;
        int fullOddCells = 0;
        for (AtlasNode node : scan(generator)) {
            AtlasCell cell = node.cell();
            if (node.isOrigin() || AtlasPositions.isLattice(cell) || generator.specialAt(cell) != null) {
                continue;
            }
            if ("sparse".equals(node.regionId())) {
                sparseCells++;
                assertTrue(node.isVoid(), cell.toString());
            } else if (!cell.mayBeVoid()) {
                fullOddCells++;
                assertFalse(node.isVoid(), cell.toString());
            }
        }

        assertTrue(sparseCells > 0 && fullOddCells > 0);
    }

    @Test
    void aRegionsDistanceBandLimitsWhereItsSitesSit() {
        AtlasRegion inner = new AtlasRegion("inner", 4f, 0, "", "", Map.of(), Map.of(), Map.of(), 12, -0.2f, 1f, 1f, Map.of());
        AtlasRegion outer = new AtlasRegion("outer", 0f, 8, "", "", Map.of(), Map.of(), Map.of(), AtlasRegion.NO_MAX_STEPS, 1f, 1f, 1f, Map.of());

        assertEquals(4f, inner.weightAt(0f));
        assertEquals(2f, inner.weightAt(10f), 1e-5f);
        assertEquals(0f, inner.weightAt(13f));
        assertEquals(0f, outer.weightAt(7f));
        assertEquals(5f, outer.weightAt(13f), 1e-5f);
        int inners = 0;
        int outers = 0;
        for (AtlasBiome site : biomes(generator(rules(List.of(region(AtlasRegion.NORMAL, 1f, 0, "", "", Map.of(), Map.of()), inner, outer)))))  {
            double distance = Math.hypot(site.centreX(), site.centreY());
            if ("inner".equals(site.regionId())) {
                inners++;
                assertTrue(distance <= 12, site.siteKey() + " sits " + distance + " out");
            } else if ("outer".equals(site.regionId())) {
                outers++;
                assertTrue(distance >= 8, site.siteKey() + " sits " + distance + " out");
            }
        }

        assertTrue(inners > 0 && outers > 0);
    }

    @Test
    void aForbiddenNeighbourNeverSharesABorder() {
        AtlasRegion hot = new AtlasRegion("hot", 1f, 0, "", "", Map.of(), Map.of(), Map.of(), AtlasRegion.NO_MAX_STEPS, 0f, 2f, 1f,
                Map.of("cold", 0f));
        List<AtlasRegion> regions = List.of(sized(AtlasRegion.NORMAL, 0.5f, 1f), hot, sized("cold", 2f, 1f));
        int hots = 0;
        int colds = 0;
        for (long seed = 1; seed <= 6; seed++) {
            AtlasGenerator generator = new AtlasGenerator(seed, rules(regions), AtlasUnlocks.NONE, AtlasMemory.NONE);
            for (AtlasCell cell : AtlasCell.ORIGIN.around(SCAN_RADIUS)) {
                String regionId = generator.biomeOf(cell).regionId();
                hots += "hot".equals(regionId) ? 1 : 0;
                colds += "cold".equals(regionId) ? 1 : 0;
                if (!"hot".equals(regionId)) {
                    continue;
                }
                for (AtlasCell neighbour : cell.around(1)) {
                    assertNotEquals("cold", generator.biomeOf(neighbour).regionId(), "seed " + seed + ": " + cell + " borders cold at " + neighbour);
                }
            }
        }

        assertTrue(hots > 0 && colds > 0);
    }

    @Test
    void cloudsNeverCoverASystemAndLanesNeverCrossThem() {
        AtlasGenerator generator = generator(rules(CLOUDED));
        int cloudedCells = 0;
        for (AtlasCell cell : AtlasCell.ORIGIN.around(SCAN_RADIUS)) {
            float x = generator.displayX(cell);
            float y = generator.displayY(cell);
            if (generator.isVoid(cell)) {
                cloudedCells += generator.cloudAt(x, y) ? 1 : 0;
                continue;
            }
            for (int tileX = -3; tileX <= 3; tileX++) {
                for (int tileY = -3; tileY <= 3; tileY++) {
                    float tileCentreX = ((float) Math.floor(x * 4f) + tileX + 0.5f) / 4f;
                    float tileCentreY = ((float) Math.floor(y * 4f) + tileY + 0.5f) / 4f;
                    if (Math.hypot(tileCentreX - x, tileCentreY - y) < 0.6) {
                        assertFalse(generator.cloudAt(tileCentreX, tileCentreY), cell + " sits under a cloud");
                    }
                }
            }
            for (AtlasCell lane : generator.lanes(cell)) {
                assertTrue(generator.laneClear(cell, lane), cell + " to " + lane + " crosses a cloud");
            }
        }

        assertTrue(cloudedCells > 0);
    }

    @Test
    void cloudRidgesOnlyRiseBetweenDifferentBiomes() {
        assertEquals(0, cloudTiles(generator(rules(List.of(clouded(AtlasRegion.NORMAL, 0f, 1f)))), 20));
        assertTrue(cloudTiles(generator(rules(List.of(clouded(AtlasRegion.NORMAL, 0f, 1f), clouded("other", 0f, 1f)))), 20) > 0);
        assertEquals(0, cloudTiles(generator(rules(List.of(clouded(AtlasRegion.NORMAL, 0f, 0f), clouded("other", 0f, 0f)))), 20));
    }

    @Test
    void systemsWithAClearLineBetweenThemAreAlwaysJoinedByLanes() {
        AtlasGenerator generator = generator(rules(CLOUDED));
        Map<Long, Long> parents = new HashMap<>();
        for (AtlasCell cell : AtlasCell.ORIGIN.around(SCAN_RADIUS)) {
            for (AtlasCell lane : generator.lanes(cell)) {
                if (inside(lane, SCAN_RADIUS)) {
                    parents.put(root(parents, cell.pack()), root(parents, lane.pack()));
                }
            }
        }
        int checked = 0;
        int blocked = 0;
        for (AtlasCell cell : AtlasCell.ORIGIN.around(SCAN_RADIUS - 10)) {
            if (generator.isVoid(cell)) {
                continue;
            }
            for (AtlasCell other : cell.around(AtlasGenerator.LANE_WINDOW)) {
                if (other.equals(cell) || generator.isVoid(other) || distance(generator, cell, other) > AtlasGenerator.LONGEST_LANE - 0.01f) {
                    continue;
                }
                if (!generator.laneClear(cell, other)) {
                    blocked++;
                    continue;
                }
                checked++;
                assertEquals(root(parents, cell.pack()), root(parents, other.pack()), cell + " and " + other + " see each other but are not joined");
            }
        }

        assertTrue(checked > 0 && blocked > 0);
    }

    @Test
    void corridorsKeepTheGatewayEveryHubAndEveryBastionJoinedUnderHeavyClouds() {
        List<AtlasRegion> walled = List.of(clouded(AtlasRegion.NORMAL, 0.9f, 1f), clouded("storm", 1f, 1f), clouded("calm", 0.8f, 1f));
        int radius = 40;
        for (long seed : new long[] {SEED, 41L}) {
            AtlasGenerator generator = new AtlasGenerator(seed, rules(walled), AtlasUnlocks.NONE, AtlasMemory.NONE);
            Set<Long> reached = new HashSet<>(Set.of(AtlasCell.ORIGIN.pack()));
            ArrayDeque<AtlasCell> queue = new ArrayDeque<>(List.of(AtlasCell.ORIGIN));
            while (!queue.isEmpty()) {
                for (AtlasCell lane : generator.lanes(queue.poll())) {
                    if (inside(lane, radius) && reached.add(lane.pack())) {
                        queue.add(lane);
                    }
                }
            }

            assertTrue(cloudTiles(generator, 20) > 20 * 20 * 16, "clouds should be heavy for seed " + seed);
            for (int hubX = -3; hubX <= 3; hubX++) {
                for (int hubY = -3; hubY <= 3; hubY++) {
                    AtlasCell hub = AtlasTerrain.hub(seed, hubX, hubY);
                    assertFalse(generator.isVoid(hub), hub.toString());
                    assertTrue(reached.contains(hub.pack()), hub + " is cut off for seed " + seed);
                }
            }
            int bastions = 0;
            for (AtlasCell cell : AtlasCell.ORIGIN.around(radius)) {
                if (generator.specialAt(cell) != null && generator.specialAt(cell).kind() == AtlasSpecial.Kind.BASTION) {
                    bastions++;
                    assertTrue(reached.contains(cell.pack()), "bastion " + cell + " is cut off for seed " + seed);
                }
            }
            assertEquals(3, bastions);
        }
    }

    @Test
    void specialPocketsNeverTouchAnotherPocketOfTheirOwnTypeWhileNormalSpaceChains() {
        AtlasGenerator generator = generator(rules(List.of(sized(AtlasRegion.NORMAL, 1f, 1f), sized("storm", 1f, 1f), sized("ice", 1f, 1f))));
        int specialCells = 0;
        int normalJoins = 0;
        for (AtlasCell cell : AtlasCell.ORIGIN.around(40)) {
            AtlasBiome biome = generator.biomeOf(cell);
            for (AtlasCell next : List.of(new AtlasCell(cell.gridX() + 1, cell.gridY()), new AtlasCell(cell.gridX(), cell.gridY() + 1))) {
                AtlasBiome neighbour = generator.biomeOf(next);
                if (neighbour.siteKey() == biome.siteKey() || !neighbour.regionId().equals(biome.regionId())) {
                    continue;
                }
                assertEquals(AtlasRegion.NORMAL, biome.regionId(), cell + " and " + next + " are separate " + biome.regionId() + " pockets");
                normalJoins++;
            }
            specialCells += AtlasRegion.NORMAL.equals(biome.regionId()) ? 0 : 1;
        }

        assertTrue(specialCells > 500, "specials " + specialCells);
        assertTrue(normalJoins > 0);
    }

    @Test
    void biomesHoldOnlyTheirMappedFactionsAndTerritoriesDrawFromNormalSpace() {
        AtlasRegion normal = new AtlasRegion(AtlasRegion.NORMAL, 2f, 0, "", "", Map.of(), Map.of("pirates", 1f, "hegemony", 1f), Map.of());
        AtlasRegion graveyard = new AtlasRegion("graveyard", 1f, 0, "", "", Map.of(), Map.of("derelict", 1f), Map.of());
        AtlasRegion storm = new AtlasRegion("storm", 1f, 0, "", "", Map.of(), Map.of(), Map.of());
        AtlasGenerator generator = generator(rules(List.of(normal, graveyard, storm)));
        int held = 0;
        Set<String> territoryFactions = new HashSet<>();
        for (AtlasNode node : scan(generator)) {
            if (node.isVoid() || node.isOrigin()) {
                continue;
            }
            assertEquals(node.factionId(), generator.factionAt(node.cell()));
            if ("graveyard".equals(node.regionId())) {
                held++;
                assertEquals("derelict", node.factionId(), node.cell().toString());
            } else {
                territoryFactions.add(node.factionId());
            }
        }

        assertTrue(held > 20);
        assertEquals(Set.of("pirates", "hegemony"), territoryFactions);
    }

    @Test
    void latticePathsStepBetweenLatticeCellsWithinLaneReach() {
        AtlasCell from = new AtlasCell(-14, 6);
        AtlasCell to = new AtlasCell(8, -2);
        List<AtlasCell> path = AtlasTerrain.latticePath(from, to);

        assertEquals(from, path.get(0));
        assertEquals(to, path.get(path.size() - 1));
        for (int index = 1; index < path.size(); index++) {
            AtlasCell step = path.get(index);
            AtlasCell previous = path.get(index - 1);
            assertTrue(AtlasPositions.isLattice(step), step.toString());
            assertTrue(Math.abs(step.gridX() - previous.gridX()) <= 2 && Math.abs(step.gridY() - previous.gridY()) <= 2, step.toString());
            assertNotEquals(previous, step);
        }
        assertEquals(List.of(from), AtlasTerrain.latticePath(from, from));
    }

    private static long root(Map<Long, Long> parents, long packed) {
        long current = packed;
        Long parent = parents.get(current);
        while (parent != null && parent != current) {
            current = parent;
            parent = parents.get(current);
        }
        return current;
    }

    @Test
    void revealedSpaceKeepsItsSystemsSpecialsAndLanesWhenAnUnlockArrives() {
        List<AtlasRegion> regions = List.of(sized(AtlasRegion.NORMAL, 0.5f, 0.3f),
                new AtlasRegion("dense", 1f, 0, "", "", Map.of(), Map.of(), Map.of(), AtlasRegion.NO_MAX_STEPS, 0f, 1.5f, 1f, Map.of(), 0.3f, 0.7f),
                new AtlasRegion("sparse", 1f, 0, "", "", Map.of(), Map.of(), Map.of(), AtlasRegion.NO_MAX_STEPS, 0f, 1f, 0.5f, Map.of(), 0f, 0.7f),
                new AtlasRegion("expanse", 4f, 0, "", THREAT_FLAG, Map.of(), Map.of(), Map.of(), AtlasRegion.NO_MAX_STEPS, 0f, 2f, 0.1f, Map.of(), 0.5f,
                        0.9f),
                new AtlasRegion("rift", 4f, 0, RAT, "", Map.of(), Map.of(), Map.of(), AtlasRegion.NO_MAX_STEPS, 0f, 0.5f, 1f, Map.of(), 0.2f, 0.9f));
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator before = generator(rules(regions));
        List<AtlasCell> revealed = AtlasCell.ORIGIN.around(12);
        state.recordReveal(before, revealed);
        AtlasGenerator after = new AtlasGenerator(SEED, rules(regions), new AtlasUnlocks(RAT::equals, Set.of(THREAT_FLAG)), state);
        AtlasGenerator fresh = new AtlasGenerator(SEED, rules(regions), new AtlasUnlocks(RAT::equals, Set.of(THREAT_FLAG)), AtlasMemory.NONE);
        int changedElsewhere = 0;

        for (AtlasCell cell : revealed) {
            assertEquals(before.isVoid(cell), after.isVoid(cell), cell + " void");
            assertEquals(before.lanes(cell), after.lanes(cell), cell + " lanes");
            assertEquals(before.specialAt(cell), after.specialAt(cell), cell + " special");
            assertEquals(before.infestationSource(cell), after.infestationSource(cell), cell + " infestation");
            assertEquals(before.biomeOf(cell), after.biomeOf(cell), cell + " biome");
            changedElsewhere += before.isVoid(cell) != fresh.isVoid(cell) ? 1 : 0;
        }
        assertTrue(changedElsewhere > 0);
    }

    @Test
    void lockedBiomesTakeTheirRunnerUpUnlessRevealedAfterTheUnlock() {
        AtlasGenerator locked = generator(rules());
        AtlasGenerator unlocked = new AtlasGenerator(SEED, rules(), new AtlasUnlocks(RAT::equals, Set.of(THREAT_FLAG)), AtlasMemory.NONE);
        int opened = 0;
        Map<Long, String> unlockedSites = new HashMap<>();
        for (AtlasBiome biome : biomes(unlocked)) {
            unlockedSites.put(biome.siteKey(), biome.regionId());
        }
        List<AtlasBiome> openedSites = biomes(unlocked).stream().filter(biome -> Set.of("rift", "expanse").contains(biome.regionId())).toList();
        for (AtlasNode node : scan(locked)) {
            assertNotEquals("rift", node.regionId());
            assertNotEquals("expanse", node.regionId());
            assertEquals(node.isVoid(), unlocked.isVoid(node.cell()));
            assertEquals(node.lanes(), unlocked.lanes(node.cell()));
            String after = unlocked.node(node.cell()).regionId();
            if (!after.equals(node.regionId())) {
                opened++;
                assertTrue("rift".equals(after) || "expanse".equals(after) || nearSwitched(unlocked.biomeOf(node.cell()), openedSites),
                        node.cell() + " became " + after);
            }
        }
        AtlasGenerator remembered = new AtlasGenerator(SEED, rules(), new AtlasUnlocks(RAT::equals, Set.of()), unlockedSites::get);
        AtlasGenerator modRemoved = new AtlasGenerator(SEED, rules(), AtlasUnlocks.NONE, unlockedSites::get);
        int expanses = 0;

        assertTrue(opened > 0);
        for (AtlasNode node : scan(unlocked)) {
            assertEquals(node.regionId(), remembered.node(node.cell()).regionId());
            String withoutMod = modRemoved.node(node.cell()).regionId();
            if ("rift".equals(node.regionId())) {
                assertNotEquals("rift", withoutMod);
            } else {
                assertEquals(node.regionId(), withoutMod);
            }
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
        List<AtlasBiome> furnaces = biomes(after).stream().filter(biome -> "furnace".equals(biome.regionId())).toList();
        int taken = 0;
        for (AtlasCell cell : AtlasCell.ORIGIN.around(SCAN_RADIUS - 4)) {
            AtlasBiome was = before.biomeOf(cell);
            AtlasBiome now = after.biomeOf(cell);
            assertEquals(was.siteKey(), now.siteKey());
            if ("furnace".equals(now.regionId())) {
                taken++;
            } else if (!was.regionId().equals(now.regionId())) {
                assertTrue(nearSwitched(now, furnaces), cell + " moved from " + was.regionId() + " to " + now.regionId());
            }
        }

        assertTrue(taken > 0);
    }

    private static boolean nearSwitched(AtlasBiome site, List<AtlasBiome> switched) {
        return switched.stream().anyMatch(other -> Math.hypot(other.centreX() - site.centreX(), other.centreY() - site.centreY())
                <= 2f * AtlasBiomes.POCKET_REACH);
    }

    @Test
    void addingAnExpansionOrPhenomenonMovesNoOtherRoll() {
        AtlasRules base = rules();
        List<AtlasMarkerCandidate> moreExpansions = new ArrayList<>(base.expansions());
        moreExpansions.add(new AtlasMarkerCandidate("convoy", 1f, ""));
        List<AtlasMarkerCandidate> morePhenomena = new ArrayList<>(base.phenomena());
        morePhenomena.add(new AtlasMarkerCandidate("nebula", 1f, ""));
        List<AtlasNode> before = scan(generator(base));
        List<AtlasNode> after = scan(generator(withPhenomena(withExpansions(base, moreExpansions), morePhenomena)));
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
        for (AtlasNode node : scan(generator(withExpansions(rules(), List.of(new AtlasMarkerCandidate("relay", 1f, RAT)))))) {
            assertNull(node.expansionId());
        }

        assertTrue((float) far / farCount > (float) near / nearCount);
    }

    @Test
    void theExpansionHookShapesTheRoll() {
        AtlasRules vetoed = withExpansions(rules(), List.of(new AtlasMarkerCandidate("relay", 1f, "",
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
            AtlasGenerator generator = new AtlasGenerator(seed, rules(), AtlasUnlocks.NONE, AtlasMemory.NONE);
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
