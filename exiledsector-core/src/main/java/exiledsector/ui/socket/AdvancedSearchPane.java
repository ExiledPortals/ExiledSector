package exiledsector.ui.socket;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TextFieldAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.items.ItemKind;
import exiledsector.items.ItemRarity;
import exiledsector.items.ItemStatPool;
import exiledsector.ui.VanillaText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

final class AdvancedSearchPane extends HoloPanel {

    interface Host {

        List<ItemKind> typeChoices();

        Collection<ItemKind> rollableKinds();

        void filtersPicked(boolean sectionsChanged);

        void searchApplied();

        void pressedInside();

        void closed();
    }

    private static final float LABEL_HEIGHT = 18f;
    private static final float LABEL_TEXT_TOP = 4f;
    private static final float TOOLTIP_WIDTH = 320f;
    private static final float COUNT_FIELD_WIDTH = 36f;
    private static final int PICKER_MAX_CHARS = 120;
    private static final int BOUND_MAX_CHARS = 10;
    private static final int COUNT_MAX_CHARS = 3;
    private static final String TITLE_KEY = "ui.advancedSearch.title";

    private enum Action {
        ADD_AND_GROUP("ui.advancedSearch.addAndGroup"),
        ADD_COUNT_GROUP("ui.advancedSearch.addCountGroup"),
        RESET("ui.advancedSearch.reset"),
        SEARCH("ui.advancedSearch.search");

        private final String labelKey;

        Action(String labelKey) {
            this.labelKey = labelKey;
        }
    }

    private record TypeChip(ItemKind sectionKind) {
    }

    private record RarityChip(ItemRarity rarity) {
    }

    private record AddRow(ModifierSearch.Group group) {
    }

    private record RemoveRow(ModifierSearch.Group group, ModifierSearch.Row row) {
    }

    private record CycleLines(ModifierSearch.Row row) {
    }

    private static final class PolledField {

        private final TextFieldAPI field;
        private final Consumer<String> store;
        private String polledText;

        PolledField(TextFieldAPI field, Consumer<String> store) {
            this.field = field;
            this.store = store;
            this.polledText = field.getText();
        }

        void poll() {
            String text = field.getText();
            if (!text.equals(polledText)) {
                polledText = text;
                store.accept(text);
            }
        }
    }

    private final Host host;
    private final ItemStorageFilter storageFilter;
    private final ModifierSearch search;
    private final AdvancedSearchLayout layout;
    private final List<StatPicker> pickers = new ArrayList<>();
    private final List<PolledField> polledFields = new ArrayList<>();
    private final Map<ModifierSearch.Row, ButtonAPI> linesButtons = new IdentityHashMap<>();
    private final Map<Action, ButtonAPI> actionButtons = new IdentityHashMap<>();
    private UIComponentAPI headerElement;
    private UIComponentAPI filtersElement;
    private TooltipMakerAPI groupsElement;
    private UIComponentAPI actionsElement;
    private StatPicker activePicker;

    private AdvancedSearchPane(CustomPanelAPI hostPanel, AdvancedSearchLayout layout, ItemStorageFilter storageFilter, Host host) {
        super(hostPanel);
        this.layout = layout;
        this.storageFilter = storageFilter;
        this.search = storageFilter.modifierSearch;
        this.host = host;
    }

    static AdvancedSearchPane open(CustomPanelAPI hostPanel, AdvancedSearchLayout layout, ItemStorageFilter storageFilter, Host host) {
        AdvancedSearchPane pane = new AdvancedSearchPane(hostPanel, layout, storageFilter, host);
        pane.keepAGroup();
        float paneHeight = layout.paneHeight(pane.contentHeight());
        pane.attach(layout.paneLeft(), layout.paneTop(paneHeight), layout.paneWidth(), paneHeight, false);
        return pane;
    }

    void refresh() {
        queue(this::rebuild);
    }

    boolean escape() {
        if (activePicker != null && activePicker.isListOpen()) {
            queue(this::closePicker);
            return true;
        }
        close();
        return true;
    }

