import { cleanup } from '@testing-library/react';
import { afterEach } from 'vitest';

class TestResizeObserver {
  private readonly callback: ResizeObserverCallback;

  constructor(callback: ResizeObserverCallback) {
    this.callback = callback;
  }

  observe(): void {
    this.callback([], this);
  }

  unobserve(): void {
    return;
  }

  disconnect(): void {
    return;
  }
}

Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: (query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: () => undefined,
    removeListener: () => undefined,
    addEventListener: () => undefined,
    removeEventListener: () => undefined,
    dispatchEvent: () => false,
  }),
});

class TestPointerEvent extends MouseEvent {
  readonly pointerId: number;

  constructor(type: string, init: PointerEventInit = {}) {
    super(type, init);
    this.pointerId = init.pointerId ?? 1;
  }
}

if (!('PointerEvent' in window)) {
  Object.defineProperty(window, 'PointerEvent', { writable: true, value: TestPointerEvent });
}

window.ResizeObserver = TestResizeObserver;
window.HTMLElement.prototype.scrollIntoView = () => undefined;
Element.prototype.setPointerCapture = () => undefined;
Element.prototype.releasePointerCapture = () => undefined;
Element.prototype.hasPointerCapture = () => false;

afterEach(() => {
  cleanup();
});
