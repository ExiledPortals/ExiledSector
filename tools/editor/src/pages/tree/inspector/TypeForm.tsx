import { Button, Checkbox, Group, Input, MultiSelect, Paper, Select, Stack, Text } from '@mantine/core';
import { IconPlus } from '@tabler/icons-react';
import type { Vocabulary } from '../../../api/vocabulary.ts';
import { moveItem } from '../../../shared/arrays.ts';
import { CommitNumberInput, CommitTextarea, CommitTextInput } from '../../../shared/CommitInputs.tsx';
import { EffectSelect } from '../../../shared/EffectSelect.tsx';
import { ImagePickButton } from '../../../shared/ImagePickButton.tsx';
import { RowButtons } from '../../../shared/MoveButtons.tsx';
import { TYPE_TAG_GROUPS } from '../model/tags.ts';
import { HULL_SIZE_OPTIONS, tierOf, UNLOCK_CONDITION_LABELS, type SkillType, type UnlockCondition } from '../model/types.ts';
import { TagGroupsField } from './TagGroupsField.tsx';
import type { TypeFormSection } from './typeFormSections.ts';

export type TypeRecipe = (type: SkillType) => void;

interface TypeFormProps {
  type: SkillType;
  onEdit: (recipe: TypeRecipe) => void;
  sections: TypeFormSection[];
  vocabulary: Vocabulary;
  allTypes: SkillType[];
  note?: string;
  idEditable?: boolean;
  onEditOptionType?: (typeId: string) => void;
}

function idListText(ids: string[] | undefined): string {
  return (ids ?? []).join(', ');
}

function idListFromText(text: string): string[] {
  return text
    .split(',')
    .map((part) => part.trim())
    .filter(Boolean);
}

function numberOrZero(value: number): number {
  return Number.isNaN(value) ? 0 : value;
}

function defaultEffectName(vocabulary: Vocabulary): string {
  return vocabulary.effectInfo.has('HULL_PERCENT') ? 'HULL_PERCENT' : (vocabulary.effectNames[0] ?? '');
}

function EffectRows({ type, onEdit, vocabulary, note }: Pick<TypeFormProps, 'type' | 'onEdit' | 'vocabulary' | 'note'>) {
  const effects = type.effects ?? [];
  return (
    <Input.Wrapper label={`Effects${note ?? ''}`}>
      <Stack gap={6} mt={4}>
        {effects.length === 0 ? (
          <Text size="xs" c="dimmed">
            No effects yet.
          </Text>
        ) : null}
        {effects.map((entry, index) => (
          <Group key={index} gap={6} wrap="nowrap" align="flex-end">
            <EffectSelect
              size="xs"
              style={{ flex: 2 }}
              vocabulary={vocabulary}
              value={entry.effect}
              onChange={(effect) =>
                onEdit((draft) => {
                  draft.effects![index].effect = effect;
                })
              }
            />
            <CommitNumberInput
              size="xs"
              style={{ flex: 1 }}
              aria-label="Magnitude"
              value={entry.magnitude}
              onCommit={(value) =>
                onEdit((draft) => {
                  draft.effects![index].magnitude = numberOrZero(value);
                })
              }
            />
            <RowButtons
              index={index}
              count={effects.length}
              onMove={(from, to) => onEdit((draft) => moveItem(draft.effects!, from, to))}
              onRemove={() => onEdit((draft) => {
                draft.effects!.splice(index, 1);
              })}
            />
          </Group>
        ))}
        <Button
          size="xs"
          variant="default"
          w="fit-content"
          leftSection={<IconPlus size={14} />}
          onClick={() => onEdit((draft) => {
            (draft.effects ??= []).push({ effect: defaultEffectName(vocabulary), magnitude: 10 });
          })}
        >
          Add effect
        </Button>
      </Stack>
    </Input.Wrapper>
  );
}

const HULL_SIZE_FIELDS = [
  { key: 'frigate', label: 'Frigate' },
  { key: 'destroyer', label: 'Destroyer' },
  { key: 'cruiser', label: 'Cruiser' },
  { key: 'capitalShip', label: 'Capital' },
] as const;

