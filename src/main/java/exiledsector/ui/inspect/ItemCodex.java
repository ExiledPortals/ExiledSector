package exiledsector.ui.inspect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.impl.codex.CodexDataV2;
import com.fs.starfarer.api.impl.codex.CodexEntryPlugin;
import com.fs.starfarer.api.impl.codex.CodexEntryV2;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.ExiledSectorModPlugin;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemUnlock;
import exiledsector.ui.VanillaText;
import exiledsector.ui.socket.ItemCodexLink;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ItemCodex {

    public static final String CATEGORY_ID = "exiledSector_socketables";
    static final String ENTRY_PREFIX = "exiledSector_socketable_";
    public static final ItemCodexLink.Codex LINK = new ItemCodexLink.Codex() {
        @Override
        public String existingEntryId(String definitionId) {
            return ItemCodex.existingEntryId(definitionId);
        }

        @Override
        public boolean open(String definitionId) {
            return ItemCodex.open(definitionId);
        }
    };

    private static volatile UnlockState unlockState = UnlockState.NONE;

    private record UnlockState(WeakReference<SectorAPI> sectorReference, Set<String> unlockedUniqueIds) {
        static final UnlockState NONE = new UnlockState(new WeakReference<>(null), Set.of());

        boolean isFor(SectorAPI sector) {
            return sector != null && sectorReference.get() == sector;
        }
    }

    private ItemCodex() {
    }

    public static void refreshUnlocks(SectorAPI sector) {
        Set<String> unlockedUniqueIds = new HashSet<>();
        try {
            for (ItemDefinition definition : ItemDefinitions.all()) {
                if (isUnlockGated(definition) && ItemUnlock.unlockConditionMet(definition, sector)) {
                    unlockedUniqueIds.add(definition.id());
                }
            }
        } catch (RuntimeException e) {
            Logger.getLogger(ItemCodex.class).warn("Could not check which unique hull items are unlocked", e);
        }
        unlockState = new UnlockState(new WeakReference<>(sector), Set.copyOf(unlockedUniqueIds));
    }

    static boolean isRevealed(ItemDefinition definition) {
        if (!isUnlockGated(definition)) {
            return true;
        }
        UnlockState currentState = unlockState;
        return currentState.isFor(Global.getSector()) && currentState.unlockedUniqueIds().contains(definition.id());
    }

    private static boolean isUnlockGated(ItemDefinition definition) {
        return definition.unique() && !definition.unlock().isEmpty();
    }

    public static String entryId(String definitionId) {
        return definitionId == null ? null : ENTRY_PREFIX + definitionId;
    }

    public static String existingEntryId(String definitionId) {
        String entryId = entryId(definitionId);
        CodexEntryPlugin entry = entryId == null || CodexDataV2.ENTRIES == null ? null : CodexDataV2.getEntry(entryId);
        if (entry == null) {
            return null;
        }
        ItemDefinition definition = ItemDefinitions.get(definitionId);
        boolean unlocked = definition == null || !isUnlockGated(definition)
                || ItemUnlock.unlockConditionMet(definition, Global.getSector());
        return unlocked ? entryId : null;
    }

    public static boolean open(String definitionId) {
        String entryId = existingEntryId(definitionId);
        if (entryId == null || Global.getSettings().isShowingCodex()) {
            return false;
        }
        Global.getSettings().showCodex(entryId);
        return true;
    }

    static ModSpecAPI sourceMod() {
        return Global.getSettings().getModManager().getModSpec(ExiledSectorModPlugin.MOD_ID);
    }

    private static void loadIcon(String iconPath) {
        try {
            Global.getSettings().loadTexture(iconPath);
        } catch (IOException | RuntimeException e) {
            Logger.getLogger(ItemCodex.class).warn("Could not load the hull item icon " + iconPath + " for the codex", e);
        }
    }

    public static void addEntries() {
        try {
            List<ItemDefinition> definitions = new ArrayList<>(ItemDefinitions.all());
            if (definitions.isEmpty() || CodexDataV2.ROOT == null) {
                return;
            }
            I18n.forGameText(() -> definitions.sort(Comparator.comparing((ItemDefinition definition) -> definition.kind().ordinal())
                    .thenComparing(ItemDefinition::unique).thenComparing(ItemDefinition::displayName)));
            CodexEntryV2 category = new CodexEntryV2(CATEGORY_ID, Translation.gameText("codex.socketables.category"), definitions.get(0).icon()) {
                @Override
                public void createTitleForList(TooltipMakerAPI info, float width, ListMode mode) {
                    info.setParaSmallInsignia();
                    VanillaText.plainPara(info, getTitle(), 0f, Misc.getBasePlayerColor());
                }

                @Override
                public ModSpecAPI getSourceMod() {
                    return sourceMod();
                }
            };
            category.setRetainOrderOfChildren(true);
            Set<String> iconPaths = new LinkedHashSet<>();
            for (ItemDefinition definition : definitions) {
                iconPaths.add(definition.icon());
                category.addChild(new ItemCodexEntry(definition));
            }
            iconPaths.forEach(ItemCodex::loadIcon);
            CodexDataV2.ROOT.addChild(category);
            CodexDataV2.rebuildIdToEntryMap();
        } catch (RuntimeException e) {
            Logger.getLogger(ItemCodex.class).error("Could not add the Exiled Sector hull item codex entries", e);
        }
    }
}
