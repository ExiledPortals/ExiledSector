package exiledsector.ui.decoration;

import com.fs.graphics.util.GLListManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Sphere;

public final class UnitSphere {

    private static final int DEFAULT_DETAIL = 32;

    private final Sphere sphere;
    private final int detail;
    private GLListManager.GLListToken displayList;

    public UnitSphere() {
        this(DEFAULT_DETAIL);
    }

    public UnitSphere(int detail) {
        this(texturedSphere(), detail);
    }

    UnitSphere(Sphere sphere) {
        this(sphere, DEFAULT_DETAIL);
    }

    private UnitSphere(Sphere sphere, int detail) {
        this.sphere = sphere;
        this.detail = detail;
    }

    private static Sphere texturedSphere() {
        Sphere sphere = new Sphere();
        sphere.setTextureFlag(true);
        return sphere;
    }

    public void draw(float radius) {
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
            sphere.draw(1f, detail, detail);
        } finally {
            if (token != null) {
                GLListManager.endList();
            }
        }
        return token;
    }
}
