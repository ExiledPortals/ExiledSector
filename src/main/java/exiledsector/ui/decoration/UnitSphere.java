package exiledsector.ui.decoration;

import com.fs.graphics.util.GLListManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Sphere;

final class UnitSphere {

    private static final int DETAIL = 32;

    private final Sphere sphere;
    private GLListManager.GLListToken displayList;

    UnitSphere() {
        this(texturedSphere());
    }

    UnitSphere(Sphere sphere) {
        this.sphere = sphere;
    }

    private static Sphere texturedSphere() {
        Sphere sphere = new Sphere();
        sphere.setTextureFlag(true);
        return sphere;
    }

    void draw(float radius) {
        GL11.glPushMatrix();
        GL11.glScalef(radius, radius, radius);
        if (!GLListManager.callList(displayList)) {
            displayList = compileAndDraw();
        }
        GL11.glPopMatrix();
    }

    private GLListManager.GLListToken compileAndDraw() {
        GLListManager.GLListToken token = GLListManager.buildingList ? null : GLListManager.beginList();
        try {
            sphere.draw(1f, DETAIL, DETAIL);
        } finally {
            if (token != null) {
                GLListManager.endList();
            }
        }
        return token;
    }
}
