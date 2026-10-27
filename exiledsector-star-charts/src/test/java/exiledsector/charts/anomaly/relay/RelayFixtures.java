package exiledsector.charts.anomaly.relay;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import org.lwjgl.util.vector.Vector2f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class RelayFixtures {

    private RelayFixtures() {
    }

    static MemoryAPI memory() {
        Map<String, Object> stored = new HashMap<>();
        MemoryAPI memory = mock(MemoryAPI.class);
        doAnswer(invocation -> stored.put(invocation.getArgument(0), invocation.getArgument(1))).when(memory).set(anyString(), any());
        doAnswer(invocation -> stored.remove((String) invocation.getArgument(0))).when(memory).unset(anyString());
        when(memory.contains(anyString())).thenAnswer(invocation -> stored.containsKey((String) invocation.getArgument(0)));
        when(memory.get(anyString())).thenAnswer(invocation -> stored.get((String) invocation.getArgument(0)));
        when(memory.getString(anyString())).thenAnswer(invocation -> stored.get((String) invocation.getArgument(0)) instanceof String text ? text : null);
        when(memory.getBoolean(anyString())).thenAnswer(invocation -> Boolean.TRUE.equals(stored.get((String) invocation.getArgument(0))));
        when(memory.getLong(anyString())).thenAnswer(invocation -> stored.get((String) invocation.getArgument(0)) instanceof Number number
                ? number.longValue() : 0L);
        return memory;
    }

    static CustomCampaignEntityAPI entity(Vector2f location) {
        Set<String> tags = new HashSet<>();
        CustomCampaignEntityAPI entity = mock(CustomCampaignEntityAPI.class);
        MemoryAPI memory = memory();
        when(entity.getMemoryWithoutUpdate()).thenReturn(memory);
        when(entity.getLocation()).thenReturn(location);
        doAnswer(invocation -> tags.add(invocation.getArgument(0))).when(entity).addTag(anyString());
        when(entity.hasTag(anyString())).thenAnswer(invocation -> tags.contains((String) invocation.getArgument(0)));
        return entity;
    }

    static SectorEntityToken relay(RelayKind kind, Vector2f location) {
        SectorEntityToken relay = entity(location);
        relay.addTag(RelayNetwork.RELAY_TAG);
        relay.getMemoryWithoutUpdate().set(RelayNetwork.KIND_MEMORY_KEY, kind.key());
        return relay;
    }

    static CampaignFleetAPI fleet(boolean player, Vector2f location) {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        MutableFleetStatsAPI stats = mock(MutableFleetStatsAPI.class);
        DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class);
        MutableStat detectedByPlayer = new MutableStat(1f);
        when(dynamic.getStat(anyString())).thenReturn(detectedByPlayer);
        when(stats.getDynamic()).thenReturn(dynamic);
        when(fleet.getStats()).thenReturn(stats);
        when(fleet.isPlayerFleet()).thenReturn(player);
        when(fleet.getLocation()).thenReturn(location);
        when(fleet.getVelocity()).thenReturn(new Vector2f());
        return fleet;
    }
}
