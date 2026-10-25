package exiledsector.ui.decoration;

import com.fs.graphics.util.GLListManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Sphere;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class UnitSphereTest {

    private MockedStatic<GL11> gl;
    private MockedStatic<GLListManager> lists;
    private Sphere geometry;
    private UnitSphere sphere;

    @BeforeEach
    void setUp() {
        gl = Mockito.mockStatic(GL11.class);
        lists = Mockito.mockStatic(GLListManager.class);
        GLListManager.buildingList = false;
        geometry = mock(Sphere.class);
        sphere = new UnitSphere(geometry);
    }

    @AfterEach
    void tearDown() {
        GLListManager.buildingList = false;
        lists.close();
        gl.close();
    }

    @Test
    void theFirstDrawCompilesTheSphereThroughTheEnginesListManagerAtUnitScale() {
        GLListManager.GLListToken token = new GLListManager.GLListToken(7);
        lists.when(GLListManager::beginList).thenReturn(token);

        sphere.draw(150f);

        InOrder order = inOrder(GL11.class, GLListManager.class, geometry);
        order.verify(gl, GL11::glPushMatrix);
        order.verify(gl, () -> GL11.glScalef(150f, 150f, 150f));
        order.verify(lists, GLListManager::beginList);
        order.verify(geometry).draw(1f, 32, 32);
        order.verify(lists, GLListManager::endList);
        order.verify(gl, GL11::glPopMatrix);
    }

    @Test
    void theListIsClosedEvenIfDrawingTheSphereFails() {
        lists.when(GLListManager::beginList).thenReturn(new GLListManager.GLListToken(7));
        doThrow(new IllegalStateException("gl")).when(geometry).draw(anyFloat(), anyInt(), anyInt());

        assertThrows(IllegalStateException.class, () -> sphere.draw(150f));

        lists.verify(GLListManager::endList);
    }

    @Test
    void laterDrawsReplayTheListWhileTheManagerStillHoldsIt() {
        GLListManager.GLListToken token = new GLListManager.GLListToken(7);
        lists.when(GLListManager::beginList).thenReturn(token);
        sphere.draw(150f);
        lists.when(() -> GLListManager.callList(token)).thenReturn(true);

        sphere.draw(150f);
        sphere.draw(150.5f);

        verify(geometry, times(1)).draw(anyFloat(), anyInt(), anyInt());
        lists.verify(GLListManager::beginList, times(1));
        lists.verify(() -> GLListManager.callList(token), times(2));
        gl.verify(() -> GL11.glScalef(150.5f, 150.5f, 150.5f));
    }

    @Test
    void aListTheManagerHasReclaimedIsRecompiledInsteadOfReplayingWhateverNowOwnsItsId() {
        GLListManager.GLListToken first = new GLListManager.GLListToken(7);
        GLListManager.GLListToken second = new GLListManager.GLListToken(8);
        lists.when(GLListManager::beginList).thenReturn(first, second);
        sphere.draw(150f);
        lists.when(() -> GLListManager.callList(first)).thenReturn(false);

        sphere.draw(150f);

        verify(geometry, times(2)).draw(1f, 32, 32);
        lists.verify(GLListManager::beginList, times(2));
    }

    @Test
    void neverStartsAListWhileTheEngineIsRecordingOneAndDrawsDirectlyInstead() {
        GLListManager.buildingList = true;

        sphere.draw(150f);

        lists.verify(GLListManager::beginList, never());
        lists.verify(GLListManager::endList, never());
        verify(geometry).draw(1f, 32, 32);
    }

    @Test
    void drawsDirectlyWhenTheManagerIsSuspended() {
        lists.when(GLListManager::beginList).thenReturn(null);

        sphere.draw(150f);

        verify(geometry).draw(1f, 32, 32);
        lists.verify(GLListManager::endList, never());
        lists.verify(() -> GLListManager.callList(any()), times(1));
    }
}
