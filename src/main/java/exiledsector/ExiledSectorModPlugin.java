package exiledsector;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import exiledsector.effects.CombatXpListener;
import exiledsector.effects.LiveMunitionsCrewListener;
import exiledsector.effects.NpcFleetDialogListener;
import exiledsector.effects.NpcFleetInflationListener;
import exiledsector.effects.NpcFleetSweepScript;
import exiledsector.effects.PhantomHullMods;
import exiledsector.effects.SalvageBonusListener;
import exiledsector.effects.SkillConflictWarningHullMod;
import exiledsector.effects.SkillTreeHullMod;
import exiledsector.effects.SkillTreeInstaller;
import exiledsector.i18n.I18n;
import exiledsector.i18n.LanguageSetting;
import exiledsector.i18n.Languages;
import exiledsector.i18n.Translation;
import exiledsector.persistence.OpSpentSlotManager;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.persistence.SkillTreeTemplateStore;
import exiledsector.skills.SkillDataResolver;
import exiledsector.skills.SkillItemCost;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.npc.NpcLayouts;
import exiledsector.skills.skilleffect.CsvIdBlocklist;
import exiledsector.skills.skilleffect.FleetWideEffects;
import exiledsector.ui.ExiledSectorSettings;
import exiledsector.ui.SkillTreeRefitButton;
import exiledsector.ui.inspect.NpcTreeInspectInput;
import exiledsector.ui.inspect.SkillTreeCodexListener;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ExiledSectorModPlugin extends BaseModPlugin {

    public static final String LOG_TAG = "ExiledSector";
    public static final String MOD_ID = "exiledSector";
    public static final List<String> LOCALISED_HULLMODS = List.of(SkillTreeHullMod.ID, SkillConflictWarningHullMod.ID);

    @Override
    public void onApplicationLoad() throws Exception {
        loadLanguage();
        Global.getLogger(ExiledSectorModPlugin.class).info(LOG_TAG + " loaded");
        localiseHullModText();
        SkillTreeRefitButton.addButton();
        ExiledSectorSettings.register();
        SkillTree.load();
        PhantomHullMods.install(phantomHullModIds());
        CsvIdBlocklist.loadAll();
        NpcLayouts.load();
    }

    private static Set<String> phantomHullModIds() {
        Set<String> ids = new LinkedHashSet<>();
        for (SkillType type : SkillTree.getAllTypes().values()) {
            ids.addAll(type.getPhantomHullModIds());
        }
        return ids;
    }

    private static void loadLanguage() {
        Locale jvmLocale = Locale.getDefault();
        boolean gameRendersCjk = LanguageSetting.gameRendersCjk(jvmLocale);
        I18n.load(LanguageSetting.resolve(LanguageSetting.AUTO, jvmLocale, gameRendersCjk));
        ExiledSectorSettings.registerLanguage();
        Languages chosen = LanguageSetting.resolve(LanguageSetting.selected(), jvmLocale, gameRendersCjk);
        if (!chosen.equals(I18n.languages())) {
            I18n.load(chosen);
        }
    }

    private static void localiseHullModText() {
        I18n.forGameText(() -> LOCALISED_HULLMODS.forEach(ExiledSectorModPlugin::localise));
    }

    private static void localise(String hullModId) {
        HullModSpecAPI spec = Global.getSettings().getHullModSpec(hullModId);
        if (spec == null) {
            return;
        }
        spec.setDisplayName(Translation.data("hullmod." + hullModId + ".name", spec.getDisplayName()));
        spec.setDescriptionFormat(Translation.data("hullmod." + hullModId + ".description", spec.getDescriptionFormat()));
    }

    private static void forgetUnknownNodes() {
        if (!SkillTree.isLoadedCompletely()) {
            Global.getLogger(ExiledSectorModPlugin.class).warn(LOG_TAG + ": the skill tree did not load completely, so saved allocations were left as they are.");
            return;
        }
        ShipSkillDataManager.forgetUnknownNodes(SkillTree.getAllNodes(), SkillTree.getAllTypes(), ExiledSectorModPlugin::refund);
    }

    private static void refund(SkillItemCost itemCost) {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (playerFleet != null) {
            playerFleet.getCargo().addCommodity(itemCost.itemId(), itemCost.quantity());
        }
    }

    @Override
    public void onGameLoad(boolean newGame) {
        SkillDataResolver.clearCache();
        OpSpentSlotManager.releaseUnless(ShipSkillDataManager::hasProgress);
        ShipSkillDataManager.removeBlankRecords();
        forgetUnknownNodes();
        SkillTreeTemplateStore.pruneAssignments(ShipSkillDataManager::hasProgress);
        FleetWideEffects.markPhaseFieldStale();
        Global.getSector().removeScriptsOfClass(SkillTreeInstaller.class);
        Global.getSector().addTransientScript(new SkillTreeInstaller());
        Global.getSector().addTransientListener(new CombatXpListener());
        Global.getSector().addTransientListener(new LiveMunitionsCrewListener());
        Global.getSector().addTransientListener(new SalvageBonusListener());
        Global.getSector().addTransientScript(new NpcFleetSweepScript());
        Global.getSector().addTransientListener(new NpcFleetDialogListener());
        Global.getSector().getListenerManager().addListener(new NpcFleetInflationListener(), true);
        Global.getSector().getListenerManager().addListener(new NpcTreeInspectInput(), true);
        Global.getSector().getListenerManager().addListener(new SkillTreeCodexListener(), true);
    }
}