    @Override
    protected void build() {
        pickers.clear();
        polledFields.clear();
        linesButtons.clear();
        actionButtons.clear();
        activePicker = null;
        headerElement = finishHeader(startHeader(TITLE_KEY, null, null), null);
        float filtersTop = PAD + HEADER_HEIGHT + GAP;
        filtersElement = buildFilters(filtersTop);
        float groupsTop = filtersTop + filtersHeight() + GAP;
        float actionsHeight = actionsHeight();
        float actionsTop = panelHeight - PAD - actionsHeight;
        groupsElement = buildGroups(groupsTop, Math.max(AdvancedSearchLayout.CONTROL_HEIGHT, actionsTop - GAP - groupsTop));
        actionsElement = buildActions(actionsTop, actionsHeight);
        refreshOpacity();
    }

    private void rebuild() {
        pollFields();
        float restoredScroll = groupsScrollOffset();
        closePicker();
        keepAGroup();
        float paneHeight = layout.paneHeight(contentHeight());
        reattach(layout.paneLeft(), layout.paneTop(paneHeight), layout.paneWidth(), paneHeight);
        build();
        if (restoredScroll > 0f && groupsElement.getExternalScroller() != null) {
            float viewHeight = groupsElement.getExternalScroller().getPosition().getHeight();
            groupsElement.getExternalScroller().setYOffset(Math.min(restoredScroll, Math.max(0f, groupsElement.getHeightSoFar() - viewHeight)));
        }
    }

    private void keepAGroup() {
        if (search.groups().isEmpty()) {
            search.addGroup(ModifierSearch.GroupKind.AND);
        }
    }

    private float groupsScrollOffset() {
        if (groupsElement == null || groupsElement.getExternalScroller() == null) {
            return 0f;
        }
        return groupsElement.getExternalScroller().getYOffset();
    }

    private float contentHeight() {
        return PAD + HEADER_HEIGHT + GAP + filtersHeight() + GAP + groupsHeight() + GAP + actionsHeight() + PAD;
    }

    private float filtersHeight() {
        List<String> typeLabels = typeLabels();
        float typesHeight = typeLabels.isEmpty() ? 0f : chipFilterHeight(typeLabels) + GAP;
        return typesHeight + chipFilterHeight(rarityLabels());
    }

    private float chipFilterHeight(List<String> labels) {
        return LABEL_HEIGHT + ChipFlow.height(ChipFlow.rowsFor(labels, layout.innerWidth()));
    }

    private float groupsHeight() {
        float rowHeight = layout.columns().rowHeight();
        float groupsHeight = 0f;
        for (ModifierSearch.Group group : search.groups()) {
            groupsHeight += groupsHeight > 0f ? PAD : 0f;
            groupsHeight += AdvancedSearchLayout.CONTROL_HEIGHT + GAP + group.rows.size() * (rowHeight + GAP) + AdvancedSearchLayout.CONTROL_HEIGHT;
        }
        return groupsHeight;
    }

    private float actionsHeight() {
        return ChipFlow.height(ChipFlow.rowsFor(actionLabels(), layout.innerWidth()));
    }

    private List<String> typeLabels() {
        return host.typeChoices().stream().map(ItemKind::displayName).toList();
    }

    private static List<String> rarityLabels() {
        return Arrays.stream(ItemRarity.values()).map(ItemStoragePanel::rarityLabel).toList();
    }

    private static List<String> actionLabels() {
        return Arrays.stream(Action.values()).map(action -> Translation.text(action.labelKey)).toList();
    }

    private UIComponentAPI buildFilters(float filtersTop) {
        TooltipMakerAPI element = panelRoot.createUIElement(layout.innerWidth(), filtersHeight(), false);
        float blockTop = 0f;
        List<ItemKind> typeChoices = host.typeChoices();
        if (!typeChoices.isEmpty()) {
            ChipFlow typeFlow = startChipFilter(element, "ui.socketStorage.menu.type", blockTop);
            for (ItemKind sectionKind : typeChoices) {
                typeFlow.place(sectionKind.displayName(), new TypeChip(sectionKind), ChipFlow.Colors.of(ItemStoragePanel.typeColor(sectionKind)),
                        storageFilter.sectionKinds.contains(sectionKind));
            }
            blockTop += chipFilterHeight(typeLabels()) + GAP;
        }
        ChipFlow rarityFlow = startChipFilter(element, "ui.socketStorage.menu.rarity", blockTop);
        for (ItemRarity rarity : ItemRarity.values()) {
            rarityFlow.place(ItemStoragePanel.rarityLabel(rarity), new RarityChip(rarity), ChipFlow.Colors.of(rarity.color()),
                    storageFilter.rarities.contains(rarity));
        }
        return replaceElement(null, element, PAD, filtersTop);
    }

