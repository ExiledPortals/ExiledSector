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
    void aSkirmishCopyTakesTheRealFactionsNameAndLookAndIsHostileToThePlayer() {
        FactionAPI copy = mock(FactionAPI.class);
        FactionAPI hegemony = mock(FactionAPI.class);
        when(copy.getId()).thenReturn(RunFactionDress.ATTACKER_FACTION_ID);
        when(hegemony.getDisplayName()).thenReturn("Hegemony");
        when(hegemony.getDisplayNameWithArticle()).thenReturn("the Hegemony");
        when(hegemony.getLogo()).thenReturn("logo");

        RunFactionDress.hostileCopyOf("hegemony").apply(copy, hegemony, List.of(hegemony));

        verify(copy).setDisplayNameOverride("Hegemony");
        verify(copy).setDisplayNameWithArticleOverride("the Hegemony");
        verify(copy).setFactionLogoOverride("logo");
        verify(copy).setRelationship(Factions.PLAYER, RepLevel.HOSTILE);
        verify(copy, never()).setRelationship(anyString(), eq(RepLevel.NEUTRAL));
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
