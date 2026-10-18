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