    private ChipFlow startChipFilter(TooltipMakerAPI element, String labelKey, float blockTop) {
        VanillaText.plainPara(element, Translation.text(labelKey), 0f, Misc.getGrayColor()).getPosition().inTL(0f, blockTop);
        return new ChipFlow(element, layout.innerWidth(), blockTop + LABEL_HEIGHT);
    }

    private TooltipMakerAPI buildGroups(float groupsTop, float viewHeight) {
        TooltipMakerAPI element = panelRoot.createUIElement(layout.innerWidth(), viewHeight, true);
        AdvancedSearchLayout.RowColumns columns = layout.columns();
        float rowTop = 0f;
        for (ModifierSearch.Group group : search.groups()) {
            rowTop += rowTop > 0f ? PAD : 0f;
            addGroupHeader(element, group, rowTop, columns);
            rowTop += AdvancedSearchLayout.CONTROL_HEIGHT + GAP;
            for (ModifierSearch.Row row : group.rows) {
                addRow(element, group, row, rowTop, columns);
                rowTop += columns.rowHeight() + GAP;
            }
            String addRowLabel = Translation.text("ui.advancedSearch.addRow");
            ChipFlow.Colors colors = ChipFlow.Colors.player();
            element.addAreaCheckbox(addRowLabel, new AddRow(group), colors.base(), colors.dark(), colors.bright(), ChipFlow.width(addRowLabel),
                    AdvancedSearchLayout.CONTROL_HEIGHT, 0f).getPosition().inTL(0f, rowTop);
            rowTop += AdvancedSearchLayout.CONTROL_HEIGHT;
        }
        element.setHeightSoFar(rowTop);
        panelRoot.addUIElement(element).inTL(PAD, groupsTop);
        return element;
    }

    private void addGroupHeader(TooltipMakerAPI element, ModifierSearch.Group group, float rowTop, AdvancedSearchLayout.RowColumns columns) {
        String kindKey = group.kind == ModifierSearch.GroupKind.AND ? "ui.advancedSearch.group.and" : "ui.advancedSearch.group.count";
        String kindText = Translation.text(kindKey);
        float kindWidth = Global.getSettings().computeStringWidth(kindText, Fonts.DEFAULT_SMALL) + LINE_PAD;
        LabelAPI kindLabel = VanillaText.plainPara(element, kindText, 0f, Misc.getBasePlayerColor());
        kindLabel.autoSizeToWidth(kindWidth).inTL(0f, rowTop + LABEL_TEXT_TOP);
        element.addTooltipToPrevious(tooltip(kindKey + ".tooltip"), TooltipMakerAPI.TooltipLocation.BELOW);
        if (group.kind == ModifierSearch.GroupKind.COUNT) {
            TextFieldAPI countField = element.addTextField(COUNT_FIELD_WIDTH, AdvancedSearchLayout.CONTROL_HEIGHT, Fonts.DEFAULT_SMALL, 0f);
            countField.getPosition().inTL(kindWidth + GAP, rowTop);
            countField.setMaxChars(COUNT_MAX_CHARS);
            countField.setText(group.countText);
            element.addTooltipToPrevious(tooltip(kindKey + ".tooltip"), TooltipMakerAPI.TooltipLocation.BELOW);
            polledFields.add(new PolledField(countField, text -> group.countText = text));
        }
        if (!layout.twoLineRows()) {
            addColumnLabel(element, "ui.advancedSearch.column.min", columns.minLeft(), rowTop, columns.boundWidth());
            addColumnLabel(element, "ui.advancedSearch.column.max", columns.maxLeft(), rowTop, columns.boundWidth());
        }
    }

    private static void addColumnLabel(TooltipMakerAPI element, String labelKey, float columnLeft, float rowTop, float columnWidth) {
        VanillaText.plainPara(element, Translation.text(labelKey), 0f, Misc.getGrayColor()).autoSizeToWidth(columnWidth)
                .inTL(columnLeft, rowTop + LABEL_TEXT_TOP);
    }

