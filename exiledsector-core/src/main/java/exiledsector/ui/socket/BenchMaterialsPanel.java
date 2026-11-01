package exiledsector.ui.socket;

import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;

import java.util.List;

final class BenchMaterialsPanel extends HoloPanel {

    private final Runnable scrolled;
    private List<SectionedTileGrid.Section> sections;
    private UIComponentAPI headerElement;
    private TooltipMakerAPI gridElement;

    private BenchMaterialsPanel(CustomPanelAPI hostPanel, List<SectionedTileGrid.Section> sections, Runnable scrolled) {
        super(hostPanel);
        this.sections = List.copyOf(sections);
        this.scrolled = scrolled;
    }

    static BenchMaterialsPanel open(CustomPanelAPI hostPanel, BenchLayout.Rect area, List<SectionedTileGrid.Section> sections, Runnable scrolled) {
        BenchMaterialsPanel materialsPanel = new BenchMaterialsPanel(hostPanel, sections, scrolled);
        float height = Math.min(area.height(), preferredHeight(area.width(), sections));
        materialsPanel.attach(area.left(), area.top(), area.width(), height, false);
        return materialsPanel;
    }

    static float preferredHeight(float width, List<SectionedTileGrid.Section> sections) {
        return PAD + HEADER_HEIGHT + GAP + SectionedTileGrid.height(width, sections.stream().map(section -> section.tiles().size()).toList()) + PAD;
    }

    void show(List<SectionedTileGrid.Section> shown) {
        queue(() -> {
            sections = List.copyOf(shown);
            rebuildHeader();
            rebuildGrid();
        });
    }

    @Override
    protected void onClose() {
        scrolled.run();
    }

    @Override
    protected UIComponentAPI[] chromeComponents() {
        return new UIComponentAPI[]{headerElement};
    }

    @Override
    protected UIComponentAPI[] contentComponents() {
        return new UIComponentAPI[]{placedComponent(gridElement)};
    }

    @Override
    protected void onScrollInside() {
        scrolled.run();
    }

    @Override
    protected void build() {
        rebuildHeader();
        rebuildGrid();
    }

    @Override
    protected void rebuildHeader() {
        TooltipMakerAPI element = startHeader("ui.workbench.materials.title", null, null);
        addPartsCount(element, 0f);
        headerElement = finishHeader(element, headerElement);
        refreshOpacity();
    }

    private float gridTop() {
        return PAD + HEADER_HEIGHT + GAP;
    }

    private void rebuildGrid() {
        TooltipMakerAPI element = SectionedTileGrid.build(panelRoot, panelWidth, panelHeight - gridTop() - PAD, sections);
        gridElement = replaceElement(placedComponent(gridElement), element, PAD, gridTop());
        refreshOpacity();
    }
}
