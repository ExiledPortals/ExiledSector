package exiledsector.ui.socket;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.atlas.ActiveCatalysts;
import exiledsector.charts.atlas.AtlasCatalysts;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.SkillTreeSounds;
import exiledsector.ui.VanillaText;
import exiledsector.ui.cargo.CargoIconLook;
import exiledsector.ui.cargo.CatalystCargo;

import java.util.List;
import java.util.function.Supplier;

public final class AtlasAnomalyPanel extends HoloPanel {

    public interface Listener {

        void closed();
    }

    public record Entry(String title, String iconPath, CargoIconLook iconLook, StyledText body, String refusal) {

        public Entry(String title, String iconPath, StyledText body) {
            this(title, iconPath, null, body, null);
        }
    }

    public record Listing(List<Entry> system, List<Entry> catalysts) {

        public Listing {
            system = List.copyOf(system);
            catalysts = List.copyOf(catalysts);
        }
    }

    public static final float PANEL_WIDTH = 380f;
    static final int CATALYST_ROWS = 3;
    private static final float ICON_SIZE = 40f;
    private static final float ICON_TEXT_GAP = 10f;
    private static final float REFUSED_ICON_ALPHA = 0.5f;
    private static final float CATALYST_TOOLTIP_WIDTH = 340f;

    private enum Control {BACK}

    private final AtlasState state;
    private final Supplier<Listing> entries;
    private final Listener anomalyListener;
    private final SpriteCache spriteCache = new SpriteCache(AtlasAnomalyPanel.class);
    private UIComponentAPI headerElement;
    private TooltipMakerAPI catalystElement;
    private TooltipMakerAPI listElement;
    private float catalystsHeight;

    private AtlasAnomalyPanel(CustomPanelAPI hostPanel, AtlasState state, Supplier<Listing> entries, Listener anomalyListener) {
        super(hostPanel);
        this.state = state;
        this.entries = entries;
        this.anomalyListener = anomalyListener;
    }

    public static AtlasAnomalyPanel open(CustomPanelAPI hostPanel, float panelLeft, float panelTop, float panelHeight, AtlasState state,
                                         Supplier<Listing> entries, Listener anomalyListener) {
        AtlasAnomalyPanel anomalyPanel = new AtlasAnomalyPanel(hostPanel, state, entries, anomalyListener);
        anomalyPanel.attach(panelLeft, panelTop, PANEL_WIDTH, panelHeight, false);
        return anomalyPanel;
    }

    static int catalystColumns(int catalystCount) {
        return Math.max(1, (catalystCount + CATALYST_ROWS - 1) / CATALYST_ROWS);
    }

    @Override
    protected void onClose() {
        hoverTooltip.hide();
        anomalyListener.closed();
    }

    @Override
    protected UIComponentAPI[] chromeComponents() {
        return new UIComponentAPI[]{headerElement};
    }

    @Override
    protected UIComponentAPI[] contentComponents() {
        return new UIComponentAPI[]{catalystElement, placedComponent(listElement)};
    }

    @Override
    protected void onScrollInside() {
        queue(hoverTooltip::hide);
    }

    @Override
    protected void advanceOpen(float amount) {
        hoverTooltip.refreshIfExpansionChanged();
    }

    @Override
    public void buttonPressed(Object id) {
        if (id == Control.BACK) {
            queue(this::close);
        }
    }

    @Override
    protected void build() {
        ActiveCatalysts.turnOffMissing(state, playerMaterials()::count);
        rebuildHeader();
        rebuildCatalysts();
        rebuildList();
    }

    @Override
    protected void rebuildHeader() {
        headerElement = finishHeader(startHeader("chart.atlas.focus.title", "chart.atlas.focus.back", Control.BACK), headerElement);
        refreshOpacity();
    }

    private float catalystsTop() {
        return PAD + HEADER_HEIGHT + GAP;
    }

    private void rebuildCatalysts() {
        hoverTooltip.hide();
        if (catalystElement != null) {
            panelRoot.removeComponent(catalystElement);
            catalystElement = null;
        }
        catalystsHeight = 0f;
        List<AtlasCatalysts.Catalyst> catalysts = List.copyOf(AtlasCatalysts.all());
        if (catalysts.isEmpty()) {
            return;
        }
        int columns = catalystColumns(catalysts.size());
        float cellSize = SectionedTileGrid.cellSizeFor(innerWidth(), columns);
        TooltipMakerAPI element = panelRoot.createUIElement(innerWidth(), panelHeight, false);
        element.addSectionHeading(Translation.text("catalyst.window.title"), Alignment.MID, 0f);
        List<SectionedTileGrid.Tile> tiles = catalysts.stream()
                .map(catalyst -> new SectionedTileGrid.Tile(catalystTile(catalyst), carried(catalyst))).toList();
        SectionedTileGrid.addRows(element, tiles, columns, innerWidth(), cellSize);
        catalystsHeight = element.getHeightSoFar();
        element.getPosition().setSize(innerWidth(), catalystsHeight);
        catalystElement = replaceElement(null, element, PAD, catalystsTop());
        refreshOpacity();
    }

    private static int carried(AtlasCatalysts.Catalyst catalyst) {
        return playerMaterials().count(catalyst.itemId());
    }