    private void addRow(TooltipMakerAPI element, ModifierSearch.Group group, ModifierSearch.Row row, float rowTop,
                        AdvancedSearchLayout.RowColumns columns) {
        TextFieldAPI pickerField = element.addTextField(columns.pickerWidth(), AdvancedSearchLayout.CONTROL_HEIGHT, Fonts.DEFAULT_SMALL, 0f);
        pickerField.getPosition().inTL(0f, rowTop);
        pickerField.setLimitByStringWidth(false);
        pickerField.setMaxChars(PICKER_MAX_CHARS);
        pickerField.setText(row.hasStat() ? ItemStatPool.label(row.statName) : "");
        element.addTooltipToPrevious(tooltip("ui.advancedSearch.picker.tooltip"), TooltipMakerAPI.TooltipLocation.BELOW);
        pickers.add(new StatPicker(row, pickerField));
        float controlsTop = rowTop + columns.controlsTop();
        ChipFlow.Colors colors = ChipFlow.Colors.player();
        element.setAreaCheckboxFont(Fonts.DEFAULT_SMALL);
        ButtonAPI linesButton = element.addAreaCheckbox(Translation.text(row.lines.labelKey()), new CycleLines(row), colors.base(), colors.dark(),
                colors.bright(), columns.linesWidth(), AdvancedSearchLayout.CONTROL_HEIGHT, 0f);
        linesButton.getPosition().inTL(columns.linesLeft(), controlsTop);
        element.addTooltipToPrevious(tooltip("ui.advancedSearch.lines.tooltip"), TooltipMakerAPI.TooltipLocation.BELOW);
        linesButtons.put(row, linesButton);
        addBoundField(element, row.minText, text -> row.minText = text, columns.minLeft(), controlsTop, columns.boundWidth(),
                "ui.advancedSearch.min.tooltip");
        addBoundField(element, row.maxText, text -> row.maxText = text, columns.maxLeft(), controlsTop, columns.boundWidth(),
                "ui.advancedSearch.max.tooltip");
        element.addAreaCheckbox(Translation.text("ui.advancedSearch.remove"), new RemoveRow(group, row), colors.base(), colors.dark(), colors.bright(),
                AdvancedSearchLayout.REMOVE_BUTTON_WIDTH, AdvancedSearchLayout.CONTROL_HEIGHT, 0f).getPosition().inTL(columns.removeLeft(), controlsTop);
        element.addTooltipToPrevious(tooltip("ui.advancedSearch.remove.tooltip"), TooltipMakerAPI.TooltipLocation.BELOW);
        element.setAreaCheckboxFontDefault();
    }

    private void addBoundField(TooltipMakerAPI element, String boundText, Consumer<String> store, float fieldLeft, float fieldTop, float fieldWidth,
                               String tooltipKey) {
        TextFieldAPI boundField = element.addTextField(fieldWidth, AdvancedSearchLayout.CONTROL_HEIGHT, Fonts.DEFAULT_SMALL, 0f);
        boundField.getPosition().inTL(fieldLeft, fieldTop);
        boundField.setMaxChars(BOUND_MAX_CHARS);
        boundField.setText(boundText);
        element.addTooltipToPrevious(tooltip(tooltipKey), TooltipMakerAPI.TooltipLocation.BELOW);
        polledFields.add(new PolledField(boundField, store));
    }

    private UIComponentAPI buildActions(float actionsTop, float actionsHeight) {
        TooltipMakerAPI element = panelRoot.createUIElement(layout.innerWidth(), actionsHeight, false);
        ChipFlow actionFlow = new ChipFlow(element, layout.innerWidth());
        for (Action action : Action.values()) {
            actionButtons.put(action, actionFlow.place(Translation.text(action.labelKey), action, ChipFlow.Colors.player(), false));
        }
        return replaceElement(null, element, PAD, actionsTop);
    }

    private static GameTextTooltip tooltip(String textKey) {
        String text = Translation.text(textKey);
        return new GameTextTooltip(TOOLTIP_WIDTH, tooltipElement -> VanillaText.plainPara(tooltipElement, text, 0f, Misc.getTextColor()));
    }

