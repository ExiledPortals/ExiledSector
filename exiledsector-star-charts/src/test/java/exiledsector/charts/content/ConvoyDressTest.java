package exiledsector.charts.content;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import exiledlib.MockSector;
import exiledlib.i18n.Translation;
import exiledsector.charts.anomaly.convoy.ConvoyRules;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConvoyDressTest {

    private MockSector mockSector;
    private FactionAPI convoy;
    private FactionAPI hegemony;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        convoy = mock(FactionAPI.class);
        hegemony = mock(FactionAPI.class);
        when(convoy.getId()).thenReturn(ConvoyRules.FACTION_ID);
        when(hegemony.getDisplayName()).thenReturn("Hegemony");
        when(hegemony.getDisplayNameWithArticle()).thenReturn("the Hegemony");
        when(hegemony.getShipNamePrefix()).thenReturn("HSS");
        when(hegemony.getCrest()).thenReturn("crest");
        when(mockSector.sector.getFaction(ConvoyRules.FACTION_ID)).thenReturn(convoy);
        when(mockSector.sector.getFaction("hegemony")).thenReturn(hegemony);
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
    }

    @Test
    void theConvoyFactionKeepsItsOwnNameSoReputationMessagesNeverNameARealFaction() {
        assertEquals(ConvoyRules.FACTION_ID, new VanillaChartContentEngine().dressConvoy("hegemony"));

        verify(convoy).setDisplayNameOverride(Translation.gameText("chart.convoy.faction.name"));
        verify(convoy).setDisplayNameWithArticleOverride(Translation.gameText("chart.convoy.faction.nameWithArticle"));
        verify(convoy, never()).setDisplayNameOverride("Hegemony");
        verify(convoy).setShipNamePrefixOverride("HSS");
        verify(convoy).setFactionCrestOverride("crest");
        verify(convoy).setRelationship(Factions.PLAYER, RepLevel.NEUTRAL);
    }

    @Test
    void theConvoyFactionIsNeutralWithEveryOtherFaction() {
        FactionAPI pirates = mock(FactionAPI.class);
        when(pirates.getId()).thenReturn(Factions.PIRATES);
        when(mockSector.sector.getAllFactions()).thenReturn(List.of(convoy, hegemony, pirates));
        when(hegemony.getId()).thenReturn("hegemony");

        new VanillaChartContentEngine().dressConvoy("hegemony");

        verify(convoy).setRelationship("hegemony", RepLevel.NEUTRAL);
        verify(convoy).setRelationship(Factions.PIRATES, RepLevel.NEUTRAL);
        verify(convoy, never()).setRelationship(ConvoyRules.FACTION_ID, RepLevel.NEUTRAL);
    }

    @Test
    void theSkirmishAlliesStillTakeTheRealFactionsName() {
        FactionAPI allies = mock(FactionAPI.class);
        when(allies.getId()).thenReturn("exiledSector_skirmish_ally");
        when(mockSector.sector.getFaction("exiledSector_skirmish_ally")).thenReturn(allies);

        new VanillaChartContentEngine().dressAllies("hegemony");

        verify(allies).setDisplayNameOverride("Hegemony");
        verify(allies).setDisplayNameWithArticleOverride("the Hegemony");
    }
}
