package exiledsector.harness;

import exiledsector.ui.refit.UiReflection;

final class GameStates {

    private static final String APP_DRIVER = "com.fs.state.AppDriver";

    private GameStates() {
    }

    static Object appDriver() throws Throwable {
        return UiReflection.callStatic(Class.forName(APP_DRIVER), "getInstance");
    }

    static Object current() throws Throwable {
        return UiReflection.call(appDriver(), "getCurrentState");
    }
}
