package exiledsector.harness;

import com.fs.starfarer.api.Global;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.util.vector.Vector2f;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

final class HarnessMouse {

    static final int LEFT_BUTTON_MASK = 1 << 10;
    static final int RIGHT_BUTTON_MASK = 1 << 12;
    static final int CONTROL_KEY = 17;
    static final int CALIBRATION_STRIDE = 200;
    static final int SETTLE_FRAMES = 3;
    static final float[] DESKTOP_SCALES = {1f, 1.25f, 1.5f, 1.75f, 2f, 2.25f, 2.5f, 3f};

    private static MethodHandle mouseMove;
    private static MethodHandle mousePress;
    private static MethodHandle mouseRelease;
    private static MethodHandle mouseWheel;
    private static MethodHandle keyPress;
    private static MethodHandle keyRelease;
    private static Object robot;
    private static Calibration calibration;
    private static Calibrator calibrator;

    record Calibration(float scaleX, float offsetX, float scaleY, float offsetY) {

        static Calibration between(int firstRobotX, int firstRobotY, float firstWindowX, float firstWindowY,
                                   int secondRobotX, int secondRobotY, float secondWindowX, float secondWindowY) {
            float scaleX = (secondWindowX - firstWindowX) / (secondRobotX - firstRobotX);
            float scaleY = (secondWindowY - firstWindowY) / (secondRobotY - firstRobotY);
            if (!(scaleX > 0f) || !(scaleY > 0f)) {
                return null;
            }
            return new Calibration(scaleX, firstWindowX - scaleX * firstRobotX, scaleY, firstWindowY - scaleY * firstRobotY);
        }

        int robotX(float windowX) {
            return Math.round((windowX - offsetX) / scaleX);
        }

        int robotY(float windowYFromTop) {
            return Math.round((windowYFromTop - offsetY) / scaleY);
        }
    }

    private static final class Calibrator {
        private int scaleIndex;
        private int frame;
        private int firstRobotX;
        private int firstRobotY;
        private float firstWindowX;
        private float firstWindowY;

        String advance() throws Throwable {
            if (scaleIndex >= DESKTOP_SCALES.length) {
                return "the cursor never reached the game window at any desktop scale";
            }
            if (frame == 0) {
                float desktopScale = DESKTOP_SCALES[scaleIndex];
                firstRobotX = Math.round((Display.getX() + Display.getWidth() / 2f) / desktopScale);
                firstRobotY = Math.round((Display.getY() + Display.getHeight() / 2f) / desktopScale);
                move(firstRobotX, firstRobotY);
            } else if (frame == SETTLE_FRAMES) {
                if (!Mouse.isInsideWindow()) {
                    nextScale();
                    return null;
                }
                firstWindowX = Mouse.getX();
                firstWindowY = windowYFromTop(Mouse.getY());
                move(firstRobotX + CALIBRATION_STRIDE, firstRobotY + CALIBRATION_STRIDE);
            } else if (frame == 2 * SETTLE_FRAMES) {
                calibration = Mouse.isInsideWindow() ? Calibration.between(firstRobotX, firstRobotY, firstWindowX, firstWindowY,
                        firstRobotX + CALIBRATION_STRIDE, firstRobotY + CALIBRATION_STRIDE, Mouse.getX(), windowYFromTop(Mouse.getY())) : null;
                if (calibration == null) {
                    nextScale();
                    return null;
                }
            }
            frame++;
            return null;
        }

        private void nextScale() {
            scaleIndex++;
            frame = 0;
        }
    }

    private HarnessMouse() {
    }

    static boolean isCalibrated() {
        return calibration != null;
    }

    static String calibrate() throws Throwable {
        if (calibrator == null) {
            calibrator = new Calibrator();
        }
        String failure = calibrator.advance();
        if (failure != null) {
            calibrator = null;
        }
        return failure;
    }

    static Vector2f position() {
        float uiScale = Global.getSettings().getScreenScaleMult();
        return new Vector2f(Mouse.getX() / uiScale, Mouse.getY() / uiScale);
    }

    static void moveTo(float uiX, float uiY) throws Throwable {
        float uiScale = Global.getSettings().getScreenScaleMult();
        move(calibration.robotX(uiX * uiScale), calibration.robotY(windowYFromTop(uiY * uiScale)));
    }

    static void press(int buttonMask) throws Throwable {
        Object activeRobot = robot();
        mousePress.invoke(activeRobot, buttonMask);
    }

    static void release(int buttonMask) throws Throwable {
        Object activeRobot = robot();
        mouseRelease.invoke(activeRobot, buttonMask);
    }

    static void pressKey(int keyCode) throws Throwable {
        Object activeRobot = robot();
        keyPress.invoke(activeRobot, keyCode);
    }

    static void releaseKey(int keyCode) throws Throwable {
        Object activeRobot = robot();
        keyRelease.invoke(activeRobot, keyCode);
    }

    static void wheel(int notches) throws Throwable {
        Object activeRobot = robot();
        mouseWheel.invoke(activeRobot, notches);
    }

    private static void move(int robotX, int robotY) throws Throwable {
        Object activeRobot = robot();
        mouseMove.invoke(activeRobot, robotX, robotY);
    }

    private static float windowYFromTop(float windowYFromBottom) {
        return Display.getHeight() - windowYFromBottom;
    }

    private static Object robot() throws Throwable {
        if (robot == null) {
            Class<?> robotClass = Class.forName("java.awt.Robot");
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            mouseMove = lookup.findVirtual(robotClass, "mouseMove", MethodType.methodType(void.class, int.class, int.class));
            mousePress = lookup.findVirtual(robotClass, "mousePress", MethodType.methodType(void.class, int.class));
            mouseRelease = lookup.findVirtual(robotClass, "mouseRelease", MethodType.methodType(void.class, int.class));
            mouseWheel = lookup.findVirtual(robotClass, "mouseWheel", MethodType.methodType(void.class, int.class));
            keyPress = lookup.findVirtual(robotClass, "keyPress", MethodType.methodType(void.class, int.class));
            keyRelease = lookup.findVirtual(robotClass, "keyRelease", MethodType.methodType(void.class, int.class));
            robot = lookup.findConstructor(robotClass, MethodType.methodType(void.class)).invoke();
        }
        return robot;
    }
}
