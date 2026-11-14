package exiledsector.charts.anomaly.convoy;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConvoyDressTest {

    @Test
    void theConvoyFactionTakesTheChartFactionsNameAndIsNeutralWithEveryOtherFaction() {
        FactionAPI convoy = mock(FactionAPI.class);
        FactionAPI hegemony = mock(FactionAPI.class);
        FactionAPI pirates = mock(FactionAPI.class);
        when(convoy.getId()).thenReturn(ConvoyRules.FACTION_ID);
        when(hegemony.getId()).thenReturn("hegemony");
        when(hegemony.getDisplayName()).thenReturn("Hegemony");
        when(hegemony.getDisplayNameWithArticle()).thenReturn("the Hegemony");
        when(hegemony.getShipNamePrefix()).thenReturn("HSS");
        when(hegemony.getCrest()).thenReturn("crest");
        when(pirates.getId()).thenReturn(Factions.PIRATES);

        SupplyConvoy.factionDress("hegemony").apply(convoy, hegemony, List.of(convoy, hegemony, pirates));

        verify(convoy).setDisplayNameOverride("Hegemony");
        verify(convoy).setDisplayNameWithArticleOverride("the Hegemony");
        verify(convoy).setShipNamePrefixOverride("HSS");
        verify(convoy).setFactionCrestOverride("crest");
        verify(convoy).setRelationship(Factions.PLAYER, RepLevel.NEUTRAL);
        verify(convoy).setRelationship("hegemony", RepLevel.NEUTRAL);
        verify(convoy).setRelationship(Factions.PIRATES, RepLevel.NEUTRAL);
        verify(convoy, never()).setRelationship(ConvoyRules.FACTION_ID, RepLevel.NEUTRAL);
    }
}
