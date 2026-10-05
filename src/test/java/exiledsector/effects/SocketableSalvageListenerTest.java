package exiledsector.effects;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.util.Misc;
import exiledsector.socketables.SocketableDefinitions;
import exiledsector.socketables.SocketableDrops;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SocketableSalvageListenerTest {

    private MockedStatic<Misc> miscMock;
    private final SocketableSalvageListener listener = new SocketableSalvageListener();

    @BeforeEach
    void setUp() throws Exception {
        miscMock = Mockito.mockStatic(Misc.class);
        miscMock.when(() -> Misc.getSalvageSeed(any())).thenReturn(42L);
        SocketableDefinitions.register(new JSONArray().put(new JSONObject().put("id", "domain_subroutine_military")
                .put("kind", "subroutine").put("rarity", "10").put("prefixes", "HULL_MULT:4:6").put("suffixes", "ARMOR_PERCENT:6:9")));
        SocketableDrops.register(new JSONArray()
                .put(new JSONObject().put("site", "station_research").put("chances", "1").put("uniqueChance", "0")));
    }

    @AfterEach
    void tearDown() {
        SocketableDrops.clear();
        SocketableDefinitions.clear();
        miscMock.close();
    }

    private static SectorEntityToken site(String customEntityType, String override) {
        Map<String, Object> values = new HashMap<>();
        if (override != null) {
            values.put(MemFlags.SALVAGE_SPEC_ID_OVERRIDE, override);
        }
        MemoryAPI memory = mock(MemoryAPI.class);
        when(memory.contains(anyString())).thenAnswer(call -> values.containsKey(call.<String>getArgument(0)));
        when(memory.getString(anyString())).thenAnswer(call -> (String) values.get(call.<String>getArgument(0)));
        when(memory.getBoolean(anyString())).thenAnswer(call -> Boolean.TRUE.equals(values.get(call.<String>getArgument(0))));
        Mockito.doAnswer(call -> values.put(call.getArgument(0), call.getArgument(1))).when(memory).set(anyString(), any());
        SectorEntityToken entity = mock(SectorEntityToken.class);
        when(entity.getCustomEntityType()).thenReturn(customEntityType);
        when(entity.getMemoryWithoutUpdate()).thenReturn(memory);
        return entity;
    }

    private static InteractionDialogAPI dialogFor(SectorEntityToken target) {
        InteractionDialogAPI dialog = mock(InteractionDialogAPI.class);
        when(dialog.getInteractionTarget()).thenReturn(target);
        return dialog;
    }

    @Test
    void aListedSiteAddsItsSocketablesToTheLootOnce() {
        SectorEntityToken station = site("station_research", null);
        CargoAPI loot = mock(CargoAPI.class);

        listener.reportAboutToShowLootToPlayer(loot, dialogFor(station));
        listener.reportAboutToShowLootToPlayer(loot, dialogFor(station));

        verify(loot, times(1)).addSpecial(any(), Mockito.eq(1f));
    }

    @Test
    void aSalvageSpecOverrideDecidesTheSite() {
        assertEquals("station_research", SocketableSalvageListener.siteId(site("custom_station", "station_research")));
        assertEquals("custom_station", SocketableSalvageListener.siteId(site("custom_station", null)));
    }

    @Test
    void unlistedSitesAndFleetsAddNothing() {
        CargoAPI loot = mock(CargoAPI.class);

        listener.reportAboutToShowLootToPlayer(loot, dialogFor(site("debris_field_shared", null)));
        listener.reportAboutToShowLootToPlayer(loot, dialogFor(mock(CampaignFleetAPI.class)));
        listener.reportAboutToShowLootToPlayer(loot, null);

        verify(loot, never()).addSpecial(any(), anyFloat());
    }
}
