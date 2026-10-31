package exiledsector.charts.layout;

import com.fs.starfarer.api.campaign.JumpPointAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI.SurveyLevel;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.procgen.ProcgenUsedNames;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import exiledsector.MockSector;
import exiledsector.Seeds;
import exiledsector.charts.ChartTables;
import exiledsector.charts.FakeSystem;
import exiledsector.charts.ChartIds;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.generation.ChartSystemFactory;
import exiledsector.charts.layout.abyss.AbyssPalette;
import exiledsector.charts.run.cleanup.ChartUsedNames;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChartLayoutGeneratorTest {

    private static final String RUN_ID = "abc";

    private MockSector mockSector;
    private SectorAPI sector;
    private EconomyAPI economy;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        sector = mockSector.sector;
        economy = mock(EconomyAPI.class);
        when(sector.getEconomy()).thenReturn(economy);
        ProcgenUsedNames.notifyUsed("Corvus");
        ProcgenUsedNames.notifyUsed(ChartIds.SYSTEM_BASE_NAME);
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
        ChartLayoutGenerator.resetForTests();
    }

    private static void buildLike(FakeSystem fake, String starId, StarSystemType systemType, Random random) {
        PlanetAPI star = fake.addStar(starId, 400f + random.nextInt(400));
        if (systemType != StarSystemType.SINGLE) {
            fake.addStar(starId + "_b", 200f + random.nextInt(200));
        }
        int planets = 2 + random.nextInt(4);
        float orbit = 2500f;
        PlanetAPI first = null;
        for (int index = 0; index < planets; index++) {
            orbit += 1200f + random.nextInt(800);
            PlanetAPI planet = fake.addPlanet(starId + ":planet_" + index, 100f + random.nextInt(200), star, orbit);
            if (first == null) {
                first = planet;
            }
            ProcgenUsedNames.notifyUsed("Planet " + index);
        }
        fake.addPlanet("moon", 60f, first, 400f);
        fake.addJumpPoint();
        ProcgenUsedNames.notifyUsed(ChartIds.SYSTEM_BASE_NAME + " B");
    }

    private FakeChartLayoutEngine installBuilding(FakeSystem fake) {
        return FakeChartLayoutEngine.install(new FakeChartLayoutEngine((system, starId, systemType, random) ->
                buildLike(fake, starId, systemType, random)));
    }

    private record Generated(ChartLayout layout, List<String> names, long engineFingerprint) {
    }

    private Generated generateOnce(long seed, StarSystemType systemType) {
        FakeSystem fake = new FakeSystem();
        FakeChartLayoutEngine engine = installBuilding(fake);
        ChartLayout layout = ChartLayoutGenerator.generate(sector, fake.system, RUN_ID, new ChartLayoutRequest(seed, systemType));
        return new Generated(layout, fake.namesInOrder(), engine.builds.get(0).firstRandomLong());
    }

    @Test
    void theSameSeedAndTypeGiveTheSameLayoutNamesAndWormholeSpot() {
        Generated first = generateOnce(1234L, StarSystemType.BINARY_CLOSE);
        Generated second = generateOnce(1234L, StarSystemType.BINARY_CLOSE);

        assertEquals(first.names(), second.names());
        assertEquals(first.layout().innerWormholeLocation(), second.layout().innerWormholeLocation());
        assertEquals(first.engineFingerprint(), second.engineFingerprint());
        assertEquals(StarSystemType.BINARY_CLOSE, first.layout().systemType());
    }

    @Test
    void differentSeedsGiveDifferentLayouts() {
        Generated first = generateOnce(1L, StarSystemType.SINGLE);
        Generated second = generateOnce(2L, StarSystemType.SINGLE);

        assertNotEquals(first.engineFingerprint(), second.engineFingerprint());
        assertNotEquals(first.layout().innerWormholeLocation(), second.layout().innerWormholeLocation());
    }

    @Test
    void aSeedWhoseTypeWasPickedReproducesWhenThatTypeIsGivenExplicitly() {
        Generated picked = generateOnce(99L, null);
        Generated explicit = generateOnce(99L, picked.layout().systemType());

        assertEquals(picked.names(), explicit.names());
        assertEquals(picked.engineFingerprint(), explicit.engineFingerprint());
        assertEquals(picked.layout().innerWormholeLocation(), explicit.layout().innerWormholeLocation());
    }

    @Test
    void anAbyssalRecipeIsNarrowedToTheSeedsPaletteSoThePreviewAndTheSystemAgree() {
        Map<AbyssPalette, Float> palettes = new LinkedHashMap<>();
        palettes.put(new AbyssPalette("red", Color.RED, Color.BLACK, 1f), 1f);
        palettes.put(new AbyssPalette("amber", Color.ORANGE, Color.BLACK, 1f), 1f);
        ChartLayoutRecipe abyssal = new ChartLayoutRecipe(new ChartLook(List.of(), List.of(), Float.NaN, null, 0f, Map.of(), Map.of(), false, "",
                palettes), null);

        for (long seed = 0; seed < 50; seed++) {
            ChartLayoutRecipe settled = ChartLayoutGenerator.settleAbyss(seed, abyssal);

            assertEquals(Map.of(ChartLayoutGenerator.abyssPalette(seed, abyssal), 1f), settled.look().abyssPalettes());
            assertEquals(abyssal.shape(), settled.shape());
        }
        assertEquals(ChartLayoutRecipe.ORDINARY, ChartLayoutGenerator.settleAbyss(1L, ChartLayoutRecipe.ORDINARY));
    }

    @Test
    void picksOnlyTypesSafeWithoutAHyperspaceAnchor() {
        for (long seed = 0; seed < 2000; seed++) {
            StarSystemType picked = ChartLayoutGenerator.typeFor(new ChartLayoutRequest(seed, null));
            assertTrue(ChartLayoutGenerator.isAllowed(picked), "seed " + seed + " picked " + picked);
        }
        assertFalse(ChartLayoutGenerator.isAllowed(StarSystemType.NEBULA));
        assertFalse(ChartLayoutGenerator.isAllowed(StarSystemType.DEEP_SPACE));
        assertNull(ChartLayoutGenerator.parseType("nebula"));
        assertNull(new ChartLayoutRequest(1L, StarSystemType.NEBULA).systemType());
        assertEquals(StarSystemType.TRINARY_2FAR, ChartLayoutGenerator.parseType("trinary_2far"));
        assertNull(ChartLayoutGenerator.parseType("deep_space"));
    }

    @Test
    void onlyARecipeCanAskForADeepSpaceSystem() {
        assertEquals(StarSystemType.DEEP_SPACE, ChartLayoutGenerator.parseRecipeType("deep_space"));
        assertNull(ChartLayoutGenerator.parseRecipeType("nebula"));
        ChartLayoutRecipe starless = new ChartLayoutRecipe(ChartLook.PLAIN,
                new ChartShape(java.util.Map.of(StarSystemType.DEEP_SPACE, 1f), 0, 0, Float.NaN, java.util.Map.of(), ""));
        for (long seed = 0; seed < 50; seed++) {
            assertEquals(StarSystemType.DEEP_SPACE, ChartLayoutGenerator.typeFor(new ChartLayoutRequest(seed, null).withRecipe(starless)));
            assertNotEquals(StarSystemType.DEEP_SPACE, ChartLayoutGenerator.typeFor(new ChartLayoutRequest(seed, null)));
        }
    }

    @Test
    void everyAllowedTypeComesUpAcrossSeeds() {
        java.util.Set<StarSystemType> seen = java.util.EnumSet.noneOf(StarSystemType.class);
        for (long seed = 0; seed < 2000; seed++) {
            seen.add(ChartLayoutGenerator.typeFor(new ChartLayoutRequest(seed, null)));
        }
        assertEquals(java.util.Set.copyOf(ChartLayoutGenerator.ALLOWED_TYPES), seen);
    }

    @Test
    void generationLeavesNoJumpPointsAnchorOrHyperspacePresence() {
        FakeSystem fake = new FakeSystem();
        installBuilding(fake);
        LocationAPI hyperspace = mock(LocationAPI.class);
        SectorEntityToken anchor = mock(SectorEntityToken.class);
        when(anchor.getContainingLocation()).thenReturn(hyperspace);
        when(fake.system.getHyperspaceAnchor()).thenReturn(anchor);

        ChartLayoutGenerator.generate(sector, fake.system, RUN_ID, new ChartLayoutRequest(5L, StarSystemType.SINGLE));

        assertTrue(fake.entities.stream().noneMatch(JumpPointAPI.class::isInstance));
        verify(hyperspace).removeEntity(anchor);
        verify(fake.system).setHyperspaceAnchor(null);
        verify(fake.system, never()).generateAnchorIfNeeded();
        verify(fake.system, never()).autogenerateHyperspaceJumpPoints();
        verify(fake.system, never()).autogenerateHyperspaceJumpPoints(Mockito.anyBoolean(), Mockito.anyBoolean());
        verify(fake.system, never()).autogenerateHyperspaceJumpPoints(Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.anyBoolean());
        verify(sector, never()).getHyperspace();
    }

    @Test
    void aSystemwideNebulaNeverPutsTheSystemOnTheHyperspaceMap() {
        FakeSystem fake = new FakeSystem();
        FakeChartLayoutEngine.install(new FakeChartLayoutEngine((system, starId, systemType, random) -> {
            buildLike(fake, starId, systemType, random);
            system.setHasSystemwideNebula(true);
        }));

        ChartLayoutGenerator.generate(sector, fake.system, RUN_ID, new ChartLayoutRequest(6L, StarSystemType.SINGLE));

        org.mockito.InOrder order = Mockito.inOrder(fake.system);
        order.verify(fake.system).setHasSystemwideNebula(true);
        order.verify(fake.system).setHasSystemwideNebula(false);
        order.verify(fake.system, never()).setHasSystemwideNebula(true);
    }

    @Test
    void namesReservedWhileGeneratingAreReleasedAndOlderNamesStay() {
        int before = ChartUsedNames.count();

        generateOnce(7L, StarSystemType.BINARY_FAR);
        generateOnce(8L, StarSystemType.TRINARY_2CLOSE);

        assertEquals(before, ChartUsedNames.count());
        assertFalse(ProcgenUsedNames.isUsed("Planet 0"));
        assertFalse(ProcgenUsedNames.isUsed(ChartIds.SYSTEM_BASE_NAME + " B"));
        assertTrue(ProcgenUsedNames.isUsed("Corvus"));
        assertTrue(ProcgenUsedNames.isUsed(ChartIds.SYSTEM_BASE_NAME));
    }

    @Test
    void bodiesGetCatalogueNamesFromTheSeedAndMarketsFollow() {
        FakeSystem fake = new FakeSystem();
        installBuilding(fake);

        ChartLayoutGenerator.generate(sector, fake.system, RUN_ID, new ChartLayoutRequest(11L, StarSystemType.SINGLE));

        String designation = ChartBodyNames.designation(Seeds.derived(11L, ChartLayoutGenerator.NAME_SALT));
        for (SectorEntityToken entity : fake.entities) {
            if (entity instanceof PlanetAPI planet) {
                String name = fake.names.get(planet);
                assertTrue(name != null && name.contains(designation), planet.getId() + " was named " + name);
                if (planet.getMarket() != null) {
                    verify(planet.getMarket()).setName(name);
                }
            }
        }
    }

    @Test
    void planetsKeepConditionOnlyMarketsSeenButNeverInTheEconomy() {
        FakeSystem fake = new FakeSystem();
        PlanetAPI star = fake.addStar("star", 500f);
        PlanetAPI planet = fake.addPlanet("planet", 150f, star, 3000f);
        MarketAPI market = planet.getMarket();
        MarketConditionAPI visible = mock(MarketConditionAPI.class);
        MarketConditionAPI hidden = mock(MarketConditionAPI.class);
        when(hidden.requiresSurveying()).thenReturn(true);
        when(market.getConditions()).thenReturn(List.of(visible, hidden));
        when(economy.getMarket("market_planet")).thenReturn(market);
        FakeChartLayoutEngine.install(FakeChartLayoutEngine.doingNothing());

        ChartLayoutGenerator.generate(sector, fake.system, RUN_ID, new ChartLayoutRequest(3L, StarSystemType.SINGLE));

        verify(economy).removeMarket(market);
        verify(planet).addTag(Tags.NOT_RANDOM_MISSION_TARGET);
        verify(star).addTag(Tags.NOT_RANDOM_MISSION_TARGET);
        verify(market).setPlanetConditionMarketOnly(true);
        verify(market).setSurveyLevel(SurveyLevel.SEEN);
        verify(visible).setSurveyed(true);
        verify(hidden, never()).setSurveyed(true);
        verify(market).removeCondition(Conditions.RUINS_VAST);
        verify(market).removeCondition(Conditions.DECIVILIZED);
        verify(economy, never()).addMarket(Mockito.any(), Mockito.anyBoolean());
    }

    @Test
    void aFailedGenerationLeavesNothingBehindAndFallsBackToOneStar() {
        FakeSystem fake = new FakeSystem();
        PlanetAPI fallbackStar = mock(PlanetAPI.class);
        when(fallbackStar.isStar()).thenReturn(true);
        when(fake.system.initStar(anyString(), anyString(), anyFloat(), anyFloat())).thenAnswer(invocation -> {
            fake.entities.add(fallbackStar);
            fake.system.setStar(fallbackStar);
            return fallbackStar;
        });
        FakeChartLayoutEngine.install(new FakeChartLayoutEngine((system, starId, systemType, random) -> {
            buildLike(fake, starId, systemType, random);
            throw new IllegalStateException("spec missing");
        }));
        int namesBefore = ChartUsedNames.count();

        ChartLayout layout = ChartLayoutGenerator.generate(sector, fake.system, RUN_ID, new ChartLayoutRequest(4L, StarSystemType.TRINARY_2FAR));

        assertFalse(layout.generated());
        assertEquals(List.of(fallbackStar), fake.entities);
        verify(fake.system).setSecondary(null);
        verify(fake.system).setTertiary(null);
        verify(fake.system).setCenter(null);
        verify(fake.system).initStar(eq(ChartIds.starId(RUN_ID)), eq(ChartSystemFactory.STAR_TYPE), anyFloat(), anyFloat());
        assertEquals(namesBefore, ChartUsedNames.count());
    }

    @Test
    void aFailedGenerationKeepsOnlyTheAnomaliesThatNeedNoLayout() {
        ChartTables.useAnomalies(List.of(new ChartAnomalies.Definition("black_hole", 1f, 1, 15, "", "", false, Set.of(), true),
                new ChartAnomalies.Definition("faction_skirmish", 1f, 1, 15, "", "", true, Set.of(), false)));
        try {
            FakeSystem fake = new FakeSystem();
            when(fake.system.initStar(anyString(), anyString(), anyFloat(), anyFloat())).thenReturn(mock(PlanetAPI.class));
            FakeChartLayoutEngine.install(new FakeChartLayoutEngine((system, starId, systemType, random) -> {
                throw new IllegalStateException("spec missing");
            }));

            ChartLayout layout = ChartLayoutGenerator.generate(sector, fake.system, RUN_ID,
                    new ChartLayoutRequest(4L, null).withAnomalies(List.of("black_hole", "faction_skirmish")));

            assertFalse(layout.generated());
            assertEquals(List.of("faction_skirmish"), layout.anomalyIds());
        } finally {
            ChartTables.resetAnomalies();
        }
    }

    @Test
    void anEngineThatLeavesNoStarCountsAsAFailure() {
        FakeSystem fake = new FakeSystem();
        when(fake.system.initStar(anyString(), anyString(), anyFloat(), anyFloat())).thenReturn(mock(PlanetAPI.class));
        FakeChartLayoutEngine.install(FakeChartLayoutEngine.doingNothing());

        assertFalse(ChartLayoutGenerator.generate(sector, fake.system, RUN_ID, new ChartLayoutRequest(4L, null)).generated());
    }
}