function HullSizeEffectRows({ type, onEdit, vocabulary, note }: Pick<TypeFormProps, 'type' | 'onEdit' | 'vocabulary' | 'note'>) {
  const entries = type.hullSizeEffects ?? [];
  return (
    <Input.Wrapper label={`Hull-size effects${note ?? ''}`}>
      <Stack gap={6} mt={4}>
        {entries.length === 0 ? (
          <Text size="xs" c="dimmed">
            No hull-size-dependent effects.
          </Text>
        ) : null}
        {entries.map((entry, index) => (
          <Paper key={index} withBorder p={6}>
            <Group gap={6} wrap="nowrap" align="flex-end">
              <EffectSelect
                size="xs"
                style={{ flex: 1 }}
                vocabulary={vocabulary}
                value={entry.effect}
                onChange={(effect) =>
                  onEdit((draft) => {
                    draft.hullSizeEffects![index].effect = effect;
                  })
                }
              />
              <RowButtons
                index={index}
                count={entries.length}
                onMove={(from, to) => onEdit((draft) => moveItem(draft.hullSizeEffects!, from, to))}
                onRemove={() => onEdit((draft) => {
                  draft.hullSizeEffects!.splice(index, 1);
                })}
              />
            </Group>
            <Group gap={6} grow mt={6}>
              {HULL_SIZE_FIELDS.map((field) => (
                <CommitNumberInput
                  key={field.key}
                  size="xs"
                  label={field.label}
                  value={entry[field.key]}
                  onCommit={(value) =>
                    onEdit((draft) => {
                      draft.hullSizeEffects![index][field.key] = numberOrZero(value);
                    })
                  }
                />
              ))}
            </Group>
          </Paper>
        ))}
        <Button
          size="xs"
          variant="default"
          w="fit-content"
          leftSection={<IconPlus size={14} />}
          onClick={() =>
            onEdit((draft) => {
              (draft.hullSizeEffects ??= []).push({ effect: defaultEffectName(vocabulary), frigate: 0, destroyer: 0, cruiser: 0, capitalShip: 0 });
            })
          }
        >
          Add hull-size effect
        </Button>
      </Stack>
    </Input.Wrapper>
  );
}

function UnlockConditionParameters({ condition, index, onEdit, vocabulary }: { condition: UnlockCondition; index: number; onEdit: TypeFormProps['onEdit']; vocabulary: Vocabulary }) {
  const edit = (change: (target: UnlockCondition) => void) => onEdit((draft) => change(draft.unlockConditions![index]));
  if (vocabulary.parameterlessUnlockConditionTypes.includes(condition.type)) {
    return null;
  }
  if (condition.type === 'characterStat') {
    return <CommitTextInput size="xs" style={{ flex: 2 }} label="Stat id" placeholder="has_neural_link" value={condition.statId ?? ''} onCommit={(text) => edit((target) => {
      target.statId = text.trim();
    })} />;
  }
  if (condition.type === 'minShipLevel') {
    return (
      <CommitNumberInput
        size="xs"
        style={{ flex: 2 }}
        label="Min level"
        value={condition.level ?? 1}
        onCommit={(value) => edit((target) => {
          target.level = Math.trunc(value) || 1;
        })}
      />
    );
  }
  if (condition.type === 'memoryFlag') {
    return <CommitTextInput size="xs" style={{ flex: 2 }} label="Memory key" placeholder="$playerCanUseGates" value={condition.key ?? ''} onCommit={(text) => edit((target) => {
      target.key = text.trim();
    })} />;
  }
  return (
    <>
      <Select
        size="xs"
        style={{ flex: 1 }}
        label="Category"
        data={vocabulary.blueprintCategories}
        value={condition.category || 'hullmod'}
        allowDeselect={false}
        onChange={(value) => edit((target) => {
          target.category = value ?? 'hullmod';
        })}
      />
      <CommitTextInput size="xs" style={{ flex: 1 }} label="Id" value={condition.id ?? ''} onCommit={(text) => edit((target) => {
        target.id = text.trim();
      })} />
    </>
  );
}

function UnlockConditionRows({ type, onEdit, vocabulary }: Pick<TypeFormProps, 'type' | 'onEdit' | 'vocabulary'>) {
  const conditions = type.unlockConditions ?? [];
  return (
    <Input.Wrapper label="Unlock conditions">
      <Stack gap={6} mt={4}>
        {conditions.length === 0 ? (
          <Text size="xs" c="dimmed">
            No unlock conditions.
          </Text>
        ) : null}
        {conditions.map((condition, index) => {
          const types = condition.type && !vocabulary.unlockConditionTypes.includes(condition.type) ? [condition.type, ...vocabulary.unlockConditionTypes] : vocabulary.unlockConditionTypes;
          return (
            <Group key={index} gap={6} wrap="nowrap" align="flex-end">
              <Select
                size="xs"
                style={{ flex: 1 }}
                label={`Condition ${index + 1}`}
                data={types.map((value) => ({ value, label: UNLOCK_CONDITION_LABELS[value] ?? value }))}
                value={condition.type}
                allowDeselect={false}
                onChange={(value) =>
                  onEdit((draft) => {
                    draft.unlockConditions![index].type = value ?? 'blueprint';
                  })
                }
              />
              <UnlockConditionParameters condition={condition} index={index} onEdit={onEdit} vocabulary={vocabulary} />
              <RowButtons
                index={index}
                count={conditions.length}
                onMove={(from, to) => onEdit((draft) => moveItem(draft.unlockConditions!, from, to))}
                onRemove={() => onEdit((draft) => {
                  draft.unlockConditions!.splice(index, 1);
                })}
              />
            </Group>
          );
        })}
        <Button
          size="xs"
          variant="default"
          w="fit-content"
          leftSection={<IconPlus size={14} />}
          onClick={() => onEdit((draft) => {
            (draft.unlockConditions ??= []).push({ type: 'blueprint', category: 'hullmod', id: '', statId: '', level: 1, key: '' });
          })}
        >
          Add condition
        </Button>
      </Stack>
    </Input.Wrapper>
  );
}

