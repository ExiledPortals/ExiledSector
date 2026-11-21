package exiledsector.harness;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.ui.ShownScreens;
import exiledsector.ui.SkillTreeCanvasPlugin;
import exiledsector.ui.SkillTreeRefitButton;
import exiledsector.ui.refit.UiReflection;
import org.lwjgl.util.vector.Vector2f;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class MoveMouseStep implements HarnessStep {

    static final String CENTRE = "centre";
    static final String SKILL_TREE_BUTTON = "skillTreeButton";
    static final String NODE_PREFIX = "node:";
    static final float ARRIVAL_TOLERANCE = 3f;
    static final float HIDDEN_BEYOND = -5000f;
    static final Pattern POINT = Pattern.compile("(\\d+(?:\\.\\d+)?),\\s*(\\d+(?:\\.\\d+)?)");

    private final String target;
    private final float seconds;
    private Vector2f start;
    private float startSeconds;

    MoveMouseStep(String target, float seconds) {
        if (!CENTRE.equals(target) && !SKILL_TREE_BUTTON.equals(target) && !target.startsWith(NODE_PREFIX) && !POINT.matcher(target).matches()) {
            throw new IllegalArgumentException("moveMouse goes to " + CENTRE + ", " + SKILL_TREE_BUTTON + ", " + NODE_PREFIX
                    + "<node id or type> or a screen point \"x,y\", not \"" + target + "\"");
        }
        if (seconds < 0f) {
            throw new IllegalArgumentException("moveMouse needs a duration of at least 0 seconds, not " + seconds);
        }
        this.target = target;
        this.seconds = seconds;
    }

    @Override
    public String name() {
        return "moveMouse to " + target + " over " + seconds + " s";
    }

    @Override
    public StepOutcome advance(StepContext context) throws Throwable {
        if (!HarnessMouse.isCalibrated()) {
            String failure = HarnessMouse.calibrate();
            return failure == null ? new StepOutcome(false, false, "calibrating the cursor") : StepOutcome.failed(failure);
        }
        Vector2f goal = goal();
        if (goal == null) {
            return new StepOutcome(false, false, target + " never appeared on screen");
        }
        if (start == null) {
            start = HarnessMouse.position();
            startSeconds = context.seconds();
        }
        float progress = seconds <= 0f ? 1f : Math.min(1f, (context.seconds() - startSeconds) / seconds);
        float eased = progress * progress * (3f - 2f * progress);
        HarnessMouse.moveTo(start.x + (goal.x - start.x) * eased, start.y + (goal.y - start.y) * eased);
        Vector2f now = HarnessMouse.position();
        if (progress >= 1f && Math.abs(now.x - goal.x) <= ARRIVAL_TOLERANCE && Math.abs(now.y - goal.y) <= ARRIVAL_TOLERANCE) {
            return StepOutcome.passed("the cursor reached " + target + " at " + Math.round(goal.x) + ", " + Math.round(goal.y));
        }
        return new StepOutcome(false, false, "the cursor never settled on " + target + "; it is at " + Math.round(now.x) + ", " + Math.round(now.y));
    }

    private Vector2f goal() throws Throwable {
        if (CENTRE.equals(target)) {
            return new Vector2f(Global.getSettings().getScreenWidth() / 2f, Global.getSettings().getScreenHeight() / 2f);
        }
        if (SKILL_TREE_BUTTON.equals(target)) {
            return skillTreeButtonCentre();
        }
        Matcher point = POINT.matcher(target);
        if (point.matches()) {
            return new Vector2f(Float.parseFloat(point.group(1)), Float.parseFloat(point.group(2)));
        }
        return nodeCentre(target.substring(NODE_PREFIX.length()));
    }

    private static Vector2f skillTreeButtonCentre() throws Throwable {
        if (Global.getSector().getCampaignUI().getCurrentCoreTab() == null) {
            return null;
        }
        Object button = UiTree.find(UiReflection.coreUI(Global.getSector().getCampaignUI()),
                component -> component instanceof ButtonAPI candidate && SkillTreeRefitButton.COLUMN_BUTTON.id().equals(candidate.getCustomData()));
        if (!(button instanceof ButtonAPI skillTreeButton)) {
            return null;
        }
        PositionAPI position = skillTreeButton.getPosition();
        return position.getX() < HIDDEN_BEYOND ? null : new Vector2f(position.getCenterX(), position.getCenterY());
    }

    private static Vector2f nodeCentre(String nodeIdOrType) {
        ShownScreens.Shown shown = ShownScreens.top();
        if (shown == null || !(shown.screen() instanceof SkillTreeCanvasPlugin canvas) || canvas.treeSession().isStartingRootMoving()) {
            return null;
        }
        SkillNode byId = SkillTree.get(nodeIdOrType);
        if (byId != null) {
            return canvas.nodeScreenCentre(byId);
        }
        for (SkillNode node : SkillTree.getAllNodes().values()) {
            if (node.getType().getId().equals(nodeIdOrType)) {
                Vector2f centre = canvas.nodeScreenCentre(node);
                if (centre != null && onScreen(centre)) {
                    return centre;
                }
            }
        }
        return null;
    }

    private static boolean onScreen(Vector2f point) {
        return point.x >= 0f && point.y >= 0f && point.x <= Global.getSettings().getScreenWidth() && point.y <= Global.getSettings().getScreenHeight();
    }

    @Override
    public float timeoutSeconds() {
        return Math.max(DEFAULT_TIMEOUT_SECONDS, seconds + DEFAULT_TIMEOUT_SECONDS);
    }
}