    @Override
    public void buttonPressed(Object id) {
        queue(() -> handleButton(id));
    }

    private void handleButton(Object id) {
        pollFields();
        if (id instanceof StatPicker.Choice choice) {
            choice.row().statName = choice.statName();
            rebuild();
        } else if (id instanceof CycleLines cycle) {
            cycle.row().lines = cycle.row().lines.next();
            ButtonAPI linesButton = linesButtons.get(cycle.row());
            if (linesButton != null) {
                linesButton.setText(Translation.text(cycle.row().lines.labelKey()));
                linesButton.setChecked(false);
            }
        } else if (id instanceof AddRow add) {
            search.addRow(add.group());
            rebuild();
        } else if (id instanceof RemoveRow remove) {
            search.removeRow(remove.group(), remove.row());
            rebuild();
        } else if (id instanceof TypeChip chip) {
            closePicker();
            storageFilter.toggleSection(chip.sectionKind());
            host.filtersPicked(true);
        } else if (id instanceof RarityChip chip) {
            storageFilter.toggleRarity(chip.rarity());
            host.filtersPicked(false);
        } else if (id instanceof Action action) {
            actionPressed(action);
        }
    }

    private void actionPressed(Action action) {
        ButtonAPI actionButton = actionButtons.get(action);
        if (actionButton != null) {
            actionButton.setChecked(false);
        }
        switch (action) {
            case ADD_AND_GROUP -> {
                search.addGroup(ModifierSearch.GroupKind.AND);
                rebuild();
            }
            case ADD_COUNT_GROUP -> {
                search.addGroup(ModifierSearch.GroupKind.COUNT);
                rebuild();
            }
            case RESET -> {
                search.clear();
                rebuild();
                host.searchApplied();
            }
            case SEARCH -> {
                closePicker();
                search.apply();
                host.searchApplied();
            }
        }
    }

    @Override
    protected void advanceOpen(float amount) {
        pollFields();
        for (StatPicker picker : pickers) {
            if (picker.poll()) {
                I18n.forGameText(() -> openPicker(picker));
            }
        }
    }

    private void pollFields() {
        for (PolledField polledField : polledFields) {
            polledField.poll();
        }
    }

    private void openPicker(StatPicker picker) {
        if (activePicker != null && activePicker != picker) {
            activePicker.closeList();
        }
        activePicker = picker;
        List<String> statNames = StatPicker.matching(ItemStatPool.stats(host.rollableKinds()), ItemStatPool::label, picker.filterText());
        picker.openList(panelRoot, panelPosition, statNames, this::buttonPressed);
        refreshOpacity();
    }

    private void reopenPickerAt(float x, float y) {
        for (StatPicker picker : pickers) {
            if (!picker.isListOpen() && picker.fieldContains(x, y)) {
                queue(() -> openPicker(picker));
                return;
            }
        }
    }

    private void closePicker() {
        if (activePicker != null) {
            activePicker.closeList();
            activePicker = null;
        }
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        if (isClosing()) {
            return;
        }
        for (InputEventAPI event : events) {
            boolean pressOrScroll = event.isMouseDownEvent() || event.isMouseScrollEvent();
            if (activePicker != null && pressOrScroll && !activePicker.contains(event.getX(), event.getY())) {
                queue(this::closePicker);
            }
            if (event.isMouseDownEvent()) {
                reopenPickerAt(event.getX(), event.getY());
            }
        }
        super.processInput(events);
        for (InputEventAPI event : events) {
            if (!event.isConsumed() && event.isMouseEvent() && contains(event.getX(), event.getY())) {
                event.consume();
            }
        }
    }

    @Override
    protected void onPressInside() {
        host.pressedInside();
    }

    @Override
    protected void onClose() {
        closePicker();
        host.closed();
    }

    @Override
    protected UIComponentAPI[] chromeComponents() {
        return new UIComponentAPI[]{headerElement};
    }

    @Override
    protected UIComponentAPI[] contentComponents() {
        return new UIComponentAPI[]{filtersElement, placedComponent(groupsElement), actionsElement,
                activePicker == null ? null : activePicker.listPanel()};
    }
}
