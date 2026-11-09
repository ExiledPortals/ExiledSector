import { fireEvent, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { renderWithProviders } from '../test/renderWithProviders.tsx';
import { startTestServer, type TestServer } from '../test/testServer.ts';
import { TooltipCodeButton } from './TooltipCodeButton.tsx';

let server: TestServer;

beforeEach(() => {
  server = startTestServer([]);
});

afterEach(async () => {
  await server.close();
});

describe('tooltip code button', () => {
  it('shows that no copy exists yet and reports why a refresh could not copy the code', async () => {
    renderWithProviders(<TooltipCodeButton />);
    const status = await screen.findByTestId('tooltip-code-status');
    await waitFor(() => expect(status.textContent).toBe('No copy yet'));

    fireEvent.click(screen.getByRole('button', { name: 'Refresh tooltip code' }));

    await waitFor(() => expect(status.textContent).toContain('needs JDK 17 and Maven'));
  });
});
