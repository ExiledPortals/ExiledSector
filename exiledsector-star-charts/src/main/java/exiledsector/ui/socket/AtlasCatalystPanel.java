package exiledsector.ui.socket;

import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.Translation;
import exiledsector.charts.atlas.ActiveCatalysts;
import exiledsector.charts.atlas.AtlasCatalysts;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.items.starchart.AtlasCatalystItems;
import exiledsector.ui.SkillTreeSounds;
import exiledsector.ui.VanillaText;
import exiledsector.ui.cargo.CatalystCargo;

import java.util.ArrayList;
import java.util.List;

public final class AtlasCatalystPanel extends HoloPanel {

    public interface Listener {

        void toggled();

        void closed();
    }

    public static final float PANEL_WIDTH = 380f;
    private static final float FAMILY_HEIGHT = 22f;
    private static final float NOTICE_HEIGHT = 40f;
    private static final float TOOLTIP_WIDTH = 340f;
    private static final int MIN_GRID_ROWS = 2;

    private enum Control {CLOSE}

    private final AtlasState state;
    private final Listener catalystListener;
    private UIComponentAPI headerElement;
    private TooltipMakerAPI gridElement;
    private UIComponentAPI noticeElement;

    private AtlasCatalystPanel(CustomPanelAPI hostPanel, AtlasState state, Listener catalystListener) {
        super(hostPanel);
        this.state = state;
        this.catalystListener = catalystListener;
    }

    public static AtlasCatalystPanel open(CustomPanelAPI hostPanel, float panelLeft, float panelTop, float maxHeight, AtlasState state,
                                          Listener catalystListener) {
        AtlasCatalystPanel catalystPanel = new AtlasCatalystPanel(hostPanel, state, catalystListener);
        float height = Math.min(maxHeight, preferredHeight());
        catalystPanel.attach(panelLeft, panelTop, PANEL_WIDTH, height, false);
        return catalystPanel;
    }

    static float preferredHeight() {
        int columns = ItemGrid.columnsFor(PANEL_WIDTH);
        float cellSize = ItemGrid.cellSize(PANEL_WIDTH - PAD * 2f, columns);
        List<Integer> familySizes = new ArrayList<>();
        for (AtlasCatalysts.Family family : AtlasCatalysts.Family.values()) {
            familySizes.add((int) AtlasCatalysts.all().stream().filter(catalyst -> catalyst.family() == family).count());
        }
        float gridHeight = SectionedTileGrid.height(PANEL_WIDTH, familySizes);
        float minimum = MIN_GRID_ROWS * (cellSize + GAP) + FAMILY_HEIGHT;
        return PAD + HEADER_HEIGHT + GAP + Math.max(minimum, gridHeight) + NOTICE_HEIGHT + PAD;
    }

    @Override
    protected void onClose() {
        hoverTooltip.hide();
        catalystListener.closed();
    }

    @Override
    protected UIComponentAPI[] chromeComponents() {
        return new UIComponentAPI[]{headerElement};
    }

    @Override
    protected UIComponentAPI[] contentComponents() {
        return new UIComponentAPI[]{placedComponent(gridElement), noticeElement};
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
        if (id == Control.CLOSE) {
            queue(this::close);
        }
    }

    @Override
    protected void build() {
        ActiveCatalysts.turnOffMissing(state, playerMaterials()::count);
        rebuildHeader();
        rebuildGrid();
    }

    @Override
    protected void rebuildHeader() {
        headerElement = finishHeader(startHeader("catalyst.window.title", "catalyst.window.close", Control.CLOSE), headerElement);
        refreshOpacity();
    }

    private float gridTop() {
        return PAD + HEADER_HEIGHT + GAP;
    }

    private void rebuildGrid() {
        hoverTooltip.hide();
        List<AtlasCatalystItems.Carried> carried = AtlasCatalystItems.carried(playerMaterials());
        List<SectionedTileGrid.Section> sections = new ArrayList<>();
        for (AtlasCatalysts.Family family : AtlasCatalysts.Family.values()) {
            List<SectionedTileGrid.Tile> tiles = carried.stream().filter(entry -> entry.catalyst().family() == family)
                    .map(entry -> new SectionedTileGrid.Tile(catalystTile(entry), entry.count())).toList();
            if (!tiles.isEmpty()) {
                sections.add(new SectionedTileGrid.Section(AtlasCatalystItems.familyName(family), AtlasCatalystItems.color(family), tiles));
            }
        }
        TooltipMakerAPI element = SectionedTileGrid.build(panelRoot, panelWidth, panelHeight - gridTop() - NOTICE_HEIGHT - PAD, sections);
        if (carried.isEmpty()) {
            VanillaText.plainPara(element, Translation.text("catalyst.window.empty"), 0f, Misc.getGrayColor());
        }
        gridElement = replaceElement(placedComponent(gridElement), element, PAD, gridTop());
        rebuildNotice();
        refreshOpacity();
    }

    private CraftingBenchTile catalystTile(AtlasCatalystItems.Carried entry) {
        AtlasCatalysts.Catalyst catalyst = entry.catalyst();
        String itemId = catalyst.itemId();
        TileInput.Handler handler = TileInput.Handler.of(tilePosition -> queue(() -> showTooltip(entry, tilePosition)), () -> queue(() -> {
            if (hoverTooltip.isShowing(itemId)) {
                hoverTooltip.hide();
            }
        }), () -> {
            queue(() -> toggle(catalyst));
            return true;
        }, () -> false);
        return CraftingBenchTile.selectable(catalyst.iconPath(), CatalystCargo.look(catalyst.family()), () -> ActiveCatalysts.isOn(state, itemId), handler);
    }

    private void toggle(AtlasCatalysts.Catalyst catalyst) {
        ActiveCatalysts.Toggle toggle = ActiveCatalysts.toggle(state, catalyst, playerMaterials().count(catalyst.itemId()));
        if (toggle == ActiveCatalysts.Toggle.NONE_OWNED) {
            SkillTreeSounds.refused();
        } else {
            SkillTreeSounds.socketed();
        }
        rebuildGrid();
        catalystListener.toggled();
    }

    private void showTooltip(AtlasCatalystItems.Carried entry, PositionAPI anchor) {
        if (isClosing()) {
            hoverTooltip.hide();
            return;
        }
        hoverTooltip.show(entry.catalyst().itemId(), TOOLTIP_WIDTH, (element, expanded) -> writeTooltip(element, entry), anchor,
                ItemHoverTooltip.Placement.BELOW, PAD);
    }

    private void writeTooltip(TooltipMakerAPI element, AtlasCatalystItems.Carried entry) {
        AtlasCatalysts.Catalyst catalyst = entry.catalyst();
        CatalystCargo.writeTooltip(element, catalyst.itemId(), GAP);
        VanillaText.plainPara(element, Translation.msg("catalyst.cell.inCargo").arg("count", entry.count()).text(), GAP, Misc.getGrayColor());
        String hintKey = ActiveCatalysts.isOn(state, catalyst.itemId()) ? "catalyst.cell.on" : "catalyst.cell.off";
        VanillaText.plainPara(element, Translation.text(hintKey), GAP, Misc.getGrayColor());
    }

    private void rebuildNotice() {
        TooltipMakerAPI notice = panelRoot.createUIElement(innerWidth(), NOTICE_HEIGHT, false);
        VanillaText.plainPara(notice, Translation.text("catalyst.window.hint"), 0f, Misc.getGrayColor());
        noticeElement = replaceElement(noticeElement, notice, PAD, panelHeight - PAD - NOTICE_HEIGHT);
    }
}
