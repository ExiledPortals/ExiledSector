package exiledsector.harness;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.characters.CharacterCreationData;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.TextFieldAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import exiledsector.ui.refit.UiReflection;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

final class TitleNewGameDriver extends BaseCustomUIPanelPlugin {

    static final String NEW_GAME_BUTTON = "New Game";
    static final String CONTINUE_OPTION = "CONTINUE_CHOICES";
    static final int MAX_FRAMES_PER_STAGE = 900;
    private static final String TITLE_WIDGET = "titleWidget";
    private static final String CHARACTER_DATA_KEY = "$characterData";
    private static final String OBJECT_TEXT_METHOD = "toString";
    private static final String STATS_DIALOG_METHOD = "getGameData";
    private static final int DISMISS_WITH_START = 0;
    private static final Logger LOG = Logger.getLogger(TitleNewGameDriver.class);

    private enum Stage { PRESS_NEW_GAME, PICK_CHOICES, CONFIRM_CHARACTER, STARTED }

    private final NewGameSetup setup;
    private final Consumer<String> failStart;
    private Stage stage = Stage.PRESS_NEW_GAME;
    private int stageFrames;
    private InteractionDialogAPI newGameDialog;
    private CharacterCreationData characterData;

    TitleNewGameDriver(NewGameSetup setup, Consumer<String> failStart) {
        this.setup = setup;
        this.failStart = failStart;
    }

    // java:S1181: the title screen is driven by reflection, and any failure must end the run with a result
    @SuppressWarnings("java:S1181")
    @Override
    public void advance(float amount) {
        if (stage == Stage.STARTED) {
            return;
        }
        if (Global.getCurrentState() != GameState.TITLE) {
            stage = Stage.STARTED;
            return;
        }
        try {
            advanceStage(TitleSaveLoader.titleScreenPanel());
        } catch (Throwable e) {
            stage = Stage.STARTED;
            LOG.error(Harness.LOG_TAG + "could not start a new game", e);
            failStart.accept(e.toString());
        }
    }

    private void advanceStage(UIPanelAPI titlePanel) throws Throwable {
        stageFrames++;
        if (stageFrames > MAX_FRAMES_PER_STAGE) {
            throw new IllegalStateException(stuckDetail(titlePanel));
        }
        switch (stage) {
            case PRESS_NEW_GAME -> {
                ButtonAPI newGameButton = UiTree.button(mainMenu(), NEW_GAME_BUTTON);
                if (newGameButton != null) {
                    UiTree.press(newGameButton);
                    nextStage(Stage.PICK_CHOICES);
                }
            }
            case PICK_CHOICES -> {
                newGameDialog = UiTree.find(titlePanel, InteractionDialogAPI.class);
                if (newGameDialog != null) {
                    pickChoices();
                    nextStage(Stage.CONFIRM_CHARACTER);
                }
            }
            case CONFIRM_CHARACTER -> {
                Object statsDialog = UiTree.find(titlePanel, component -> UiReflection.hasMethod(component, STATS_DIALOG_METHOD, 0));
                if (statsDialog != null) {
                    stage = Stage.STARTED;
                    TextFieldAPI nameField = UiTree.find(statsDialog, TextFieldAPI.class);
                    if (nameField != null) {
                        nameField.setText(setup.characterName());
                    }
                    characterData.getCharacterData().setName(setup.characterName(), FullName.Gender.ANY);
                    LOG.info(Harness.LOG_TAG + "starting the new game as " + setup.characterName()
                            + (nameField == null ? " with no name field on the stats dialog" : ""));
                    UiReflection.call(statsDialog, "dismiss", DISMISS_WITH_START);
                }
            }
            case STARTED -> {
            }
        }
    }

    private static Object mainMenu() throws Throwable {
        Object titleWidget = UiReflection.fieldNamed(GameStates.current(), TITLE_WIDGET, Object.class);
        if (titleWidget == null) {
            throw new IllegalStateException("the title screen has no title widget");
        }
        return UiReflection.call(titleWidget, "getMainMenu");
    }

    private void nextStage(Stage next) {
        stage = next;
        stageFrames = 0;
    }

    private void pickChoices() throws Throwable {
        characterData = (CharacterCreationData) newGameDialog.getInteractionTarget().getMemoryWithoutUpdate().get(CHARACTER_DATA_KEY);
        if (characterData == null) {
            throw new IllegalStateException("the new game dialog has no character data");
        }
        characterData.getCharacterData().setName(setup.characterName(), FullName.Gender.ANY);
        InteractionDialogPlugin plugin = newGameDialog.getPlugin();
        plugin.optionSelected(null, continueOption(plugin));
        for (String choice : setup.choices()) {
            LOG.info(Harness.LOG_TAG + "new game offers " + offeredOptions(newGameDialog) + "; choosing " + choice
                    + (newGameDialog.getOptionPanel().hasOption(choice) ? "" : ", which the dialog doesn't show"));
            plugin.optionSelected(null, choice);
        }    }

    private static Object continueOption(InteractionDialogPlugin plugin) throws ClassNotFoundException {
        Class<?> optionIds = Class.forName(plugin.getClass().getName() + "$OptionId");
        for (Object optionId : optionIds.getEnumConstants()) {
            if (((Enum<?>) optionId).name().equals(CONTINUE_OPTION)) {
                return optionId;
            }
        }
        throw new IllegalStateException(plugin.getClass().getName() + " has no " + CONTINUE_OPTION + " option");
    }

    private String stuckDetail(UIPanelAPI titlePanel) throws Throwable {
        return switch (stage) {
            case PRESS_NEW_GAME -> "the title screen never showed a \"" + NEW_GAME_BUTTON + "\" button; its main menu shows "
                    + UiTree.buttonTexts(mainMenu()) + " and the screen shows " + UiTree.buttonTexts(titlePanel);
            case PICK_CHOICES -> "the new game dialog never opened";
            case CONFIRM_CHARACTER -> characterData != null && characterData.isDone()
                    ? "the character stats dialog never opened after the new game choices " + setup.choices()
                    : "the new game choices " + setup.choices() + " never finished character creation; the dialog offers "
                    + offeredOptions(newGameDialog);
            case STARTED -> "the new game already started";
        };
    }

    static List<String> offeredOptions(InteractionDialogAPI dialog) throws Throwable {
        List<String> offered = new ArrayList<>();
        if (dialog == null) {
            return offered;
        }
        for (Object option : dialog.getOptionPanel().getSavedOptionList()) {
            offered.add(describeOption(option));
        }
        return offered;
    }

    private static String describeOption(Object option) throws Throwable {
        for (String methodName : UiReflection.noArgMethodNamesReturning(option.getClass(), String.class)) {
            if (!OBJECT_TEXT_METHOD.equals(methodName) && UiReflection.call(option, methodName) instanceof String text) {
                return text;
            }
        }
        return String.valueOf(option);
    }
}
