package exiledsector.ui.infestation;

import exiledsector.ui.TreeLayers;

import java.util.Objects;

public final class InfestationEffects {

    private final InfestationSprites sprites = new InfestationSprites();
    private InfestationLook shownLook;
    private InfestationScene shownScene;
    private InfestationPainter painter;
    private float elapsedSeconds;

    public void show(InfestationLook look, InfestationScene scene) {
        if (look == null || scene == null || scene.socket() == null || scene.worldRadius() <= 0f) {
            hide();
            return;
        }
        if (painter != null && look == shownLook && Objects.equals(scene, shownScene)) {
            return;
        }
        InfestedGeometry geometry = InfestedGeometry.of(scene);
        shownLook = look;
        shownScene = scene;
        painter = switch (look) {
            case THREAT -> new ThreatInfestation(geometry, sprites);
            case SHROUD -> new ShroudInfestation(geometry, sprites);
        };
    }

    public void advance(float amount) {
        if (painter != null && amount > 0f) {
            elapsedSeconds += amount;
        }
    }

    public void renderUnderNodes(TreeLayers layers) {
        if (painter != null) {
            painter.renderUnderNodes(layers, elapsedSeconds);
        }
    }

    public void renderOverNodes(TreeLayers layers) {
        if (painter != null) {
            painter.renderOverNodes(layers, elapsedSeconds);
        }
    }

    private void hide() {
        shownLook = null;
        shownScene = null;
        painter = null;
    }
}
