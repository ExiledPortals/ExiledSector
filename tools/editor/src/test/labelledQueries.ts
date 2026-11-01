import { waitFor } from '@testing-library/react';

const FIND_TIMEOUT_MS = 30000;

export function queryLabelled(text: string, container: ParentNode = document): HTMLElement | null {
  const byAriaLabel = [...container.querySelectorAll<HTMLElement>('[aria-label]')].find((element) => element.getAttribute('aria-label') === text);
  if (byAriaLabel) {
    return byAriaLabel;
  }
  const label = [...container.querySelectorAll('label')].find((candidate) => candidate.textContent?.trim() === text);
  return label?.htmlFor ? document.getElementById(label.htmlFor) : null;
}

export function getLabelled(text: string, container: ParentNode = document): HTMLElement {
  const byAriaLabel = [...container.querySelectorAll<HTMLElement>('[aria-label]')].filter((element) => element.getAttribute('aria-label') === text);
  const byLabel = [...container.querySelectorAll('label')]
    .filter((candidate) => candidate.textContent?.trim() === text && candidate.htmlFor)
    .map((label) => document.getElementById(label.htmlFor))
    .filter((element): element is HTMLElement => element !== null);
  const matches = new Set([...byAriaLabel, ...byLabel]);
  if (matches.size !== 1) {
    throw new Error(`${matches.size === 0 ? 'Nothing is' : `${matches.size} elements are`} labelled "${text}"`);
  }
  return [...matches][0];
}

export function findLabelled(text: string, container: ParentNode = document): Promise<HTMLElement> {
  return waitFor(
    () => {
      const element = queryLabelled(text, container);
      if (!element) {
        throw new Error(`Nothing is labelled "${text}"`);
      }
      return element;
    },
    { timeout: FIND_TIMEOUT_MS },
  );
}