    private CraftingBenchTile catalystTile(AtlasCatalysts.Catalyst catalyst) {
        String itemId = catalyst.itemId();
        int count = carried(catalyst);
        TileInput.Handler handler = TileInput.Handler.of(tilePosition -> queue(() -> showCatalystTooltip(catalyst, tilePosition)), () -> queue(() -> {
            if (hoverTooltip.isShowing(itemId)) {
                hoverTooltip.hide();
            }
        }), () -> {
            queue(() -> toggle(catalyst));
            return true;
        }, () -> false);
        return CraftingBenchTile.selectable(catalyst.iconPath(), CatalystCargo.look(catalyst.family()), () -> count > 0,
                () -> ActiveCatalysts.isOn(state, itemId), handler);
    }

    private void toggle(AtlasCatalysts.Catalyst catalyst) {
        ActiveCatalysts.Toggle toggle = ActiveCatalysts.toggle(state, catalyst, carried(catalyst));
        if (toggle == ActiveCatalysts.Toggle.NONE_OWNED) {
            SkillTreeSounds.refused();
            return;
        }
        SkillTreeSounds.socketed();
        rebuildCatalysts();
        rebuildList();
    }

    private void showCatalystTooltip(AtlasCatalysts.Catalyst catalyst, PositionAPI anchor) {
        if (isClosing()) {
            hoverTooltip.hide();
            return;
        }
        hoverTooltip.show(catalyst.itemId(), CATALYST_TOOLTIP_WIDTH, (element, expanded) -> writeCatalystTooltip(element, catalyst), anchor,
                ItemHoverTooltip.Placement.BELOW, PAD);
    }

    private void writeCatalystTooltip(TooltipMakerAPI element, AtlasCatalysts.Catalyst catalyst) {
        CatalystCargo.writeTooltip(element, catalyst.itemId(), GAP);
        if (ActiveCatalysts.isOn(state, catalyst.itemId())) {
            VanillaText.plainPara(element, Translation.text("catalyst.cell.on"), GAP, Misc.getGrayColor());
        }
    }

    private void rebuildList() {
        float listTop = catalystsTop() + (catalystsHeight > 0f ? catalystsHeight + PAD : 0f);
        TooltipMakerAPI element = panelRoot.createUIElement(innerWidth(), Math.max(0f, panelHeight - listTop - PAD), true);
        Listing shown = entries.get();
        if (shown.system().isEmpty()) {
            VanillaText.plainPara(element, Translation.text("chart.atlas.focus.empty"), 0f, Misc.getGrayColor());
        }
        shown.system().forEach(entry -> addEntry(element, entry));
        if (!shown.catalysts().isEmpty()) {
            element.addSectionHeading(Translation.text("chart.atlas.focus.catalysts"), Alignment.MID, PAD);
            shown.catalysts().forEach(entry -> addEntry(element, entry));
        }
        listElement = replaceElement(placedComponent(listElement), element, PAD, listTop);
        refreshOpacity();
    }

    private void addEntry(TooltipMakerAPI element, Entry entry) {
        float pad = element.getHeightSoFar() > 0f ? PAD : 0f;
        boolean withIcon = !entry.iconPath().isEmpty() && spriteCache.ensureLoaded(entry.iconPath());
        if (withIcon && entry.iconLook() != null) {
            addAnimatedEntry(element, entry, pad);
            return;
        }
        TooltipMakerAPI text = withIcon ? element.beginImageWithText(entry.iconPath(), ICON_SIZE) : element;
        writeEntryText(text, entry, withIcon ? 0f : pad);
        if (withIcon) {
            element.addImageWithText(pad);
        }
    }

    private void addAnimatedEntry(TooltipMakerAPI element, Entry entry, float pad) {
        float iconAlpha = entry.refusal() != null ? REFUSED_ICON_ALPHA : 1f;
        element.addCustom(Global.getSettings().createCustom(ICON_SIZE, ICON_SIZE, new CargoLookIcon(entry.iconPath(), entry.iconLook(), iconAlpha)), pad);
        UIComponentAPI icon = element.getPrev();
        TooltipMakerAPI text = element.beginSubTooltip(innerWidth() - ICON_SIZE - ICON_TEXT_GAP);
        writeEntryText(text, entry, 0f);
        element.endSubTooltip();
        element.addCustomDoNotSetPosition(text).getPosition().rightOfTop(icon, ICON_TEXT_GAP);
        float overhang = text.getHeightSoFar() - ICON_SIZE;
        if (overhang > 0f) {
            element.addSpacer(overhang);
        }
    }

    private static void writeEntryText(TooltipMakerAPI text, Entry entry, float pad) {
        boolean refused = entry.refusal() != null;
        text.setParaFont(Fonts.ORBITRON_12);
        VanillaText.plainPara(text, entry.title(), pad, refused ? Misc.getGrayColor() : Misc.getHighlightColor());
        text.setParaFontDefault();
        if (refused) {
            VanillaText.plainPara(text, entry.refusal(), LINE_PAD, Misc.getNegativeHighlightColor());
        }
        VanillaText.addPara(text, entry.body(), LINE_PAD, refused ? Misc.getGrayColor() : Misc.getTextColor());
    }
}
