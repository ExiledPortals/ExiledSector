package exiledsector.ui.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lazywizard.lazylib.ui.LazyFont;
import org.lwjgl.opengl.GL11;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.awt.Color;
import java.nio.FloatBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ItalicTextLabelTest {

    private static final float FONT_SIZE = 20f;

    private final LazyFont font = mock(LazyFont.class);
    private final LazyFont.DrawableString drawable = mock(LazyFont.DrawableString.class);
    private MockedStatic<GL11> gl;

    @BeforeEach
    void setUp() {
        gl = Mockito.mockStatic(GL11.class);
        when(font.createText(anyString(), any(Color.class), anyFloat())).thenReturn(drawable);
        when(font.calcWidth(anyString(), anyFloat())).thenAnswer(invocation -> invocation.<String>getArgument(0).length() * 10f);
    }

    @AfterEach
    void tearDown() {
        gl.close();
    }

    private ItalicTextLabel label(String text) {
        return new ItalicTextLabel(() -> font, FONT_SIZE, Color.GRAY).setWrapped(text, 10_000f, 800f);
    }

    @Test
    void eachLineIsSlantedAroundItsOwnMiddleSoLaterLinesDoNotDriftLeft() {
        label("First\nSecond").draw(5f, 100f);

        InOrder order = inOrder(GL11.class, drawable);
        order.verify(gl, GL11::glPushMatrix);
        order.verify(gl, () -> GL11.glTranslatef(5f, 90f, 0f));
        order.verify(gl, () -> GL11.glMultMatrix(any(FloatBuffer.class)));
        order.verify(drawable).draw(0f, 10f);
        order.verify(gl, GL11::glPopMatrix);
        order.verify(gl, GL11::glPushMatrix);
        order.verify(gl, () -> GL11.glTranslatef(5f, 70f, 0f));
    }

    @Test
    void blankParagraphGapsTakeSpaceWithoutDrawing() {
        ItalicTextLabel label = label("First\n\nSecond");
        label.draw(0f, 0f);

        assertEquals(3 * FONT_SIZE, label.height());
        verify(drawable, times(2)).draw(anyFloat(), anyFloat());
        verify(font, times(2)).createText(anyString(), any(Color.class), anyFloat());
    }

    @Test
    void widthCoversTheSlantedTopOfTheWidestLine() {
        assertEquals(60f + ItalicTextLabel.SLANT * FONT_SIZE / 2f, label("abc\nabcdef").width());
    }
}
