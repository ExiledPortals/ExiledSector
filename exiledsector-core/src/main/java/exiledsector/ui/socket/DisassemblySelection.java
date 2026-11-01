package exiledsector.ui.socket;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.items.Item;
import exiledsector.items.ItemRarity;
import exiledsector.ui.VanillaText;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

public final class DisassemblySelection {

    public record Texts(String hintKey, String batchTitleKey, String disassembledManyKey, boolean warnsAboutUniques) {
    }

    static final Texts ITEM_TEXTS = new Texts("ui.socketStorage.disassemblyHint", "ui.socketStorage.confirm.batchTitle",
            "ui.socketStorage.disassembledMany", true);
    private static final float CONFIRM_HEIGHT = 130f;
    private static final float BATCH_CONFIRM_HEIGHT = 170f;
    private static final float BUTTON_TEXT_PADDING = 30f;

    private final Texts texts;
    private final Set<Item> marked = new LinkedHashSet<>();
    private List<Item> pending = List.of();
    private boolean active;

    public DisassemblySelection(Texts texts) {
        this.texts = texts;
    }

    public boolean active() {
        return active;
    }

    public void enter() {
        active = true;
    }

    public void leave() {
        active = false;
        marked.clear();
    }

    public void toggle(Item item) {
        if (!marked.remove(item)) {
            marked.add(item);
        }
    }

    public boolean isMarked(Item item) {
        return marked.contains(item);
    }

    public boolean nothingMarked() {
        return marked.isEmpty();
    }

    public List<Item> marked() {
        return List.copyOf(marked);
    }

    public void forget(Predicate<Item> gone) {
        marked.removeIf(gone);
    }

    public void forget(Collection<Item> gone) {
        marked.removeAll(gone);
    }

    public List<Item> pending() {
        return pending;
    }

    public void confirming(List<Item> targets) {
        pending = List.copyOf(targets);
    }

    public void confirmClosed() {
        pending = List.of();
    }

    public float confirmHeight(List<Item> targets) {
        return targets.size() == 1 ? CONFIRM_HEIGHT : BATCH_CONFIRM_HEIGHT;
    }

    public void writeConfirm(TooltipMakerAPI element, List<Item> targets) {
        int totalParts = partsOf(targets);
        if (targets.size() == 1) {
            Item item = targets.get(0);
            VanillaText.plainPara(element, Translation.msg("ui.socketStorage.confirm.title").arg("name", item.name()).text(), 0f,
                    item.rarity().color());
            VanillaText.plainPara(element, Translation.msg("ui.socketStorage.confirm.body").count(totalParts).arg("count", totalParts).text(),
                    HoloPanel.LINE_PAD * 2f, Misc.getTextColor());
            return;
        }
        VanillaText.plainPara(element, Translation.msg(texts.batchTitleKey()).arg("count", targets.size()).text(), 0f, Misc.getBasePlayerColor());
        addRarityBreakdown(element, targets);
        VanillaText.plainPara(element, Translation.msg("ui.socketStorage.confirm.batchBody").arg("count", totalParts).text(), HoloPanel.LINE_PAD * 2f,
                Misc.getTextColor());
        if (texts.warnsAboutUniques() && targets.stream().anyMatch(item -> item.rarity() == ItemRarity.UNIQUE)) {
            VanillaText.plainPara(element, Translation.text("ui.socketStorage.confirm.includesUnique"), HoloPanel.LINE_PAD * 2f,
                    Misc.getNegativeHighlightColor());
        }
    }

    private static void addRarityBreakdown(TooltipMakerAPI element, List<Item> targets) {
        List<String> rarityCounts = new ArrayList<>();
        List<Color> rarityColors = new ArrayList<>();
        for (ItemRarity rarity : ItemRarity.values()) {
            long count = targets.stream().filter(item -> item.rarity() == rarity).count();
            if (count > 0) {
                rarityCounts.add(Translation.msg("ui.socketStorage.confirm.rarityCount").arg("count", count)
                        .arg("rarity", Translation.text("ui.socketStorage.rarity." + rarity.name().toLowerCase(Locale.ROOT))).text());
                rarityColors.add(rarity.color());
            }
        }
        String format = String.join(VanillaText.escapeFormat(Translation.text("ui.socketStorage.confirm.raritySeparator")),
                rarityCounts.stream().map(text -> "%s").toList());
        element.addPara(format, HoloPanel.LINE_PAD * 2f, rarityColors.toArray(new Color[0]), rarityCounts.toArray(new String[0]));
    }

    public String hint() {
        if (marked.isEmpty()) {
            return Translation.text(texts.hintKey());
        }
        return Translation.msg("ui.socketStorage.disassemblyMarked").arg("count", marked.size()).arg("parts", partsOf(marked)).text();
    }

    public float addModeButton(TooltipMakerAPI element, Object buttonId, float buttonHeight, boolean enabled) {
        String modeKey = !active ? "ui.socketStorage.disassemblyMode"
                : marked.isEmpty() ? "ui.socketStorage.exitDisassemblyMode" : "ui.socketStorage.confirmDisassembly";
        String modeLabel = Translation.text(modeKey);
        float modeWidth = Global.getSettings().computeStringWidth(modeLabel, Fonts.ORBITRON_20AA) + BUTTON_TEXT_PADDING;
        ButtonAPI modeButton = element.addButton(modeLabel, buttonId, modeWidth, buttonHeight, 0f);
        modeButton.getPosition().inTR(0f, 0f);
        modeButton.setEnabled(active || enabled);
        return modeWidth;
    }

    public StyledText cellLine(Item item) {
        int partCount = item.rarity().disassemblyParts();
        String key = marked.contains(item) ? "ui.socketStorage.cell.unmark" : "ui.socketStorage.cell.mark";
        return Translation.msg(key).count(partCount).arg("count", partCount).styled();
    }

    public static StyledText disassembleHint(Item item) {
        int partCount = item.rarity().disassemblyParts();
        return Translation.msg("ui.socketStorage.cell.disassemble").count(partCount).arg("count", partCount).styled();
    }

    public String disassemble(List<Item> targets, ToIntFunction<Item> disassembler) {
        int totalParts = 0;
        int disassembledCount = 0;
        Item lastDisassembled = null;
        for (Item item : targets) {
            int gainedParts = disassembler.applyAsInt(item);
            if (gainedParts > 0) {
                totalParts += gainedParts;
                disassembledCount++;
                lastDisassembled = item;
            }
        }
        marked.removeAll(targets);
        if (disassembledCount == 1) {
            return Translation.msg("ui.socketStorage.disassembled").count(totalParts).arg("name", lastDisassembled.name()).arg("count", totalParts)
                    .text();
        }
        return disassembledCount > 1
                ? Translation.msg(texts.disassembledManyKey()).arg("count", disassembledCount).arg("parts", totalParts).text() : null;
    }

    private static int partsOf(Collection<Item> items) {
        return items.stream().mapToInt(item -> item.rarity().disassemblyParts()).sum();
    }
}
