package exiledsector.charts.content;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionDoctrineAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChartFleetUpscalerTest {

    private final List<Integer> builtShipSizes = new ArrayList<>();

    @Test
    void aFleetUnderTheHullCountKeepsTheFactionsShipSize() {
        CampaignFleetAPI fleet = fleetOf(ChartFleetUpscaler.UPSCALE_HULL_COUNT - 1);

        CampaignFleetAPI built = ChartFleetUpscaler.build(new FleetParamsV3(), doctrineOfSize(2), factory(2, Map.of(2, fleet)));

        assertSame(fleet, built);
        assertEquals(List.of(2), builtShipSizes);
    }

    @Test
    void aFleetAtTheHullCountIsRebuiltOneShipSizeLargerUntilItFits() {
        CampaignFleetAPI fitting = fleetOf(30);
        Map<Integer, CampaignFleetAPI> bySize = Map.of(2, fleetOf(80), 3, fleetOf(ChartFleetUpscaler.UPSCALE_HULL_COUNT), 4, fitting);

        CampaignFleetAPI built = ChartFleetUpscaler.build(new FleetParamsV3(), doctrineOfSize(2), factory(2, bySize));

        assertSame(fitting, built);
        assertEquals(List.of(2, 3, 4), builtShipSizes);
    }

    @Test
    void theLargestShipSizeIsKeptEvenWithTooManyHulls() {
        CampaignFleetAPI largest = fleetOf(70);
        Map<Integer, CampaignFleetAPI> bySize = Map.of(4, fleetOf(90), 5, largest);

        CampaignFleetAPI built = ChartFleetUpscaler.build(new FleetParamsV3(), doctrineOfSize(4), factory(4, bySize));

        assertSame(largest, built);
        assertEquals(List.of(4, 5), builtShipSizes);
    }

    @Test
    void aFailedRebuildKeepsTheLastFleet() {
        CampaignFleetAPI crowded = fleetOf(60);
        Map<Integer, CampaignFleetAPI> bySize = Map.of(3, crowded);

        CampaignFleetAPI built = ChartFleetUpscaler.build(new FleetParamsV3(), doctrineOfSize(3), factory(3, bySize));

        assertSame(crowded, built);
        assertEquals(List.of(3, 4), builtShipSizes);
    }

    @Test
    void withoutADoctrineTheFleetIsBuiltOnce() {
        CampaignFleetAPI crowded = fleetOf(60);
        int[] builds = {0};

        CampaignFleetAPI built = ChartFleetUpscaler.build(new FleetParamsV3(), null, params -> {
            builds[0]++;
            return crowded;
        });

        assertSame(crowded, built);
        assertEquals(1, builds[0]);
    }

    private Function<FleetParamsV3, CampaignFleetAPI> factory(int baseSize, Map<Integer, CampaignFleetAPI> bySize) {
        return params -> {
            int size = params.doctrineOverride != null ? params.doctrineOverride.getShipSize() : baseSize;
            builtShipSizes.add(size);
            return bySize.get(size);
        };
    }

    private static FactionDoctrineAPI doctrineOfSize(int shipSize) {
        FactionDoctrineAPI doctrine = mutableDoctrine(shipSize);
        when(doctrine.clone()).thenAnswer(invocation -> mutableDoctrine(shipSize));
        return doctrine;
    }

    private static FactionDoctrineAPI mutableDoctrine(int initialShipSize) {
        FactionDoctrineAPI doctrine = mock(FactionDoctrineAPI.class);
        int[] shipSize = {initialShipSize};
        when(doctrine.getShipSize()).thenAnswer(invocation -> shipSize[0]);
        doAnswer(invocation -> {
            shipSize[0] = invocation.getArgument(0);
            return null;
        }).when(doctrine).setShipSize(anyInt());
        return doctrine;
    }

    private static CampaignFleetAPI fleetOf(int hulls) {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class, RETURNS_DEEP_STUBS);
        when(fleet.getFleetData().getNumMembers()).thenReturn(hulls);
        return fleet;
    }
}
