import { Alert, Button, Code, Group, List, Stack, Text, Textarea } from '@mantine/core';
import { IconAlertTriangle, IconArrowBackUp, IconInfoCircle } from '@tabler/icons-react';
import { MARKUP_COLOURS } from '../../shared/tooltipMarkup.ts';
import { analyseTemplate, stringIssues, TEMPLATE_STYLES, type StringEntry, type StringsModel } from './model/strings.ts';
import classes from './TextPage.module.css';

function TemplatePreview({ value }: { value: string }) {
  const { segments } = analyseTemplate(value);
  return (
    <div className={classes.templatePreview} data-testid="template-preview">
      {segments.map((segment, index) => {
        const colour = segment.style ? MARKUP_COLOURS[segment.style] : undefined;
        return segment.kind === 'placeholder' ? (
          <span key={index} className={classes.placeholder} style={{ color: colour }}>
            {`{${segment.name}}`}
          </span>
        ) : (
          <span key={index} style={{ color: colour }}>
            {segment.text}
          </span>
        );
      })}
    </div>
  );
}

interface StringEditorProps {
  entry: StringEntry;
  model: StringsModel;
  onChange: (value: string) => void;
}

export function StringEditor({ entry, model, onChange }: StringEditorProps) {
  const { errors, warnings } = stringIssues(entry, model);
  const edited = entry.value !== entry.savedValue;
  return (
    <Stack gap="sm">
      <Group justify="space-between" wrap="nowrap">
        <Code fz="sm">{entry.key}</Code>
        <Button size="xs" variant="default" leftSection={<IconArrowBackUp size={14} />} disabled={!edited} onClick={() => onChange(entry.savedValue)}>
          Revert to saved
        </Button>
      </Group>
      <Textarea
        autosize
        minRows={2}
        maxRows={14}
        value={entry.value}
        onChange={(event) => onChange(event.currentTarget.value)}
        aria-label="English text"
      />
      {errors.length > 0 ? (
        <Alert color="red" icon={<IconAlertTriangle size={16} />} p="xs">
          <List size="sm">
            {errors.map((error) => (
              <List.Item key={error}>{error}</List.Item>
            ))}
          </List>
        </Alert>
      ) : null}
      {warnings.length > 0 ? (
        <Alert color="yellow" icon={<IconInfoCircle size={16} />} p="xs">
          <List size="sm">
            {warnings.map((warning) => (
              <List.Item key={warning}>{warning}</List.Item>
            ))}
          </List>
        </Alert>
      ) : null}
      <TemplatePreview value={entry.value} />
      {edited ? (
        <Text size="xs" c="dimmed">
          Saved text: {entry.savedValue}
        </Text>
      ) : null}
      <Text size="xs" c="dimmed">
        {'{name}'} values are filled in by the game. Highlight tags: {TEMPLATE_STYLES.map((style) => `<${style}>`).join(' ')}; close each one before opening the next.
        Keys ending in .one and .other are the singular and plural forms.
      </Text>
    </Stack>
  );
}
