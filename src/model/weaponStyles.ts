import type { CombatStyle, EquipmentItem } from './types';

const s = (name: string, type: CombatStyle['type'], stance: CombatStyle['stance']): CombatStyle =>
  ({ name, type, stance });

const STYLES_BY_CATEGORY: Record<string, CombatStyle[]> = {
  '2h Sword': [
    s('Chop', 'slash', 'Accurate'), s('Slash', 'slash', 'Aggressive'),
    s('Smash', 'crush', 'Aggressive'), s('Block', 'slash', 'Defensive')],
  Banner: [
    s('Lunge', 'stab', 'Accurate'), s('Swipe', 'slash', 'Aggressive'),
    s('Pound', 'crush', 'Controlled'), s('Block', 'stab', 'Defensive')],
  'Bladed Staff': [
    s('Jab', 'stab', 'Accurate'), s('Swipe', 'slash', 'Aggressive'),
    s('Fend', 'crush', 'Defensive'),
    s('Spell (Defensive)', 'magic', 'Defensive Autocast'), s('Spell', 'magic', 'Autocast')],
  Bow: [
    s('Accurate', 'ranged', 'Accurate'), s('Rapid', 'ranged', 'Rapid'),
    s('Longrange', 'ranged', 'Longrange')],
  Crossbow: [
    s('Accurate', 'ranged', 'Accurate'), s('Rapid', 'ranged', 'Rapid'),
    s('Longrange', 'ranged', 'Longrange')],
  Thrown: [
    s('Accurate', 'ranged', 'Accurate'), s('Rapid', 'ranged', 'Rapid'),
    s('Longrange', 'ranged', 'Longrange')],
  Gun: [s('Kick', 'crush', 'Aggressive')],
  Bulwark: [s('Pummel', 'crush', 'Accurate')],
  'Multi-Melee': [
    s('Poke', 'stab', 'Accurate'), s('Slash', 'slash', 'Aggressive'),
    s('Pound', 'crush', 'Aggressive'), s('Block', 'slash', 'Defensive')],
  Partisan: [
    s('Stab', 'stab', 'Accurate'), s('Lunge', 'stab', 'Aggressive'),
    s('Pound', 'crush', 'Aggressive'), s('Block', 'stab', 'Defensive')],
  Pickaxe: [
    s('Spike', 'stab', 'Accurate'), s('Impale', 'stab', 'Aggressive'),
    s('Smash', 'crush', 'Aggressive'), s('Block', 'stab', 'Defensive')],
  Polearm: [
    s('Jab', 'stab', 'Controlled'), s('Swipe', 'slash', 'Aggressive'),
    s('Fend', 'stab', 'Defensive')],
  'Powered Staff': [
    s('Accurate', 'magic', 'Accurate'), s('Longrange', 'magic', 'Longrange')],
  'Powered Wand': [
    s('Accurate', 'magic', 'Accurate'), s('Longrange', 'magic', 'Longrange')],
  Salamander: [
    s('Scorch', 'slash', 'Aggressive'), s('Flare', 'ranged', 'Rapid'),
    s('Blaze', 'magic', 'Defensive')],
  Chinchompas: [
    s('Short fuse', 'ranged', 'Accurate'), s('Medium fuse', 'ranged', 'Rapid'),
    s('Long fuse', 'ranged', 'Longrange')],
  Claw: [
    s('Chop', 'slash', 'Accurate'), s('Slash', 'slash', 'Aggressive'),
    s('Lunge', 'stab', 'Controlled'), s('Block', 'slash', 'Defensive')],
  Bludgeon: [
    s('Pound', 'crush', 'Aggressive'), s('Pummel', 'crush', 'Aggressive'),
    s('Smash', 'crush', 'Aggressive')],
  Blunt: [
    s('Pound', 'crush', 'Accurate'), s('Pummel', 'crush', 'Aggressive'),
    s('Block', 'crush', 'Defensive')],
  Polestaff: [
    s('Bash', 'crush', 'Accurate'), s('Pound', 'crush', 'Aggressive'),
    s('Block', 'crush', 'Defensive')],
  Spiked: [
    s('Pound', 'crush', 'Accurate'), s('Pummel', 'crush', 'Aggressive'),
    s('Spike', 'stab', 'Controlled'), s('Block', 'crush', 'Defensive')],
  Staff: [
    s('Bash', 'crush', 'Accurate'), s('Pound', 'crush', 'Aggressive'),
    s('Focus', 'crush', 'Defensive'),
    s('Spell (Defensive)', 'magic', 'Defensive Autocast'), s('Spell', 'magic', 'Autocast')],
  Axe: [
    s('Chop', 'slash', 'Accurate'), s('Hack', 'slash', 'Aggressive'),
    s('Smash', 'crush', 'Aggressive'), s('Block', 'slash', 'Defensive')],
  Scythe: [
    s('Reap', 'slash', 'Accurate'), s('Chop', 'slash', 'Aggressive'),
    s('Jab', 'crush', 'Aggressive'), s('Block', 'slash', 'Defensive')],
  'Slash Sword': [
    s('Chop', 'slash', 'Accurate'), s('Slash', 'slash', 'Aggressive'),
    s('Lunge', 'stab', 'Controlled'), s('Block', 'slash', 'Defensive')],
  Spear: [
    s('Lunge', 'stab', 'Controlled'), s('Swipe', 'slash', 'Controlled'),
    s('Pound', 'crush', 'Controlled'), s('Block', 'stab', 'Defensive')],
  'Stab Sword': [
    s('Stab', 'stab', 'Accurate'), s('Lunge', 'stab', 'Aggressive'),
    s('Slash', 'slash', 'Aggressive'), s('Block', 'stab', 'Defensive')],
  Whip: [
    s('Flick', 'slash', 'Accurate'), s('Lash', 'slash', 'Controlled'),
    s('Deflect', 'slash', 'Defensive')],
  Flail: [
    s('Chop', 'slash', 'Accurate'), s('Slash', 'slash', 'Aggressive'),
    s('Block', 'slash', 'Defensive')],
};

const UNARMED: CombatStyle[] = [
  s('Punch', 'crush', 'Accurate'), s('Kick', 'crush', 'Aggressive'),
  s('Block', 'crush', 'Defensive')];

export const stylesForCategory = (category: string | null | undefined): CombatStyle[] => {
  const base = (category && STYLES_BY_CATEGORY[category]) || UNARMED;
  return [...base, s('Spell (Manual cast)', 'magic', 'Manual Cast')];
};

export const stylesForWeapon = (weapon: EquipmentItem | undefined): CombatStyle[] =>
  stylesForCategory(weapon?.category);
