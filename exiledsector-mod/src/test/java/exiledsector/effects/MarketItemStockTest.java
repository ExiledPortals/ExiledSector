package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import exiledsector.MockSector;
import exiledsector.compat.RandomAssortmentCompat;
import exiledsector.items.ItemData;
import exiledsector.items.HullUpgradesSetting;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.JanusDevice;
import exiledsector.items.MarketHullItems;
import exiledsector.items.WormholeAnchorDefinitions;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.layout.SkillNodeDecoration;
import exiledsector.skills.npc.NpcFactionVolumes;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketItemStockTest {

    private static final String HEGEMONY_ANCHOR = "wormhole_anchor_hegemony";

    private final MarketItemStock listener = new MarketItemStock(new Random(3L) {
        @Override
        public float nextFloat() {
            return 0f;
        }
    });

    private MockSector mockSector;
    private CampaignClockAPI clock;
    private MutableCharacterStatsAPI playerStats;
    private final Map<String, Object> marketMemoryValues = new HashMap<>();

    @BeforeEach
    void setUp() throws Exception {
        clock = mock(CampaignClockAPI.class);
        when(clock.getTimestamp()).thenReturn(1000L);
        playerStats = mock(MutableCharacterStatsAPI.class);
        when(playerStats.getLevel()).thenReturn(15);
        mockSector = new MockSector();
        when(mockSector.sector.getClock()).thenReturn(clock);
        when(mockSector.sector.getPlayerStats()).thenReturn(playerStats);
        MemoryAPI sectorMemory = mock(MemoryAPI.class);
        when(sectorMemory.getBoolean(JanusDevice.FLAG)).thenReturn(true);
        when(mockSector.sector.getMemoryWithoutUpdate()).thenReturn(sectorMemory);
        SkillType exitType = new SkillType.Builder("wormhole_exit", "exit", "a.png", SkillTier.WORMHOLE_EXIT).build();
        SkillTree.registerType(exitType);
        SkillTree.register(new SkillNode("hegemony_2", exitType, List.of(), 0f, 0f, SkillNodeDecoration.NONE, List.of("hegemony")));
        NpcFactionVolumes.register(new JSONArray("[{\"faction\": \"hegemony\", \"region\": \"hegemony\"}]"));
        WormholeAnchorDefinitions.register(new JSONArray().put(new JSONObject().put("id", HEGEMONY_ANCHOR).put("volume", "hegemony")
                .put("exit_node", "hegemony_2").put("name", "Hegemony Anchor").put("icon", "graphics/icons/nav_relay.png").put("rarity", "10")
                .put("description", "Opens a wormhole.").put("market_chance", "0.3")));
        ItemDefinitions.register(new JSONArray()
                .put(new JSONObject().put("id", "bridge_basic").put("section", "bridge").put("rarity", "10")
                        .put("prefixes", "HULL_PERCENT:4:6; ARMOR_PERCENT:4:6").put("suffixes", "FLUX_CAPACITY_MULT:4:6"))
                .put(new JSONObject().put("id", "bridge_relic").put("section", "bridge").put("rarity", "10").put("unique", "true")
                        .put("prefixes", "HULL_PERCENT:4:6")));
        MarketHullItems.register(new JSONArray()
                .put(new JSONObject().put("submarket", Submarkets.SUBMARKET_BLACK).put("min_count", "3").put("max_count", "3")));
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
        MarketHullItems.clear();
        ItemDefinitions.clear();
        WormholeAnchorDefinitions.clear();
        NpcFactionVolumes.clear();
        SkillTree.clearNodes();
        SkillTree.clearTypes();
    }

    private SubmarketAPI submarket(String specId, String factionId, float sinceStockRefresh, CargoAPI cargo) {
        BaseSubmarketPlugin plugin = mock(BaseSubmarketPlugin.class);
        when(plugin.getSinceSWUpdate()).thenReturn(sinceStockRefresh);
        MemoryAPI marketMemory = mock(MemoryAPI.class);
        when(marketMemory.contains(anyString())).thenAnswer(invocation -> marketMemoryValues.containsKey((String) invocation.getArgument(0)));
        when(marketMemory.getLong(anyString())).thenAnswer(invocation -> (Long) marketMemoryValues.get((String) invocation.getArgument(0)));
        doAnswer(invocation -> marketMemoryValues.put(invocation.getArgument(0), invocation.getArgument(1))).when(marketMemory).set(anyString(), any());
        MarketAPI market = mock(MarketAPI.class);
        when(market.getFactionId()).thenReturn(factionId);
        when(market.getMemoryWithoutUpdate()).thenReturn(marketMemory);
        SubmarketAPI submarket = mock(SubmarketAPI.class);
        when(submarket.getSpecId()).thenReturn(specId);
        when(submarket.getPlugin()).thenReturn(plugin);
        when(submarket.getMarket()).thenReturn(market);
        when(submarket.getCargo()).thenReturn(cargo);
        return submarket;
    }

    private static CargoAPI emptyCargo() {
        CargoAPI cargo = mock(CargoAPI.class);
        when(cargo.getStacksCopy()).thenReturn(List.of());
        return cargo;
    }

    private static CargoStackAPI specialStack(SpecialItemData special) {
        CargoStackAPI stack = mock(CargoStackAPI.class);
        when(stack.isSpecialStack()).thenReturn(true);
        when(stack.getSpecialDataIfSpecial()).thenReturn(special);
        when(stack.getSize()).thenReturn(1f);
        return stack;
    }

    private static List<SpecialItemData> added(CargoAPI cargo, int expected) {
        ArgumentCaptor<SpecialItemData> stocked = ArgumentCaptor.forClass(SpecialItemData.class);
        verify(cargo, times(expected)).addSpecial(stocked.capture(), anyFloat());
        return stocked.getAllValues();
    }

    @Test
    void theOpenAndMilitaryMarketsOfTheAnchorsFactionListItForSaleWhenTheirStockRefreshes() {
        for (String submarketId : List.of(Submarkets.SUBMARKET_OPEN, Submarkets.GENERIC_MILITARY)) {
            CargoAPI cargo = emptyCargo();

            listener.reportSubmarketCargoAndShipsUpdated(submarket(submarketId, "hegemony", 0f, cargo));

            SpecialItemData listing = added(cargo, 1).get(0);
            assertTrue(ItemData.isMarketListing(listing));
            assertEquals(HEGEMONY_ANCHOR, ItemData.of(listing).definitionId());
        }
    }

    @Test
    void nothingIsStockedBetweenRefreshesOrInOtherMarkets() {
        CargoAPI cargo = emptyCargo();

        listener.reportSubmarketCargoAndShipsUpdated(submarket(Submarkets.SUBMARKET_OPEN, "hegemony", 2f, cargo));
        listener.reportSubmarketCargoAndShipsUpdated(submarket(Submarkets.SUBMARKET_STORAGE, "hegemony", 0f, cargo));
        listener.reportSubmarketCargoAndShipsUpdated(submarket(Submarkets.SUBMARKET_OPEN, "tritachyon", 0f, cargo));
        listener.reportSubmarketCargoAndShipsUpdated(null);

        verify(cargo, never()).addSpecial(any(), anyFloat());
    }

    @Test
    void aRestockReplacesTheOldListingButLeavesACopyThePlayerSold() {
        CargoAPI cargo = mock(CargoAPI.class);
        CargoStackAPI oldListing = specialStack(new SpecialItemData(ItemData.LISTING_ITEM_ID, HEGEMONY_ANCHOR + "|5"));
        CargoStackAPI soldCopy = specialStack(new SpecialItemData(ItemData.ITEM_ID, HEGEMONY_ANCHOR + "|6"));
        when(cargo.getStacksCopy()).thenReturn(List.of(oldListing, soldCopy));

        listener.reportSubmarketCargoAndShipsUpdated(submarket(Submarkets.SUBMARKET_OPEN, "hegemony", 0f, cargo));

        verify(cargo).removeStack(oldListing);
        verify(cargo, never()).removeStack(soldCopy);
        assertTrue(ItemData.isMarketListing(added(cargo, 1).get(0)));
    }

    @Test
    void reopeningAMarketBeforeItsNextRestockNeverRollsAgainButEachSubmarketRollsItsOwn() {
        CargoAPI cargo = emptyCargo();

        listener.reportSubmarketCargoAndShipsUpdated(submarket(Submarkets.SUBMARKET_OPEN, "hegemony", 0f, cargo));
        listener.reportSubmarketCargoAndShipsUpdated(submarket(Submarkets.SUBMARKET_OPEN, "hegemony", 0f, cargo));
        listener.reportSubmarketCargoAndShipsUpdated(submarket(Submarkets.GENERIC_MILITARY, "hegemony", 0f, cargo));
        when(clock.getTimestamp()).thenReturn(5000L);
        listener.reportSubmarketCargoAndShipsUpdated(submarket(Submarkets.SUBMARKET_OPEN, "hegemony", 0f, cargo));

        verify(cargo, times(3)).addSpecial(any(), anyFloat());
    }

    @Test
    void theExoshipMarketRollsOnEveryRestockOfItsOwnCounterEvenAtTheSameGameTime() {
        CargoAPI cargo = emptyCargo();

        listener.reportSubmarketCargoAndShipsUpdated(submarket(RandomAssortmentCompat.EXOSHIP_SUBMARKET, "hegemony", 0f, cargo));
        listener.reportSubmarketCargoAndShipsUpdated(submarket(RandomAssortmentCompat.EXOSHIP_SUBMARKET, "hegemony", 0.01f, cargo));
        listener.reportSubmarketCargoAndShipsUpdated(submarket(RandomAssortmentCompat.EXOSHIP_SUBMARKET, "hegemony", 0f, cargo));

        verify(cargo, times(2)).addSpecial(any(), anyFloat());
    }

    @Test
    void aMarketRestocksHullItemsWithOneModifierAndNoUniquesReplacingTheOldOnes() {
        CargoAPI cargo = mock(CargoAPI.class);
        CargoStackAPI oldStock = specialStack(ItemData.rolledWithEffectCount(ItemDefinitions.get("bridge_basic"), 1L, 1).toMarketListing());
        CargoStackAPI soldHullItem = specialStack(ItemData.rolledWithEffectCount(ItemDefinitions.get("bridge_basic"), 3L, 1).toSpecialItem());
        CargoStackAPI oldAnchor = specialStack(ItemData.rolled(ItemDefinitions.get(HEGEMONY_ANCHOR), 2L).toMarketListing());
        when(cargo.getStacksCopy()).thenReturn(List.of(oldStock, soldHullItem, oldAnchor));

        listener.reportSubmarketCargoAndShipsUpdated(submarket(Submarkets.SUBMARKET_BLACK, "pirates", 0f, cargo));

        verify(cargo).removeStack(oldStock);
        verify(cargo, never()).removeStack(soldHullItem);
        verify(cargo).removeStack(oldAnchor);
        List<SpecialItemData> stocked = added(cargo, 3);
        for (SpecialItemData special : stocked) {
            ItemData item = ItemData.of(special);
            assertEquals("bridge_basic", item.definitionId());
            assertEquals(1, item.explicits().size());
            assertTrue(ItemData.isMarketListing(special));
        }
    }

    @Test
    void aSwitchedOffDomainsListingsAreClearedOnRestockAndNotReplaced() {
        CargoAPI cargo = mock(CargoAPI.class);
        CargoStackAPI oldStock = specialStack(ItemData.rolledWithEffectCount(ItemDefinitions.get("bridge_basic"), 1L, 1).toMarketListing());
        when(cargo.getStacksCopy()).thenReturn(List.of(oldStock));
        HullUpgradesSetting.setEnabledForTests(false);
        try {
            listener.reportSubmarketCargoAndShipsUpdated(submarket(Submarkets.SUBMARKET_BLACK, "pirates", 0f, cargo));
        } finally {
            HullUpgradesSetting.setEnabledForTests(true);
        }

        verify(cargo).removeStack(oldStock);
        verify(cargo, never()).addSpecial(any(), anyFloat());
    }

    @Test
    void marketsSellNoHullItemsBeforePlayerLevelFifteen() {
        when(playerStats.getLevel()).thenReturn(14);
        CargoAPI cargo = emptyCargo();

        listener.reportSubmarketCargoAndShipsUpdated(submarket(Submarkets.SUBMARKET_BLACK, "pirates", 0f, cargo));

        verify(cargo, never()).addSpecial(any(), anyFloat());
    }

    @Test
    void aBoughtListingBecomesAnOrdinaryItemInThePlayersCargo() {
        CargoAPI playerCargo = mock(CargoAPI.class);
        ItemData anchor = ItemData.rolled(ItemDefinitions.get(HEGEMONY_ANCHOR), 7L);
        CargoStackAPI bought = specialStack(anchor.toMarketListing());
        CargoStackAPI owned = specialStack(anchor.toSpecialItem());
        when(playerCargo.getStacksCopy()).thenReturn(new ArrayList<>(List.of(bought, owned)));

        MarketItemStock.unlistBought(playerCargo);

        verify(playerCargo).removeStack(bought);
        verify(playerCargo, never()).removeStack(owned);
        SpecialItemData converted = added(playerCargo, 1).get(0);
        assertEquals(ItemData.ITEM_ID, converted.getId());
        assertEquals(anchor.definitionId(), ItemData.of(converted).definitionId());
        verify(Global.getSector().getMemoryWithoutUpdate()).set("$exiledSector_anchorFound_hegemony", true);
    }

    @Test
    void anAnchorSellsForAShareOfWhatItCostsToBuy() {
        int sellBase = WormholeAnchorDefinitions.sellBaseValue(100000, 1.2f, 0.8f);

        assertEquals(0.01f, sellBase * 0.8f / (100000 * 1.2f), 1e-4f);
    }
}
