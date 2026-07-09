import { describe, expect, it } from 'vitest';
import { stylesForCategory, stylesForWeapon } from '../src/model/weaponStyles';

describe('weapon styles', () => {
  it('whip only has slash styles', () => {
    const styles = stylesForCategory('Whip');
    expect(styles).toHaveLength(4); // Flick, Lash, Deflect + manual cast
    expect(styles.filter((s) => s.stance !== 'Manual Cast')
      .every((s) => s.type === 'slash')).toBe(true);
    expect(styles[0].name).toBe('Flick');
    expect(styles[1].stance).toBe('Controlled');
  });

  it('stab sword has slash option', () => {
    const styles = stylesForCategory('Stab Sword');
    expect(styles.some((s) => s.type === 'slash' && s.name === 'Slash')).toBe(true);
    expect(styles[0].type).toBe('stab');
  });

  it('unarmed falls back to punch/kick/block', () => {
    const styles = stylesForWeapon(undefined);
    expect(styles[0].name).toBe('Punch');
    expect(styles[0].type).toBe('crush');
  });

  it('staff has autocast styles', () => {
    const styles = stylesForCategory('Staff');
    expect(styles.some((s) => s.stance === 'Autocast')).toBe(true);
    expect(styles.some((s) => s.stance === 'Defensive Autocast')).toBe(true);
    expect(styles.some((s) => s.stance === 'Manual Cast')).toBe(true);
  });
});
