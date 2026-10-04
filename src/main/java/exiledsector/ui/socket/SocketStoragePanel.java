package exiledsector.ui.socket;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TextFieldAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.SkillTree;
import exiledsector.skills.tags.SkillTags;
import exiledsector.socketables.EffectThemes;
import exiledsector.socketables.Socketable;
import exiledsector.socketables.SocketableDefinition;
import exiledsector.socketables.SocketableKind;
import exiledsector.socketables.SocketableStore;
import exiledsector.ui.VanillaText;
import exiledsector.ui.util.BorderedPanel;
import exiledsector.ui.util.GLDraw;
import exiledsector.ui.util.SpriteCache;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class SocketStoragePanel extends BaseCustomUIPanelPlugin {

    private static final float WIDTH_FRACTION = 0.7f;
    private static final float HEIGHT_FRACTION = 0.85f;
    private static final float PAD = 16f;
    private static final float LINE_PAD = 3f;
    private static final float HEADER_HEIGHT = 30f;
    private static final float FIELD_HEIGHT = 26f;
    private static final float CHIP_HEIGHT = 24f;
    private static final float CHIP_GAP = 6f;
    private static final float CHIP_TEXT_PADDING = 28f;
    private static final float ROW_GAP = 6f;
    private static final float LABEL_WIDTH = 110f;
    private static final float CONTROL_BUTTON_WIDTH = 150f;
    private static final float PAGER_HEIGHT = 30f;
    private static final float PAGER_BUTTON_WIDTH = 110f;
    private static final float ICON_SIZE = 48f;
    private static final float ACTION_WIDTH = 170f;
    private static final float ACTION_BUTTON_HEIGHT = 24f;
    private static final float ROW_MIN_HEIGHT = ICON_SIZE + 8f;
    private static final float CONFIRM_WIDTH = 420f;
    private static final float CONFIRM_HEIGHT = 130f;
    private static final float SEARCH_DELAY_SECONDS = 0.25f;
    private static final float CONTROLS_MAX_HEIGHT = 400f;
    private static final Color DIM_COLOR = new Color(0, 0, 0, 170);
    private static final SpriteCache ICONS = new SpriteCache(SocketStoragePanel.class);

    private enum Control {CLOSE, SORT, DIRECTION, PREVIOUS, NEXT, CONFIRM_DESTROY, CANCEL_DESTROY}

    private record KindChip(SocketableKind kind) {
    }

    private record GradeChip(String grade) {
    }

    private record AlignmentChip(String alignment) {
    }

    private record ThemeChip(String theme) {
    }

    private record StatusChip(SocketStorageFilter.Status status) {
    }

    private record Destroy(Socketable socketable) {
    }

    private final CustomPanelAPI host;
    private final SocketStorageMode mode;
    private final Function<Socketable, String> installedIn;
    private final Runnable onClose;
    private final SocketStorageFilter filter = SocketStorageFilter.SESSION;
    private final BorderedPanel frame = new BorderedPanel(SocketStoragePanel.class);
    private final EffectThemes themes = EffectThemes.from(SkillTree.getAllTypes().values());

    private CustomPanelAPI root;
    private PositionAPI position;
    private float windowLeft;
    private float windowTop;
    private float windowWidth;
    private float windowHeight;
    private float listTop;
    private UIComponentAPI controls;
    private TooltipMakerAPI list;
    private UIComponentAPI pager;
    private CustomPanelAPI confirm;
    private CustomPanelAPI confirmBlocker;
    private TextFieldAPI searchField;
    private String typedQuery;
    private float searchDelay;
    private List<SocketStorageRow> rows = List.of();
    private Socketable pendingDestroy;
    private Runnable queued;
    private int movedFromCargo;

    private SocketStoragePanel(CustomPanelAPI host, SocketStorageMode mode, Function<Socketable, String> installedIn, Runnable onClose) {
        this.host = host;
        this.mode = mode;
        this.installedIn = installedIn;
        this.onClose = onClose;
    }

    public static SocketStoragePanel open(CustomPanelAPI host, SocketStorageMode mode, Function<Socketable, String> installedIn,
                                          Runnable onClose) {
        SocketStoragePanel panel = new SocketStoragePanel(host, mode, installedIn, onClose);
        PositionAPI hostPosition = host.getPosition();
        panel.root = Global.getSettings().createCustom(hostPosition.getWidth(), hostPosition.getHeight(), panel);
        host.addComponent(panel.root).inTL(0f, 0f);
        panel.movedFromCargo = absorbPlayerCargo();
        I18n.forGameText(panel::build);
        return panel;
    }

    private static int absorbPlayerCargo() {
        CampaignFleetAPI fleet = Global.getSector() == null ? null : Global.getSector().getPlayerFleet();
        return fleet == null ? 0 : SocketableStore.get().absorbFrom(fleet.getCargo());
    }

    private void close() {
        if (root != null) {
            host.removeComponent(root);
            root = null;
            onClose.run();
        }
    }

    public void escape() {
        queued = this::escapeNow;
    }

    private void escapeNow() {
        if (confirm != null) {
            closeConfirm();
        } else {
            close();
        }
    }

    @Override
    public void positionChanged(PositionAPI position) {
        this.position = position;
    }

    @Override
    public void renderBelow(float alphaMult) {
        if (position == null) {
            return;
        }
        GLDraw.fillQuad(position.getX(), position.getY(), position.getWidth(), position.getHeight(), DIM_COLOR, alphaMult);
        frame.draw(position.getX() + windowLeft, position.getY() + position.getHeight() - windowTop - windowHeight,
                windowWidth, windowHeight, alphaMult);
    }

    @Override
    public void advance(float amount) {
        if (queued != null) {
            Runnable action = queued;
            queued = null;
            I18n.forGameText(action);
        }
        if (root == null || searchField == null || confirm != null) {
            return;
        }
        String text = searchField.getText();
        if (!text.equals(typedQuery)) {
            typedQuery = text;
            searchDelay = SEARCH_DELAY_SECONDS;
        } else if (searchDelay > 0f) {
            searchDelay -= amount;
            if (searchDelay <= 0f) {
                filter.setQuery(typedQuery);
                I18n.forGameText(this::rebuildList);
            }
        }
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        consumeAll(events, false);
    }

    private static void consumeAll(List<InputEventAPI> events, boolean keyboardToo) {
        for (InputEventAPI event : events) {
            if (event.isConsumed()) {
                continue;
            }
            boolean escape = event.isKeyboardEvent() && event.getEventValue() == Keyboard.KEY_ESCAPE;
            if (event.isMouseEvent() || (keyboardToo && event.isKeyboardEvent() && !escape)) {
                event.consume();
            }
        }
    }

    @Override
    public void buttonPressed(Object id) {
        queued = () -> handleButton(id);
    }

    private void handleButton(Object id) {
        if (confirm != null) {
            if (id == Control.CONFIRM_DESTROY) {
                SocketableStore.get().remove(pendingDestroy);
                closeConfirm();
                reloadRows();
                rebuildControls();
            } else if (id == Control.CANCEL_DESTROY) {
                closeConfirm();
            }
            return;
        }
        if (id == Control.CLOSE) {
            close();
        } else if (id == Control.SORT) {
            filter.cycleSort();
            rebuildControls();
        } else if (id == Control.DIRECTION) {
            filter.flipDirection();
            rebuildControls();
        } else if (id == Control.PREVIOUS || id == Control.NEXT) {
            filter.page += id == Control.NEXT ? 1 : -1;
            rebuildList();
        } else if (id instanceof KindChip chip) {
            filter.toggleKind(chip.kind());
            rebuildList();
        } else if (id instanceof GradeChip chip) {
            filter.toggle(filter.hiddenGrades, chip.grade());
            rebuildList();
        } else if (id instanceof AlignmentChip chip) {
            filter.toggle(filter.hiddenAlignments, chip.alignment());
            rebuildList();
        } else if (id instanceof ThemeChip chip) {
            filter.toggleTheme(chip.theme());
            rebuildList();
        } else if (id instanceof StatusChip chip) {
            filter.setStatus(chip.status());
            rebuildControls();
        } else if (id instanceof Destroy destroy) {
            openConfirm(destroy.socketable());
        }
    }

    private void build() {
        float rootWidth = root.getPosition().getWidth();
        float rootHeight = root.getPosition().getHeight();
        windowWidth = rootWidth * WIDTH_FRACTION;
        windowHeight = rootHeight * HEIGHT_FRACTION;
        windowLeft = (rootWidth - windowWidth) / 2f;
        windowTop = (rootHeight - windowHeight) / 2f;
        buildHeader();
        buildSearch();
        reloadRows();
        rebuildControls();
    }

    private float innerWidth() {
        return windowWidth - PAD * 2f;
    }

    private void buildHeader() {
        TooltipMakerAPI element = root.createUIElement(innerWidth(), HEADER_HEIGHT, false);
        element.setParaFont(Fonts.ORBITRON_20AABOLD);
        element.addPara("%s", 0f, Misc.getBasePlayerColor(), Misc.getBasePlayerColor(), Translation.text("ui.socketStorage.title"));
        ButtonAPI close = element.addButton(Translation.text("ui.socketStorage.close"), Control.CLOSE, CONTROL_BUTTON_WIDTH / 1.5f,
                FIELD_HEIGHT, 0f);
        close.getPosition().inTR(0f, 0f);
        root.addUIElement(element).inTL(windowLeft + PAD, windowTop + PAD);
    }

    private void buildSearch() {
        TooltipMakerAPI element = root.createUIElement(innerWidth(), FIELD_HEIGHT, false);
        element.addPara("%s", 0f, Misc.getGrayColor(), Misc.getGrayColor(), Translation.text("ui.socketStorage.search"))
                .getPosition().inTL(0f, LINE_PAD * 2f);
        searchField = element.addTextField(innerWidth() - LABEL_WIDTH, FIELD_HEIGHT, Fonts.DEFAULT_SMALL, 0f);
        searchField.getPosition().inTL(LABEL_WIDTH, 0f);
        searchField.setText(filter.query);
        typedQuery = filter.query;
        root.addUIElement(element).inTL(windowLeft + PAD, windowTop + PAD + HEADER_HEIGHT + ROW_GAP);
    }

    private void reloadRows() {
        List<Socketable> owned = SocketableStore.get().owned();
        List<SocketStorageRow> built = new ArrayList<>(owned.size());
        for (int i = 0; i < owned.size(); i++) {
            built.add(SocketStorageRow.of(owned.get(i), i, themes, installedIn));
        }
        rows = built;
        Set<String> present = new LinkedHashSet<>();
        built.forEach(row -> present.addAll(row.themes()));
        filter.themes.retainAll(present);
    }

    private void rebuildControls() {
        if (controls != null) {
            root.removeComponent(controls);
        }
        TooltipMakerAPI element = root.createUIElement(innerWidth(), CONTROLS_MAX_HEIGHT, false);
        float y = 0f;
        y = chipRow(element, y, "ui.socketStorage.filter.kind", kindChips()) + ROW_GAP;
        y = chipRow(element, y, "ui.socketStorage.filter.grade", gradeChips()) + ROW_GAP;
        y = chipRow(element, y, "ui.socketStorage.filter.alignment", alignmentChips()) + ROW_GAP;
        y = chipRow(element, y, "ui.socketStorage.filter.theme", themeChips()) + ROW_GAP;
        y = statusAndSortRow(element, y);
        element.getPosition().setSize(innerWidth(), y);
        float top = windowTop + PAD + HEADER_HEIGHT + ROW_GAP + FIELD_HEIGHT + ROW_GAP;
        root.addUIElement(element).inTL(windowLeft + PAD, top);
        controls = element;
        listTop = top + y + ROW_GAP;
        rebuildList();
    }

    private record Chip(String label, Object data, boolean checked) {
    }

    private List<Chip> kindChips() {
        Map<SocketableKind, Integer> counts = new LinkedHashMap<>();
        for (SocketableKind kind : SocketableKind.values()) {
            counts.put(kind, 0);
        }
        rows.forEach(row -> counts.merge(row.kind(), 1, Integer::sum));
        List<Chip> chips = new ArrayList<>();
        counts.forEach((kind, count) -> chips.add(new Chip(countLabel(kind.displayName(), count), new KindChip(kind),
                !filter.hiddenKinds.contains(kind))));
        return chips;
    }

    private List<Chip> gradeChips() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        rows.forEach(row -> counts.merge(row.grade(), 1, Integer::sum));
        List<Chip> chips = new ArrayList<>();
        counts.forEach((grade, count) -> chips.add(new Chip(countLabel(SocketableDefinition.gradeName(grade), count),
                new GradeChip(grade), !filter.hiddenGrades.contains(grade))));
        return chips;
    }

    private List<Chip> alignmentChips() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        rows.forEach(row -> counts.merge(row.alignment(), 1, Integer::sum));
        List<Chip> chips = new ArrayList<>();
        counts.forEach((alignment, count) -> chips.add(new Chip(countLabel(SocketableDefinition.alignmentName(alignment), count),
                new AlignmentChip(alignment), !filter.hiddenAlignments.contains(alignment))));
        return chips;
    }

    private List<Chip> themeChips() {
        Set<String> present = new LinkedHashSet<>();
        rows.forEach(row -> present.addAll(row.themes()));
        List<Chip> chips = new ArrayList<>();
        for (String theme : SkillTags.THEME) {
            if (present.contains(theme)) {
                chips.add(new Chip(Translation.text("theme." + theme), new ThemeChip(theme), filter.themes.contains(theme)));
            }
        }
        return chips;
    }

    private static String countLabel(String name, int count) {
        return Translation.msg("ui.socketStorage.chip").arg("name", name).arg("count", count).text();
    }

    private float chipRow(TooltipMakerAPI element, float y, String labelKey, List<Chip> chips) {
        element.addPara("%s", 0f, Misc.getGrayColor(), Misc.getGrayColor(), Translation.text(labelKey))
                .getPosition().inTL(0f, y + LINE_PAD * 2f);
        if (chips.isEmpty()) {
            element.addPara("%s", 0f, Misc.getGrayColor(), Misc.getGrayColor(), Translation.text("ui.socketStorage.filter.none"))
                    .getPosition().inTL(LABEL_WIDTH, y + LINE_PAD * 2f);
            return y + CHIP_HEIGHT;
        }
        float x = LABEL_WIDTH;
        for (Chip chip : chips) {
            float width = Global.getSettings().computeStringWidth(chip.label(), Fonts.ORBITRON_12) + CHIP_TEXT_PADDING;
            if (x > LABEL_WIDTH && x + width > innerWidth()) {
                x = LABEL_WIDTH;
                y += CHIP_HEIGHT + CHIP_GAP;
            }
            ButtonAPI button = element.addAreaCheckbox(chip.label(), chip.data(), Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(),
                    Misc.getBrightPlayerColor(), width, CHIP_HEIGHT, 0f);
            button.setChecked(chip.checked());
            button.getPosition().inTL(x, y);
            x += width + CHIP_GAP;
        }
        return y + CHIP_HEIGHT;
    }

    private float statusAndSortRow(TooltipMakerAPI element, float y) {
        List<Chip> statuses = new ArrayList<>();
        for (SocketStorageFilter.Status status : SocketStorageFilter.Status.values()) {
            statuses.add(new Chip(Translation.text("ui.socketStorage.status." + status.name().toLowerCase(Locale.ROOT)), new StatusChip(status),
                    filter.status == status));
        }
        float bottom = chipRow(element, y, "ui.socketStorage.filter.status", statuses);
        String sortLabel = Translation.msg("ui.socketStorage.sort").arg("sort",
                Translation.text("ui.socketStorage.sort." + filter.sort.name().toLowerCase(Locale.ROOT))).text();
        ButtonAPI sort = element.addButton(sortLabel, Control.SORT, CONTROL_BUTTON_WIDTH * 1.3f, CHIP_HEIGHT, 0f);
        sort.getPosition().inTL(innerWidth() - CONTROL_BUTTON_WIDTH * 2.3f - CHIP_GAP, y);
        ButtonAPI direction = element.addButton(Translation.text(filter.descending ? "ui.socketStorage.descending"
                : "ui.socketStorage.ascending"), Control.DIRECTION, CONTROL_BUTTON_WIDTH, CHIP_HEIGHT, 0f);
        direction.getPosition().inTL(innerWidth() - CONTROL_BUTTON_WIDTH, y);
        return bottom;
    }

    private void rebuildList() {
        if (list != null) {
            root.removeComponent(list.getExternalScroller() != null ? list.getExternalScroller() : list);
        }
        if (pager != null) {
            root.removeComponent(pager);
        }
        List<SocketStorageRow> matching = SocketStorageQuery.apply(rows, filter);
        filter.page = SocketStorageQuery.clampPage(filter.page, matching.size());
        float listHeight = Math.max(ROW_MIN_HEIGHT, windowTop + windowHeight - PAD - PAGER_HEIGHT - ROW_GAP - listTop);
        TooltipMakerAPI element = root.createUIElement(innerWidth(), listHeight, true);
        if (movedFromCargo > 0) {
            element.addPara("%s", 0f, Misc.getPositiveHighlightColor(), Misc.getPositiveHighlightColor(),
                    Translation.msg("ui.socketStorage.moved").count(movedFromCargo).arg("count", movedFromCargo).text());
        }
        if (matching.isEmpty()) {
            String key = rows.isEmpty() ? "ui.socketStorage.empty" : "ui.socketStorage.noMatches";
            element.addPara("%s", PAD, Misc.getGrayColor(), Misc.getGrayColor(), Translation.text(key));
        }
        for (SocketStorageRow row : SocketStorageQuery.page(matching, filter.page)) {
            element.addCustom(rowPanel(row), ROW_GAP);
        }
        root.addUIElement(element).inTL(windowLeft + PAD, listTop);
        list = element;
        buildPager(matching.size());
    }

    private CustomPanelAPI rowPanel(SocketStorageRow row) {
        float width = innerWidth() - PAD;
        CustomPanelAPI panel = Global.getSettings().createCustom(width, ROW_MIN_HEIGHT, new Child(this, false));
        TooltipMakerAPI text = panel.createUIElement(width - ACTION_WIDTH - PAD, ROW_MIN_HEIGHT, false);
        String icon = row.socketable().iconPath();
        ICONS.ensureLoaded(icon);
        TooltipMakerAPI body = text.beginImageWithText(icon, ICON_SIZE);
        body.addPara("%s", 0f, row.rarity().color(), row.rarity().color(), row.name());
        if (row.baseName() != null) {
            body.addPara("%s", 0f, Misc.getGrayColor(), Misc.getGrayColor(), row.baseName());
        }
        for (StyledText line : row.headerLines()) {
            VanillaText.addPara(body, line, LINE_PAD, Misc.getTextColor());
        }
        body.setBulletedListMode(BaseIntelPlugin.BULLET);
        for (StyledText line : row.effectLines()) {
            VanillaText.addPara(body, line, LINE_PAD, Misc.getTextColor());
        }
        body.setBulletedListMode(null);
        text.addImageWithText(0f);
        panel.addUIElement(text).inTL(0f, 0f);
        float height = Math.max(ROW_MIN_HEIGHT, text.getHeightSoFar());
        text.getPosition().setSize(width - ACTION_WIDTH - PAD, height);
        TooltipMakerAPI action = panel.createUIElement(ACTION_WIDTH, height, false);
        String status = row.installed() ? Translation.msg("ui.socketStorage.installedIn").arg("ship", row.installedIn()).text()
                : Translation.text("ui.socketStorage.free");
        action.addPara("%s", 0f, Misc.getGrayColor(), Misc.getGrayColor(), status);
        addRowAction(action, row);
        panel.addUIElement(action).inTR(0f, 0f);
        panel.getPosition().setSize(width, height);
        return panel;
    }

    private void addRowAction(TooltipMakerAPI action, SocketStorageRow row) {
        if (mode instanceof SocketStorageMode.Manage) {
            ButtonAPI destroy = action.addButton(Translation.text("ui.socketStorage.destroy"), new Destroy(row.socketable()),
                    ACTION_WIDTH, ACTION_BUTTON_HEIGHT, LINE_PAD * 2f);
            destroy.setEnabled(!row.installed());
        }
    }

    private void buildPager(int matchCount) {
        int pages = SocketStorageQuery.pageCount(matchCount);
        TooltipMakerAPI element = root.createUIElement(innerWidth(), PAGER_HEIGHT, false);
        ButtonAPI previous = element.addButton(Translation.text("ui.socketStorage.previous"), Control.PREVIOUS, PAGER_BUTTON_WIDTH,
                FIELD_HEIGHT, 0f);
        previous.getPosition().inTL(0f, 0f);
        previous.setEnabled(filter.page > 0);
        ButtonAPI next = element.addButton(Translation.text("ui.socketStorage.next"), Control.NEXT, PAGER_BUTTON_WIDTH, FIELD_HEIGHT, 0f);
        next.getPosition().inTR(0f, 0f);
        next.setEnabled(filter.page < pages - 1);
        int installed = (int) rows.stream().filter(SocketStorageRow::installed).count();
        String summary = Translation.msg("ui.socketStorage.page").arg("page", filter.page + 1).arg("pages", pages).text() + "    "
                + Translation.msg("ui.socketStorage.summary").arg("stored", rows.size()).arg("installed", installed)
                .arg("shown", matchCount).text();
        element.addPara("%s", 0f, Misc.getGrayColor(), Misc.getGrayColor(), summary).getPosition()
                .inTL(PAGER_BUTTON_WIDTH + PAD, LINE_PAD * 2f);
        root.addUIElement(element).inTL(windowLeft + PAD, windowTop + windowHeight - PAD - PAGER_HEIGHT);
        pager = element;
    }

    private void openConfirm(Socketable socketable) {
        pendingDestroy = socketable;
        confirm = Global.getSettings().createCustom(CONFIRM_WIDTH, CONFIRM_HEIGHT, new Child(this, true));
        TooltipMakerAPI element = confirm.createUIElement(CONFIRM_WIDTH - PAD * 2f, CONFIRM_HEIGHT - PAD * 2f, false);
        element.addPara("%s", 0f, Misc.getHighlightColor(), Misc.getHighlightColor(),
                Translation.msg("ui.socketStorage.confirm.title").arg("name", socketable.name()).text());
        element.addPara("%s", LINE_PAD * 2f, Misc.getTextColor(), Misc.getTextColor(), Translation.text("ui.socketStorage.confirm.body"));
        float buttonWidth = PAGER_BUTTON_WIDTH;
        ButtonAPI destroy = element.addButton(Translation.text("ui.socketStorage.confirm.destroy"), Control.CONFIRM_DESTROY, buttonWidth,
                FIELD_HEIGHT, 0f);
        destroy.getPosition().inBR(0f, 0f);
        ButtonAPI cancel = element.addButton(Translation.text("ui.socketStorage.confirm.cancel"), Control.CANCEL_DESTROY, buttonWidth,
                FIELD_HEIGHT, 0f);
        cancel.getPosition().inBR(buttonWidth + CHIP_GAP, 0f);
        confirm.addUIElement(element).inTL(PAD, PAD);
        confirmBlocker = Global.getSettings().createCustom(root.getPosition().getWidth(), root.getPosition().getHeight(), new Blocker());
        root.addComponent(confirmBlocker).inTL(0f, 0f);
        root.addComponent(confirm).inTL(windowLeft + (windowWidth - CONFIRM_WIDTH) / 2f, windowTop + (windowHeight - CONFIRM_HEIGHT) / 2f);
    }

    private void closeConfirm() {
        root.removeComponent(confirm);
        root.removeComponent(confirmBlocker);
        confirmBlocker = null;
        confirm = null;
        pendingDestroy = null;
    }

    private static final class Blocker extends BaseCustomUIPanelPlugin {

        @Override
        public void processInput(List<InputEventAPI> events) {
            consumeAll(events, true);
        }
    }

    private static final class Child extends BaseCustomUIPanelPlugin {

        private final SocketStoragePanel owner;
        private final boolean framed;
        private final BorderedPanel frame = new BorderedPanel(Child.class);
        private PositionAPI position;

        Child(SocketStoragePanel owner, boolean framed) {
            this.owner = owner;
            this.framed = framed;
        }

        @Override
        public void positionChanged(PositionAPI position) {
            this.position = position;
        }

        @Override
        public void renderBelow(float alphaMult) {
            if (framed && position != null) {
                frame.draw(position.getX(), position.getY(), position.getWidth(), position.getHeight(), alphaMult);
            }
        }

        @Override
        public void buttonPressed(Object id) {
            owner.buttonPressed(id);
        }
    }
}
