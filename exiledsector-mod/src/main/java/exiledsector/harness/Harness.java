package exiledsector.harness;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.ui.UIPanelAPI;
import org.apache.log4j.Logger;
import org.json.JSONException;

import java.io.IOException;

public final class Harness {

    static final String SAVE_PROPERTY = "exiledsector.harness.save";
    static final String NEW_GAME_PROPERTY = "exiledsector.harness.newGame";
    static final String NEW_GAME = "a new game";
    static final String LOG_TAG = "[ExiledSector] [harness] ";
    private static final Logger LOG = Logger.getLogger(Harness.class);
    private static final String IDLE_WHILE_UNFOCUSED_SETTING = "idleWhileWindowNotVisible";
    private static final int FAILED_LOAD_EXIT_CODE = 2;
    private static Harness launched;

    private final String saveDir;
    private final HarnessLog log;
    private final HarnessResults results;
    private HarnessScenario scenario;
    private HarnessRun run;
    private boolean titleScreenHandled;
    private boolean scenarioStarted;

    private Harness(String saveDir) {
        this.saveDir = saveDir;
        this.log = new HarnessLog();
        this.results = new HarnessResults(saveDir, log);
    }

    public static void startIfLaunched() {
        String saveDir = System.getProperty(SAVE_PROPERTY);
        boolean newGame = Boolean.getBoolean(NEW_GAME_PROPERTY);
        if (!newGame && (saveDir == null || saveDir.isBlank())) {
            return;
        }
        Harness harness = new Harness(newGame ? NEW_GAME : saveDir);
        Logger.getRootLogger().addAppender(harness.log);
        LOG.info(LOG_TAG + "launched to " + (newGame ? "start " + NEW_GAME : "load " + saveDir));
        Global.getSettings().setBoolean(IDLE_WHILE_UNFOCUSED_SETTING, false);
        try {
            harness.scenario = HarnessScenario.parse(HarnessFiles.readScenario(), SuiteCase.Filter.fromSystemProperties(),
                    modId -> Global.getSettings().getModManager().isModEnabled(modId));
            LOG.info(LOG_TAG + "scenario has " + harness.scenario.steps().size() + " steps");
        } catch (IOException | JSONException | IllegalArgumentException e) {
            LOG.error(LOG_TAG + "could not read the scenario", e);
            harness.results.failRun("Could not read the scenario: " + e.getMessage());
        }
        launched = harness;
    }

    public static void runIfLaunched(SectorAPI sector) {
        Harness harness = launched;
        if (harness == null || harness.scenario == null || harness.scenarioStarted) {
            return;
        }
        harness.scenarioStarted = true;
        NewGameSetup newGame = harness.scenario.newGame();
        if (newGame != null) {
            FullName characterName = new FullName(newGame.characterName(), "", FullName.Gender.ANY);
            sector.getCharacterData().setName(newGame.characterName(), FullName.Gender.ANY);
            sector.getPlayerPerson().setName(characterName);
        }
        LOG.info(LOG_TAG + "campaign ready, running the scenario");
        harness.run = new HarnessRun(harness.scenario, harness.log, harness.results, Harness::quit);
        sector.addTransientScript(harness.run);
    }

    static void advanceInCombat() {
        HarnessRun activeRun = activeRun();
        if (activeRun != null) {
            activeRun.advance(0f);
        }
    }

    static void renderInCombat() {
        HarnessRun activeRun = activeRun();
        if (activeRun != null) {
            activeRun.renderInCombat();
        }
    }

    private static HarnessRun activeRun() {
        Harness harness = launched;
        return harness == null ? null : harness.run;
    }

    static void atTitleScreen() {
        Harness harness = launched;
        if (harness == null || harness.titleScreenHandled) {
            return;
        }
        harness.titleScreenHandled = true;
        if (harness.scenario == null) {
            quit();
            return;
        }
        NewGameSetup newGame = harness.scenario.newGame();
        if (newGame != null) {
            harness.driveTitleScreen(new TitleNewGameDriver(newGame, harness::failNewGame));
        } else if (NEW_GAME.equals(harness.saveDir)) {
            harness.results.failRun("The run was launched for a new game, but the scenario has no newGame");
            stopAfterFailedLoad();
        } else {
            harness.driveTitleScreen(new TitleLoadTrigger(harness::loadSave));
        }
    }

    // java:S1181: the title screen is reached by reflection, and any failure must end the run with a result
    @SuppressWarnings("java:S1181")
    private void driveTitleScreen(CustomUIPanelPlugin titleDriver) {
        try {
            UIPanelAPI titlePanel = TitleSaveLoader.titleScreenPanel();
            if (titlePanel == null) {
                throw new IllegalStateException("the title screen has no screen panel");
            }
            titlePanel.addComponent(Global.getSettings().createCustom(0f, 0f, titleDriver)).inTL(0f, 0f);
        } catch (Throwable e) {
            LOG.error(LOG_TAG + "could not hook the title screen", e);
            results.failRun("Could not hook the title screen: " + e);
            stopAfterFailedLoad();
        }
    }

    private void failNewGame(String failure) {
        results.failRun("Could not start a new game: " + failure);
        stopAfterFailedLoad();
    }

    // java:S1181: the save loader reaches engine internals by reflection, and any failure must end the run with a result
    @SuppressWarnings("java:S1181")
    private void loadSave() {
        LOG.info(LOG_TAG + "loading " + saveDir);
        String loadError;
        try {
            loadError = TitleSaveLoader.load(saveDir);
        } catch (Throwable e) {
            LOG.error(LOG_TAG + "could not load the save", e);
            loadError = e.toString();
        }
        if (loadError != null) {
            results.failRun("Could not load the save: " + loadError);
            stopAfterFailedLoad();
        }
    }

    private static void stopAfterFailedLoad() {
        stop(FAILED_LOAD_EXIT_CODE);
    }

    static void quit() {
        stop(0);
    }

    // java:S1147: the results are written and the run's save is a throwaway copy, so nothing is lost and no exit screen can hang the run
    @SuppressWarnings("java:S1147")
    private static void stop(int exitCode) {
        LOG.info(LOG_TAG + "stopping the game");
        System.exit(exitCode);
    }

    static boolean launched() {
        return launched != null;
    }

    static String describe(Object screen) {
        return screen == null ? ExpectScreenStep.NONE : screen.getClass().getSimpleName();
    }
}
