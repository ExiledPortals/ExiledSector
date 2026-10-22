package exiledsector.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.thoughtworks.xstream.XStream;
import exiledlib.skills.SkillTypeEffect;
import exiledsector.ExiledSector;
import exiledlib.PlayerFleet;
import exiledsector.compat.AbyssSingularity;
import exiledlib.compat.CompatTarget;
import exiledlib.compat.LostSectorCompat;
import exiledsector.compat.MagicLibCompat;
import exiledsector.compat.NexerelinCompat;
import exiledsector.compat.NightcrossCompat;
import exiledsector.compat.NightcrossUnlocks;
import exiledsector.compat.RandomAssortmentCompat;
import exiledsector.compat.TaniaCompat;
import exiledsector.domain.ExiledSectorDomain;
import exiledsector.effects.CombatXpAward;
import exiledsector.effects.HyperNavBeaconGrant;
import exiledsector.effects.NpcBonusScale;
import exiledsector.effects.NpcFleetDialogListener;
import exiledsector.effects.NpcFleetInflationListener;
import exiledsector.effects.NpcFleetSweepScript;
import exiledsector.effects.OpReserveHullMods;
import exiledsector.effects.PhantomConflictWatch;
import exiledsector.effects.PhantomHullMods;
import exiledsector.effects.PlayerEngagementPipeline;
import exiledsector.effects.PlayerTreePower;
import exiledsector.effects.ShipTreeSync;
import exiledsector.effects.SkillConflictWarningHullMod;
import exiledsector.effects.ShipEffectSource;
import exiledsector.effects.SkillTreeEffectSource;
import exiledsector.effects.SkillTreeInstaller;
import exiledsector.effects.SkillTreeLootSource;
import exiledlib.i18n.I18n;
import exiledlib.i18n.SpecLocalisation;
import exiledsector.items.DeletedSubroutines;
import exiledsector.items.DoctrineArchives;
import exiledsector.items.DoctrineOverrides;
import exiledsector.items.ItemDataRules;
import exiledsector.items.ItemTypeSupport;
import exiledsector.items.ModspecCaches;
import exiledsector.items.NodeGrantLinks;
import exiledsector.items.NodeGrantStatus;
import exiledsector.items.NpcItems;
import exiledsector.items.ShipCustody;
import exiledsector.items.SocketCustody;
import exiledsector.items.Subroutine;
import exiledsector.items.SubroutineDataRules;
import exiledsector.items.SubroutineImplicits;
import exiledsector.items.TreeSocketCustody;
import exiledsector.items.UniqueSubroutinesSetting;
import exiledsector.items.WormholeAnchor;
import exiledsector.items.WormholeAnchorDataRules;
import exiledsector.items.WormholeAnchorDefinitions;
import exiledsector.items.WormholeAnchorLinks;
import exiledsector.items.WormholeAnchorMigration;
import exiledsector.loot.FleetDifficulty;
import exiledsector.loot.LootSource;
import exiledsector.persistence.RetiredReserveIds;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.persistence.SkillTreeTemplateStore;
import exiledsector.skills.npc.NpcFactionVolumes;
import exiledsector.skills.npc.NpcFocus;
import exiledsector.skills.npc.NpcLevelCaps;
import exiledsector.skills.radius.NodeRadii;
import exiledsector.skills.tags.AreaToggles;
import exiledsector.skills.unlock.NodeGates;
import exiledsector.skills.unlock.RelockedGates;
import exiledsector.skills.unlock.RemnantEncounter;
import exiledsector.skills.unlock.UnlockConditionOverrides;
import exiledsector.skills.unlock.UnlockConditionType;
import exiledsector.ui.SkillTreeRefitButton;
import exiledsector.ui.SkillTreeSettings;
import exiledsector.ui.TreeCameraMemory;
import exiledsector.ui.cargo.ItemIconLooks;
import exiledsector.ui.cargo.SkillTreeIconLooks;
import exiledsector.ui.inspect.ItemCodex;
import exiledsector.ui.inspect.NpcTreeInspectInput;
import exiledsector.ui.inspect.ShipTreeSummaryRenderer;
import exiledsector.ui.inspect.SkillTreeCodexListener;
import exiledsector.ui.refit.PhantomHullModRefitHider;
import exiledsector.ui.refit.RefitButtonColumn;
import exiledsector.ui.socket.ItemCodexLink;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public final class SkillTreeDomain implements ExiledSectorDomain {

    public static final List<String> LOCALISED_HULLMODS = List.of(SkillConflictWarningHullMod.ID);

    @Override
    public boolean enabled() {
        return SkillTreeSetting.enabled();
    }

    @Override
    public List<ShipEffectSource> shipEffectSources() {
        return List.of(SkillTreeEffectSource.INSTANCE);
    }

    @Override
    public List<LootSource> lootSources() {
        return List.of(SkillTreeLootSource.INSTANCE);
    }

    @Override
    public List<FleetDifficulty> fleetDifficulties() {
        return List.of(SkillTreeLootSource.INSTANCE);
    }

    @Override
    public void onApplicationLoad() {
        I18n.forGameText(() -> LOCALISED_HULLMODS.forEach(SpecLocalisation::hullMod));
        SkillTreeRefitButton.addButton();
    }

    @Override
    public void registerSettings() {
        SkillTreeSettings.register();
    }

    @Override
    public void reloadSettings() {
        SkillTreeSetting.reloadSettings();
        UniqueSubroutinesSetting.reloadSettings();
    }

    @Override
    public void loadData() {
        NodeRadii.load();
        SkillTree.load();
        SkillTree.applyDisabledRegions(AreaToggles.disabledRegions());
        PhantomHullMods.install(phantomHullModIds());
        LearnedPhantomConflicts.load();
        NpcFactionVolumes.load();
        NpcFocus.load();
        NpcLevelCaps.load();
        NodeReplacements.load();
        NodeGates.load();
    }

    @Override
    public void loadItems() {
        SubroutineImplicits.load();
        WormholeAnchorDefinitions.load();
        WormholeAnchorLinks.install();
        DoctrineArchives.load();
        ModspecCaches.load();
        NodeGrantLinks.install();
    }

    @Override
    public void addCodexEntries() {
        ItemCodex.addEntries();
        ItemCodexLink.register(ItemCodex.LINK);
    }

    @Override
    public void configureXStream(XStream xstream) {
        RetiredReserveIds.register(xstream);
    }

    @Override
    public void onGameLoad(boolean newGame, List<CargoAPI> savedCargoes) {
        SkillTree.applyDisabledRegions(AreaToggles.disabledRegions());
        SkillDataResolver.clearCache();
        DoctrineOverrides.clearCache();
        NodeGrantLinks.refresh();
        NodeGrantStatus.forgetAll();
        PhantomConflictWatch.clearPendingReverts();
        TreeCameraMemory.forgetAll();
        NpcBonusScale.invalidate();
        NpcItems.clearCache();
        SectorAPI sector = Global.getSector();
        DeletedSubroutines.removeFromSave(savedCargoes);
        OpReserveHullMods.forgetLegacySlots(sector.getPersistentData());
    }

    @Override
    public void afterSaveRepair() {
        forgetUnknownNodes();
        SkillTreeTemplateStore.pruneAssignments(ShipSkillDataManager::hasProgress);
        SectorAPI sector = Global.getSector();
        sector.removeScriptsOfClass(SkillTreeInstaller.class);
        sector.addTransientScript(new SkillTreeInstaller());
        sector.addTransientScript(new NpcFleetSweepScript());
        sector.addTransientListener(new NpcFleetDialogListener());
        sector.getListenerManager().addListener(new NpcFleetInflationListener(), true);
        sector.getListenerManager().addListener(new NpcTreeInspectInput(), true);
        sector.getListenerManager().addListener(new SkillTreeCodexListener(), true);
        sector.getListenerManager().addListener(new PhantomHullModRefitHider(), true);
    }

    @Override
    public void afterLootListeners() {
        SectorAPI sector = Global.getSector();
        sector.addTransientListener(new RemnantEncounter.BattleListener());
        sector.getListenerManager().addListener(new RelockedGates.LocationListener(), true);
        if (RandomAssortmentCompat.isModEnabled()) {
            AbyssSingularity.checkSaveOnLoad(sector);
            sector.addTransientListener(new AbyssSingularity.BattleListener());
        }
        if (NightcrossCompat.isModEnabled()) {
            NightcrossUnlocks.checkSaveOnLoad(sector);
            sector.getListenerManager().addListener(new NightcrossUnlocks.DiscoveryListener(), true);
        }
    }

    @Override
    public void afterSocketCustody() {
        WormholeAnchorMigration.run();
        SectorAPI sector = Global.getSector();
        sector.getListenerManager().addListener(new HyperNavBeaconGrant(), true);
        HyperNavBeaconGrant.grantOnce(sector);
        PlayerTreePower.logNpcScaling();
    }

    @Override
    public List<ItemTypeSupport> itemTypes() {
        return List.of(Subroutine.SUPPORT, WormholeAnchor.SUPPORT);
    }

    @Override
    public List<ItemDataRules> itemDataRules() {
        return List.of(SubroutineDataRules.INSTANCE, WormholeAnchorDataRules.INSTANCE);
    }

    @Override
    public List<ItemIconLooks> itemIconLooks() {
        return List.of(SkillTreeIconLooks.INSTANCE);
    }

    @Override
    public List<RefitButtonColumn.ButtonSpec> refitButtons() {
        return List.of(SkillTreeRefitButton.COLUMN_BUTTON);
    }

    @Override
    public Map<PlayerEngagementPipeline.Stage, PlayerEngagementPipeline.Step> engagementSteps() {
        return Map.of(PlayerEngagementPipeline.Stage.SYNC_TREES, engagement -> ShipTreeSync.afterPlayerEngagement(PlayerFleet.orNull()),
                PlayerEngagementPipeline.Stage.AWARD_XP, CombatXpAward::award);
    }

    @Override
    public void fleetChanged(CampaignFleetAPI fleet) {
        ShipTreeSync.fleetChanged(fleet);
    }

    @Override
    public void memberChanged(FleetMemberAPI member, ShipVariantAPI editedVariant) {
        ShipTreeSync.memberChanged(member, editedVariant);
    }

    @Override
    public List<SkillTypeEffect> fitEffects(ShipSkillData shipData, HullSize hullSize) {
        return AllocatedSkillEffects.treeEffects(shipData, AllocatedNode.of(shipData), hullSize);
    }

    @Override
    public ShipSkillData shipRecord(ShipVariantAPI variant) {
        return SkillDataResolver.npcRecord(variant);
    }

    @Override
    public void describeShip(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship) {
        ShipTreeSummaryRenderer.renderForCoreHullMod(tooltip, hullSize, ship);
    }

    @Override
    public void refitScreenListed() {
        PhantomHullModRefitHider.requestRefresh();
    }

    @Override
    public void playerFleetSynced() {
        ShipTreeSync.requestPlayerFleetSync();
    }

    @Override
    public List<ShipCustody> shipCustodies() {
        return List.of(TreeSocketCustody.INSTANCE);
    }

    @Override
    public boolean ignoresMemoryFlagUnlocks() {
        return UnlockConditionOverrides.isDisabled(UnlockConditionType.MEMORY_FLAG);
    }

    @Override
    public List<CompatTarget> compatTargets() {
        return List.of(LostSectorCompat.TARGET, MagicLibCompat.TARGET, NightcrossCompat.TARGET, TaniaCompat.TARGET,
                NexerelinCompat.TARGET);
    }

    private static Set<String> phantomHullModIds() {
        Set<String> ids = new LinkedHashSet<>();
        for (SkillType type : SkillTree.getAllTypes().values()) {
            ids.addAll(type.getPhantomHullModIds());
        }
        return ids;
    }

    private static void forgetUnknownNodes() {
        if (!SkillTree.isLoadedCompletely()) {
            Global.getLogger(SkillTreeDomain.class).warn(ExiledSector.LOG_TAG + ": the skill tree did not load completely, so saved allocations were left as they are.");
            return;
        }
        ShipTreeRepairs.replaceRemovedNodes(SkillTree.getDeclaredNodes(), NodeReplacements.all());
        ShipTreeRepairs.forgetUnknownNodes(SkillTree.getAllNodes(), SkillTree.getDeclaredNodes(), SkillTree.getAllTypes(),
                new OwnedShips(), charged -> charged.refundTo(PlayerFleet.cargoOrNull()));
    }

    private static final class OwnedShips implements Predicate<String> {

        private Set<String> ownedShipIds;

        @Override
        public boolean test(String shipId) {
            if (ownedShipIds == null) {
                ownedShipIds = SocketCustody.ownedShipIds();
            }
            return ownedShipIds.contains(shipId);
        }
    }
}
