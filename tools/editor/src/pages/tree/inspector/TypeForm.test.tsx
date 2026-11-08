import { fireEvent, screen } from '@testing-library/react';
import { produce } from 'immer';
import { useState } from 'react';
import { describe, expect, it } from 'vitest';
import { buildVocabulary } from '../../../api/vocabulary.ts';
import { renderWithProviders } from '../../../test/renderWithProviders.tsx';
import type { SkillType } from '../model/types.ts';
import { TypeForm, type TypeRecipe } from './TypeForm.tsx';
import { DIALOG_TYPE_SECTIONS } from './typeFormSections.ts';

const vocabulary = buildVocabulary({
  effects: ['HULL_PERCENT', 'SHIELD_UPKEEP_MULT', 'SHIELD_SOFT_FLUX_CONVERSION_PERCENT'].map((name) => ({ name, statMode: 'PERCENT', lowerIsBetter: false })),
  tags: { theme: [], volume: [], requirement: [] },
  tiers: ['SMALL', 'NOTABLE'],
  unlockConditionTypes: ['blueprint'],
  blueprintCategories: ['hullmod'],
  itemTypes: [],
  itemKinds: [],
  itemUnlocks: [],
});

const shieldType: SkillType = {
  id: 'stabilized_shields',
  name: 'Stabilized Shields',
  icon: '',
  tier: 'NOTABLE',
  effects: [{ effect: 'SHIELD_UPKEEP_MULT', magnitude: -40 }],
};

function renderForm(initial: SkillType) {
  const state = { current: initial };
  function Harness() {
    const [type, setType] = useState(initial);
    state.current = type;
    const edit = (recipe: TypeRecipe) => setType((previous) => produce(previous, recipe));
    return <TypeForm type={type} onEdit={edit} sections={DIALOG_TYPE_SECTIONS} vocabulary={vocabulary} allTypes={[type]} />;
  }
  renderWithProviders(<Harness />);
  return state;
}

function magnitudeInputs(): HTMLInputElement[] {
  return screen.getAllByLabelText('Magnitude') as HTMLInputElement[];
}

describe('type form integrated effects', () => {
  it('starts the integrated effects as a copy of the normal ones and edits them separately', () => {
    const state = renderForm(shieldType);
    const integrable = screen.getByLabelText(/^Can be integrated/) as HTMLInputElement;
    expect(integrable.checked).toBe(false);
    expect(screen.queryByText('Integrated effects')).toBeNull();

    fireEvent.click(integrable);
    expect(screen.getByText('Integrated effects')).toBeTruthy();
    expect(state.current.integrated?.effects).toEqual(shieldType.effects);

    const integratedMagnitude = magnitudeInputs()[1];
    fireEvent.change(integratedMagnitude, { target: { value: '-50' } });
    fireEvent.blur(integratedMagnitude);
    expect(state.current.integrated?.effects?.[0].magnitude).toBe(-50);
    expect(state.current.effects).toEqual(shieldType.effects);
  });

  it('drops the integrated effects when integration is turned off', () => {
    const state = renderForm({ ...shieldType, integrated: { effects: [{ effect: 'SHIELD_SOFT_FLUX_CONVERSION_PERCENT', magnitude: 10 }] } });
    const integrable = screen.getByLabelText(/^Can be integrated/) as HTMLInputElement;
    expect(integrable.checked).toBe(true);

    fireEvent.click(integrable);
    expect(state.current.integrated).toBeUndefined();
    expect(screen.queryByText('Integrated effects')).toBeNull();
  });
});
