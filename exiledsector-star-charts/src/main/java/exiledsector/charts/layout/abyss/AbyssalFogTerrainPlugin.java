package exiledsector.charts.layout.abyss;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.terrain.NebulaTerrainPlugin;

import java.awt.Color;

public class AbyssalFogTerrainPlugin extends NebulaTerrainPlugin {

    public static final String TERRAIN_ID = "exiledSector_chart_abyssal_fog";
    static final String FALLBACK_CATEGORY = "terrain";
    static final String FALLBACK_KEY = "nebula";
    static final int FOG_ALPHA = 225;

    private Color colour;

    void setColour(Color darkColour) {
        colour = darkColour == null ? null : new Color(darkColour.getRed(), darkColour.getGreen(), darkColour.getBlue(), FOG_ALPHA);
    }

    @Override
    protected Object readResolve() {
        super.readResolve();
        SpriteAPI fog = AbyssArt.texture(AbyssArt.FOG);
        SpriteAPI fogMap = AbyssArt.texture(AbyssArt.FOG_MAP);
        if (fog != null && fogMap != null) {
            texture = fog;
            mapTexture = fogMap;
        }
        return this;
    }

    @Override
    public String getNebulaTex() {
        return AbyssArt.texture(AbyssArt.FOG) == null ? super.getNebulaTex() : AbyssArt.FOG;
    }

    @Override
    public String getNebulaMapTex() {
        return AbyssArt.texture(AbyssArt.FOG_MAP) == null ? super.getNebulaMapTex() : AbyssArt.FOG_MAP;
    }

    @Override
    public Color getRenderColor() {
        return colour == null ? super.getRenderColor() : colour;
    }

    @Override
    protected boolean shouldCheckFleetsToApplyEffect() {
        return false;
    }

    @Override
    public void applyEffect(SectorEntityToken entity, float days) {
    }

    @Override
    public boolean hasTooltip() {
        return false;
    }

    @Override
    public boolean hasAIFlag(Object flag) {
        return false;
    }

    @Override
    public String getEffectCategory() {
        return TERRAIN_ID;
    }
}
