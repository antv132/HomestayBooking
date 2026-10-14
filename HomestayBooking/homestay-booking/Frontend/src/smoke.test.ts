import { describe, expect, it } from 'vitest';

/** Smoke test — proves Vitest is wired (TD-012). Expand suites later. */
describe('vitest wiring', () => {
  it('runs', () => {
    expect(1 + 1).toBe(2);
  });
});
