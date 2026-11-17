package exiledsector.items.hull;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import exiledsector.domain.ExiledSectorDomain;
import exiledsector.effects.ShipEffectSource;
import exiledsector.items.ItemDataRules;
import exiledsector.items.ItemStore;
import exiledsector.items.ItemTypeSupport;
import exiledsector.items.custody.SectionCustody;
import exiledsector.items.custody.ShipCustody;
import exiledsector.items.drop.MarketHullItems;
import exiledsector.items.schematic.MasterSchematic;
import exiledsector.items.schematic.MasterSchematicActivation;
import exiledsector.items.schematic.RefitSchematic;
import exiledsector.skills.SocketedItem;
import exiledsector.skills.skilleffect.special.SlotItemEffect;
import exiledsector.loot.LootSource;
import exiledsector.loot.MarketStock;
import exiledsector.skills.ShipSections;
import exiledsector.ui.ExiledSectorSettings;
import exiledsector.ui.HullItemsRefitButton;
import exiledsector.ui.SettingsPage;
import exiledsector.ui.cargo.ItemIconLooks;
import exiledsector.ui.inspect.ShipSummaryPart;
import exiledsector.ui.refit.RefitButtonColumn;
import exiledsector.ui.section.SectionIconLooks;
import exiledsector.ui.section.SectionShipSummary;

import java.util.List;

public final class HullUpgradesDomain implements ExiledSectorDomain {

    public HullUpgradesDomain() {
        ShipSection.all().forEach(section -> SlotItemEffect.register(SocketedItem.HULL_ITEM, section.id(), section::displayName));
        ItemStore.absorbStacksWith(RefitSchematic.STACK_ABSORBER);
        ItemStore.absorbStacksWith(MasterSchematic.STACK_ABSORBER);
    }

    @Override
    public boolean enabled() {
        return HullUpgradesSetting.enabled();
    }

    @Override
    public List<ItemTypeSupport> itemTypes() {
        return List.of(HullItem.SUPPORT);
    }

    @Override
    public List<ItemDataRules> itemDataRules() {
        return List.of(HullItemDataRules.INSTANCE);
    }

    @Override
    public List<ItemIconLooks> itemIconLooks() {
        return List.of(SectionIconLooks.INSTANCE);
    }

    @Override
    public List<RefitButtonColumn.ButtonSpec> refitButtons() {
        return List.of(HullItemsRefitButton.COLUMN_BUTTON);
    }

    @Override
    public List<ShipCustody> shipCustodies() {
        return List.of(SectionCustody.INSTANCE);
    }

    @Override
    public List<ShipEffectSource> shipEffectSources() {
        return List.of(HullUpgradeEffectSource.INSTANCE);
    }

    @Override
    public List<ShipSummaryPart> shipSummaryParts() {
        return List.of(SectionShipSummary.INSTANCE);
    }

    @Override
    public List<LootSource> lootSources() {
        return List.of(HullUpgradeLootSource.INSTANCE);
    }

    @Override
    public List<MarketStock> marketStocks() {
        return List.of(HullUpgradeLootSource.INSTANCE);
    }

    @Override
    public void onApplicationLoad() {
        HullUpgradePoints.install();
        MasterSchematic.requiredWhen(HullUpgradesSetting::masterSchematicsRequired);
        HullItemsRefitButton.addButton();
    }

    @Override
    public void registerSettings() {
        String itemsTab = ExiledSectorSettings.itemsTab();
        HullUpgradesSetting.SWITCH.addSettings("exiledSector_hullUpgradesHeader", "settings.hullUpgrades.header",
                "settings.hullItems.hullUpgrades.name", "settings.hullItems.hullUpgrades.tooltip", itemsTab);
        SettingsPage itemsPage = new SettingsPage(itemsTab);
        HullUpgradesSetting.UNLOCK_LEVEL.addSetting(itemsPage, "settings.hullItems.dropLevel");
        itemsPage.radioSetting(HullUpgradesSetting.POINTS_FIELD_ID, "settings.hullItems.hullUpgradePoints",
                HullUpgradesSetting.REFIT_SCHEMATICS, HullUpgradesSetting.POINTS_OPTIONS);
        itemsPage.booleanSetting(HullUpgradesSetting.MASTER_SCHEMATICS_FIELD_ID, "settings.hullItems.masterSchematics",
                HullUpgradesSetting.DEFAULT_MASTER_SCHEMATICS_REQUIRED);
    }

    @Override
    public void reloadSettings() {
        HullUpgradesSetting.reloadSettings();
    }

    @Override
    public void loadItems() {
        MarketHullItems.load();
    }

    @Override
    public void onGameLoad(boolean newGame, List<CargoAPI> savedCargoes) {
        ShipSections.clearCache();
        HullUpgradeMigration.runOnSave(savedCargoes);
        HullUpgradePoints.markChanged();
        MasterSchematicActivation.sync(Global.getSector());
    }
}
