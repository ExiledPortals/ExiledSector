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
import exiledsector.ui.util.GLDraw;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
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

        boolean wantsEnter();
    }

    private static final float LABEL_TEXT_TOP = 4f;
    private static final float TOOLTIP_WIDTH = 320f;
    private static final float COUNT_FIELD_WIDTH = 36f;
    private static final int PICKER_MAX_CHARS = 120;
    private static final int BOUND_MAX_CHARS = 10;
    private static final int COUNT_MAX_CHARS = 3;
    private static final String TITLE_KEY = "ui.advancedSearch.title";
    private static final String HELP_ICON_PATH = "graphics/ui/icons/fleettab/more_info.png";
    private static final float HELP_ICON_SIZE = 24f;
    private static final float HELP_TOOLTIP_WIDTH = 450f;
    private static final List<String> HELP_PARAGRAPH_KEYS = List.of("ui.advancedSearch.help.rows", "ui.advancedSearch.help.totals",
            "ui.advancedSearch.help.groups", "ui.advancedSearch.help.apply");

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

    private enum Menu {
        TYPE("ui.socketStorage.menu.type"),
        RARITY("ui.socketStorage.menu.rarity");

        private final String nameKey;

        Menu(String nameKey) {
            this.nameKey = nameKey;
        }
    }

    private record MenuButton(Menu menu) {
    }

    private record TypeChip(ItemKind sectionKind) {
    }

    private record RarityChip(ItemRarity rarity) {
    }

    private record AddRow(ModifierSearch.Group group) {
    }

    private record RemoveRow(ModifierSearch.Group group, ModifierSearch.Row row) {
    }

    private record ClearStat(ModifierSearch.Row row) {
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
    private final List<TextFieldAPI> tabOrder = new ArrayList<>();
    private final Map<Action, ButtonAPI> actionButtons = new IdentityHashMap<>();
    private final Map<Menu, ButtonAPI> menuButtons = new EnumMap<>(Menu.class);
    private final FilterMenu filterMenu = new FilterMenu(this::buttonPressed, () -> queue(this::closeFilterMenu));
    private UIComponentAPI headerElement;
    private UIComponentAPI filtersElement;
    private TooltipMakerAPI groupsElement;
    private UIComponentAPI actionsElement;
    private StatPicker activePicker;
    private boolean fieldFocused;

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
        pane.attach(layout.paneLeft(), layout.paneTop(), layout.paneWidth(), layout.paneHeight(), false);
        return pane;
    }

    void refresh() {
        queue(this::rebuild);
    }

    boolean escape() {
        if (filterMenu.isOpen()) {
            queue(this::closeFilterMenu);
            return true;
        }
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
        tabOrder.clear();
        fieldFocused = false;
        actionButtons.clear();
        activePicker = null;
        TooltipMakerAPI header = startHeader(TITLE_KEY, null, null);
        addHelpIcon(header);
        headerElement = finishHeader(header, null);
        filtersElement = null;
        rebuildFilters();
        float groupsTop = filtersTop() + ChipFlow.CHIP_HEIGHT + GAP;
        float actionsHeight = actionsHeight();
        float actionsTop = panelHeight - PAD - actionsHeight;
        groupsElement = buildGroups(groupsTop, Math.max(AdvancedSearchLayout.CONTROL_HEIGHT, actionsTop - GAP - groupsTop));
        actionsElement = buildActions(actionsTop, actionsHeight);
        refreshOpacity();
    }

    private static void addHelpIcon(TooltipMakerAPI header) {
        header.addImage(HELP_ICON_PATH, HELP_ICON_SIZE, HELP_ICON_SIZE, 0f);
        header.getPrev().getPosition().inTR(0f, (HEADER_HEIGHT - HELP_ICON_SIZE) / 2f);
        header.addTooltipToPrevious(new GameTextTooltip(HELP_TOOLTIP_WIDTH, AdvancedSearchPane::writeHelp), TooltipMakerAPI.TooltipLocation.BELOW);
    }

    private static void writeHelp(TooltipMakerAPI tooltip) {
        tooltip.addTitle(Translation.text("ui.advancedSearch.help.title"));
        for (String paragraphKey : HELP_PARAGRAPH_KEYS) {
            VanillaText.addPara(tooltip, Translation.styled(paragraphKey), PAD, Misc.getTextColor());
        }
    }

    private void rebuild() {
        pollFields();
        float restoredScroll = groupsScrollOffset();
        closePicker();
        closeFilterMenu();
        keepAGroup();
        reattach(layout.paneLeft(), layout.paneTop(), layout.paneWidth(), layout.paneHeight());
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

    private float actionsHeight() {
        return ChipFlow.height(ChipFlow.rowsFor(actionLabels(), layout.innerWidth()));
    }

    private static List<String> actionLabels() {
        return Arrays.stream(Action.values()).map(action -> Translation.text(action.labelKey)).toList();
    }

    private static float filtersTop() {
        return PAD + HEADER_HEIGHT + GAP;
    }

    private List<Menu> menus() {
        return host.typeChoices().isEmpty() ? List.of(Menu.RARITY) : List.of(Menu.TYPE, Menu.RARITY);
    }

    private float menuButtonWidth(int menuCount) {
        return (layout.innerWidth() - (menuCount - 1) * GAP) / menuCount;
    }

    private void rebuildFilters() {
        List<Menu> menus = menus();
        float buttonWidth = menuButtonWidth(menus.size());
        TooltipMakerAPI element = panelRoot.createUIElement(layout.innerWidth(), ChipFlow.CHIP_HEIGHT, false);
        ChipFlow.Colors colors = ChipFlow.Colors.player();
        menuButtons.clear();
        for (int index = 0; index < menus.size(); index++) {
            Menu menu = menus.get(index);
            ButtonAPI menuButton = element.addAreaCheckbox(menuButtonLabel(menu, buttonWidth), new MenuButton(menu), colors.base(), colors.dark(),
                    colors.bright(), buttonWidth, ChipFlow.CHIP_HEIGHT, 0f);
            menuButton.getPosition().inTL(index * (buttonWidth + GAP), 0f);
            menuButtons.put(menu, menuButton);
        }
        syncMenuButtons();
        filtersElement = replaceElement(filtersElement, element, PAD, filtersTop());
    }

    private String menuButtonLabel(Menu menu, float buttonWidth) {
        List<String> chosenLabels = switch (menu) {
            case TYPE -> FilterMenu.chosenLabels(host.typeChoices(), storageFilter.sectionKinds, ItemKind::displayName);
            case RARITY -> FilterMenu.chosenLabels(Arrays.asList(ItemRarity.values()), storageFilter.rarities, ItemStoragePanel::rarityLabel);
        };
        return FilterMenu.buttonLabel(menu.nameKey, chosenLabels, buttonWidth);
    }

    private List<FilterMenu.Option> menuOptions(Menu menu) {
        return switch (menu) {
            case TYPE -> FilterMenu.typeOptions(host.typeChoices(), storageFilter.sectionKinds, TypeChip::new);
            case RARITY -> FilterMenu.rarityOptions(storageFilter.rarities, RarityChip::new);
        };
    }

    private void openFilterMenu(Menu menu) {
        List<Menu> menus = menus();
        int menuIndex = menus.indexOf(menu);
        if (menuIndex < 0) {
            return;
        }
        closePicker();
        float buttonWidth = menuButtonWidth(menus.size());
        float buttonLeft = PAD + menuIndex * (buttonWidth + GAP);
        float menuTop = filtersTop() + ChipFlow.CHIP_HEIGHT + LINE_PAD;
        filterMenu.open(panelRoot, panelWidth, panelHeight, menu, new FilterMenu.Anchor(buttonLeft, menuTop, buttonWidth), menuOptions(menu));
        syncMenuButtons();
        refreshOpacity();
    }

    private void closeFilterMenu() {
        filterMenu.close();
        syncMenuButtons();
    }

    private void syncMenuButtons() {
        menuButtons.forEach((menu, button) -> button.setChecked(filterMenu.isOpen(menu)));
    }

    private void filtersPicked(boolean sectionsChanged) {
        Object openMenu = filterMenu.openMenuId();
        rebuildFilters();
        if (openMenu instanceof Menu menu) {
            openFilterMenu(menu);
        }
        host.filtersPicked(sectionsChanged);
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
            tabOrder.add(countField);
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
        pickerField.setLimitByStringWidth(true);
        pickerField.setMaxChars(PICKER_MAX_CHARS);
        element.addTooltipToPrevious(pickerTooltip(row), TooltipMakerAPI.TooltipLocation.BELOW);
        pickers.add(new StatPicker(row, pickerField));
        tabOrder.add(pickerField);
        float controlsTop = rowTop + columns.controlsTop();
        ChipFlow.Colors colors = ChipFlow.Colors.player();
        element.setAreaCheckboxFont(Fonts.DEFAULT_SMALL);
        element.addAreaCheckbox(Translation.text("ui.advancedSearch.clear"), new ClearStat(row), colors.base(), colors.dark(), colors.bright(),
                AdvancedSearchLayout.CLEAR_BUTTON_WIDTH, AdvancedSearchLayout.CONTROL_HEIGHT, 0f).getPosition().inTL(columns.clearLeft(), rowTop);
        element.addTooltipToPrevious(tooltip("ui.advancedSearch.clear.tooltip"), TooltipMakerAPI.TooltipLocation.BELOW);
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
        tabOrder.add(boundField);
    }

    private UIComponentAPI buildActions(float actionsTop, float actionsHeight) {
        TooltipMakerAPI element = panelRoot.createUIElement(layout.innerWidth(), actionsHeight, false);
        ChipFlow actionFlow = new ChipFlow(element, layout.innerWidth());
        for (Action action : Action.values()) {
            actionButtons.put(action, actionFlow.place(Translation.text(action.labelKey), action, ChipFlow.Colors.player(), false));
        }
        return replaceElement(null, element, PAD, actionsTop);
    }

    private static GameTextTooltip pickerTooltip(ModifierSearch.Row row) {
        String helpText = Translation.text("ui.advancedSearch.picker.tooltip");
        if (!row.hasStat()) {
            return new GameTextTooltip(TOOLTIP_WIDTH, tooltipElement -> VanillaText.plainPara(tooltipElement, helpText, 0f, Misc.getTextColor()));
        }
        String statLabel = ItemStatPool.label(row.statName);
        return new GameTextTooltip(TOOLTIP_WIDTH, tooltipElement -> {
            VanillaText.plainPara(tooltipElement, statLabel, 0f, Misc.getHighlightColor());
            VanillaText.plainPara(tooltipElement, helpText, PAD, Misc.getTextColor());
        });
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
        } else if (id instanceof ClearStat clear) {
            clearStat(clear.row());
        } else if (id instanceof AddRow add) {
            search.addRow(add.group());
            rebuild();
        } else if (id instanceof RemoveRow remove) {
            search.removeRow(remove.group(), remove.row());
            rebuild();
        } else if (id instanceof MenuButton button) {
            if (filterMenu.isOpen(button.menu())) {
                closeFilterMenu();
            } else {
                openFilterMenu(button.menu());
            }
        } else if (id instanceof TypeChip chip) {
            closePicker();
            storageFilter.toggleSection(chip.sectionKind());
            filtersPicked(true);
        } else if (id instanceof RarityChip chip) {
            storageFilter.toggleRarity(chip.rarity());
            filtersPicked(false);
        } else if (id instanceof Action action) {
            actionPressed(action);
        }
    }

    private void clearStat(ModifierSearch.Row row) {
        row.statName = null;
        rebuild();
        for (StatPicker picker : pickers) {
            if (picker.isFor(row)) {
                picker.focus();
            }
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
        fieldFocused = focusedFieldIndex() >= 0;
    }

    private int focusedFieldIndex() {
        for (int index = 0; index < tabOrder.size(); index++) {
            if (tabOrder.get(index).hasFocus()) {
                return index;
            }
        }
        return -1;
    }

    private void handleKey(InputEventAPI event) {
        if (!event.isKeyboardEvent() || !(event.isKeyDownEvent() || event.isRepeat())) {
            return;
        }
        int key = event.getEventValue();
        if (key == Keyboard.KEY_TAB && !event.isConsumed()) {
            moveFocus(event.isShiftDown() ? -1 : 1, event);
        } else if ((key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) && !event.isRepeat() && enterMeansSearch(event)) {
            event.consume();
            buttonPressed(Action.SEARCH);
        }
    }

    private void moveFocus(int step, InputEventAPI event) {
        int focusedIndex = focusedFieldIndex();
        if (focusedIndex < 0) {
            return;
        }
        tabOrder.get(Math.floorMod(focusedIndex + step, tabOrder.size())).grabFocus();
        event.consume();
        queue(this::closePicker);
    }

    private boolean enterMeansSearch(InputEventAPI event) {
        if (event.isConsumed()) {
            return fieldFocused;
        }
        return !isConfirmOpen() && !host.wantsEnter();
    }

    private void pollFields() {
        for (PolledField polledField : polledFields) {
            polledField.poll();
        }
    }

    private void openPicker(StatPicker picker) {
        closeFilterMenu();
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
            handleKey(event);
            boolean pressOrScroll = event.isMouseDownEvent() || event.isMouseScrollEvent();
            if (activePicker != null && pressOrScroll && !activePicker.contains(event.getX(), event.getY())) {
                queue(this::closePicker);
            }
            if (event.isMouseDownEvent() && !filterMenu.isOpen()) {
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
        closeFilterMenu();
        host.closed();
    }

    @Override
    protected UIComponentAPI[] chromeComponents() {
        return new UIComponentAPI[]{headerElement};
    }

    @Override
    protected UIComponentAPI[] contentComponents() {
        return new UIComponentAPI[]{filtersElement, placedComponent(groupsElement), actionsElement,
                activePicker == null ? null : activePicker.listPanel(), filterMenu.panel()};
    }

    @Override
    public void renderBelow(float alphaMult) {
        if (panelPosition != null) {
            GLDraw.fillQuad(panelPosition.getX(), panelPosition.getY(), panelPosition.getWidth(), panelPosition.getHeight(), Color.BLACK,
                    contentAlpha() * alphaMult);
        }
        super.renderBelow(alphaMult);
    }
}