function OptionalOptions({ type, onEdit, allTypes, onEditOptionType }: Pick<TypeFormProps, 'type' | 'onEdit' | 'allTypes' | 'onEditOptionType'>) {
  const selected = type.optionalOptions ?? [];
  const candidates = allTypes.filter((candidate) => candidate.id !== type.id).map((candidate) => ({ value: candidate.id, label: candidate.name || candidate.id }));
  const known = new Set(allTypes.map((candidate) => candidate.id));
  return (
    <Stack gap={6}>
      <MultiSelect
        size="xs"
        label="Optional slot options"
        data={[...candidates, ...selected.filter((id) => !known.has(id)).map((id) => ({ value: id, label: `${id} (missing!)` }))]}
        value={selected}
        onChange={(chosen) =>
          onEdit((draft) => {
            draft.optionalOptions = chosen;
          })
        }
        searchable
        disabled={candidates.length === 0}
        placeholder={candidates.length === 0 ? 'No other types exist yet' : undefined}
      />
      {selected.length > 0 && onEditOptionType ? (
        <Stack gap={4}>
          {selected.map((optionId) => {
            const optionType = allTypes.find((candidate) => candidate.id === optionId);
            return (
              <Group key={optionId} justify="space-between" gap={6}>
                <Text size="xs" ff="monospace">
                  {optionType ? `${optionType.name || optionType.id} (${optionType.id})` : `${optionId} (missing!)`}
                </Text>
                {optionType ? (
                  <Button size="compact-xs" variant="default" onClick={() => onEditOptionType(optionId)}>
                    Edit
                  </Button>
                ) : null}
              </Group>
            );
          })}
        </Stack>
      ) : null}
    </Stack>
  );
}

