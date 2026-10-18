package exiledsector.ui.socket;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.items.MaterialItems;
import exiledsector.items.CraftingCurrency;
import exiledsector.items.CraftingWard;
import exiledsector.items.CorruptionOutcome;
import exiledsector.items.SocketCustody;
import exiledsector.items.CraftingMaterials;
import exiledsector.items.Item;
import exiledsector.items.ItemCrafting;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemNature;
import exiledsector.items.ItemKind;
import exiledsector.items.ItemName;
import exiledsector.items.ItemRarity;
import exiledsector.items.ItemStore;
import exiledsector.items.ItemUnlock;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.SkillTreeSounds;
import exiledsector.ui.VanillaText;
import exiledsector.ui.cargo.CargoIconLook;
import exiledsector.ui.cargo.CargoIconLooks;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.HoloShapes;

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
    }

    public static final float WIDTH = 520f;
    private static final float BUTTON_HEIGHT = 24f;
    private static final float MIN_TILE_SIZE = 52f;
    private static final float MAX_TILE_SIZE = 72f;
    private static final float TILE_SHARE_OF_RING = 0.17f;
    private static final float TILE_EDGE_MARGIN = 2f;
    private static final float SLOT_CLEARANCE = 10f;
    private static final float COUNT_INSET = 4f;
    private static final float NOTICE_HEIGHT = 36f;
    private static final float MIN_RING_SIZE = 280f;
    private static final float MAX_RING_SIZE = 420f;
    private static final float MIN_DETAILS_HEIGHT = 140f;
    private static final float ORBIT_ALPHA = 0.18f;
    private static final float WARD_TILE_SIZE = 52f;
    private static final float TOOLTIP_WIDTH = 340f;
    private static final String FALLBACK_ICON = ItemDefinition.FALLBACK_ICON;

    private enum Control {CLOSE}

    private final Listener workbenchListener;
    private final SocketWorkbenchFlair socketFlair = new SocketWorkbenchFlair();
    private final Random craftingRandom = new Random();
    private float ringSize;
    private UIComponentAPI headerElement;
    private UIComponentAPI recipesElement;
    private UIComponentAPI detailsElement;
    private UIComponentAPI noticeElement;
    private Item loadedItem;
    private CargoIconLook loadedLook;
    private final Set<CraftingWard> activeWards = EnumSet.noneOf(CraftingWard.class);
    private String noticeText;

    private SocketWorkbenchPanel(CustomPanelAPI hostPanel, Listener workbenchListener) {
        super(hostPanel);
        this.workbenchListener = workbenchListener;
    }

    public static SocketWorkbenchPanel open(CustomPanelAPI hostPanel, float panelLeft, float panelTop, float panelHeight,
                                            Listener workbenchListener) {
        SocketWorkbenchPanel workbenchPanel = new SocketWorkbenchPanel(hostPanel, workbenchListener);
        workbenchPanel.ringSize = ringSizeFor(panelHeight);
        workbenchPanel.attach(panelLeft, panelTop, WIDTH, panelHeight, false);
        return workbenchPanel;
    }

    private static float ringSizeFor(float panelHeight) {
        float freeHeight = panelHeight - ringTop() - GAP - bottomReserve() - MIN_DETAILS_HEIGHT;
        return Math.max(MIN_RING_SIZE, Math.min(Math.min(MAX_RING_SIZE, WIDTH - PAD * 2f), freeHeight));
    }

    private static float ringTop() {
        return PAD + HEADER_HEIGHT + GAP;
    }

    private float tileSize() {
        return Math.max(MIN_TILE_SIZE, Math.min(MAX_TILE_SIZE, ringSize * TILE_SHARE_OF_RING));
    }

    private float tileOrbit() {
        return ringSize / 2f - tileSize() / 2f - TILE_EDGE_MARGIN;
    }

    private float slotRadius() {
        return tileOrbit() - tileSize() / 2f - SLOT_CLEARANCE;
    }

    static float tileAngle(int index, int tileCount) {
        return (float) (Math.PI / 2.0 - index * 2.0 * Math.PI / tileCount);
    }

    private static float bottomReserve() {
        return GAP + NOTICE_HEIGHT + PAD;
    }

    public Item loaded() {
        return loadedItem;
    }

    public void load(Item item) {
        queue(() -> {
            if (item == loadedItem) {
                return;
            }
            loadedItem = item;
            noticeText = null;
            socketFlair.restartLoad();
            rebuildContent();
        });
    }

    public void refresh() {
        queue(() -> {
            if (loadedItem != null && !ItemStore.get().owned().contains(loadedItem)) {
                loadedItem = null;
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
        return new UIComponentAPI[]{recipesElement, detailsElement, noticeElement};
    }

    @Override
    public void renderBelow(float alphaMult) {
        super.renderBelow(alphaMult);
        if (panelPosition == null) {
            return;
        }
        float contentAlpha = contentAlpha() * alphaMult;
        float cx = panelPosition.getX() + panelWidth / 2f;
        float cy = panelPosition.getY() + panelHeight - ringTop() - ringSize / 2f;
        Color subjectColor = loadedItem == null ? null : loadedItem.rarity().color();
        try (GlScope scope = GlScope.flat()) {
            HoloShapes.circle(cx, cy, tileOrbit(), SkillTreePanelStyle.GLOW_COLOR, 1f, ORBIT_ALPHA * contentAlpha);
        }
        socketFlair.render(cx, cy, slotRadius(), SkillTreePanelStyle.GLOW_COLOR, subjectColor,
                loadedItem == null ? null : loadedItem.iconPath(), loadedLook, contentAlpha);
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
        if (id == Control.CLOSE) {
            close();
        }
    }

    private void tileClicked(CraftingCurrency currency) {
        hoverTooltip.hide();
        use(currency);
    }

    private void tileRightClicked(CraftingCurrency currency) {
        hoverTooltip.hide();
        synthesise(currency);
    }

    private boolean synthesise(CraftingCurrency currency) {
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
        if (!ItemCrafting.synthesise(currency, materials)) {
            return false;
        }
        noticeText = Translation.msg("ui.workbench.notice.synthesisedCurrency").arg("name", recipeName(currency)).text();
        SkillTreeSounds.crafted();
        socketFlair.flash(SkillTreePanelStyle.GLOW_COLOR);
        afterChange();
        return true;
    }

    private void use(CraftingCurrency currency) {
        CraftingMaterials materials = playerMaterials();
        if (materials.count(currency.itemId()) < 1) {
            String noneOwnedKey = currency.synthesisable() ? "ui.workbench.notice.noneOwned" : "ui.workbench.notice.noneFound";
            showNotice(Translation.msg(noneOwnedKey).arg("name", MaterialItems.displayName(currency.itemId())).text());
            return;
        }
        if (loadedItem == null) {
            showNotice(Translation.text("ui.workbench.notice.loadFirst"));
            return;
        }
        List<CraftingWard> missingWards = activeWards.stream().filter(ward -> materials.count(ward.itemId()) < 1).toList();
        if (!missingWards.isEmpty()) {
            missingWards.forEach(activeWards::remove);
            showNotice(Translation.msg("ui.workbench.notice.wardMissing").arg("name", MaterialItems.displayName(missingWards.get(0).itemId())).text());
            return;
        }
        if (!ItemCrafting.canUse(currency, loadedItem, allowedUniques(), activeWards)) {
            if (ItemCrafting.canUse(currency, loadedItem, allowedUniques())) {
                String wardKey = currency == CraftingCurrency.TRANSPOSITION
                        ? "ui.workbench.notice.wardBlocksTransposition" : "ui.workbench.notice.wardBlocks";
                showNotice(Translation.msg(wardKey).arg("name", MaterialItems.displayName(currency.itemId())).text());
                return;
            }
            if (loadedItem.mirrored()) {
                showNotice(Translation.msg("ui.workbench.notice.mirrored").arg("name", loadedItem.name()).text());
                return;
            }
            if (loadedItem.corrupted()) {
                showNotice(Translation.msg("ui.workbench.notice.corruptedItem").arg("name", loadedItem.name()).text());
                return;
            }
            if (currency == CraftingCurrency.FIXATION && loadedItem.hasLockedModifier()) {
                showNotice(Translation.msg("ui.workbench.notice.alreadyFixed").arg("name", loadedItem.name()).text());
                return;
            }
            if (currency == CraftingCurrency.TRANSPOSITION && loadedItem.rarity() == ItemRarity.UNIQUE) {
                showNotice(Translation.text("ui.workbench.notice.noOtherUnique"));
                return;
            }
            showNotice(Translation.msg("ui.workbench.notice.cannotUse").arg("name", MaterialItems.displayName(currency.itemId()))
                    .arg("target", loadedItem.name()).text());
            return;
        }
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
        String usedKey = usedNoticeKey(currency, result.corruption(), ItemNature.of(result.kind()));
        noticeText = Translation.msg(usedKey)
                .arg("before", nameBefore).arg("name", result.name()).text();
        SocketCustody.markHostsChanged(result);
        if ((currency == CraftingCurrency.TRANSPOSITION || currency == CraftingCurrency.CORRUPTION) && SocketCustody.unsocketIfMisfit(result)) {
            String movedNotice = Translation.msg("ui.workbench.notice.movedToStorage").arg("name", result.name())
                    .arg("type", result.kind().displayName()).text();
            noticeText = Translation.msg("format.sentences").arg("a", noticeText).arg("b", movedNotice).text();
        }
        for (CraftingWard ward : spentWards) {
            String spentNotice = Translation.msg("ui.workbench.notice.wardSpent").arg("name", MaterialItems.displayName(ward.itemId())).text();
            noticeText = Translation.msg("format.sentences").arg("a", noticeText).arg("b", spentNotice).text();
        }
        SkillTreeSounds.socketed();
        socketFlair.flash(result.rarity().color());
        afterChange();
    }

    private void showNotice(String text) {
        noticeText = text;
        rebuildNotice();
        refreshOpacity();
    }

    private void afterChange() {
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
        TooltipMakerAPI element = startHeader("ui.workbench.title", "ui.workbench.close", Control.CLOSE);
        addPartsCount(element, CLOSE_BUTTON_WIDTH + GAP * 2f);
        headerElement = finishHeader(element, headerElement);
    }

    private void rebuildContent() {
        rebuildTiles();
        rebuildDetails();
        rebuildNotice();
        refreshOpacity();
    }

    private void rebuildTiles() {
        hoverTooltip.hide();
        TooltipMakerAPI element = panelRoot.createUIElement(innerWidth(), ringSize, false);
        CraftingMaterials materials = playerMaterials();
        CraftingCurrency[] allCurrencies = CraftingCurrency.values();
        float tileSize = tileSize();
        float orbit = tileOrbit();
        for (int i = 0; i < allCurrencies.length; i++) {
            float angle = tileAngle(i, allCurrencies.length);
            float tileCentreX = innerWidth() / 2f + orbit * (float) Math.cos(angle);
            float tileCentreY = ringSize / 2f - orbit * (float) Math.sin(angle);
            addTile(element, allCurrencies[i], tileCentreX - tileSize / 2f, tileCentreY - tileSize / 2f, tileSize, materials);
        }
        CraftingWard[] allWards = CraftingWard.values();
        for (int i = 0; i < allWards.length; i++) {
            float wardLeft = i == 0 ? 0f : innerWidth() - WARD_TILE_SIZE;
            addWardTile(element, allWards[i], wardLeft, ringSize - WARD_TILE_SIZE, materials);
        }
        recipesElement = replaceElement(recipesElement, element, PAD, ringTop());
    }

    private void addWardTile(TooltipMakerAPI element, CraftingWard ward, float tileLeft, float tileTop, CraftingMaterials materials) {
        String iconPath = MaterialItems.loadedIconPath(ward.itemId());
        WorkbenchTile wardTile = new WorkbenchTile(List.of(iconPath == null ? FALLBACK_ICON : iconPath), CargoIconLooks.forMaterial(ward.itemId()),
                () -> activeWards.contains(ward), tileHandler(ward, anchor -> showWardTooltip(ward, anchor), () -> toggleWard(ward),
                () -> showNotice(Translation.msg("ui.workbench.notice.notSynthesisable").arg("name", MaterialItems.displayName(ward.itemId()))
                        .text())));
        CustomPanelAPI tilePanel = Global.getSettings().createCustom(WARD_TILE_SIZE, WARD_TILE_SIZE, wardTile);
        element.addCustom(tilePanel, 0f).getPosition().inTL(tileLeft, tileTop);
        addCount(element, ward.itemId(), tileLeft, tileTop, WARD_TILE_SIZE, materials);
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
        return "ui.workbench.ward." + ward.name().toLowerCase(Locale.ROOT) + ".description";
    }

    private TileInput.Handler tileHandler(Object tooltipKey, Consumer<PositionAPI> showTooltip, Runnable clicked,
                                          Runnable rightClicked) {
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
                queue(clicked);
                return true;
            }

            @Override
            public boolean rightClicked() {
                queue(rightClicked);
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

    private void addTile(TooltipMakerAPI element, CraftingCurrency currency, float tileLeft, float tileTop, float tileSize,
                         CraftingMaterials materials) {
        CustomPanelAPI tilePanel = Global.getSettings().createCustom(tileSize, tileSize, new WorkbenchTile(recipeIcons(currency), recipeLook(currency),
                tileHandler(currency, anchor -> showTooltip(currency, anchor), () -> tileClicked(currency), () -> tileRightClicked(currency))));
        element.addCustom(tilePanel, 0f).getPosition().inTL(tileLeft, tileTop);
        addCount(element, currency.itemId(), tileLeft, tileTop, tileSize, materials);
    }

    private void showTooltip(CraftingCurrency currency, PositionAPI anchor) {
        if (isClosing()) {
            hoverTooltip.hide();
            return;
        }
        hoverTooltip.show(currency, TOOLTIP_WIDTH, (element, expanded) -> writeTooltip(element, currency), anchor,
                ItemHoverTooltip.Placement.BELOW, PAD);
    }

    private void writeTooltip(TooltipMakerAPI element, CraftingCurrency currency) {
        element.setParaFont(Fonts.ORBITRON_20AA);
        VanillaText.plainPara(element, recipeName(currency), 0f, Misc.getBrightPlayerColor());
        element.setParaFontDefault();
        VanillaText.plainPara(element, Translation.text(descriptionKey(currency)), GAP, Misc.getTextColor());
        for (String line : tooltipHints(currency)) {
            VanillaText.plainPara(element, line, GAP, Misc.getGrayColor());
        }
    }

    private List<String> tooltipHints(CraftingCurrency currency) {
        String synthesiseHint = currency.synthesisable()
                ? Translation.msg("ui.workbench.tile.synthesise").arg("cost", currency.partsCost()).text()
                : Translation.text("ui.workbench.tile.notSynthesisable");
        List<String> hints = new ArrayList<>();
        int ownedCount = playerMaterials().count(currency.itemId());
        hints.add(Translation.msg("ui.workbench.tile.inStorage").arg("count", ownedCount).text());
        if (ownedCount == 0) {
            hints.add(Translation.text(currency.synthesisable() ? "ui.workbench.tile.noneOwned" : "ui.workbench.tile.noneFound"));
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

    private static List<String> recipeIcons(CraftingCurrency currency) {
        String iconPath = MaterialItems.loadedIconPath(currency.itemId());
        return List.of(iconPath == null ? FALLBACK_ICON : iconPath);
    }

    static String descriptionKey(CraftingCurrency currency) {
        return "ui.workbench.recipe." + currency.name().toLowerCase(Locale.ROOT) + ".description";
    }

    static String usedNoticeKey(CraftingCurrency currency, CorruptionOutcome outcome, ItemNature nature) {
        String baseKey = currency == CraftingCurrency.CORRUPTION && outcome != null
                ? "ui.workbench.notice.corrupted." + outcome.name().toLowerCase(Locale.ROOT)
                : "ui.workbench.notice.used." + currency.name().toLowerCase(Locale.ROOT);
        return baseKey + "." + nature.name().toLowerCase(Locale.ROOT);
    }

    private static CargoIconLook recipeLook(CraftingCurrency currency) {
        return CargoIconLooks.forMaterial(currency.itemId());
    }

    private static String recipeName(CraftingCurrency currency) {
        return MaterialItems.displayName(currency.itemId());
    }

    private void rebuildDetails() {
        loadedLook = CargoIconLooks.forItem(loadedItem);
        float detailsTop = ringTop() + ringSize + GAP;
        float detailsHeight = Math.max(BUTTON_HEIGHT, panelHeight - detailsTop - bottomReserve());
        TooltipMakerAPI element = panelRoot.createUIElement(innerWidth(), detailsHeight, true);
        if (loadedItem == null) {
            centred(VanillaText.plainPara(element, Translation.text("ui.workbench.empty"), 0f, Misc.getGrayColor()));
        } else {
            ItemName displayName = loadedItem.displayName();
            element.setParaFont(Fonts.ORBITRON_20AA);
            centred(VanillaText.plainPara(element, displayName.title(), 0f, displayName.rarity().color()));
            element.setParaFontDefault();
            if (displayName.baseName() != null) {
                centred(VanillaText.plainPara(element, displayName.baseName(), LINE_PAD, Misc.getGrayColor()));
            }
            if (loadedItem.mirrored()) {
                centred(VanillaText.addPara(element, Translation.styled("socketable.tooltip.mirrored"), LINE_PAD, Misc.getGrayColor()));
            }
            if (loadedItem.corrupted()) {
                centred(VanillaText.addPara(element, Translation.styled("socketable.tooltip.corrupted"), LINE_PAD, Misc.getGrayColor()));
            }
            float paraPad = GAP * 2f;
            List<StyledText> implicitLines = loadedItem.implicitLines(true);
            for (StyledText line : implicitLines) {
                centred(VanillaText.addPara(element, line, paraPad, Misc.getTextColor()));
                paraPad = LINE_PAD;
            }
            if (!implicitLines.isEmpty()) {
                paraPad = GAP * 2f;
            }
            for (StyledText line : loadedItem.effectLines(true)) {
                centred(VanillaText.addPara(element, line, paraPad, Misc.getTextColor()));
                paraPad = LINE_PAD;
            }
        }
        detailsElement = placedComponent(replaceElement(detailsElement, element, PAD, detailsTop));
    }

    private static void centred(LabelAPI label) {
        if (label != null) {
            label.setAlignment(Alignment.MID);
        }
    }

    private void rebuildNotice() {
        TooltipMakerAPI element = panelRoot.createUIElement(innerWidth(), NOTICE_HEIGHT, false);
        if (noticeText != null) {
            centred(VanillaText.plainPara(element, noticeText, 0f, Misc.getPositiveHighlightColor()));
        }
        noticeElement = replaceElement(noticeElement, element, PAD, panelHeight - PAD - NOTICE_HEIGHT);
    }

    private float innerWidth() {
        return panelWidth - PAD * 2f;
    }

}
