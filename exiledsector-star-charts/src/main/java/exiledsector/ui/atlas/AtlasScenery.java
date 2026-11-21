package exiledsector.ui.atlas;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import exiledsector.charts.atlas.AtlasAmbience;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasData;
import exiledsector.charts.atlas.AtlasDecoration;
import exiledsector.charts.atlas.AtlasNode;
import exiledsector.charts.atlas.AtlasRegion;
import exiledsector.charts.atlas.TerritoryFactions;
import exiledsector.charts.content.ChartFactions;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.decoration.FactionStarColours;
import exiledsector.ui.decoration.HyperspaceClouds;

import java.awt.Color;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

final class AtlasScenery {

    static final float CLOUD_TILE = AtlasMapCanvas.CELL_PIXELS / 4f;
    static final float CLOUD_ALPHA = 0.35f;
    static final float KNOWN_SAMPLE_REACH = 0.5f;
    static final int VOID_JOIN_SIDES = 2;
    private static final int[][] SIDES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private final Function<String, AtlasAmbience> ambiences;
    private final Function<String, AtlasDecoration> decorationLookup;
    private final HyperspaceClouds clouds;
    private final AtlasAbyssShade abyssShade = new AtlasAbyssShade();
    private final AtlasDecorations decorations = new AtlasDecorations();
    private final AtlasFleetTraffic fleets = new AtlasFleetTraffic();
    private final AtlasTerritoryBorders borders;
    private final AtlasTerritoryBorders biomes;
    private final Set<Long> known = new HashSet<>();
    private final Set<Long> territoryCells = new HashSet<>();
    private AtlasTerritoryBridges.Bridges bridges = new AtlasTerritoryBridges.Bridges(Map.of(), Map.of());
    private AtlasMapCanvas canvas;

    AtlasScenery(long seed) {
        this(seed, AtlasData::ambience, AtlasData::decoration);
    }

    AtlasScenery(long seed, Function<String, AtlasAmbience> ambiences, Function<String, AtlasDecoration> decorationLookup) {
        this.ambiences = ambiences;
        this.decorationLookup = decorationLookup;
        clouds = new HyperspaceClouds(CLOUD_TILE, seed, CLOUD_ALPHA);
        borders = new AtlasTerritoryBorders(seed, territoryId -> factionColour(TerritoryFactions.factionOfTerritory(territoryId)),
                territoryId -> ChartFactions.displayName(TerritoryFactions.factionOfTerritory(territoryId)));
        biomes = new AtlasTerritoryBorders(seed, AtlasData::regionTint, AtlasNodeText::regionName);
    }

    AtlasDecorations decorations() {
        return decorations;
    }

    AtlasTerritoryBorders borders() {
        return borders;
    }

    AtlasTerritoryBorders biomes() {
        return biomes;
    }

    AtlasDecoration decoration(String decorationId) {
        return decorationLookup.apply(decorationId);
    }

    void attach(AtlasMapCanvas canvas) {
        this.canvas = canvas;
        known.clear();
        for (AtlasCell cell : canvas.visibleCells()) {
            for (AtlasCell around : cell.around(1)) {
                known.add(around.pack());
            }
        }
        abyssShade.attach(cell -> isKnown(cell) && ambience(cell).abyss(), known.stream().map(AtlasCell::unpack).toList());
        clouds.setCover(new Cover());
        decorations.attach(canvas, known);
        fleets.attach(canvas);
        List<AtlasCell> knownSystems = known.stream().map(AtlasCell::unpack).filter(this::hasSystem).toList();
        bridges = AtlasTerritoryBridges.bridges(knownSystems, canvas.state()::territoryAt, canvas.generator()::lanes,
                cell -> !canvas.generator().isVoid(cell));
        territoryCells.clear();
        territoryCells.addAll(known);
        territoryCells.addAll(bridges.cells().keySet());
        borders.attach(territoryCells, this::territoryAt, this::hasSystem, bridges.corners());
        biomes.attach(known, this::biomeAt, this::hasSystem);
    }

    private boolean hasSystem(AtlasCell cell) {
        AtlasNode node = canvas.nodeAt(cell);
        return node != null && !node.isVoid();
    }

