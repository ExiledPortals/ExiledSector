package exiledsector.ui.inspect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;
import com.fs.starfarer.api.impl.codex.CodexDataV2;
import com.fs.starfarer.api.impl.codex.CodexEntryV2;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.ExiledSectorModPlugin;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.socketables.SocketableDefinition;
import exiledsector.socketables.SocketableDefinitions;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SocketableCodex {

    public static final String CATEGORY_ID = "exiledSector_socketables";
    static final String ENTRY_PREFIX = "exiledSector_socketable_";

    private SocketableCodex() {
    }

    public static String entryId(String definitionId) {
        return definitionId == null ? null : ENTRY_PREFIX + definitionId;
    }

    public static String existingEntryId(String definitionId) {
        String entryId = entryId(definitionId);
        return entryId != null && CodexDataV2.ENTRIES != null && CodexDataV2.getEntry(entryId) != null ? entryId : null;
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
                    info.addPara("%s", 0f, Misc.getBasePlayerColor(), Misc.getBasePlayerColor(), getTitle());
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
