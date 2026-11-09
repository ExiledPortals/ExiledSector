import { Switch, Tooltip, useComputedColorScheme } from '@mantine/core';
import { rememberShowScenery } from './state/sceneryPreference.ts';
import { useTreeEditor, useTreeState } from './state/treeEditorContext.ts';

export function SceneryToggle() {
  const { store } = useTreeEditor();
  const showScenery = useTreeState((state) => state.showScenery);
  const darkScheme = useComputedColorScheme('dark') === 'dark';
  const toggle = (showSceneryNext: boolean) => {
    rememberShowScenery(showSceneryNext);
    store.setState({ showScenery: showSceneryNext });
  };
  return (
    <Tooltip label="The in-game scenery and starscape draw in the dark theme only" disabled={darkScheme}>
      <div>
        <Switch size="xs" label="Scenery" checked={showScenery && darkScheme} disabled={!darkScheme} onChange={(event) => toggle(event.currentTarget.checked)} />
      </div>
    </Tooltip>
  );
}