    void advance(float amount) {
        clouds.advance(amount);
        decorations.advance(amount);
        fleets.advance(amount);
        borders.advance(amount);
        biomes.advance(amount);
    }

    void renderBackdrop(TreeViewport view, float alphaMult) {
        abyssShade.render(view, alphaMult);
        clouds.render(view, alphaMult);
        decorations.render(view, alphaMult);
        borders.render(view, alphaMult);
        biomes.render(view, alphaMult);
    }

    void renderAbove(TreeViewport view, float alphaMult) {
        fleets.render(view, alphaMult);
        clouds.renderFlashOver(view, alphaMult);
    }

    Set<Long> territoryCells() {
        return territoryCells;
    }

    boolean isKnown(AtlasCell cell) {
        return known.contains(cell.pack());
    }

    AtlasAmbience ambience(AtlasCell cell) {
        AtlasAmbience ambience = ambiences.apply(regionAt(cell));
        return ambience == null ? AtlasAmbience.BARE : ambience;
    }

    String regionAt(AtlasCell cell) {
        return cell.equals(AtlasCell.ORIGIN) ? AtlasRegion.NORMAL : canvas.generator().biomeOf(cell).regionId();
    }

    String biomeAt(AtlasCell cell) {
        String regionId = regionAt(cell);
        return AtlasRegion.NORMAL.equals(regionId) ? null : regionId;
    }

    String territoryAt(AtlasCell cell) {
        if (hasSystem(cell)) {
            return canvas.state().territoryAt(cell);
        }
        String bridged = bridges.cells().get(cell.pack());
        if (bridged != null) {
            return bridged;
        }
        Map<String, Integer> sidesByTerritory = new TreeMap<>();
        for (int[] side : SIDES) {
            AtlasCell neighbour = new AtlasCell(cell.gridX() + side[0], cell.gridY() + side[1]);
            String territoryId = hasSystem(neighbour) ? canvas.state().territoryAt(neighbour) : null;
            if (territoryId != null) {
                sidesByTerritory.merge(territoryId, 1, Integer::sum);
            }
        }
        String joined = null;
        int joinedSides = VOID_JOIN_SIDES - 1;
        for (Map.Entry<String, Integer> entry : sidesByTerritory.entrySet()) {
            if (entry.getValue() > joinedSides) {
                joined = entry.getKey();
                joinedSides = entry.getValue();
            }
        }
        return joined;
    }

    private static Color factionColour(String factionId) {
        Color starColour = FactionStarColours.colourOf(factionId);
        if (starColour != null) {
            return starColour;
        }
        FactionAPI faction = faction(factionId);
        return faction == null ? null : faction.getColor();
    }

    private static FactionAPI faction(String factionId) {
        return Global.getSector() == null || factionId == null ? null : Global.getSector().getFaction(factionId);
    }

    static AtlasCell cellAt(float worldX, float worldY) {
        return new AtlasCell(Math.round(worldX / AtlasMapCanvas.CELL_PIXELS), Math.round(worldY / AtlasMapCanvas.CELL_PIXELS));
    }

    private final class Cover implements HyperspaceClouds.Cover {

        @Override
        public float cover(float worldX, float worldY) {
            return canvas.generator().cloudAt(worldX / AtlasMapCanvas.CELL_PIXELS, worldY / AtlasMapCanvas.CELL_PIXELS) ? 1f : 0f;
        }

        @Override
        public float storms(float worldX, float worldY) {
            return ambience(cellAt(worldX, worldY)).storms();
        }

        @Override
        public Color colour(float worldX, float worldY) {
            return ambience(cellAt(worldX, worldY)).cloudColour();
        }

        @Override
        public float alpha(float worldX, float worldY) {
            float reach = AtlasMapCanvas.CELL_PIXELS * KNOWN_SAMPLE_REACH;
            int knownSamples = 0;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    knownSamples += isKnown(cellAt(worldX + dx * reach, worldY + dy * reach)) ? 1 : 0;
                }
            }
            return knownSamples / 9f;
        }
    }
}
