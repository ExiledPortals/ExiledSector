package exiledsector.ui.decoration;

import com.fs.starfarer.api.campaign.PlanetSpecAPI;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlanetCloudsTest {

    private static PlanetSpecAPI spec(String cloudTexture, Color cloudColour, float cloudRotation) {
        PlanetSpecAPI spec = mock(PlanetSpecAPI.class);
        when(spec.getCloudTexture()).thenReturn(cloudTexture);
        when(spec.getCloudColor()).thenReturn(cloudColour);
        when(spec.getCloudRotation()).thenReturn(cloudRotation);
        return spec;
    }

    @Test
    void onlyAPlanetWithACloudTextureAndColourHasClouds() {
        assertEquals("graphics/planets/clouds_terran01.png",
                PlanetClouds.texturePath(spec("graphics/planets/clouds_terran01.png", Color.WHITE, 2f)));
        assertNull(PlanetClouds.texturePath(spec("", Color.WHITE, 2f)));
        assertNull(PlanetClouds.texturePath(spec(null, Color.WHITE, 2f)));
        assertNull(PlanetClouds.texturePath(spec("graphics/planets/clouds_terran01.png", null, 2f)));
        assertNull(PlanetClouds.texturePath(null));
    }

    @Test
    void cloudsTurnOnTheirOwnClockAtTheSpecsCloudRotation() {
        PlanetSpecAPI spec = spec("clouds", Color.WHITE, 3f);

        assertEquals(40f, PlanetClouds.turn(spec, 40f, 0f), 1e-4f);
        assertEquals(70f, PlanetClouds.turn(spec, 40f, 10f), 1e-4f);
    }

    @Test
    void theCloudColoursAlphaScalesTheLayer() {
        PlanetSpecAPI spec = spec("clouds", new Color(255, 255, 255, 102), 0f);

        assertEquals(0.2f, PlanetClouds.layerAlpha(spec, 0.5f), 1e-4f);
    }
}
