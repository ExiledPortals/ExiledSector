package exiledsector.ui.socket;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.util.Misc;
import exiledlib.i18n.StyledText;
import exiledlib.i18n.Translation;
import exiledsector.items.MaterialItems;
import exiledsector.items.CraftingCurrency;
import exiledsector.items.CraftingWard;
import exiledsector.items.CorruptionOutcome;
import exiledsector.items.SocketCustody;
import exiledsector.items.CraftingMaterials;
import exiledsector.items.Item;
import exiledsector.items.ItemCrafting;
import exiledsector.items.ItemCurrency;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemNature;
import exiledsector.items.ItemKind;
import exiledsector.items.ItemName;
import exiledsector.items.ItemStore;
import exiledsector.items.ItemUnlock;
import exiledsector.items.SlottedLine;
import exiledsector.modifiers.ModifierSlot;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.SkillTreeSounds;
import exiledsector.ui.TooltipExpansion;
import exiledsector.ui.VanillaText;
import exiledsector.ui.cargo.CargoIconLook;
import exiledsector.ui.cargo.CargoIconLooks;
import exiledsector.ui.util.GLDraw;
import com.fs.starfarer.api.input.InputEventAPI;

import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class SocketWorkbenchPanel extends HoloPanel {

    public interface Listener {

        void changed(Item loaded);

        void closed();

        void pressedInside();

        default boolean offersItemPicker() {
            return false;
        }

        default void requestItem() {
        }

        default BenchModeSupport.OpenTarget openTarget() {
            return null;
        }

        default void wormholeOpened(String message) {
        }

        default void debugActionApplied() {
        }
    }

    static final float FRAME_INSET = 8f;
    private static final float COUNT_INSET = 4f;
    private static final float PLATE_ALPHA = 0.62f;
    private static final float PLATE_PAD = 6f;
    private static final float VIGNETTE_SHARE = 0.06f;
    private static final float PICK_BUTTON_WIDTH = 120f;
    private static final float TOOLTIP_WIDTH = 340f;
    private static final float SLOT_TAG_WIDTH = 20f;
    private static final float SCROLLBAR_ROOM = 20f;
    private static final float MIN_OPEN_BUTTON_HEIGHT = 26f;
    private static final float OPEN_CONFIRM_HEIGHT = 170f;
    private static final float DEBUG_BUTTON_SHARE = 0.32f;
    private static final float DEBUG_BUTTON_GAP = 6f;
    private static final Color PLATE_COLOR = Color.BLACK;
    private static final String FALLBACK_ICON = ItemDefinition.FALLBACK_ICON;

    private enum Control {CLOSE, PICK, CONFIRM_USE, CONFIRM_GROW, CANCEL_CONFIRM, OPEN_WORMHOLE, CONFIRM_OPEN, DEBUG_ACTION}

    private record OpenChoiceButton(Object target) {
    }

    private static final float CONFIRM_WIDTH = 460f;
    private static final float USE_CONFIRM_HEIGHT = 180f;
    private static final float WARD_LINE_HEIGHT = 22f;
    private static final float GROW_CONFIRM_HEIGHT = 140f;

    private final Listener workbenchListener;
    private final SocketWorkbenchFlair socketFlair = new SocketWorkbenchFlair();
    private final Random craftingRandom = new Random();
    private float sceneScale;
    private UIComponentAPI headerElement;
    private BenchMode benchMode = BenchMode.HULL_ITEMS;
    private ItemCurrency pendingCurrency;
    private boolean detailsShowRollRanges;
    private UIComponentAPI recipesElement;
    private UIComponentAPI detailsElement;
    private UIComponentAPI noticeElement;
    private UIComponentAPI openElement;
    private Item pendingOpenItem;
    private Object pendingOpenTarget;
    private boolean wormholeOpened;
    private Item loadedItem;
    private CargoIconLook loadedLook;
    private final Set<CraftingWard> activeWards = EnumSet.noneOf(CraftingWard.class);
    private String noticeText;
    private boolean noticeIsWarning;

    private SocketWorkbenchPanel(CustomPanelAPI hostPanel, Listener workbenchListener) {
        super(hostPanel);
        this.workbenchListener = workbenchListener;
    }

    public static SocketWorkbenchPanel open(CustomPanelAPI hostPanel, SocketPanelPlacement area, boolean centred, Listener workbenchListener) {
        return open(hostPanel, area, centred, BenchMode.HULL_ITEMS, workbenchListener);
    }

    public static SocketWorkbenchPanel open(CustomPanelAPI hostPanel, SocketPanelPlacement area, boolean centred, BenchMode mode,
                                            Listener workbenchListener) {
        SocketWorkbenchPanel workbenchPanel = new SocketWorkbenchPanel(hostPanel, workbenchListener);
        workbenchPanel.benchMode = mode;
        workbenchPanel.sceneScale = scaleFor(area.panelWidth(), area.panelHeight());
        float width = sizeFor(workbenchPanel.sceneScale, BenchScene.ART_WIDTH);
        float height = sizeFor(workbenchPanel.sceneScale, BenchScene.ART_HEIGHT);
        float left = centred ? area.panelLeft() + (area.panelWidth() - width) / 2f : area.panelLeft();
        float top = centred ? area.panelTop() + (area.panelHeight() - height) / 2f : area.panelTop();
        workbenchPanel.attach(left, top, width, height, area.instantly());
        return workbenchPanel;
    }

    static float scaleFor(float areaWidth, float areaHeight) {
        return BenchScene.scaleFor(areaWidth - FRAME_INSET * 2f, areaHeight - FRAME_INSET * 2f);
    }

    static float sizeFor(float scale, float artSize) {
        return artSize * scale + FRAME_INSET * 2f;
    }

    static float tileAngle(int index, int tileCount) {
        if (tileCount <= 1) {
            return (float) (Math.PI / 2.0);
        }
        float firstAngle = (float) Math.PI + BenchScene.ORBIT_DROP_RADIANS;
        float lastAngle = -BenchScene.ORBIT_DROP_RADIANS;
        return firstAngle - index * (firstAngle - lastAngle) / (tileCount - 1);
    }

    private float artX(float artPixels) {
        return FRAME_INSET + artPixels * sceneScale;
    }

    private float artY(float artPixels) {
        return FRAME_INSET + artPixels * sceneScale;
    }

    private float tileSize() {
        return BenchScene.tileSize(sceneScale);
    }

    private float slotRadius() {
        return BenchScene.SLOT_RADIUS * sceneScale;
    }

    private float socketScreenX() {
        return panelPosition.getX() + artX(benchMode.socketX());
    }

    private float socketScreenY() {
        return panelPosition.getY() + panelHeight - artY(benchMode.socketY());
    }

    public Item loaded() {
        return loadedItem;
    }

    public BenchMode mode() {
        return benchMode;
    }

    public void load(Item item) {
        queue(() -> {
            if (item == loadedItem || (item != null && BenchModes.modeFor(item.type()) != benchMode)) {
                return;
            }
            cancelConfirm();
            loadedItem = item;
            noticeText = null;
            socketFlair.restartLoad();
            rebuildContent();
        });
    }

    public void refresh() {
        queue(() -> {
            ItemStore store = modeStore();
            if (loadedItem != null && (store == null || !store.owned().contains(loadedItem))) {
                loadedItem = null;
                cancelConfirm();
            }
            buildHeader();
            rebuildContent();
        });
    }

    @Override
    protected void onClose() {
        hoverTooltip.hide();
        workbenchListener.closed();
    }

    @Override
    protected void onPressInside() {
        workbenchListener.pressedInside();
    }

    @Override
    protected UIComponentAPI[] chromeComponents() {
        return new UIComponentAPI[]{headerElement};
    }

    @Override
    protected UIComponentAPI[] contentComponents() {
        return new UIComponentAPI[]{recipesElement, detailsElement, noticeElement, openElement};
    }

    @Override
    public void renderBelow(float alphaMult) {
        super.renderBelow(alphaMult);
        if (panelPosition == null) {
            return;
        }
        float contentAlpha = contentAlpha() * alphaMult;
        if (contentAlpha <= 0f) {
            return;
        }
        float artLeft = panelPosition.getX() + FRAME_INSET;
        float artBottom = panelPosition.getY() + FRAME_INSET;
        float artWidth = BenchScene.ART_WIDTH * sceneScale;
        float artHeight = BenchScene.ART_HEIGHT * sceneScale;
        BenchScene.drawArt(benchMode, artLeft, artBottom, sceneScale, contentAlpha);
        float vignette = Math.min(artWidth, artHeight) * VIGNETTE_SHARE;
        GLDraw.innerGlow(artLeft, artBottom, artWidth, artHeight, vignette, PLATE_COLOR, contentAlpha);
        drawPlate(BenchScene.detailsLeft(benchMode), BenchScene.detailsTop(benchMode), BenchScene.DETAILS_WIDTH, detailsArtHeight(), contentAlpha);
        Color subjectColor = loadedItem == null ? null : loadedItem.frameColor();
        socketFlair.render(socketScreenX(), socketScreenY(), slotRadius(), SkillTreePanelStyle.GLOW_COLOR, subjectColor,
                loadedItem == null ? null : loadedItem.iconPath(), loadedLook, contentAlpha);
    }

    private void drawPlate(float artLeft, float artTop, float artWidth, float artHeight, float alpha) {
        float plateWidth = artWidth * sceneScale + PLATE_PAD * 2f;
        float plateHeight = artHeight * sceneScale + PLATE_PAD * 2f;
        float plateLeft = panelPosition.getX() + artX(artLeft) - PLATE_PAD;
        float plateBottom = panelPosition.getY() + panelHeight - artY(artTop) - artHeight * sceneScale - PLATE_PAD;
        GLDraw.fillQuad(plateLeft, plateBottom, plateWidth, plateHeight, PLATE_COLOR, PLATE_ALPHA * alpha);
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        if (!isClosing()) {
            for (InputEventAPI event : events) {
                if (!event.isConsumed() && TooltipExpansion.isToggle(event)) {
                    TooltipExpansion.toggle();
                    event.consume();
                }
            }
        }
        if (!isClosing() && panelPosition != null && workbenchListener.offersItemPicker()) {
            for (InputEventAPI event : events) {
                if (!event.isConsumed() && event.isLMBDownEvent() && insideSocket(event.getX(), event.getY())) {
                    event.consume();
                    queue(this::requestItem);
                }
            }
        }
        super.processInput(events);
    }

    private boolean insideSocket(float x, float y) {
        float dx = x - socketScreenX();
        float dy = y - socketScreenY();
        float radius = slotRadius() * BenchScene.SOCKET_CLICK_SHARE;
        return dx * dx + dy * dy <= radius * radius;
    }

    private void requestItem() {
        hoverTooltip.hide();
        workbenchListener.requestItem();
    }

    @Override
    protected void advanceOpen(float amount) {
        hoverTooltip.refreshIfExpansionChanged();
        if (detailsShowRollRanges != TooltipExpansion.isExpandedOrHeld()) {
            rebuildDetails();
            refreshOpacity();
        }
    }

    @Override
    protected void advanceAlways(float amount) {
        socketFlair.advance(amount, loadedItem != null);
    }

    @Override
    public void buttonPressed(Object id) {
        queue(() -> handleButton(id));
    }

    private void handleButton(Object id) {
        if (wormholeOpened) {
            return;
        }
        if (id == Control.CONFIRM_OPEN) {
            confirmOpen();
        } else if (id instanceof OpenChoiceButton choice) {
            pendingOpenTarget = choice.target();
            confirmOpen();
        } else if (id == Control.CONFIRM_USE || id == Control.CONFIRM_GROW) {
            ItemCurrency currency = pendingCurrency;
            cancelConfirm();
            if (currency == null) {
                return;
            }
            if (id == Control.CONFIRM_USE && usable(currency)) {
                applyUse(currency);
            } else if (id == Control.CONFIRM_GROW && growable(currency)) {
                applyGrowth(currency);
            }
        } else if (id == Control.CANCEL_CONFIRM) {
            cancelConfirm();
        } else if (isConfirmOpen()) {
            return;
        } else if (id == Control.CLOSE) {
            close();
        } else if (id == Control.PICK) {
            requestItem();
        } else if (id == Control.OPEN_WORMHOLE) {
            openPressed();
        } else if (id == Control.DEBUG_ACTION) {
            debugPressed();
        }
    }

    public boolean escape() {
        if (!isConfirmOpen()) {
            return false;
        }
        queue(this::cancelConfirm);
        return true;
    }

    private void cancelConfirm() {
        pendingCurrency = null;
        pendingOpenItem = null;
        closeConfirm();
    }

    @Override
    protected float confirmWidth() {
        return Math.min(CONFIRM_WIDTH, panelWidth - PAD * 2f);
    }

    private void tileClicked(ItemCurrency currency, boolean skipConfirm) {
        hoverTooltip.hide();
        if (wormholeOpened || isConfirmOpen() || !usable(currency)) {
            return;
        }
        if (skipConfirm) {
            applyUse(currency);
        } else {
            openCraftConfirm(currency, Control.CONFIRM_USE);
        }
    }

    private void tileRightClicked(ItemCurrency currency, boolean skipConfirm) {
        hoverTooltip.hide();
        if (wormholeOpened || isConfirmOpen() || !growable(currency)) {
            return;
        }
        if (skipConfirm) {
            applyGrowth(currency);
        } else {
            openCraftConfirm(currency, Control.CONFIRM_GROW);
        }
    }

    private void openCraftConfirm(ItemCurrency currency, Control confirmId) {
        pendingCurrency = currency;
        boolean growing = confirmId == Control.CONFIRM_GROW;
        Set<CraftingWard> spentWards = growing ? Set.of() : ItemCrafting.wardsSpentBy(currency, loadedItem, activeWards);
        float confirmHeight = growing ? GROW_CONFIRM_HEIGHT : USE_CONFIRM_HEIGHT + spentWards.size() * WARD_LINE_HEIGHT;
        openConfirm(confirmHeight, element -> writeCraftConfirm(element, currency, growing, spentWards),
                growing ? "ui.workbench.confirm.grow" : "ui.workbench.confirm.use", confirmId, "ui.workbench.confirm.cancel", Control.CANCEL_CONFIRM);
    }

    private void writeCraftConfirm(TooltipMakerAPI element, ItemCurrency currency, boolean growing, Set<CraftingWard> spentWards) {
        String name = MaterialItems.displayName(currency.itemId());
        String title = growing ? Translation.msg("ui.workbench.confirm.growTitle").arg("name", name).text()
                : Translation.msg("ui.workbench.confirm.useTitle").arg("name", name).arg("target", loadedItem.name()).text();
        VanillaText.plainPara(element, title, 0f, Misc.getBasePlayerColor());
        String body = growing
                ? Translation.msg("ui.workbench.confirm.growBody").arg("cost", currency.partsCost()).arg("parts", ItemCrafting.parts(playerMaterials())).text()
                : Translation.text(modeDescriptionKey(currency));
        VanillaText.plainPara(element, body, LINE_PAD * 2f, Misc.getTextColor());
        for (CraftingWard ward : spentWards) {
            VanillaText.plainPara(element, Translation.msg("ui.workbench.confirm.wardSpent").arg("name", MaterialItems.displayName(ward.itemId())).text(),
                    LINE_PAD * 2f, Misc.getHighlightColor());
        }
        VanillaText.plainPara(element, Translation.text("ui.workbench.confirm.skipHint"), LINE_PAD * 2f, Misc.getGrayColor());
    }

    private boolean growable(ItemCurrency currency) {
        if (!currency.synthesisable()) {
            showNotice(Translation.msg("ui.workbench.notice.notSynthesisable").arg("name", recipeName(currency)).text());
            return false;
        }
        CraftingMaterials materials = playerMaterials();
        int ownedParts = ItemCrafting.parts(materials);
        if (ownedParts < currency.partsCost()) {
            showNotice(Translation.msg("ui.workbench.notice.notEnoughParts").arg("cost", currency.partsCost()).arg("parts", ownedParts)
                    .arg("name", recipeName(currency)).text());
            return false;
        }
        return true;
    }

    private void applyGrowth(ItemCurrency currency) {
        if (!ItemCrafting.synthesise(currency, playerMaterials())) {
            return;
        }
        noticeText = Translation.msg("ui.workbench.notice.synthesisedCurrency").arg("name", recipeName(currency)).text();
        SkillTreeSounds.crafted();
        socketFlair.flash(SkillTreePanelStyle.GLOW_COLOR);
        afterChange();
    }

    private boolean usable(ItemCurrency currency) {
        if (domainSupport() != null) {
            return domainUsable(currency);
        }
        CraftingMaterials materials = playerMaterials();
        if (materials.count(currency.itemId()) < 1) {
            String noneOwnedKey = currency.synthesisable() ? "ui.workbench.notice.noneOwned" : "ui.workbench.notice.noneFound";
            showNotice(Translation.msg(noneOwnedKey).arg("name", MaterialItems.displayName(currency.itemId())).text());
            return false;
        }
        if (loadedItem == null) {
            showNotice(Translation.text("ui.workbench.notice.loadFirst"));
            return false;
        }
        if (droppedMissingWards(materials)) {
            return false;
        }
        if (!ItemCrafting.canUse(currency, loadedItem, allowedUniques(), activeWards)) {
            if (ItemCrafting.canUse(currency, loadedItem, allowedUniques())) {
                String wardKey = currency == CraftingCurrency.TRANSPOSITION
                        ? "ui.workbench.notice.wardBlocksTransposition" : "ui.workbench.notice.wardBlocks";
                showNotice(Translation.msg(wardKey).arg("name", MaterialItems.displayName(currency.itemId())).text());
                return false;
            }
            if (loadedItem.mirrored()) {
                showNotice(Translation.msg("ui.workbench.notice.mirrored").arg("name", loadedItem.name()).text());
                return false;
            }
            if (loadedItem.corrupted()) {
                showNotice(Translation.msg("ui.workbench.notice.corruptedItem").arg("name", loadedItem.name()).text());
                return false;
            }
            if (ItemCrafting.isLockedFromCurrency(loadedItem.definition())) {
                showNotice(Translation.msg("ui.workbench.notice.uniqueSubroutine").arg("name", loadedItem.name()).text());
                return false;
            }
            if (currency == CraftingCurrency.FIXATION && loadedItem.hasLockedModifier()) {
                showNotice(Translation.msg("ui.workbench.notice.alreadyFixed").arg("name", loadedItem.name()).text());
                return false;
            }
            if (currency == CraftingCurrency.TRANSPOSITION && loadedItem.definition() != null && loadedItem.definition().unique()) {
                showNotice(Translation.text("ui.workbench.notice.noOtherUnique"));
                return false;
            }
            showNotice(Translation.msg("ui.workbench.notice.cannotUse").arg("name", MaterialItems.displayName(currency.itemId()))
                    .arg("target", loadedItem.name()).text());
            return false;
        }
        return true;
    }

    private boolean droppedMissingWards(CraftingMaterials materials) {
        List<CraftingWard> missingWards = activeWards.stream().filter(ward -> materials.count(ward.itemId()) < 1).toList();
        if (missingWards.isEmpty()) {
            return false;
        }
        missingWards.forEach(activeWards::remove);
        showNotice(Translation.msg("ui.workbench.notice.wardMissing").arg("name", MaterialItems.displayName(missingWards.get(0).itemId())).text());
        return true;
    }

    private boolean domainUsable(ItemCurrency currency) {
        CraftingMaterials materials = playerMaterials();
        BenchModeSupport.Refusal refusal = modeSupport().refusal(currency, loadedItem, modeStore(), materials, allowedUniques(),
                activeWards);
        if (refusal.checksWards() && droppedMissingWards(materials)) {
            return false;
        }
        if (refusal.allowed()) {
            return true;
        }
        showNotice(refusalText(currency, refusal.noticeKey()));
        return false;
    }

    private String refusalText(ItemCurrency currency, String noticeKey) {
        return Translation.msg(noticeKey).arg("name", MaterialItems.displayName(currency.itemId()))
                .arg("target", loadedItem == null ? "" : loadedItem.name()).text();
    }

    private BenchModeSupport domainSupport() {
        return BenchModes.of(benchMode);
    }

    private ItemStore modeStore() {
        BenchModeSupport support = domainSupport();
        return support != null ? support.store() : ItemStore.get();
    }

    private BenchModeSupport modeSupport() {
        BenchModeSupport support = domainSupport();
        if (support == null) {
            throw new IllegalStateException("No domain supports the " + benchMode + " bench mode");
        }
        return support;
    }

    private void applyDomainUse(ItemCurrency currency) {
        String nameBefore = loadedItem.name();
        Set<CraftingWard> spentWards = ItemCrafting.wardsSpentBy(currency, loadedItem, activeWards);
        BenchModeSupport.Use used = modeSupport().use(currency, loadedItem, modeStore(), playerMaterials(), craftingRandom,
                allowedUniques(), activeWards, nameBefore);
        if (used == null) {
            showNotice(refusalText(currency, modeSupport().cannotUseNoticeKey()));
            return;
        }
        activeWards.removeAll(spentWards);
        loadedItem = used.selectedAfter();
        noticeText = withSpentWards(used.notice(), spentWards);
        SkillTreeSounds.socketed();
        socketFlair.flash(used.result().frameColor());
        afterChange();
    }

    private void applyUse(ItemCurrency currency) {
        if (domainSupport() != null) {
            applyDomainUse(currency);
            return;
        }
        CraftingMaterials materials = playerMaterials();
        String nameBefore = loadedItem.name();
        Set<CraftingWard> spentWards = ItemCrafting.wardsSpentBy(currency, loadedItem, activeWards);
        Item result = ItemCrafting.use(currency, loadedItem, materials, ItemStore.get(), craftingRandom, allowedUniques(), activeWards);
        if (result == null) {
            return;
        }
        activeWards.removeAll(spentWards);
        if (currency != CraftingCurrency.REPLICATION) {
            loadedItem = result;
        }
        String usedKey = usedNoticeKey((CraftingCurrency) currency, result.corruption(), ItemNature.of(result.kind()));
        noticeText = Translation.msg(usedKey)
                .arg("before", nameBefore).arg("name", result.name()).text();
        SocketCustody.markHostsChanged(result);
        if ((currency == CraftingCurrency.TRANSPOSITION || currency == CraftingCurrency.CORRUPTION) && SocketCustody.unsocketIfMisfit(result)) {
            String movedNotice = Translation.msg("ui.workbench.notice.movedToStorage").arg("name", result.name())
                    .arg("type", result.kindName()).text();
            noticeText = Translation.msg("format.sentences").arg("a", noticeText).arg("b", movedNotice).text();
        }
        noticeText = withSpentWards(noticeText, spentWards);
        SkillTreeSounds.socketed();
        socketFlair.flash(result.frameColor());
        afterChange();
    }

    private static String withSpentWards(String notice, Set<CraftingWard> spentWards) {
        String combined = notice;
        for (CraftingWard ward : spentWards) {
            String spentNotice = Translation.msg("ui.workbench.notice.wardSpent").arg("name", MaterialItems.displayName(ward.itemId())).text();
            combined = Translation.msg("format.sentences").arg("a", combined).arg("b", spentNotice).text();
        }
        return combined;
    }

    private void showNotice(String text) {
        noticeText = text;
        noticeIsWarning = false;
        rebuildNotice();
        refreshOpacity();
    }

    private void showWarning(String text) {
        noticeText = text;
        noticeIsWarning = true;
        rebuildNotice();
        refreshOpacity();
    }

    private void afterChange() {
        noticeIsWarning = false;
        buildHeader();
        rebuildContent();
        workbenchListener.changed(loadedItem);
    }

    private static Predicate<ItemDefinition> allowedUniques() {
        return definition -> ItemUnlock.unlockConditionMet(definition, Global.getSector());
    }

    @Override
    protected void build() {
        buildHeader();
        rebuildContent();
    }

    private void buildHeader() {
        BenchModeSupport support = domainSupport();
        TooltipMakerAPI element = startHeader(support != null ? support.titleKey() : "ui.workbench.title", "ui.workbench.close", Control.CLOSE);
        float partsRight = CLOSE_BUTTON_WIDTH + GAP * 2f;
        if (workbenchListener.offersItemPicker()) {
            element.addButton(Translation.text("ui.workbench.pickItem"), Control.PICK, PICK_BUTTON_WIDTH, HEADER_BUTTON_HEIGHT, 0f)
                    .getPosition().inTR(partsRight, 0f);
            partsRight += PICK_BUTTON_WIDTH + GAP * 2f;
        }
        addPartsCount(element, partsRight);
        headerElement = finishHeader(element, headerElement);
    }

    private List<ItemCurrency> currencies() {
        BenchModeSupport support = domainSupport();
        return support != null ? support.currencies() : List.of(CraftingCurrency.values());
    }

    private void rebuildContent() {
        rebuildTiles();
        rebuildDetails();
        rebuildNotice();
        rebuildOpenButton();
        refreshOpacity();
    }

    private boolean showsOpenButton() {
        BenchModeSupport support = domainSupport();
        return support != null && support.openTexts() != null && workbenchListener.openTarget() != null;
    }

    private float detailsArtHeight() {
        float detailsBottom = showsOpenButton() ? BenchScene.CHART_DETAILS_BOTTOM : BenchScene.DETAILS_BOTTOM;
        return detailsBottom - BenchScene.detailsTop(benchMode);
    }

    private BenchModeSupport.OpenCheck openBlocker() {
        BenchModeSupport.OpenTarget openTarget = workbenchListener.openTarget();
        return modeSupport().openCheck(Global.getSector(), modeStore(), loadedItem, openTarget == null ? null : openTarget.target());
    }

    private void rebuildOpenButton() {
        if (!showsOpenButton()) {
            if (openElement != null) {
                panelRoot.removeComponent(openElement);
                openElement = null;
            }
            return;
        }
        BenchModeSupport.OpenCheck blocker = openBlocker();
        BenchModeSupport.OpenTexts openTexts = modeSupport().openTexts();
        float buttonWidth = BenchScene.DETAILS_WIDTH * sceneScale;
        float plateBottom = artY(BenchScene.CHART_DETAILS_BOTTOM) + PLATE_PAD;
        float buttonTop = Math.max(artY(BenchScene.OPEN_BUTTON_TOP), plateBottom + PLATE_PAD);
        float artBottom = artY(BenchScene.ART_HEIGHT);
        float buttonHeight = Math.min(Math.max(MIN_OPEN_BUTTON_HEIGHT, BenchScene.OPEN_BUTTON_HEIGHT * sceneScale), artBottom - buttonTop);
        BenchModeSupport.DebugAction debugAction = modeSupport().debugAction();
        float debugWidth = debugAction == null ? 0f : buttonWidth * DEBUG_BUTTON_SHARE;
        float openWidth = debugAction == null ? buttonWidth : buttonWidth - debugWidth - DEBUG_BUTTON_GAP;
        TooltipMakerAPI element = panelRoot.createUIElement(buttonWidth, buttonHeight, false);
        element.setButtonFontOrbitron20Bold();
        ButtonAPI openButton = element.addButton(Translation.text(openTexts.buttonKey()), Control.OPEN_WORMHOLE, Misc.getBrightPlayerColor(),
                Misc.getDarkPlayerColor(), Alignment.MID, CutStyle.ALL, openWidth, buttonHeight, 0f);
        openButton.setEnabled(blocker.allowsOpening() && !wormholeOpened);
        openButton.setShowTooltipWhileInactive(true);
        String tooltipText = Translation.text(blocker.allowsOpening() ? openTexts.tooltipKey() : blocker.reasonKey());
        element.addTooltipToPrevious(new GameTextTooltip(TOOLTIP_WIDTH, tooltip -> VanillaText.plainPara(tooltip, tooltipText, 0f,
                Misc.getTextColor())), TooltipMakerAPI.TooltipLocation.BELOW);
        element.setButtonFontDefault();
        if (debugAction != null) {
            addDebugButton(element, debugAction, openWidth + DEBUG_BUTTON_GAP, debugWidth, buttonHeight);
        }
        openElement = replaceElement(openElement, element, artX(BenchScene.detailsLeft(benchMode)), buttonTop);
    }

    private void addDebugButton(TooltipMakerAPI element, BenchModeSupport.DebugAction debugAction, float left, float width, float height) {
        ButtonAPI debugButton = element.addButton(Translation.text(debugAction.buttonKey()), Control.DEBUG_ACTION, Misc.getNegativeHighlightColor(),
                Misc.getDarkPlayerColor(), Alignment.MID, CutStyle.ALL, width, height, 0f);
        debugButton.setEnabled(!wormholeOpened);
        debugButton.getPosition().inTL(left, 0f);
        String tooltipText = Translation.text(debugAction.tooltipKey());
        element.addTooltipToPrevious(new GameTextTooltip(TOOLTIP_WIDTH, tooltip -> VanillaText.plainPara(tooltip, tooltipText, 0f,
                Misc.getTextColor())), TooltipMakerAPI.TooltipLocation.BELOW);
    }

    private void debugPressed() {
        hoverTooltip.hide();
        BenchModeSupport.OpenTarget openTarget = workbenchListener.openTarget();
        if (openTarget == null || modeSupport().debugAction() == null) {
            return;
        }
        modeSupport().runDebugAction(Global.getSector(), loadedItem, openTarget.target());
        SkillTreeSounds.panelOpened();
        workbenchListener.debugActionApplied();
    }

    private void openPressed() {
        hoverTooltip.hide();
        BenchModeSupport.OpenCheck blocker = openBlocker();
        if (!blocker.allowsOpening()) {
            SkillTreeSounds.refused();
            showWarning(Translation.text(blocker.reasonKey()));
            rebuildOpenButton();
            refreshOpacity();
            return;
        }
        BenchModeSupport.OpenTarget openTarget = workbenchListener.openTarget();
        pendingOpenItem = loadedItem;
        pendingOpenTarget = openTarget == null ? null : openTarget.target();
        List<String> confirmLines = openTarget == null ? List.of() : openTarget.confirmLines().get();
        List<BenchModeSupport.OpenChoice> choices = openTarget == null ? List.of() : openTarget.choices().get();
        if (choices.isEmpty()) {
            showOpenConfirm(confirmLines);
        } else {
            showChoiceConfirm(confirmLines, choices);
        }
    }

    private void showChoiceConfirm(List<String> confirmLines, List<BenchModeSupport.OpenChoice> choices) {
        Item confirmedItem = pendingOpenItem;
        int entries = modeSupport().openEntries();
        BenchModeSupport.OpenTexts openTexts = modeSupport().openTexts();
        List<String> lines = new ArrayList<>(confirmLines);
        List<String> labels = new ArrayList<>();
        List<Object> ids = new ArrayList<>();
        for (BenchModeSupport.OpenChoice choice : choices) {
            lines.add(choice.description());
            labels.add(choice.label());
            ids.add(new OpenChoiceButton(choice.target()));
        }
        openChoiceConfirm(element -> writeOpenConfirm(element, openTexts, confirmedItem, entries, lines), labels, ids, openTexts.confirmNoKey(),
                Control.CANCEL_CONFIRM);
    }

    private void showOpenConfirm(List<String> confirmLines) {
        Item confirmedItem = pendingOpenItem;
        int entries = modeSupport().openEntries();
        BenchModeSupport.OpenTexts openTexts = modeSupport().openTexts();
        openConfirm(OPEN_CONFIRM_HEIGHT + confirmLines.size() * WARD_LINE_HEIGHT,
                element -> writeOpenConfirm(element, openTexts, confirmedItem, entries, confirmLines), openTexts.confirmYesKey(),
                Control.CONFIRM_OPEN, openTexts.confirmNoKey(), Control.CANCEL_CONFIRM);
    }

    private static void writeOpenConfirm(TooltipMakerAPI element, BenchModeSupport.OpenTexts openTexts, Item openedItem, int entries,
                                         List<String> confirmLines) {
        VanillaText.plainPara(element, Translation.msg(openTexts.confirmTitleKey()).arg("name", openedItem.name()).text(), 0f,
                openedItem.rarity().color());
        VanillaText.plainPara(element, Translation.text(openTexts.confirmBodyKey()), LINE_PAD * 2f, Misc.getTextColor());
        for (String line : confirmLines) {
            VanillaText.plainPara(element, line, LINE_PAD, Misc.getHighlightColor());
        }
        VanillaText.plainPara(element, Translation.msg(openTexts.confirmEntriesKey()).count(entries).text(), LINE_PAD * 2f,
                Misc.getGrayColor());
    }

    private void confirmOpen() {
        Item openedItem = pendingOpenItem;
        Object openedTarget = pendingOpenTarget;
        cancelConfirm();
        pendingOpenTarget = null;
        if (openedItem == null || openedItem != loadedItem) {
            return;
        }
        BenchModeSupport.OpenOutcome outcome = modeSupport().openAt(Global.getSector(), modeStore(), openedItem, modeSupport().openEntries(),
                craftingRandom, openedTarget);
        String message = Translation.msg(outcome.messageKey()).arg("name", openedItem.name()).text();
        if (!outcome.opened()) {
            SkillTreeSounds.refused();
            showWarning(message);
            rebuildOpenButton();
            refreshOpacity();
            return;
        }
        wormholeOpened = true;
        SkillTreeSounds.wormholeJumped();
        rebuildOpenButton();
        refreshOpacity();
        workbenchListener.wormholeOpened(message);
    }

    private void rebuildTiles() {
        hoverTooltip.hide();
        float artWidth = BenchScene.ART_WIDTH * sceneScale;
        float artHeight = BenchScene.ART_HEIGHT * sceneScale;
        TooltipMakerAPI element = panelRoot.createUIElement(artWidth, artHeight, false);
        CraftingMaterials materials = playerMaterials();
        List<ItemCurrency> shownCurrencies = currencies();
        float tileSize = tileSize();
        float socketX = benchMode.socketX() * sceneScale;
        float socketY = benchMode.socketY() * sceneScale;
        for (int i = 0; i < shownCurrencies.size(); i++) {
            float angle = tileAngle(i, shownCurrencies.size());
            float tileCentreX = socketX + benchMode.orbitRadiusX() * sceneScale * (float) Math.cos(angle);
            float tileCentreY = socketY - benchMode.orbitRadiusY() * sceneScale * (float) Math.sin(angle);
            addTile(element, shownCurrencies.get(i), tileCentreX - tileSize / 2f, tileCentreY - tileSize / 2f, tileSize, materials);
        }
        addInhibitorTiles(element, tileSize, materials);
        recipesElement = replaceElement(recipesElement, element, FRAME_INSET, FRAME_INSET);
    }

    private void addInhibitorTiles(TooltipMakerAPI element, float tileSize, CraftingMaterials materials) {
        addWardTile(element, CraftingWard.SUFFIX, benchMode.leftConsoleX() * sceneScale - tileSize / 2f,
                benchMode.leftConsoleY() * sceneScale - tileSize / 2f, tileSize, materials);
        addWardTile(element, CraftingWard.PREFIX, benchMode.rightConsoleX() * sceneScale - tileSize / 2f,
                benchMode.rightConsoleY() * sceneScale - tileSize / 2f, tileSize, materials);
    }

    private void addWardTile(TooltipMakerAPI element, CraftingWard ward, float tileLeft, float tileTop, float tileSize,
                             CraftingMaterials materials) {
        String iconPath = MaterialItems.loadedIconPath(ward.itemId());
        WorkbenchTile wardTile = new WorkbenchTile(List.of(iconPath == null ? FALLBACK_ICON : iconPath), CargoIconLooks.forMaterial(ward.itemId()),
                () -> activeWards.contains(ward), true, tileHandler(ward, anchor -> showWardTooltip(ward, anchor), skipConfirm -> toggleWard(ward),
                skipConfirm -> showNotice(Translation.msg("ui.workbench.notice.notSynthesisable").arg("name", MaterialItems.displayName(ward.itemId()))
                        .text())));
        CustomPanelAPI tilePanel = Global.getSettings().createCustom(tileSize, tileSize, wardTile);
        element.addCustom(tilePanel, 0f).getPosition().inTL(tileLeft, tileTop);
        addCount(element, ward.itemId(), tileLeft, tileTop, tileSize, materials);
    }

    private void toggleWard(CraftingWard ward) {
        hoverTooltip.hide();
        String wardName = MaterialItems.displayName(ward.itemId());
        if (activeWards.remove(ward)) {
            showNotice(Translation.msg("ui.workbench.notice.wardOff").arg("name", wardName).text());
            return;
        }
        if (playerMaterials().count(ward.itemId()) < 1) {
            showNotice(Translation.msg("ui.workbench.notice.noneFound").arg("name", wardName).text());
            return;
        }
        activeWards.add(ward);
        showNotice(Translation.msg("ui.workbench.notice.wardOn." + ward.name().toLowerCase(Locale.ROOT)).arg("name", wardName).text());
    }

    private void showWardTooltip(CraftingWard ward, PositionAPI anchor) {
        if (isClosing()) {
            hoverTooltip.hide();
            return;
        }
        hoverTooltip.show(ward, TOOLTIP_WIDTH, (element, expanded) -> writeWardTooltip(element, ward), anchor,
                ItemHoverTooltip.Placement.BELOW, PAD);
    }

    private void writeWardTooltip(TooltipMakerAPI element, CraftingWard ward) {
        element.setParaFont(Fonts.ORBITRON_20AA);
        VanillaText.plainPara(element, MaterialItems.displayName(ward.itemId()), 0f, ward.color());
        element.setParaFontDefault();
        VanillaText.plainPara(element, Translation.text(wardDescriptionKey(ward)), GAP, Misc.getTextColor());
        int ownedCount = playerMaterials().count(ward.itemId());
        VanillaText.plainPara(element, Translation.msg("ui.workbench.tile.inStorage").arg("count", ownedCount).text(), GAP, Misc.getGrayColor());
        String stateKey;
        if (activeWards.contains(ward)) {
            stateKey = "ui.workbench.ward.on";
        } else {
            stateKey = ownedCount > 0 ? "ui.workbench.ward.off" : "ui.workbench.tile.noneFound";
        }
        VanillaText.plainPara(element, Translation.text(stateKey), GAP, Misc.getGrayColor());
        VanillaText.plainPara(element, Translation.text("ui.workbench.tile.notSynthesisable"), GAP, Misc.getGrayColor());
    }

    static String wardDescriptionKey(CraftingWard ward) {
        return MaterialItems.descriptionKey(ward.itemId());
    }

    private TileInput.Handler tileHandler(Object tooltipKey, Consumer<PositionAPI> showTooltip, Consumer<Boolean> clicked,
                                          Consumer<Boolean> rightClicked) {
        return new TileInput.Handler() {
            @Override
            public void entered(PositionAPI tilePosition) {
                queue(() -> showTooltip.accept(tilePosition));
            }

            @Override
            public void left() {
                queue(() -> {
                    if (hoverTooltip.isShowing(tooltipKey)) {
                        hoverTooltip.hide();
                    }
                });
            }

            @Override
            public boolean clicked() {
                boolean skipConfirm = ctrlHeld();
                queue(() -> clicked.accept(skipConfirm));
                return true;
            }

            @Override
            public boolean rightClicked() {
                boolean skipConfirm = ctrlHeld();
                queue(() -> rightClicked.accept(skipConfirm));
                return true;
            }
        };
    }

    private static void addCount(TooltipMakerAPI element, String itemId, float tileLeft, float tileTop, float tileSize,
                                 CraftingMaterials materials) {
        int ownedCount = materials.count(itemId);
        Color countColor = ownedCount > 0 ? Misc.getBrightPlayerColor() : Misc.getGrayColor();
        element.setParaFont(Fonts.ORBITRON_12);
        LabelAPI countLabel = VanillaText.plainPara(element, String.valueOf(ownedCount), 0f, countColor);
        countLabel.setAlignment(Alignment.RMID);
        countLabel.autoSizeToWidth(tileSize - COUNT_INSET * 2f).inTL(tileLeft + COUNT_INSET,
                tileTop + tileSize - countLabel.getPosition().getHeight() - COUNT_INSET);
        element.setParaFontDefault();
    }

    private void addTile(TooltipMakerAPI element, ItemCurrency currency, float tileLeft, float tileTop, float tileSize,
                         CraftingMaterials materials) {
        boolean owned = materials.count(currency.itemId()) > 0;
        CustomPanelAPI tilePanel = Global.getSettings().createCustom(tileSize, tileSize, new WorkbenchTile(recipeIcons(currency), recipeLook(currency),
                () -> owned, false, tileHandler(currency, anchor -> showTooltip(currency, anchor), skipConfirm -> tileClicked(currency, skipConfirm), skipConfirm -> tileRightClicked(currency, skipConfirm))));
        element.addCustom(tilePanel, 0f).getPosition().inTL(tileLeft, tileTop);
        addCount(element, currency.itemId(), tileLeft, tileTop, tileSize, materials);
    }

    private void showTooltip(ItemCurrency currency, PositionAPI anchor) {
        if (isClosing()) {
            hoverTooltip.hide();
            return;
        }
        hoverTooltip.show(currency, TOOLTIP_WIDTH, (element, expanded) -> writeTooltip(element, currency), anchor,
                ItemHoverTooltip.Placement.BELOW, PAD);
    }

    private void writeTooltip(TooltipMakerAPI element, ItemCurrency currency) {
        element.setParaFont(Fonts.ORBITRON_20AA);
        VanillaText.plainPara(element, recipeName(currency), 0f, Misc.getBrightPlayerColor());
        element.setParaFontDefault();
        VanillaText.plainPara(element, Translation.text(modeDescriptionKey(currency)), GAP, Misc.getTextColor());
        for (String line : tooltipHints(currency)) {
            VanillaText.plainPara(element, line, GAP, Misc.getGrayColor());
        }
    }

    private List<String> tooltipHints(ItemCurrency currency) {
        String synthesiseHint = currency.synthesisable()
                ? Translation.msg("ui.workbench.tile.synthesise").arg("cost", currency.partsCost()).text()
                : Translation.text("ui.workbench.tile.notSynthesisable");
        List<String> hints = new ArrayList<>();
        int ownedCount = playerMaterials().count(currency.itemId());
        hints.add(Translation.msg("ui.workbench.tile.inStorage").arg("count", ownedCount).text());
        if (ownedCount == 0) {
            hints.add(Translation.text(currency.synthesisable() ? "ui.workbench.tile.noneOwned" : "ui.workbench.tile.noneFound"));
        } else if (domainSupport() != null) {
            BenchModeSupport.Refusal refusal = modeSupport().refusal(currency, loadedItem, modeStore(), playerMaterials(), allowedUniques(),
                    activeWards);
            hints.add(refusal.allowed()
                    ? Translation.msg(modeSupport().useHintKey()).arg("target", loadedItem.name()).text() : refusalText(currency, refusal.noticeKey()));
        } else if (loadedItem != null && ItemCrafting.canUse(currency, loadedItem, allowedUniques(), activeWards)) {
            hints.add(Translation.msg("ui.workbench.tile.use").arg("target", loadedItem.name()).text());
        } else if (loadedItem != null && ItemCrafting.canUse(currency, loadedItem, allowedUniques())) {
            hints.add(Translation.text("ui.workbench.tile.wardBlocks"));
        } else {
            hints.add(Translation.text("ui.workbench.tile.noTarget"));
        }
        hints.add(synthesiseHint);
        return hints;
    }

    private static List<String> recipeIcons(ItemCurrency currency) {
        String iconPath = MaterialItems.loadedIconPath(currency.itemId());
        return List.of(iconPath == null ? FALLBACK_ICON : iconPath);
    }

    static String descriptionKey(ItemCurrency currency) {
        return MaterialItems.descriptionKey(currency.itemId());
    }

    private String modeDescriptionKey(ItemCurrency currency) {
        return domainSupport() != null ? modeSupport().currencyDescriptionKey(currency) : descriptionKey(currency);
    }

    static String usedNoticeKey(CraftingCurrency currency, CorruptionOutcome outcome, ItemNature nature) {
        String baseKey = currency == CraftingCurrency.CORRUPTION && outcome != null
                ? "ui.workbench.notice.corrupted." + outcome.name().toLowerCase(Locale.ROOT)
                : "ui.workbench.notice.used." + currency.name().toLowerCase(Locale.ROOT);
        return baseKey + "." + nature.name().toLowerCase(Locale.ROOT);
    }

    private static CargoIconLook recipeLook(ItemCurrency currency) {
        return CargoIconLooks.forMaterial(currency.itemId());
    }

    private static String recipeName(ItemCurrency currency) {
        return MaterialItems.displayName(currency.itemId());
    }

    private void rebuildDetails() {
        loadedLook = CargoIconLooks.forItem(loadedItem);
        detailsShowRollRanges = TooltipExpansion.isExpandedOrHeld();
        float detailsWidth = BenchScene.DETAILS_WIDTH * sceneScale;
        float detailsHeight = detailsArtHeight() * sceneScale;
        TooltipMakerAPI element = panelRoot.createUIElement(detailsWidth, detailsHeight, true);
        if (loadedItem == null) {
            String emptyKey = domainSupport() != null ? modeSupport().emptyKey() : "ui.workbench.empty";
            centred(VanillaText.plainPara(element, Translation.text(emptyKey), 0f, Misc.getGrayColor()));
        } else {
            ItemName displayName = loadedItem.displayName();
            element.setParaFont(Fonts.ORBITRON_20AA);
            centred(VanillaText.plainPara(element, displayName.title(), 0f, displayName.rarity().color()));
            element.setParaFontDefault();
            if (displayName.baseName() != null) {
                centred(VanillaText.plainPara(element, displayName.baseName(), LINE_PAD, Misc.getGrayColor()));
            }
            if (domainSupport() != null) {
                for (StyledText line : loadedItem.headerLines()) {
                    centred(VanillaText.addPara(element, line, LINE_PAD, Misc.getGrayColor()));
                }
            } else {
                if (loadedItem.mirrored()) {
                    centred(VanillaText.addPara(element, Translation.styled("socketable.tooltip.mirrored"), LINE_PAD, Misc.getGrayColor()));
                }
                if (loadedItem.corrupted()) {
                    centred(VanillaText.addPara(element, Translation.styled("socketable.tooltip.corrupted"), LINE_PAD, Misc.getGrayColor()));
                }
            }
            float paraPad = GAP * 2f;
            float linePad = paraPad;
            for (StyledText line : BenchModes.detailLines(loadedItem)) {
                centred(VanillaText.addPara(element, line, linePad, Misc.getTextColor()));
                linePad = LINE_PAD;
            }
            addDetailSection(element, "hullItem.tooltip.implicit", loadedItem.implicitLines(detailsShowRollRanges), paraPad);
            if (detailsShowRollRanges) {
                addSlottedDetailSection(element, "socketable.tooltip.primaryData", loadedItem.slottedEffectLines(true, null), paraPad,
                        detailsWidth);
            } else {
                addDetailSection(element, "socketable.tooltip.primaryData", loadedItem.effectLines(false), paraPad);
            }
        }
        detailsElement = placedComponent(replaceElement(detailsElement, element, artX(BenchScene.detailsLeft(benchMode)), artY(BenchScene.detailsTop(benchMode))));
    }

    private static void addDetailSection(TooltipMakerAPI element, String headingKey, List<StyledText> lines, float headingPad) {
        if (lines.isEmpty()) {
            return;
        }
        element.addSectionHeading(Translation.text(headingKey), Alignment.MID, headingPad);
        float linePad = GAP;
        for (StyledText line : lines) {
            VanillaText.addPara(element, line, linePad, Misc.getTextColor());
            linePad = LINE_PAD;
        }
    }

    private static void addSlottedDetailSection(TooltipMakerAPI element, String headingKey, List<SlottedLine> lines, float headingPad,
                                                float sectionWidth) {
        if (lines.isEmpty()) {
            return;
        }
        element.addSectionHeading(Translation.text(headingKey), Alignment.MID, headingPad);
        float textWidth = sectionWidth - SCROLLBAR_ROOM - SLOT_TAG_WIDTH - GAP;
        float linePad = GAP;
        for (SlottedLine line : lines) {
            TooltipMakerAPI textCell = element.beginSubTooltip(textWidth);
            VanillaText.addPara(textCell, line.text(), 0f, Misc.getTextColor());
            element.endSubTooltip();
            element.addCustom(textCell, linePad);
            linePad = LINE_PAD;
            if (line.slot() == null) {
                continue;
            }
            TooltipMakerAPI slotCell = element.beginSubTooltip(SLOT_TAG_WIDTH);
            VanillaText.plainPara(slotCell, Translation.text(slotTagKey(line.slot())), 0f, Misc.getGrayColor()).setAlignment(Alignment.RMID);
            element.endSubTooltip();
            element.addCustomDoNotSetPosition(slotCell).getPosition().rightOfTop(textCell, GAP);
        }
    }

    static String slotTagKey(ModifierSlot slot) {
        return slot == ModifierSlot.PREFIX ? "ui.workbench.slotTag.prefix" : "ui.workbench.slotTag.suffix";
    }

    private static void centred(LabelAPI label) {
        if (label != null) {
            label.setAlignment(Alignment.MID);
        }
    }

    private void rebuildNotice() {
        TooltipMakerAPI element = panelRoot.createUIElement(innerWidth(), BenchScene.NOTICE_HEIGHT * sceneScale, false);
        if (noticeText != null) {
            Color noticeColor = noticeIsWarning ? Misc.getNegativeHighlightColor() : Misc.getPositiveHighlightColor();
            centred(VanillaText.plainPara(element, noticeText, 0f, noticeColor));
        }
        noticeElement = replaceElement(noticeElement, element, PAD, artY(BenchScene.NOTICE_TOP));
    }

}
