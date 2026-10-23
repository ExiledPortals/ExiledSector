package exiledsector.ui.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledlib.skills.skilleffect.render.SpriteCache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpriteDrawTest {

    private static final String PATH = "graphics/test.png";

    private MockedStatic<Global> globalMock;
    private SettingsAPI settings;

    @BeforeEach
    void setUp() {
        settings = mock(SettingsAPI.class);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSettings).thenReturn(settings);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    @Test
    void drawingTheSamePathTwiceReusesOneSprite() {
        SpriteAPI sprite = mock(SpriteAPI.class);
        when(settings.getSprite(PATH)).thenReturn(sprite);
        SpriteCache cache = new SpriteCache(SpriteDrawTest.class);

        SpriteDraw.drawAtCenter(cache, PATH, 1f, 2f, 3f, 4f, null, 1f);
        SpriteDraw.drawAtCenter(cache, PATH, 5f, 6f, 7f, 8f, null, 1f);

        verify(settings, times(1)).getSprite(PATH);
        verify(sprite).renderAtCenter(1f, 2f);
        verify(sprite).renderAtCenter(5f, 6f);
    }

    @Test
    void everyDrawResetsTheStateAnEarlierDrawChanged() {
        SpriteAPI sprite = mock(SpriteAPI.class);
        when(settings.getSprite(PATH)).thenReturn(sprite);
        SpriteCache cache = new SpriteCache(SpriteDrawTest.class);

        SpriteDraw.drawAdditiveAtCenter(cache, PATH, 0f, 0f, 10f, 10f, Color.RED, 0.5f, 45f);
        SpriteDraw.drawAtCenter(cache, PATH, 0f, 0f, 20f, 30f, null, 1f);

        InOrder order = inOrder(sprite);
        order.verify(sprite).setAdditiveBlend();
        order.verify(sprite).renderAtCenter(0f, 0f);
        order.verify(sprite).setSize(20f, 30f);
        order.verify(sprite).setColor(Color.WHITE);
        order.verify(sprite).setAlphaMult(1f);
        order.verify(sprite).setAngle(0f);
        order.verify(sprite).setNormalBlend();
        order.verify(sprite).renderAtCenter(0f, 0f);
    }

    @Test
    void pivotDrawsSetTheCentreAfterTheSizeAndBeforeRendering() {
        SpriteAPI sprite = mock(SpriteAPI.class);
        when(settings.getSprite(PATH)).thenReturn(sprite);
        SpriteCache cache = new SpriteCache(SpriteDrawTest.class);

        SpriteDraw.drawAtPivot(cache, PATH, 1f, 2f, 30f, 40f, 12f, 18f, null, 0.5f, 90f);

        InOrder order = inOrder(sprite);
        order.verify(sprite).setSize(30f, 40f);
        order.verify(sprite).setCenter(12f, 18f);
        order.verify(sprite).setColor(Color.WHITE);
        order.verify(sprite).setAlphaMult(0.5f);
        order.verify(sprite).setAngle(90f);
        order.verify(sprite).setNormalBlend();
        order.verify(sprite).renderAtCenter(1f, 2f);
    }

    @Test
    void texturesAreKeptApartFromDrawnSpritesSoResizingOneNeverChangesTheOther() {
        SpriteAPI drawn = mock(SpriteAPI.class);
        SpriteAPI texture = mock(SpriteAPI.class);
        when(settings.getSprite(PATH)).thenReturn(drawn, texture);
        SpriteCache cache = new SpriteCache(SpriteDrawTest.class);

        SpriteAPI first = cache.sprite(PATH);
        SpriteAPI second = cache.texture(PATH);

        assertNotSame(first, second);
        assertSame(texture, cache.texture(PATH));
    }
}
