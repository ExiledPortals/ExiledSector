package exiledsector.charts.wormhole;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.ChartIds;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class ChartWormholeVisual {

    static final String SPRITE_CATEGORY = "misc";
    static final String BANDS_SPRITE = "wormhole_bands";
    static final String CORONA_SPRITE = "wormhole_corona";
    static final String RING_SPRITE = "wormhole_ring";
    static final String SECOND_RING_SPRITE = "wormhole_ring2";
    static final String STARFIELD_BACKDROP = "wormhole_stars";
    static final String HYPERSPACE_BACKDROP = "wormhole_hyper";
    static final String MASK_TEXTURE = "graphics/fx/circle64.png";
    static final String GLOW_TEXTURE = "graphics/fx/hit_glow.png";
    static final String OPEN_SOUND = "jump_point_open";
    static final String CLOSE_SOUND = "jump_point_close";
    static final float VISIBLE_RANGE_SHARE = 1.75f * 2.2f;
    static final int RING_COUNT = 100;
    static final int CORONA_COUNT = 6;
    static final float MIN_SCALE = 0.25f;
    static final float OPEN_TRIGGER_RANGE = 600f;
    static final float SECONDS_BEFORE_CLOSING = 5f;
    private static final float ADVANCE_EXTRA_RANGE = 1000f;
    private static final float SIZE_SHARE = 1.55f;
    private static final float CORONA_SIZE_SHARE = 2.3f * 0.9f;
    private static final float RING_SIZE_SHARE = 0.61f;
    private static final float RING_SHRINK_SHARE = 0.28320312f / 75f;
    private static final float RING_SHRINK_DECAY = 0.995f;
    private static final float GLOW_SCALE = 1.5f;
    private static final float GLOW_EXTRA_RADIUS = 70f;
    private static final float BAND_ANGULAR_VELOCITY = 5f;
    private static final Color GLOW_COLOUR = Color.WHITE;

    private final SpriteAPI ring;
    private final SpriteAPI secondRing;
    private final SpriteAPI corona;
    private final SpriteAPI mask;
    private final SpriteAPI glow;
    private final SpriteAPI backdrop;
    private final List<ChartWormholeBand> bands = new ArrayList<>();
    private final List<Drift> rings = new ArrayList<>();
    private final List<Drift> coronas = new ArrayList<>();
    private final Drift backdropDrift = new Drift(0.1f);
    private final FaderUtil openFader;
    private float secondsSinceOpened = 1000f;

    private ChartWormholeVisual(SpriteAPI bandTexture, SpriteAPI ring, SpriteAPI secondRing, SpriteAPI corona, SpriteAPI mask,
            SpriteAPI glow, SpriteAPI backdrop, float radius, boolean startOpen) {
        this.openFader = new FaderUtil(startOpen ? 1f : 0f, 1f, 1f);
        this.ring = ring;
        this.secondRing = secondRing;
        this.corona = corona;
        this.mask = mask;
        this.glow = glow;
        this.backdrop = backdrop;
        float bandWidth = radius * 1.25f;
        float innerRadius = radius * 0.45f + radius * 0.25f - bandWidth * 0.05f;
        float pixelsPerSegment = clamp(radius * (float) Math.PI * 2f / 50f, 3f, 10f);
        float maxFluctuation = Math.min(radius * 0.005f, 0.5f);
        bands.add(new ChartWormholeBand(bandTexture, bandWidth, pixelsPerSegment, innerRadius, BAND_ANGULAR_VELOCITY, maxFluctuation));
        bands.add(new ChartWormholeBand(bandTexture, bandWidth, pixelsPerSegment, innerRadius, -BAND_ANGULAR_VELOCITY, maxFluctuation));
        for (int index = 0; index < RING_COUNT; index++) {
            rings.add(new Drift(1f));
        }
        for (int index = 0; index < CORONA_COUNT; index++) {
            coronas.add(Drift.corona());
        }
    }

    static ChartWormholeVisual create(SectorEntityToken wormhole, boolean newlyCreated) throws IOException {
        Global.getSettings().loadTexture(MASK_TEXTURE);
        Global.getSettings().loadTexture(GLOW_TEXTURE);
        SpriteAPI[] sprites = {
                Global.getSettings().getSprite(SPRITE_CATEGORY, BANDS_SPRITE),
                Global.getSettings().getSprite(SPRITE_CATEGORY, RING_SPRITE),
                Global.getSettings().getSprite(SPRITE_CATEGORY, SECOND_RING_SPRITE),
                Global.getSettings().getSprite(SPRITE_CATEGORY, CORONA_SPRITE),
                Global.getSettings().getSprite(MASK_TEXTURE),
                Global.getSettings().getSprite(GLOW_TEXTURE),
                Global.getSettings().getSprite(SPRITE_CATEGORY, backdropFor(Global.getSector(), wormhole))};
        for (SpriteAPI sprite : sprites) {
            if (sprite == null) {
                return null;
            }
        }
        return new ChartWormholeVisual(sprites[0], sprites[1], sprites[2], sprites[3], sprites[4], sprites[5], sprites[6],
                wormhole.getRadius(), startsOpen(wormhole, newlyCreated));
    }

    static boolean startsOpen(SectorEntityToken wormhole, boolean newlyCreated) {
        LocationAPI location = wormhole.getContainingLocation();
        return newlyCreated || location == null || location.isHyperspace();
    }

    static String backdropFor(SectorAPI sector, SectorEntityToken wormhole) {
        if (!wormhole.hasTag(ChartIds.INNER_WORMHOLE_TAG)) {
            return STARFIELD_BACKDROP;
        }
        ChartRun run = ChartRuns.unclosedOwning(wormhole);
        LocationAPI origin = run == null ? null : ChartIds.resolveLocation(sector, run.originLocationId());
        return origin != null && origin.isHyperspace() ? HYPERSPACE_BACKDROP : STARFIELD_BACKDROP;
    }

    static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    static float scaleFor(float openBrightness) {
        float eased = openBrightness * openBrightness;
        return MIN_SCALE + (1f - MIN_SCALE) * eased;
    }

    static boolean isNearViewport(SectorEntityToken wormhole, ViewportAPI viewport, float extraRange) {
        return viewport != null && wormhole.getContainingLocation() == Global.getSector().getCurrentLocation()
                && viewport.isNearViewport(wormhole.getLocation(), wormhole.getRadius() + extraRange);
    }

    void advance(SectorEntityToken wormhole, float amount) {
        openFader.advance(amount);
        if (openFader.isFadedIn()) {
            secondsSinceOpened += amount;
        }
        ViewportAPI viewport = Global.getSector().getViewport();
        float radius = wormhole.getRadius();
        if (!isNearViewport(wormhole, viewport, radius * 1.75f + ADVANCE_EXTRA_RANGE)) {
            return;
        }
        updateOpenState(wormhole);
        if (!isNearViewport(wormhole, viewport, radius * VISIBLE_RANGE_SHARE)) {
            return;
        }
        float brightness = openFader.getBrightness();
        brightness *= brightness;
        float scale = scaleFor(openFader.getBrightness());
        float bandWidth = radius * 1.25f;
        float innerRadius = radius * 0.45f + radius * 0.25f - bandWidth * 0.05f;
        float pixelsPerSegment = clamp(radius * (float) Math.PI * 2f * scale / 50f, 3f, 10f);
        float maxFluctuation = brightness == 0f ? 35f : Math.min(radius * 0.01f, 0.5f);
        for (ChartWormholeBand band : bands) {
            band.advance(amount);
            band.setBandWidth(bandWidth * 0.3f + 20f + 0.6f * bandWidth / scale + 20f * (1f - brightness) / scale);
            band.setInnerRadius(innerRadius);
            band.setPixelsPerSegment(pixelsPerSegment);
            band.setMaxFluctuation(maxFluctuation);
        }
        float days = Global.getSector().getClock().convertToDays(amount);
        for (Drift drift : rings) {
            drift.advance(days);
        }
        for (Drift drift : coronas) {
            drift.advanceCorona(days);
        }
        backdropDrift.advance(days * 0.25f * 3f);
    }

    private void updateOpenState(SectorEntityToken wormhole) {
        LocationAPI location = wormhole.getContainingLocation();
        if (location == null || location.isHyperspace()) {
            return;
        }
        boolean approached = false;
        for (CampaignFleetAPI fleet : location.getFleets()) {
            if (fleet.getInteractionTarget() == wormhole && Misc.getDistance(fleet.getLocation(), wormhole.getLocation()) < OPEN_TRIGGER_RANGE) {
                approached = true;
                break;
            }
        }
        if (approached) {
            open(wormhole);
        } else if (secondsSinceOpened > SECONDS_BEFORE_CLOSING) {
            close(wormhole);
        }
    }

    private void open(SectorEntityToken wormhole) {
        if (!openFader.isFadingIn() && !openFader.isFadedIn() && wormhole.isInCurrentLocation()) {
            Global.getSoundPlayer().playSound(OPEN_SOUND, 1f, 1f, wormhole.getLocation(), wormhole.getVelocity());
        }
        secondsSinceOpened = 0f;
        openFader.fadeIn();
    }

    private void close(SectorEntityToken wormhole) {
        if (!openFader.isFadingOut() && !openFader.isFadedOut() && wormhole.isInCurrentLocation()) {
            Global.getSoundPlayer().playSound(CLOSE_SOUND, 1f, 1f, wormhole.getLocation(), wormhole.getVelocity());
        }
        openFader.fadeOut();
    }

    static float abyssalAlphaMult(float playerDepth, float wormholeDepth, float contactBrightness) {
        if (playerDepth <= 0f && wormholeDepth <= 0f) {
            return 1f;
        }
        if (playerDepth > 0f && wormholeDepth <= 0f) {
            return 1f - playerDepth;
        }
        return contactBrightness;
    }

    void render(SectorEntityToken wormhole, ViewportAPI viewport) {
        float radius = wormhole.getRadius();
        if (!isNearViewport(wormhole, viewport, radius * VISIBLE_RANGE_SHARE)) {
            return;
        }
        float alphaMult = viewport.getAlphaMult() * abyssalAlphaMult(Misc.getAbyssalDepthOfPlayer(), Misc.getAbyssalDepth(wormhole),
                wormhole.getSensorContactFaderBrightness());
        if (wormhole.hasSensorProfile()) {
            alphaMult *= wormhole.getSensorFaderBrightness();
        }
        if (alphaMult <= 0f) {
            return;
        }
        float brightness = openFader.getBrightness();
        brightness *= brightness;
        float scale = scaleFor(openFader.getBrightness());
        GL11.glPushMatrix();
        GL11.glTranslatef(wormhole.getLocation().x, wormhole.getLocation().y, 0f);
        if (scale < 1f) {
            GL11.glScalef(scale, scale, 1f);
        }
        renderBackdrop(radius, alphaMult);
        float coronaSize = radius * 2f * SIZE_SHARE * CORONA_SIZE_SHARE;
        renderCoronas(coronaSize, alphaMult);
        if (brightness < 1f) {
            for (ChartWormholeBand band : bands) {
                float bandWidth = band.bandWidth();
                band.setBandWidth(bandWidth * 0.5f);
                band.render(0f, 0f, alphaMult * (1f - brightness) * 0.75f);
                band.setBandWidth(bandWidth);
            }
        }
        if (brightness > 0f) {
            renderRings(radius * 2f * SIZE_SHARE, coronaSize * RING_SHRINK_SHARE, alphaMult * brightness);
        }
        GL11.glScalef(GLOW_SCALE / scale, GLOW_SCALE / scale, 1f);
        if (brightness < 1f) {
            renderGlow(radius, alphaMult, brightness);
        }
        GL11.glPopMatrix();
    }

    private void renderBackdrop(float radius, float alphaMult) {
        float maskSize = radius * 1.2f;
        mask.setSize(maskSize, maskSize);
        GL11.glColorMask(false, false, false, true);
        float clearSize = Math.max(backdrop.getWidth(), backdrop.getHeight());
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ZERO);
        GL11.glColor4ub((byte) 0, (byte) 0, (byte) 0, (byte) 0);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(-clearSize, -clearSize);
        GL11.glVertex2f(-clearSize, clearSize);
        GL11.glVertex2f(clearSize, clearSize);
        GL11.glVertex2f(clearSize, -clearSize);
        GL11.glEnd();
        mask.setAlphaMult(alphaMult);
        mask.setBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ZERO);
        mask.renderAtCenter(0f, 0f);
        GL11.glColorMask(true, true, true, false);
        float backdropWidth = backdrop.getWidth();
        float backdropHeight = backdrop.getHeight();
        float drift = (float) Math.sqrt(Math.sqrt(backdropDrift.xScale));
        backdrop.setAlphaMult(alphaMult);
        backdrop.setSize(backdropWidth * drift, backdropHeight * drift);
        backdrop.setAngle(backdropDrift.angle);
        backdrop.setBlendFunc(GL11.GL_DST_ALPHA, GL11.GL_ONE_MINUS_DST_ALPHA);
        backdrop.renderAtCenter(backdropDrift.xOffset, backdropDrift.yOffset);
        backdrop.setSize(backdropWidth, backdropHeight);
    }

    private void renderCoronas(float coronaSize, float alphaMult) {
        corona.setAdditiveBlend();
        corona.setAlphaMult(alphaMult);
        for (Drift drift : coronas) {
            corona.setAngle(drift.angle);
            float sizeMult = (1f + drift.coronaSizeSine * 0.1f) * 0.8f;
            corona.setSize(coronaSize * sizeMult, coronaSize * sizeMult);
            corona.renderAtCenter(drift.xOffset, drift.yOffset);
        }
    }

    private void renderRings(float startSize, float startShrink, float alphaMult) {
        ring.setAdditiveBlend();
        ring.setAlphaMult(alphaMult);
        secondRing.setAdditiveBlend();
        secondRing.setAlphaMult(alphaMult);
        float ringSize = startSize;
        float shrink = startShrink;
        for (int index = 0; index < rings.size(); index++) {
            Drift drift = rings.get(index);
            SpriteAPI drawn = index % 2 == 0 ? secondRing : ring;
            drawn.setAngle(drift.angle);
            drawn.setSize(ringSize * drift.xScale * RING_SIZE_SHARE, ringSize * drift.yScale * RING_SIZE_SHARE);
            drawn.renderAtCenter(drift.xOffset, drift.yOffset);
            ringSize -= shrink;
            shrink *= RING_SHRINK_DECAY;
        }
    }

    private void renderGlow(float radius, float alphaMult, float brightness) {
        float closedness = 1f - brightness;
        float glowSize = (radius + GLOW_EXTRA_RADIUS) * 2f * 0.75f * 0.25f;
        glow.setAngle(backdropDrift.angle);
        glow.setAdditiveBlend();
        glow.setColor(GLOW_COLOUR);
        glow.setSize(glowSize, glowSize);
        glow.setAlphaMult(alphaMult * closedness * 0.67f * closedness * closedness);
        glow.renderAtCenter(0f, 0f);
        glow.renderAtCenter(0f, 0f);
    }

    static final class Drift {

        float xOffset;
        float yOffset;
        float xScale = 1f;
        float yScale = 1f;
        float angle = (float) Math.random() * 360f;
        float coronaSizeSine = ((float) Math.random() - 0.5f) * 2f;
        private float elapsedDays = 1000f * (float) Math.random();
        private final float wobbleSpeed = (200f + 100f * (float) Math.random()) * randomSign();
        private final float turnSpeed;
        private final float turnMult;
        private final float pulseDegreesPerDay;

        Drift(float turnMult) {
            this(turnMult, (100f + 100f * (float) Math.random()) * randomSign(), 0f);
        }

        private Drift(float turnMult, float turnSpeed, float pulseDegreesPerDay) {
            this.turnMult = turnMult;
            this.turnSpeed = turnSpeed;
            this.pulseDegreesPerDay = pulseDegreesPerDay;
        }

        static Drift corona() {
            return new Drift(1f, (100f + 100f * (float) Math.random()) * randomSign(), 200f + (float) Math.random() * 200f);
        }

        private static float randomSign() {
            return Math.random() < 0.5 ? -1f : 1f;
        }

        void advance(float days) {
            elapsedDays += days;
            float sine = (float) Math.sin(Math.toRadians(elapsedDays * wobbleSpeed));
            float cosine = (float) Math.cos(Math.toRadians(elapsedDays * wobbleSpeed));
            xOffset = cosine * 2f;
            yOffset = sine * 2f;
            xScale = 1f + sine * 0.25f;
            yScale = 1f - sine * 0.25f;
            angle += turnSpeed * days * turnMult;
        }

        void advanceCorona(float days) {
            float doubled = days * 2f;
            angle += turnSpeed * doubled * 0.25f;
            elapsedDays += doubled;
            coronaSizeSine = (float) Math.sin(Math.toRadians(elapsedDays * pulseDegreesPerDay));
            float sine = (float) Math.sin(Math.toRadians(elapsedDays * turnSpeed));
            float cosine = (float) Math.cos(Math.toRadians(elapsedDays * turnSpeed));
            xOffset = cosine * 6f;
            yOffset = sine * 6f;
        }
    }
}
