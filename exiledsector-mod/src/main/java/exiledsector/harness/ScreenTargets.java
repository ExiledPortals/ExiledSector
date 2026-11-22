package exiledsector.harness;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasReach;
import exiledsector.charts.atlas.AtlasRuntime;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.officer.OfficerSightings;
import exiledsector.items.hull.ShipSection;
import exiledsector.items.schematic.RefitSchematic;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.ui.HullItemsRefitButton;
import exiledsector.ui.ShownScreens;
import exiledsector.ui.SkillTreeCanvasPlugin;
import exiledsector.ui.SkillTreeRefitButton;
import exiledsector.ui.atlas.AtlasScreen;
import exiledsector.ui.atlas.AtlasTreeScreen;
import exiledsector.ui.refit.UiReflection;
import exiledsector.ui.socket.IconTileCell;
import exiledsector.ui.socket.ItemCell;
import exiledsector.ui.socket.SectionTile;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ScreenTargets {

    static final String CENTRE = "centre";
    static final String SKILL_TREE_BUTTON = "skillTreeButton";
    static final String HULL_ITEMS_BUTTON = "hullItemsButton";
    static final String NODE_PREFIX = "node:";
    static final String ITEM_PREFIX = "item:";
    static final String BUTTON_PREFIX = "button:";
    static final String SECTION_PREFIX = "section:";
    static final String SCHEMATIC_PREFIX = "schematic:";
    static final String PANEL_PREFIX = "panel:";
    static final String ATLAS_SYSTEM_PREFIX = "atlasSystem:";
    static final String ATLAS_NODE_PREFIX = "atlasNode:";
    static final String ATLAS_TERRITORY_PREFIX = "atlasTerritory:";
    static final String OFFICER_PREFIX = "officer:";
    static final String BATTLE_MARK_PREFIX = "battleMark:";
    static final String DESCRIPTION = CENTRE + ", " + SKILL_TREE_BUTTON + ", " + HULL_ITEMS_BUTTON + ", " + NODE_PREFIX + "<node id or type>, "
            + ITEM_PREFIX + "<item definition id>, " + BUTTON_PREFIX + "<button text or custom data>, " + SECTION_PREFIX + "<section id>, "
            + SCHEMATIC_PREFIX + "<refit schematic tier>, " + PANEL_PREFIX + "<plugin class>[#n], " + ATLAS_SYSTEM_PREFIX + "<n>, " + ATLAS_TERRITORY_PREFIX + "<n>, " + OFFICER_PREFIX + "<n>, " + BATTLE_MARK_PREFIX + "<n>, " + ATLAS_NODE_PREFIX + "<n> or a screen point \"x,y\"";
    static final float HIDDEN_BEYOND = -5000f;
    static final Pattern PANEL = Pattern.compile("([A-Za-z0-9_$]+)(?:#([1-9]\\d*))?");
    static final Pattern ORDINAL = Pattern.compile("[1-9]\\d*");
    static final Pattern POINT = Pattern.compile("(\\d+(?:\\.\\d+)?),\\s*(\\d+(?:\\.\\d+)?)");
    private static final List<String> PREFIXES = List.of(NODE_PREFIX, ITEM_PREFIX, BUTTON_PREFIX, SECTION_PREFIX, SCHEMATIC_PREFIX, PANEL_PREFIX,
            ATLAS_SYSTEM_PREFIX, ATLAS_TERRITORY_PREFIX, OFFICER_PREFIX, BATTLE_MARK_PREFIX, ATLAS_NODE_PREFIX);

    private ScreenTargets() {
    }

    static boolean isKnown(String target) {
        if (target.startsWith(SCHEMATIC_PREFIX)) {
            return schematicTier(target) != null;
        }
        for (String ordinalPrefix : List.of(ATLAS_SYSTEM_PREFIX, ATLAS_TERRITORY_PREFIX, OFFICER_PREFIX, BATTLE_MARK_PREFIX, ATLAS_NODE_PREFIX)) {
            if (target.startsWith(ordinalPrefix)) {
                return ORDINAL.matcher(target.substring(ordinalPrefix.length())).matches();
            }
        }
        if (target.startsWith(PANEL_PREFIX)) {
            return PANEL.matcher(target.substring(PANEL_PREFIX.length())).matches();
        }
        return CENTRE.equals(target) || SKILL_TREE_BUTTON.equals(target) || HULL_ITEMS_BUTTON.equals(target)
                || PREFIXES.stream().anyMatch(prefix -> target.startsWith(prefix) && target.length() > prefix.length())
                || POINT.matcher(target).matches();
    }

    static Vector2f locate(String target) throws Throwable {
        if (CENTRE.equals(target)) {
            return new Vector2f(Global.getSettings().getScreenWidth() / 2f, Global.getSettings().getScreenHeight() / 2f);
        }
        if (SKILL_TREE_BUTTON.equals(target)) {
            return refitButtonCentre(SkillTreeRefitButton.COLUMN_BUTTON.id());
        }
        if (HULL_ITEMS_BUTTON.equals(target)) {
            return refitButtonCentre(HullItemsRefitButton.COLUMN_BUTTON.id());
        }
        Matcher point = POINT.matcher(target);
        if (point.matches()) {
            return new Vector2f(Float.parseFloat(point.group(1)), Float.parseFloat(point.group(2)));
        }
        if (target.startsWith(ITEM_PREFIX)) {
            String definitionId = target.substring(ITEM_PREFIX.length());
            return panelCentre(ItemCell.class, cell -> definitionId.equals(cell.item().definitionId()), ItemCell::cellPosition);
        }
        if (target.startsWith(BUTTON_PREFIX)) {
            return buttonCentre(target.substring(BUTTON_PREFIX.length()));
        }
        if (target.startsWith(SECTION_PREFIX)) {
            String sectionId = target.substring(SECTION_PREFIX.length());
            return panelCentre(SectionTile.class, tile -> tile.section() == ShipSection.byIdOrNull(sectionId), SectionTile::tilePosition);
        }
        if (target.startsWith(SCHEMATIC_PREFIX)) {
            String iconPath = schematicTier(target).iconPath();
            return panelCentre(IconTileCell.class, cell -> iconPath.equals(cell.iconPath()), IconTileCell::cellPosition);
        }
        if (target.startsWith(PANEL_PREFIX)) {
            return pluginPanelCentre(target.substring(PANEL_PREFIX.length()));
        }
        if (target.startsWith(ATLAS_SYSTEM_PREFIX)) {
            return atlasSystemCentre(Integer.parseInt(target.substring(ATLAS_SYSTEM_PREFIX.length())));
        }
        if (target.startsWith(ATLAS_TERRITORY_PREFIX)) {
            return atlasTerritoryCentre(Integer.parseInt(target.substring(ATLAS_TERRITORY_PREFIX.length())));
        }
        if (target.startsWith(OFFICER_PREFIX)) {
            return nthInReadingOrder(atlasOrEmpty(AtlasScreen::officerFaceScreenCentres), Integer.parseInt(target.substring(OFFICER_PREFIX.length())));
        }
        if (target.startsWith(BATTLE_MARK_PREFIX)) {
            return nthInReadingOrder(atlasOrEmpty(AtlasScreen::battleMarkScreenCentres), Integer.parseInt(target.substring(BATTLE_MARK_PREFIX.length())));
        }
        if (target.startsWith(ATLAS_NODE_PREFIX)) {
            ShownScreens.Shown shown = ShownScreens.top();
            return shown != null && shown.screen() instanceof AtlasTreeScreen tree
                    ? nthNearestCentre(tree.allocatableNodeScreenCentres(), Integer.parseInt(target.substring(ATLAS_NODE_PREFIX.length())))
                    : null;
        }
        return nodeCentre(target.substring(NODE_PREFIX.length()));
    }

    private static Vector2f pluginPanelCentre(String panelTarget) throws Throwable {
        Matcher panelMatch = PANEL.matcher(panelTarget);
        if (!panelMatch.matches()) {
            return null;
        }
        String pluginName = panelMatch.group(1);
        int wantedIndex = panelMatch.group(2) == null ? 1 : Integer.parseInt(panelMatch.group(2));
        List<Vector2f> centres = new ArrayList<>();
        findShown(component -> {
            if (component instanceof CustomPanelAPI panel && panel.getPlugin() != null
                    && pluginName.equals(panel.getPlugin().getClass().getSimpleName())) {
                Vector2f centre = onScreenOrNull(panel.getPosition());
                if (centre != null) {
                    centres.add(centre);
                }
            }
            return centres.size() >= wantedIndex;
        });
        return centres.size() >= wantedIndex ? centres.get(wantedIndex - 1) : null;
    }

    private static Vector2f atlasSystemCentre(int wantedIndex) {
        ShownScreens.Shown shown = ShownScreens.top();
        if (shown == null || !(shown.screen() instanceof AtlasScreen atlas) || Global.getSector() == null) {
            return null;
        }
        List<Vector2f> systemCentres = AtlasReach.reachable(AtlasRuntime.generator(Global.getSector()), AtlasState.get(Global.getSector())).stream()
                .map(atlas::cellScreenCentre)
                .toList();
        return nthNearestCentre(systemCentres, wantedIndex);
    }

    private static Vector2f atlasTerritoryCentre(int wantedIndex) {
        ShownScreens.Shown shown = ShownScreens.top();
        if (shown == null || !(shown.screen() instanceof AtlasScreen atlas) || Global.getSector() == null) {
            return null;
        }
        AtlasState state = AtlasState.get(Global.getSector());
        AtlasGenerator generator = AtlasRuntime.generator(Global.getSector());
        Set<AtlasCell> shownSystems = new LinkedHashSet<>(AtlasReach.visible(generator, state));
        shownSystems.addAll(OfficerSightings.shownSystems(state));
        List<Vector2f> territoryCentres = shownSystems.stream()
                .filter(cell -> !cell.equals(AtlasCell.ORIGIN) && !generator.isVoid(cell) && !state.isCompleted(cell) && state.commandCentreOf(cell) != null)
                .map(atlas::cellScreenCentre)
                .toList();
        return nthNearestCentre(territoryCentres, wantedIndex);
    }

    private static List<Vector2f> atlasOrEmpty(Function<AtlasScreen, List<Vector2f>> centres) {
        ShownScreens.Shown shown = ShownScreens.top();
        return shown != null && shown.screen() instanceof AtlasScreen atlas ? centres.apply(atlas) : List.of();
    }

    private static Vector2f nthInReadingOrder(List<Vector2f> centres, int wantedIndex) {
        List<Vector2f> shownCentres = centres.stream()
                .filter(ScreenTargets::onScreen)
                .sorted(Comparator.comparingDouble((Vector2f centre) -> -centre.y).thenComparingDouble(centre -> centre.x))
                .toList();
        return shownCentres.size() >= wantedIndex ? shownCentres.get(wantedIndex - 1) : null;
    }

    private static Vector2f nthNearestCentre(List<Vector2f> centres, int wantedIndex) {
        float screenCentreX = Global.getSettings().getScreenWidth() / 2f;
        float screenCentreY = Global.getSettings().getScreenHeight() / 2f;
        List<Vector2f> shownCentres = centres.stream()
                .filter(centre -> centre != null && onScreen(centre))
                .sorted(Comparator.comparingDouble((Vector2f centre) -> Math.hypot(centre.x - screenCentreX, centre.y - screenCentreY)))
                .toList();
        return shownCentres.size() >= wantedIndex ? shownCentres.get(wantedIndex - 1) : null;
    }

    private static RefitSchematic schematicTier(String target) {
        String tierName = target.substring(SCHEMATIC_PREFIX.length()).toUpperCase(Locale.ROOT);
        return Arrays.stream(RefitSchematic.values()).filter(tier -> tier.name().equals(tierName)).findFirst().orElse(null);
    }

    static List<Object> roots() throws Throwable {
        List<Object> roots = new ArrayList<>();
        if (Global.getSector() == null || Global.getSector().getCampaignUI() == null) {
            return roots;
        }
        Object coreUI = UiReflection.coreUI(Global.getSector().getCampaignUI());
        if (coreUI != null) {
            roots.add(coreUI);
        }
        if (Global.getSector().getCampaignUI().getCurrentInteractionDialog() != null) {
            roots.add(Global.getSector().getCampaignUI().getCurrentInteractionDialog());
        }
        return roots;
    }

    static Object findShown(UiTree.Match match) throws Throwable {
        for (Object root : roots()) {
            Object found = UiTree.find(root, match);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static Vector2f refitButtonCentre(String buttonId) throws Throwable {
        Object button = findShown(component -> component instanceof ButtonAPI candidate && buttonId.equals(candidate.getCustomData()));
        if (!(button instanceof ButtonAPI refitButton)) {
            return null;
        }
        PositionAPI position = refitButton.getPosition();
        return position.getX() < HIDDEN_BEYOND ? null : new Vector2f(position.getCenterX(), position.getCenterY());
    }

    private static Vector2f buttonCentre(String textStart) throws Throwable {
        String wanted = textStart.toLowerCase(Locale.ROOT);
        Object button = findShown(component -> component instanceof ButtonAPI candidate && candidate.getPosition().getX() > HIDDEN_BEYOND
                && (startsWithIgnoringCase(candidate.getText(), wanted) || startsWithIgnoringCase(candidate.getCustomData(), wanted))
                && onScreen(new Vector2f(candidate.getPosition().getCenterX(), candidate.getPosition().getCenterY())));
        return button instanceof ButtonAPI found ? onScreenOrNull(found.getPosition()) : null;
    }

    private static boolean startsWithIgnoringCase(Object labelOrData, String wanted) {
        return labelOrData != null && labelOrData.toString().toLowerCase(Locale.ROOT).startsWith(wanted);
    }

    private static <T> Vector2f panelCentre(Class<T> pluginType, Predicate<T> wanted, Function<T, PositionAPI> positionOf) throws Throwable {
        Object found = findShown(component -> component instanceof CustomPanelAPI panel
                && pluginType.isInstance(panel.getPlugin()) && positionOf.apply(pluginType.cast(panel.getPlugin())) != null
                && wanted.test(pluginType.cast(panel.getPlugin())));
        return found instanceof CustomPanelAPI panel ? onScreenOrNull(positionOf.apply(pluginType.cast(panel.getPlugin()))) : null;
    }

    private static Vector2f onScreenOrNull(PositionAPI position) {
        Vector2f centre = new Vector2f(position.getCenterX(), position.getCenterY());
        return onScreen(centre) ? centre : null;
    }

    private static Vector2f nodeCentre(String nodeIdOrType) {
        ShownScreens.Shown shown = ShownScreens.top();
        if (shown == null || !(shown.screen() instanceof SkillTreeCanvasPlugin canvas) || canvas.treeSession().isStartingRootMoving()) {
            return null;
        }
        SkillNode byId = SkillTree.get(nodeIdOrType);
        if (byId != null) {
            return canvas.nodeScreenCentre(byId);
        }
        List<SkillNode> matches = new ArrayList<>();
        for (SkillNode node : SkillTree.getAllNodes().values()) {
            Vector2f centre = node.getType().getId().equals(nodeIdOrType) ? canvas.nodeScreenCentre(node) : null;
            if (centre != null && onScreen(centre)) {
                matches.add(node);
            }
        }
        return matches.stream()
                .min(Comparator.comparing((SkillNode node) -> !canvas.treeSession().isAllocated(node)).thenComparing(SkillNode::getId))
                .map(canvas::nodeScreenCentre)
                .orElse(null);
    }

    private static boolean onScreen(Vector2f point) {
        return point.x >= 0f && point.y >= 0f && point.x <= Global.getSettings().getScreenWidth() && point.y <= Global.getSettings().getScreenHeight();
    }
}
