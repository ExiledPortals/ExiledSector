import type { ThemeTagScope, Vocabulary } from '../api/vocabulary.ts';

export const THEME_TAG_SCOPE_LABELS: Record<ThemeTagScope, string> = { ship: 'ship', star_chart: 'star chart' };

export function themeTagsFor(vocabulary: Vocabulary, scope: ThemeTagScope): string[] {
  return [...vocabulary.themeTagInfo.values()].filter((tag) => tag.scopes.includes(scope)).map((tag) => tag.id);
}

export function ancestorsOf(vocabulary: Vocabulary, tag: string): string[] {
  const ancestors: string[] = [];
  let parent = vocabulary.themeTagInfo.get(tag)?.parent ?? null;
  while (parent !== null && !ancestors.includes(parent)) {
    ancestors.push(parent);
    parent = vocabulary.themeTagInfo.get(parent)?.parent ?? null;
  }
  return ancestors;
}

export function ancestorsListedBeside(vocabulary: Vocabulary, tag: string, listedTags: readonly string[]): string[] {
  return ancestorsOf(vocabulary, tag).filter((ancestor) => listedTags.includes(ancestor));
}

export function themeTagIssues(vocabulary: Vocabulary, scope: ThemeTagScope, tags: readonly string[]): string[] {
  const issues: string[] = [];
  for (const tag of tags) {
    const info = vocabulary.themeTagInfo.get(tag);
    if (!info) {
      issues.push(`tag "${tag}" is not a theme tag`);
    } else if (!info.scopes.includes(scope)) {
      issues.push(`tag "${tag}" is not a ${THEME_TAG_SCOPE_LABELS[scope]} theme tag`);
    }
    for (const ancestor of ancestorsListedBeside(vocabulary, tag, tags)) {
      issues.push(`tag "${tag}" sits beside its ancestor "${ancestor}", which it already counts as`);
    }
  }
  return issues;
}
