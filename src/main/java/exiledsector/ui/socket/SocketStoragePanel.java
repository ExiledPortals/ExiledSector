package exiledsector.ui.socket;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TextFieldAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.socketables.SocketCustody;
import exiledsector.socketables.Socketable;
import exiledsector.socketables.SocketableDisassembly;
import exiledsector.socketables.SocketableRarity;
import exiledsector.socketables.SocketableStore;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.util.BorderedPanel;
import exiledsector.ui.util.FramedPanelPlugin;
import exiledsector.ui.util.HoloTransition;
import exiledsector.ui.util.Rects;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public final class SocketStoragePanel extends BaseCustomUIPanelPlugin {

    public interface Listener {

        void selected(Socketable socketable);

        void closed();
    }

    private static final float PAD = 12f;
    private static final float LINE_PAD = 3f;
    private static final float GAP = 6f;
    private static final float HEADER_HEIGHT = 28f;
    private static final float FIELD_HEIGHT = 26f;
    private static final float CHIP_HEIGHT = 24f;
    private static final float CHIP_TEXT_PADDING = 28f;
    private static final float CHIP_DARK_SCALE = 0.3f;
    private static final int CONTROL_ROWS = 2;
    private static final float LABEL_WIDTH = 70f;
    private static final float CLOSE_BUTTON_WIDTH = 70f;
    private static final float NOTICE_HEIGHT = 56f;
    private static final float FOOTER_HEIGHT = 24f;
    private static final float SCROLLBAR_ROOM = 14f;
    private static final float CELL_SIZE = 96f;
    private static final int COLUMNS = 6;
    private static final float WIDTH = PAD * 2f + SCROLLBAR_ROOM + COLUMNS * CELL_SIZE + (COLUMNS - 1) * GAP;
    private static final float CONFIRM_HEIGHT = 130f;
    private static final float BATCH_CONFIRM_HEIGHT = 170f;
    private static final float BUTTON_TEXT_PADDING = 30f;
    private static final float PARTS_ICON_SIZE = 24f;
    private static final float PARTS_TEXT_TOP = 5f;
    private static final float SEARCH_DELAY_SECONDS = 0.25f;
    private static final SocketableCell.Look CELL_LOOK = new SocketableCell.Look(9f, true, 1f);

    private enum Control {CLOSE, DISASSEMBLE_SHOWN, CONFIRM_DISASSEMBLE, CANCEL_DISASSEMBLE}

    private record StatusChip(SocketStorageFilter.Status status) {
    }

    private record RarityChip(SocketableRarity rarity) {
    }

    private record ChipColors(Color base, Color dark, Color bright) {

        static ChipColors of(Color base) {
            return new ChipColors(base, Misc.scaleColorOnly(base, CHIP_DARK_SCALE), base.brighter());
        }

        static ChipColors player() {
            return new ChipColors(Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor());
        }
    }

    private final CustomPanelAPI host;
    private final Listener listener;
    private final SocketStorageFilter filter = SocketStorageFilter.SESSION;
    private final BorderedPanel frame = new BorderedPanel(SocketStoragePanel.class);
    private final HoloTransition transition = new HoloTransition();
    private boolean closing;

    private Function<Socketable, String> installedIn;
    private CustomPanelAPI root;
    private PositionAPI position;
    private float width;
    private float height;
    private float gridTop;
    private UIComponentAPI header;
    private UIComponentAPI searchRow;
    private UIComponentAPI controls;
    private TooltipMakerAPI grid;
    private UIComponentAPI notice;
    private UIComponentAPI summary;
    private CustomPanelAPI confirm;
    private CustomPanelAPI confirmBlocker;
    private TextFieldAPI searchField;
    private String typedQuery;
    private float searchDelay;
    private List<SocketStorageRow> rows = List.of();
    private Socketable selected;
    private boolean targetingSocket;
    private List<Socketable> pendingDisassembly = List.of();
    private final List<Runnable> queued = new ArrayList<>();
    private int movedFromCargo;
    private String disassembledNotice;
    private final SocketableHoverTooltip hoverTooltip;

    private SocketStoragePanel(CustomPanelAPI host, Function<Socketable, String> installedIn, Listener listener) {
        this.host = host;
        this.hoverTooltip = new SocketableHoverTooltip(host);
        this.installedIn = installedIn;
        this.listener = listener;
    }

    public static SocketStoragePanel open(CustomPanelAPI host, float left, float top, float height,
                                          Function<Socketable, String> installedIn, Listener listener) {
        SocketStoragePanel panel = new SocketStoragePanel(host, installedIn, listener);
        panel.width = WIDTH;
        panel.height = height;
        panel.root = Global.getSettings().createCustom(WIDTH, height, panel);
        host.addComponent(panel.root).inTL(left, top);
        panel.movedFromCargo = absorbPlayerCargo();
        I18n.forGameText(panel::build);
        panel.transition.open();
        panel.applyContentOpacity(0f);
        return panel;
    }

    private static int absorbPlayerCargo() {
        CampaignFleetAPI fleet = Global.getSector() == null ? null : Global.getSector().getPlayerFleet();
        return fleet == null ? 0 : SocketableStore.get().absorbFrom(fleet.getCargo());
    }

    public void close() {
        if (root == null || closing) {
            return;
        }
        closing = true;
        hideHoverTooltip();
        transition.close();
        applyContentOpacity(transition.contentAlpha());
        listener.closed();
    }

    private void applyContentOpacity(float opacity) {
        float chromeOpacity = opacity >= 1f && !closing ? 1f : 0f;
        for (UIComponentAPI component : new UIComponentAPI[]{header, searchRow}) {
            if (component != null) {
                component.setOpacity(chromeOpacity);
            }
        }
        for (UIComponentAPI component : new UIComponentAPI[]{controls, grid == null ? null : gridComponent(), notice, summary, confirm, confirmBlocker}) {
            if (component != null) {
                component.setOpacity(opacity);
            }
        }
    }

    private UIComponentAPI gridComponent() {
        return grid.getExternalScroller() != null ? grid.getExternalScroller() : grid;
    }

    public boolean contains(float x, float y) {
        return root != null && !closing && Rects.contains(position, x, y);
    }

    public void setSelected(Socketable socketable) {
        selected = socketable;
        queued.add(() -> rebuildFooter(SocketStorageQuery.apply(rows, filter)));
    }

    public void setTargetingSocket(boolean targeting) {
        if (targeting == targetingSocket) {
            return;
        }
        targetingSocket = targeting;
        queued.add(() -> rebuildFooter(SocketStorageQuery.apply(rows, filter)));
    }

    public void refresh(Function<Socketable, String> installedIn) {
        this.installedIn = installedIn;
        queued.add(() -> {
            if (pendingDisassembly.stream().anyMatch(SocketCustody::isInstalled)) {
                closeConfirm();
            }
            reloadRows();
            rebuildGrid();
        });
    }

    public boolean escape() {
        if (confirm == null) {
            return false;
        }
        queued.add(this::closeConfirm);
        return true;
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
        transition.drawProjection(position.getX(), position.getY(), position.getWidth(), position.getHeight(),
                SkillTreePanelStyle.GLOW_COLOR, alphaMult);
        float content = transition.contentAlpha();
        if (content > 0f) {
            frame.draw(position.getX(), position.getY(), position.getWidth(), position.getHeight(), content * alphaMult);
        }
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        if (closing) {
            return;
        }
        for (InputEventAPI event : events) {
            if (event.isMouseScrollEvent() && contains(event.getX(), event.getY())) {
                queued.add(this::hideHoverTooltip);
            }
            boolean press = event.isMouseDownEvent() || event.isMouseScrollEvent();
            if (!event.isConsumed() && press && contains(event.getX(), event.getY())) {
                event.consume();
            }
        }
    }

    @Override
    public void advance(float amount) {
        if (transition.isAnimating()) {
            transition.advance(amount);
            applyContentOpacity(transition.contentAlpha());
        }
        if (closing) {
            if (transition.isFullyClosed() && root != null) {
                host.removeComponent(root);
                root = null;
            }
            return;
        }
        if (!queued.isEmpty()) {
            List<Runnable> actions = List.copyOf(queued);
            queued.clear();
            for (Runnable action : actions) {
                if (root != null) {
                    I18n.forGameText(action);
                }
            }
        }
        if (root != null) {
            hoverTooltip.refreshIfExpansionChanged();
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
                I18n.forGameText(this::rebuildGrid);
            }
        }
    }

    @Override
    public void buttonPressed(Object id) {
        queued.add(() -> handleButton(id));
    }

    private void handleButton(Object id) {
        if (confirm != null) {
            if (id == Control.CONFIRM_DISASSEMBLE) {
                List<Socketable> targets = pendingDisassembly.stream().filter(socketable -> !SocketCustody.isInstalled(socketable)).toList();
                if (selected != null && targets.contains(selected)) {
                    listener.selected(null);
                }
                disassemble(targets);
                closeConfirm();
                reloadRows();
                buildHeader();
                rebuildControls();
            } else if (id == Control.CANCEL_DISASSEMBLE) {
                closeConfirm();
            }
            return;
        }
        if (id == Control.CLOSE) {
            close();
        } else if (id == Control.DISASSEMBLE_SHOWN) {
            openConfirm(freeShown(SocketStorageQuery.apply(rows, filter)));
        } else if (id instanceof StatusChip chip) {
            filter.setStatus(chip.status());
            rebuildControls();
        } else if (id instanceof RarityChip chip) {
            filter.toggleRarity(chip.rarity());
            rebuildControls();
        }
    }

    private void cellClicked(SocketStorageRow row) {
        if (confirm == null && !row.installed()) {
            queued.add(() -> listener.selected(row.socketable() == selected ? null : row.socketable()));
        }
    }

    private void cellRightClicked(SocketStorageRow row) {
        if (confirm == null && !row.installed()) {
            queued.add(() -> openConfirm(List.of(row.socketable())));
        }
    }

    private float innerWidth() {
        return width - PAD * 2f;
    }

    private void build() {
        buildHeader();
        buildSearch();
        reloadRows();
        rebuildControls();
    }

    private void buildHeader() {
        if (header != null) {
            root.removeComponent(header);
        }
        TooltipMakerAPI element = root.createUIElement(innerWidth(), HEADER_HEIGHT, false);
        element.setParaFont(Fonts.ORBITRON_20AABOLD);
        element.addPara("%s", 0f, Misc.getBasePlayerColor(), Misc.getBasePlayerColor(), Translation.text("ui.socketStorage.title"));
        ButtonAPI close = element.addButton(Translation.text("ui.socketStorage.close"), Control.CLOSE, CLOSE_BUTTON_WIDTH, CHIP_HEIGHT, 0f);
        close.getPosition().inTR(0f, 0f);
        addPartsCount(element);
        root.addUIElement(element).inTL(PAD, PAD);
        header = element;
    }

    private static void addPartsCount(TooltipMakerAPI element) {
        CommoditySpecAPI parts = Global.getSettings().getCommoditySpec(SocketableDisassembly.PARTS_COMMODITY_ID);
        if (parts == null) {
            return;
        }
        String count = String.valueOf(partsInCargo());
        float countWidth = Global.getSettings().computeStringWidth(count, Fonts.ORBITRON_12) + LINE_PAD;
        float countRight = CLOSE_BUTTON_WIDTH + GAP * 2f;
        element.setParaFont(Fonts.ORBITRON_12);
        LabelAPI label = element.addPara("%s", 0f, Misc.getTextColor(), Misc.getTextColor(), count);
        label.autoSizeToWidth(countWidth).inTR(countRight, PARTS_TEXT_TOP);
        element.addImage(parts.getIconName(), PARTS_ICON_SIZE, PARTS_ICON_SIZE, 0f);
        element.getPrev().getPosition().inTR(countRight + countWidth + LINE_PAD, 0f);
        element.addTooltipToPrevious(new PartsTooltip(parts.getName()), TooltipMakerAPI.TooltipLocation.BELOW);
    }

    private static int partsInCargo() {
        CampaignFleetAPI fleet = Global.getSector() == null ? null : Global.getSector().getPlayerFleet();
        return fleet == null ? 0 : Math.round(fleet.getCargo().getCommodityQuantity(SocketableDisassembly.PARTS_COMMODITY_ID));
    }

    private record PartsTooltip(String name) implements TooltipMakerAPI.TooltipCreator {

        private static final float WIDTH = 300f;

        @Override
        public boolean isTooltipExpandable(Object tooltipParam) {
            return false;
        }

        @Override
        public float getTooltipWidth(Object tooltipParam) {
            return WIDTH;
        }

        @Override
        public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
            tooltip.addPara("%s", 0f, Misc.getTextColor(), Misc.getTextColor(),
                    Translation.msg("ui.socketStorage.parts.tooltip").arg("name", name).text());
        }
    }

    private void buildSearch() {
        TooltipMakerAPI element = root.createUIElement(innerWidth(), FIELD_HEIGHT, false);
        element.addPara("%s", 0f, Misc.getGrayColor(), Misc.getGrayColor(), Translation.text("ui.socketStorage.search"))
                .getPosition().inTL(0f, LINE_PAD * 2f);
        searchField = element.addTextField(innerWidth() - LABEL_WIDTH, FIELD_HEIGHT, Fonts.DEFAULT_SMALL, 0f);
        searchField.getPosition().inTL(LABEL_WIDTH, 0f);
        searchField.setText(filter.query);
        typedQuery = filter.query;
        root.addUIElement(element).inTL(PAD, PAD + HEADER_HEIGHT + GAP);
        searchRow = element;
    }

    private void reloadRows() {
        List<Socketable> owned = SocketableStore.get().owned();
        List<SocketStorageRow> built = new ArrayList<>(owned.size());
        for (int i = 0; i < owned.size(); i++) {
            built.add(SocketStorageRow.of(owned.get(i), i, installedIn));
        }
        rows = built;
    }

    private void rebuildControls() {
        if (controls != null) {
            root.removeComponent(controls);
        }
        TooltipMakerAPI element = root.createUIElement(innerWidth(), CONTROL_ROWS * CHIP_HEIGHT + (CONTROL_ROWS - 1) * GAP, false);
        float x = 0f;
        for (SocketStorageFilter.Status status : SocketStorageFilter.Status.values()) {
            String label = Translation.text("ui.socketStorage.status." + status.name().toLowerCase(Locale.ROOT));
            x += addChip(element, label, new StatusChip(status), ChipColors.player(), filter.status == status, x, 0) + GAP;
        }
        x = 0f;
        for (SocketableRarity rarity : SocketableRarity.values()) {
            String label = Translation.text("ui.socketStorage.rarity." + rarity.name().toLowerCase(Locale.ROOT));
            x += addChip(element, label, new RarityChip(rarity), ChipColors.of(rarity.color()), filter.rarities.contains(rarity), x, 1)
                    + GAP;
        }
        float top = PAD + HEADER_HEIGHT + GAP + FIELD_HEIGHT + GAP;
        root.addUIElement(element).inTL(PAD, top);
        controls = element;
        gridTop = top + CONTROL_ROWS * (CHIP_HEIGHT + GAP) + NOTICE_HEIGHT;
        rebuildGrid();
    }

    private static float addChip(TooltipMakerAPI element, String label, Object data, ChipColors colors, boolean checked, float x, int row) {
        float width = Global.getSettings().computeStringWidth(label, Fonts.ORBITRON_12) + CHIP_TEXT_PADDING;
        ButtonAPI chip = element.addAreaCheckbox(label, data, colors.base(), colors.dark(), colors.bright(), width, CHIP_HEIGHT, 0f);
        chip.setChecked(checked);
        chip.getPosition().inTL(x, row * (CHIP_HEIGHT + GAP));
        return width;
    }

    private void rebuildGrid() {
        hideHoverTooltip();
        if (grid != null) {
            root.removeComponent(grid.getExternalScroller() != null ? grid.getExternalScroller() : grid);
        }
        List<SocketStorageRow> matching = SocketStorageQuery.apply(rows, filter);
        float gridHeight = Math.max(CELL_SIZE, height - PAD - FOOTER_HEIGHT - GAP - gridTop);
        TooltipMakerAPI element = root.createUIElement(innerWidth(), gridHeight, true);
        if (matching.isEmpty()) {
            String key = rows.isEmpty() ? "ui.socketStorage.empty" : "ui.socketStorage.noMatches";
            element.addPara("%s", 0f, Misc.getGrayColor(), Misc.getGrayColor(), Translation.text(key));
        }
        for (int start = 0; start < matching.size(); start += COLUMNS) {
            CustomPanelAPI line = Global.getSettings().createCustom(innerWidth() - SCROLLBAR_ROOM, CELL_SIZE, null);
            for (int i = start; i < Math.min(matching.size(), start + COLUMNS); i++) {
                SocketStorageRow row = matching.get(i);
                CustomPanelAPI cell = Global.getSettings().createCustom(CELL_SIZE, CELL_SIZE, cellFor(row));
                line.addComponent(cell).inTL((i - start) * (CELL_SIZE + GAP), 0f);
            }
            element.addCustom(line, start == 0 ? 0f : GAP);
        }
        root.addUIElement(element).inTL(PAD, gridTop);
        grid = element;
        rebuildFooter(matching);
    }

    private static List<Socketable> freeShown(List<SocketStorageRow> matching) {
        return matching.stream().filter(row -> !row.installed()).map(SocketStorageRow::socketable).toList();
    }

    private void rebuildFooter(List<SocketStorageRow> matching) {
        int shown = matching.size();
        if (notice != null) {
            root.removeComponent(notice);
        }
        if (summary != null) {
            root.removeComponent(summary);
        }
        TooltipMakerAPI noticeElement = root.createUIElement(innerWidth(), NOTICE_HEIGHT, false);
        if (selected != null) {
            noticeElement.addPara("%s", 0f, Misc.getPositiveHighlightColor(), Misc.getPositiveHighlightColor(),
                    Translation.msg("ui.socketStorage.placingHint").arg("name", selected.name()).text());
        } else if (targetingSocket) {
            noticeElement.addPara("%s", 0f, Misc.getPositiveHighlightColor(), Misc.getPositiveHighlightColor(),
                    Translation.text("ui.socketStorage.targetHint"));
        } else if (disassembledNotice != null) {
            noticeElement.addPara("%s", 0f, Misc.getPositiveHighlightColor(), Misc.getPositiveHighlightColor(), disassembledNotice);
        } else if (movedFromCargo > 0) {
            noticeElement.addPara("%s", 0f, Misc.getPositiveHighlightColor(), Misc.getPositiveHighlightColor(),
                    Translation.msg("ui.socketStorage.moved").count(movedFromCargo).arg("count", movedFromCargo).text());
        }
        root.addUIElement(noticeElement).inTL(PAD, gridTop - NOTICE_HEIGHT);
        notice = noticeElement;

        TooltipMakerAPI summaryElement = root.createUIElement(innerWidth(), FOOTER_HEIGHT, false);
        int installed = (int) rows.stream().filter(SocketStorageRow::installed).count();
        summaryElement.addPara("%s", 0f, Misc.getGrayColor(), Misc.getGrayColor(), Translation.msg("ui.socketStorage.summary")
                .arg("shown", shown).arg("stored", rows.size()).arg("installed", installed).text())
                .getPosition().inTL(0f, PARTS_TEXT_TOP);
        int free = freeShown(matching).size();
        String batchLabel = Translation.msg("ui.socketStorage.disassembleShown").arg("count", free).text();
        float batchWidth = Global.getSettings().computeStringWidth(batchLabel, Fonts.ORBITRON_20AA) + BUTTON_TEXT_PADDING;
        ButtonAPI batch = summaryElement.addButton(batchLabel, Control.DISASSEMBLE_SHOWN, batchWidth, FOOTER_HEIGHT, 0f);
        batch.getPosition().inTR(0f, 0f);
        batch.setEnabled(free > 0);
        root.addUIElement(summaryElement).inTL(PAD, height - PAD - FOOTER_HEIGHT);
        summary = summaryElement;
    }

    private void openConfirm(List<Socketable> targets) {
        if (targets.isEmpty()) {
            return;
        }
        pendingDisassembly = List.copyOf(targets);
        boolean single = targets.size() == 1;
        float confirmHeight = single ? CONFIRM_HEIGHT : BATCH_CONFIRM_HEIGHT;
        confirmBlocker = Global.getSettings().createCustom(width, height, new Blocker());
        root.addComponent(confirmBlocker).inTL(0f, 0f);
        float confirmWidth = innerWidth();
        confirm = Global.getSettings().createCustom(confirmWidth, confirmHeight,
                new FramedPanelPlugin(SocketStoragePanel.class, this::buttonPressed));
        TooltipMakerAPI element = confirm.createUIElement(confirmWidth - PAD * 2f, confirmHeight - PAD * 2f, false);
        int parts = targets.stream().mapToInt(socketable -> socketable.rarity().disassemblyParts()).sum();
        if (single) {
            Socketable socketable = targets.get(0);
            element.addPara("%s", 0f, socketable.rarity().color(), socketable.rarity().color(),
                    Translation.msg("ui.socketStorage.confirm.title").arg("name", socketable.name()).text());
            element.addPara("%s", LINE_PAD * 2f, Misc.getTextColor(), Misc.getTextColor(), partsText("ui.socketStorage.confirm.body", parts));
        } else {
            element.addPara("%s", 0f, Misc.getBasePlayerColor(), Misc.getBasePlayerColor(),
                    Translation.msg("ui.socketStorage.confirm.batchTitle").arg("count", targets.size()).text());
            addRarityBreakdown(element, targets);
            element.addPara("%s", LINE_PAD * 2f, Misc.getTextColor(), Misc.getTextColor(),
                    Translation.msg("ui.socketStorage.confirm.batchBody").arg("count", parts).text());
            if (targets.stream().anyMatch(socketable -> socketable.rarity() == SocketableRarity.UNIQUE)) {
                element.addPara("%s", LINE_PAD * 2f, Misc.getNegativeHighlightColor(), Misc.getNegativeHighlightColor(),
                        Translation.text("ui.socketStorage.confirm.includesUnique"));
            }
        }
        float buttonWidth = (confirmWidth - PAD * 2f - GAP) / 2f;
        element.addButton(Translation.text("ui.socketStorage.confirm.disassemble"), Control.CONFIRM_DISASSEMBLE, buttonWidth, FIELD_HEIGHT, 0f)
                .getPosition().inBR(0f, 0f);
        element.addButton(Translation.text("ui.socketStorage.confirm.cancel"), Control.CANCEL_DISASSEMBLE, buttonWidth, FIELD_HEIGHT, 0f)
                .getPosition().inBL(0f, 0f);
        confirm.addUIElement(element).inTL(PAD, PAD);
        root.addComponent(confirm).inTL(PAD, (height - confirmHeight) / 2f);
    }

    private static void addRarityBreakdown(TooltipMakerAPI element, List<Socketable> targets) {
        List<String> counts = new ArrayList<>();
        List<Color> colors = new ArrayList<>();
        for (SocketableRarity rarity : SocketableRarity.values()) {
            long count = targets.stream().filter(socketable -> socketable.rarity() == rarity).count();
            if (count > 0) {
                counts.add(Translation.msg("ui.socketStorage.confirm.rarityCount").arg("count", count)
                        .arg("rarity", Translation.text("ui.socketStorage.rarity." + rarity.name().toLowerCase(Locale.ROOT))).text());
                colors.add(rarity.color());
            }
        }
        String format = String.join(Translation.text("ui.socketStorage.confirm.raritySeparator"), counts.stream().map(text -> "%s").toList());
        element.addPara(format, LINE_PAD * 2f, colors.toArray(new Color[0]), counts.toArray(new String[0]));
    }

    private void disassemble(List<Socketable> targets) {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        CargoAPI cargo = playerFleet == null ? null : playerFleet.getCargo();
        int parts = 0;
        int disassembled = 0;
        Socketable last = null;
        for (Socketable socketable : targets) {
            int gained = SocketableDisassembly.disassemble(socketable, cargo);
            if (gained > 0) {
                parts += gained;
                disassembled++;
                last = socketable;
            }
        }
        if (disassembled == 1) {
            disassembledNotice = Translation.msg("ui.socketStorage.disassembled").count(parts)
                    .arg("name", last.name()).arg("count", parts).text();
        } else if (disassembled > 1) {
            disassembledNotice = Translation.msg("ui.socketStorage.disassembledMany").arg("count", disassembled).arg("parts", parts).text();
        }
    }

    private static String partsText(String key, int parts) {
        return Translation.msg(key).count(parts).arg("count", parts).text();
    }

    private void closeConfirm() {
        if (confirm == null) {
            return;
        }
        root.removeComponent(confirm);
        root.removeComponent(confirmBlocker);
        confirm = null;
        confirmBlocker = null;
        pendingDisassembly = List.of();
    }

    private List<StyledText> cellFooter(SocketStorageRow row) {
        if (row.installed()) {
            return List.of(Translation.msg("ui.socketStorage.cell.installed").arg("ship", row.installedIn()).styled());
        }
        String install = targetingSocket ? "ui.socketStorage.cell.freeTarget" : "ui.socketStorage.cell.free";
        return List.of(Translation.styled(install), Translation.msg("ui.socketStorage.cell.disassemble").count(row.socketable().rarity().disassemblyParts())
                .arg("count", row.socketable().rarity().disassemblyParts()).styled());
    }

    private SocketableCell cellFor(SocketStorageRow row) {
        return new SocketableCell(row.socketable(), CELL_LOOK, new SocketableCell.Listener() {
            @Override
            public void hovered(PositionAPI cell) {
                cellHovered(row, cell);
            }

            @Override
            public void left() {
                cellLeft(row);
            }

            @Override
            public boolean clicked() {
                cellClicked(row);
                return true;
            }

            @Override
            public boolean rightClicked() {
                cellRightClicked(row);
                return true;
            }

            @Override
            public boolean selected() {
                return row.socketable() == selected;
            }

            @Override
            public boolean dimmed() {
                return row.installed();
            }
        });
    }

    private void cellHovered(SocketStorageRow row, PositionAPI cell) {
        queued.add(() -> showHoverTooltip(row, cell));
    }

    private void cellLeft(SocketStorageRow row) {
        queued.add(() -> {
            if (hoverTooltip.isShowing(row)) {
                hideHoverTooltip();
            }
        });
    }

    private void showHoverTooltip(SocketStorageRow row, PositionAPI cell) {
        hoverTooltip.show(row, row.socketable(), () -> cellFooter(row), cell);
    }

    private void hideHoverTooltip() {
        hoverTooltip.hide();
    }



    private static final class Blocker extends BaseCustomUIPanelPlugin {

        private PositionAPI position;

        @Override
        public void positionChanged(PositionAPI position) {
            this.position = position;
        }

        @Override
        public void processInput(List<InputEventAPI> events) {
            for (InputEventAPI event : events) {
                boolean escape = event.isKeyboardEvent() && event.getEventValue() == Keyboard.KEY_ESCAPE;
                boolean press = (event.isMouseDownEvent() || event.isMouseScrollEvent()) && position != null && position.containsEvent(event);
                if (!event.isConsumed() && (press || (event.isKeyboardEvent() && !escape))) {
                    event.consume();
                }
            }
        }
    }
}