export function TypeForm(props: TypeFormProps) {
  const { type, onEdit, sections, vocabulary, note = '', idEditable = false } = props;
  const renderSection = (section: TypeFormSection) => {
    switch (section) {
      case 'identity':
        return (
          <Stack gap={6} key={section}>
            <CommitTextInput size="xs" label="Id" value={type.id} disabled={!idEditable} onCommit={(text) => onEdit((draft) => {
              draft.id = text.trim();
            })} />
            <CommitTextInput size="xs" label="Display name" value={type.name ?? ''} onCommit={(text) => onEdit((draft) => {
              draft.name = text.trim();
            })} />
            <Group gap={6} align="flex-end" wrap="nowrap">
              <CommitTextInput size="xs" style={{ flex: 1 }} label="Icon path" value={type.icon ?? ''} onCommit={(text) => onEdit((draft) => {
                draft.icon = text.trim();
              })} />
              <ImagePickButton size="xs" onPicked={(path) => onEdit((draft) => {
                draft.icon = path;
              })} />
            </Group>
            <Select size="xs" label="Tier" data={vocabulary.tiers} value={tierOf(type)} allowDeselect={false} onChange={(value) => onEdit((draft) => {
              draft.tier = value ?? 'SMALL';
            })} />
          </Stack>
        );
      case 'tags':
        return (
          <TagGroupsField
            key={section}
            label={`Type tags (region tags go on nodes)${note}`}
            vocabulary={vocabulary}
            tags={type.tags ?? []}
            groups={TYPE_TAG_GROUPS}
            onChange={(tags) => onEdit((draft) => {
              draft.tags = tags;
            })}
          />
        );
      case 'effects':
        return <EffectRows key={section} type={type} onEdit={onEdit} vocabulary={vocabulary} note={note} />;
      case 'hullSizeEffects':
        return <HullSizeEffectRows key={section} type={type} onEdit={onEdit} vocabulary={vocabulary} note={note} />;
      case 'vanillaHullMod':
        return (
          <CommitTextInput
            key={section}
            size="xs"
            label="vanillaHullMod id"
            value={type.vanillaHullMod ?? ''}
            onCommit={(text) => onEdit((draft) => {
              draft.vanillaHullMod = text.trim();
            })}
          />
        );
      case 'unlockConditions':
        return <UnlockConditionRows key={section} type={type} onEdit={onEdit} vocabulary={vocabulary} />;
      case 'exclusivity':
        return (
          <Stack gap={6} key={section}>
            <CommitTextInput
              size="xs"
              label="Exclusive with hullmod ids (comma-separated)"
              value={idListText(type.exclusiveHullMods)}
              onCommit={(text) => onEdit((draft) => {
                draft.exclusiveHullMods = idListFromText(text);
              })}
            />
            <CommitTextInput
              size="xs"
              label="Counts as hullmod ids, effect suppressed (comma-separated)"
              value={idListText(type.phantomHullMods)}
              onCommit={(text) => onEdit((draft) => {
                draft.phantomHullMods = idListFromText(text);
              })}
            />
            <CommitTextInput
              size="xs"
              label="Exclusive with skill type ids (comma-separated)"
              value={idListText(type.exclusiveSkillTypes)}
              onCommit={(text) => onEdit((draft) => {
                draft.exclusiveSkillTypes = idListFromText(text);
              })}
            />
          </Stack>
        );
      case 'itemCost':
        return (
          <Input.Wrapper key={section} label={`Item cost${note}`}>
            <Group gap={6} wrap="nowrap" mt={2}>
              <CommitTextInput
                size="xs"
                style={{ flex: 2 }}
                placeholder="commodity id, e.g. lobster"
                value={type.itemCost?.itemId ?? ''}
                onCommit={(text) => onEdit((draft) => {
                  draft.itemCost = { itemId: text.trim(), quantity: draft.itemCost?.quantity || 0 };
                })}
              />
              <CommitNumberInput
                size="xs"
                style={{ flex: 1 }}
                placeholder="quantity"
                value={type.itemCost?.quantity || null}
                onCommit={(value) => onEdit((draft) => {
                  draft.itemCost = { itemId: draft.itemCost?.itemId ?? '', quantity: numberOrZero(value) };
                })}
              />
            </Group>
          </Input.Wrapper>
        );
      case 'temporary':
        return (
          <CommitNumberInput
            key={section}
            size="xs"
            label="Temporary after deployment (seconds)"
            placeholder="leave blank for always-on"
            value={type.temporaryAfterDeploymentSeconds || null}
            onCommit={(value) =>
              onEdit((draft) => {
                if (value > 0) {
                  draft.temporaryAfterDeploymentSeconds = value;
                } else {
                  delete draft.temporaryAfterDeploymentSeconds;
                }
              })
            }
          />
        );
      case 'requiredHullSizes':
        return (
          <Checkbox.Group
            key={section}
            label="Required hull sizes (none = any)"
            value={type.requiredHullSizes ?? []}
            onChange={(sizes) => onEdit((draft) => {
              draft.requiredHullSizes = HULL_SIZE_OPTIONS.map((option) => option.value).filter((size) => sizes.includes(size));
            })}
          >
            <Group gap="md" mt={4}>
              {HULL_SIZE_OPTIONS.map((option) => (
                <Checkbox key={option.value} size="xs" value={option.value} label={option.label} />
              ))}
            </Group>
          </Checkbox.Group>
        );
      case 'optionalOptions':
        return <OptionalOptions key={section} type={type} onEdit={onEdit} allTypes={props.allTypes} onEditOptionType={props.onEditOptionType} />;
      case 'texts':
        return (
          <Stack gap={6} key={section}>
            <CommitTextarea
              size="xs"
              label={`Flavour text (optional, grey line under the title)${note}`}
              value={type.flavour ?? ''}
              onCommit={(text) => onEdit((draft) => {
                draft.flavour = text.trim();
              })}
            />
            <CommitTextarea
              size="xs"
              label={`Description override (optional)${note}`}
              minRows={3}
              value={type.description ?? ''}
              onCommit={(text) => onEdit((draft) => {
                draft.description = text.trim();
              })}
            />
          </Stack>
        );
      case 'todo':
        return <CommitTextInput key={section} size="xs" label="Todo (optional)" value={type.todo ?? ''} onCommit={(text) => onEdit((draft) => {
          draft.todo = text.trim();
        })} />;
    }
  };
  return <Stack gap="sm">{sections.map(renderSection)}</Stack>;
}
