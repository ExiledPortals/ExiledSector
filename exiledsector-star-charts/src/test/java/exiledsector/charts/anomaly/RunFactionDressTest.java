package exiledsector.charts.anomaly;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RunFactionDressTest {

    @Test
    void theAlliesTakeTheRealFactionsNameAndLookAndOnlyBefriendThePlayer() {
        FactionAPI allies = mock(FactionAPI.class);
        FactionAPI hegemony = mock(FactionAPI.class);
        when(allies.getId()).thenReturn(RunFactionDress.ALLY_FACTION_ID);
        when(hegemony.getDisplayName()).thenReturn("Hegemony");
        when(hegemony.getDisplayNameWithArticle()).thenReturn("the Hegemony");
        when(hegemony.getLogo()).thenReturn("logo");

        RunFactionDress.alliesOf("hegemony").apply(allies, hegemony, List.of(hegemony));

        verify(allies).setDisplayNameOverride("Hegemony");
        verify(allies).setDisplayNameWithArticleOverride("the Hegemony");
        verify(allies).setFactionLogoOverride("logo");
        verify(allies).setRelationship(Factions.PLAYER, RepLevel.FRIENDLY);
        verify(allies, never()).setRelationship(anyString(), eq(RepLevel.NEUTRAL));
    }

    @Test
    void aDressWithoutALikenessKeepsItsOwnLook() {
        FactionAPI dormant = mock(FactionAPI.class);
        when(dormant.getId()).thenReturn("dormant");

        new RunFactionDress(null, "Dormant", "the Dormant", "are", RepLevel.NEUTRAL, true).apply(dormant, null, null);

        verify(dormant).setDisplayNameOverride("Dormant");
        verify(dormant, never()).setFactionCrestOverride(anyString());
        verify(dormant).setRelationship(Factions.PLAYER, RepLevel.NEUTRAL);
    }
}
