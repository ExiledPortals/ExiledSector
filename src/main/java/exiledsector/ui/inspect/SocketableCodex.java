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
import exiledsector.socketables.SocketableDefinition;
import exiledsector.socketables.SocketableDefinitions;
import exiledsector.socketables.SocketableUnlock;
import exiledsector.ui.VanillaText;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SocketableCodex {

    public static final String CATEGORY_ID = "exiledSector_socketables";
    static final String ENTRY_PREFIX = "exiledSector_socketable_";

    private static volatile UnlockState unlockState = UnlockState.NONE;

    private record UnlockState(WeakReference<SectorAPI> sectorReference, Set<String> unlockedUniqueIds) {
        static final UnlockState NONE = new UnlockState(new WeakReference<>(null), Set.of());

        boolean isFor(SectorAPI sector) {
            return sector != null && sectorReference.get() == sector;
        }
    }

    private SocketableCodex() {
    }

    public static void refreshUnlocks(SectorAPI sector) {
        Set<String> unlockedUniqueIds = new HashSet<>();
        try {
            for (SocketableDefinition definition : SocketableDefinitions.all()) {
                if (isUnlockGated(definition) && SocketableUnlock.unlockConditionMet(definition, sector)) {
                    unlockedUniqueIds.add(definition.id());
                }
            }
        } catch (RuntimeException e) {
            Logger.getLogger(SocketableCodex.class).warn("Could not check which unique socketables are unlocked", e);
        }
        unlockState = new UnlockState(new WeakReference<>(sector), Set.copyOf(unlockedUniqueIds));
    }

    static boolean isRevealed(SocketableDefinition definition) {
        if (!isUnlockGated(definition)) {
            return true;
        }
        UnlockState currentState = unlockState;
        return currentState.isFor(Global.getSector()) && currentState.unlockedUniqueIds().contains(definition.id());
    }

    private static boolean isUnlockGated(SocketableDefinition definition) {
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
        SocketableDefinition definition = SocketableDefinitions.get(definitionId);
        boolean unlocked = definition == null || !isUnlockGated(definition)
                || SocketableUnlock.unlockConditionMet(definition, Global.getSector());
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
            Logger.getLogger(SocketableCodex.class).warn("Could not load the socketable icon " + iconPath + " for the codex", e);
        }
    }

    public static void addEntries() {
        try {
            List<SocketableDefinition> definitions = new ArrayList<>(SocketableDefinitions.all());
            if (definitions.isEmpty() || CodexDataV2.ROOT == null) {
                return;
            }
            I18n.forGameText(() -> definitions.sort(Comparator.comparing((SocketableDefinition definition) -> definition.kind().ordinal())
                    .thenComparing(SocketableDefinition::unique).thenComparing(SocketableDefinition::displayName)));
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
            for (SocketableDefinition definition : definitions) {
                iconPaths.add(definition.icon());
                category.addChild(new SocketableCodexEntry(definition));
            }
            iconPaths.forEach(SocketableCodex::loadIcon);
            CodexDataV2.ROOT.addChild(category);
            CodexDataV2.rebuildIdToEntryMap();
        } catch (RuntimeException e) {
            Logger.getLogger(SocketableCodex.class).error("Could not add the Exiled Sector socketable codex entries", e);
        }
    }
}
