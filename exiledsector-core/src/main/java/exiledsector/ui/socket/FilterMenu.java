package exiledsector.ui.socket;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.Translation;
import exiledsector.items.ItemKind;
import exiledsector.items.ItemRarity;
import exiledsector.ui.VanillaText;
import exiledsector.ui.util.FramedPanelPlugin;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

final class FilterMenu {

    record Option(String label, Object optionId, ChipFlow.Colors colors, boolean checked, String tooltip) {
    }

    record Anchor(float left, float top, float minWidth) {
    }

    private static final float TOOLTIP_WIDTH = 320f;

    private final Consumer<Object> buttonListener;
    private final Runnable outsidePressed;
    private CustomPanelAPI hostRoot;
    private CustomPanelAPI blocker;
    private CustomPanelAPI menuPanel;
    private Object openMenuId;

    FilterMenu(Consumer<Object> buttonListener, Runnable outsidePressed) {
        this.buttonListener = buttonListener;
        this.outsidePressed = outsidePressed;
    }

    boolean isOpen() {
        return openMenuId != null;
    }

    boolean isOpen(Object menuId) {
        return openMenuId != null && openMenuId.equals(menuId);
    }

    Object openMenuId() {
        return openMenuId;
    }

    void open(CustomPanelAPI root, float rootWidth, float rootHeight, Object menuId, Anchor anchor, List<Option> options) {
        close();
        hostRoot = root;
        openMenuId = menuId;
        blocker = Global.getSettings().createCustom(rootWidth, rootHeight, new InputBlocker(outsidePressed));
        float menuWidth = Math.min(rootWidth - HoloPanel.PAD * 2f, Math.max(anchor.minWidth(), widestOption(options) + HoloPanel.GAP * 2f));
        float optionWidth = menuWidth - HoloPanel.GAP * 2f;
        float menuHeight = options.size() * ChipFlow.CHIP_HEIGHT + (options.size() - 1) * HoloPanel.LINE_PAD + HoloPanel.GAP * 2f;
        menuPanel = Global.getSettings().createCustom(menuWidth, menuHeight, new FramedPanelPlugin(buttonListener));
        TooltipMakerAPI element = menuPanel.createUIElement(optionWidth, menuHeight - HoloPanel.GAP * 2f, false);
        float optionTop = 0f;
        for (Option option : options) {
            ChipFlow.Colors colors = option.colors();
            ButtonAPI optionButton = element.addAreaCheckbox(option.label(), option.optionId(), colors.base(), colors.dark(), colors.bright(),
                    optionWidth, ChipFlow.CHIP_HEIGHT, 0f);
            optionButton.setChecked(option.checked());
            optionButton.getPosition().inTL(0f, optionTop);
            if (option.tooltip() != null) {
                element.addTooltipToPrevious(new GameTextTooltip(TOOLTIP_WIDTH, tooltip -> VanillaText.plainPara(tooltip, option.tooltip(), 0f,
                        Misc.getTextColor())), TooltipMakerAPI.TooltipLocation.BELOW);
            }
            optionTop += ChipFlow.CHIP_HEIGHT + HoloPanel.LINE_PAD;
        }
        menuPanel.addUIElement(element).inTL(HoloPanel.GAP, HoloPanel.GAP);
        float menuLeft = Math.max(HoloPanel.PAD, Math.min(anchor.left(), rootWidth - HoloPanel.PAD - menuWidth));
        root.addComponent(blocker).inTL(0f, 0f);
        root.addComponent(menuPanel).inTL(menuLeft, anchor.top());
    }

    static String buttonLabel(String menuNameKey, List<String> chosenLabels, float buttonWidth) {
        String menuName = Translation.text(menuNameKey);
        String choice = chosenLabels.isEmpty() ? Translation.text("ui.socketStorage.menu.any")
                : String.join(Translation.text("ui.socketStorage.menu.separator"), chosenLabels);
        String label = Translation.msg("ui.socketStorage.menu.label").arg("menu", menuName).arg("choice", choice).text();
        if (chosenLabels.size() > 1 && ChipFlow.width(label) > buttonWidth) {
            String countChoice = Translation.msg("ui.socketStorage.menu.count").arg("count", chosenLabels.size()).text();
            label = Translation.msg("ui.socketStorage.menu.label").arg("menu", menuName).arg("choice", countChoice).text();
        }
        return label;
    }

    static <T> List<String> chosenLabels(List<T> options, Set<T> chosen, Function<T, String> labeller) {
        List<T> picked = options.stream().filter(chosen::contains).toList();
        return picked.size() == options.size() ? List.of() : picked.stream().map(labeller).toList();
    }

    static List<Option> rarityOptions(Set<ItemRarity> chosen, Function<ItemRarity, Object> optionId) {
        return Arrays.stream(ItemRarity.values()).map(rarity -> new Option(ItemStoragePanel.rarityLabel(rarity), optionId.apply(rarity),
                ChipFlow.Colors.of(rarity.color()), chosen.contains(rarity), null)).toList();
    }

    static List<Option> typeOptions(List<ItemKind> sectionKinds, Set<ItemKind> chosen, Function<ItemKind, Object> optionId) {
        return sectionKinds.stream().map(sectionKind -> new Option(sectionKind.displayName(), optionId.apply(sectionKind),
                ChipFlow.Colors.of(ItemStoragePanel.typeColor(sectionKind)), chosen.contains(sectionKind), null)).toList();
    }

    private static float widestOption(List<Option> options) {
        float widest = 0f;
        for (Option option : options) {
            widest = Math.max(widest, ChipFlow.width(option.label()));
        }
        return widest;
    }

    void raise() {
        if (!isOpen()) {
            return;
        }
        hostRoot.bringComponentToTop(blocker);
        hostRoot.bringComponentToTop(menuPanel);
    }

    void close() {
        if (!isOpen()) {
            return;
        }
        hostRoot.removeComponent(menuPanel);
        hostRoot.removeComponent(blocker);
        hostRoot = null;
        blocker = null;
        menuPanel = null;
        openMenuId = null;
    }

    void setOpacity(float opacity) {
        if (isOpen()) {
            blocker.setOpacity(opacity);
            menuPanel.setOpacity(opacity);
        }
    }

    UIComponentAPI panel() {
        return menuPanel;
    }
}
