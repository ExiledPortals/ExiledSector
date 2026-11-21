package exiledsector.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ShownScreensTest {

    @AfterEach
    void forget() {
        ShownScreens.clear();
    }

    @Test
    void theLatestShownScreenIsOnTopUntilItIsHidden() {
        Object atlas = new Object();
        Object atlasTree = new Object();
        Runnable closeAtlasTree = () -> { };
        ShownScreens.shown(atlas, () -> { });
        ShownScreens.shown(atlasTree, closeAtlasTree);

        assertSame(atlasTree, ShownScreens.top().screen());
        assertSame(closeAtlasTree, ShownScreens.top().close());
        ShownScreens.hidden(atlasTree);
        assertSame(atlas, ShownScreens.top().screen());
    }

    @Test
    void showingAScreenAgainMovesItToTheTopOnce() {
        Object atlas = new Object();
        Object atlasTree = new Object();
        ShownScreens.shown(atlas, () -> { });
        ShownScreens.shown(atlasTree, () -> { });
        ShownScreens.shown(atlas, () -> { });

        assertSame(atlas, ShownScreens.top().screen());
        ShownScreens.hidden(atlas);
        assertSame(atlasTree, ShownScreens.top().screen());
        ShownScreens.hidden(atlasTree);
        assertNull(ShownScreens.top());
    }
}
