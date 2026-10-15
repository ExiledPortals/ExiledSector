package exiledsector.ui;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;

record TemplateAction(Kind kind, String templateId, HullSize hullSize) {

    enum Kind {
        TOGGLE_TEMPLATE_MODE, OPEN_SAVE, OPEN_LOAD, TOGGLE_AUTO_ALLOCATE, DIALOG_SAVE, DIALOG_CANCEL, SELECT, DELETE, TOGGLE_HULL, CLEAR, CLOSE,
        NONE
    }

    static final TemplateAction NONE = new TemplateAction(Kind.NONE, null, null);

    static TemplateAction of(Kind kind) {
        return new TemplateAction(kind, null, null);
    }

    static TemplateAction forTemplate(Kind kind, String templateId) {
        return new TemplateAction(kind, templateId, null);
    }

    static TemplateAction forHullSize(HullSize hullSize) {
        return new TemplateAction(Kind.TOGGLE_HULL, null, hullSize);
    }
}
