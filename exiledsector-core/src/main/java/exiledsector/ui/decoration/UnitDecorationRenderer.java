package exiledsector.ui.decoration;

import exiledsector.skills.decoration.Decoration;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.TreeViewport;

import java.util.List;

abstract class UnitDecorationRenderer<T extends Decoration> implements DecorationRenderer {

    protected final SpriteCache spriteCache = new SpriteCache(getClass());
    private final List<T> units;
    protected float elapsedSeconds;

    UnitDecorationRenderer(List<T> units) {
        this.units = List.copyOf(units);
    }

    @Override
    public final void advance(float amount) {
        if (!units.isEmpty()) {
            elapsedSeconds += amount;
        }
    }

    @Override
    public final void render(DecorationScene scene, DecorationDepth depth) {
        if (depth != DecorationDepth.UNITS || units.isEmpty() || scene.sceneryAlpha() <= 0f) {
            return;
        }
        TreeViewport viewport = scene.viewport();
        for (T unit : units) {
            float unitAlpha = scene.alphaFor(unit.getId());
            if (unitAlpha > 0f) {
                renderUnit(viewport, unit, unitAlpha);
            }
        }
    }

    abstract void renderUnit(TreeViewport viewport, T unit, float alphaMult);
}
