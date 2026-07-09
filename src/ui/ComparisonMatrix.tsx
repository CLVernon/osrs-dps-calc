import { useMemo, useState } from 'react';
import { useStore } from '../state/store';
import { calculate, type DpsResult } from '../calc/calc';
import { displayName } from '../model/types';
import { formatSeconds, scaleSuffix } from './shared';

export const ComparisonMatrix = () => {
  const repo = useStore((s) => s.repo);
  const setups = useStore((s) => s.setups);
  const targets = useStore((s) => s.targets);
  const character = useStore((s) => s.character);
  const [detail, setDetail] = useState<string>('');

  const rows = useMemo(() => {
    if (!repo) return [];
    const ctx = { character, allSpells: repo.spells };
    return setups.map((setup) => {
      const results = targets.map((monster) => calculate(setup, monster, ctx));
      const totalTtk = results.reduce((sum, r) => sum + r.ttkSeconds, 0);
      return { setup, results, totalTtk };
    });
  }, [repo, setups, targets, character]);

  const bestDps = useMemo(() => targets.map((_, col) =>
    Math.max(...rows.map((r) => r.results[col]?.dps ?? 0))), [rows, targets]);

  const bestTotal = useMemo(() => {
    const finite = rows.map((r) => r.totalTtk).filter(Number.isFinite);
    return finite.length ? Math.min(...finite) : Infinity;
  }, [rows]);

  if (!repo || setups.length === 0 || targets.length === 0) {
    return (
      <div className="matrix-wrap subtle">
        Add gear setups and targets to compare DPS.
      </div>
    );
  }

  const showDetail = (setupName: string, monsterName: string, r: DpsResult) => {
    const parts = [
      `${setupName} vs ${monsterName}: max hit ${r.maxHit}`,
      `accuracy ${(r.accuracy * 100).toFixed(1)}%`,
      `TTK ${formatSeconds(r.ttkSeconds)}`,
    ];
    setDetail(parts.join(', ') + (r.notes.length ? `  |  ${r.notes.join('; ')}` : ''));
  };

  return (
    <div className="matrix-wrap">
      <table className="matrix">
        <thead>
          <tr>
            <th>Setup</th>
            {targets.map((m, i) => (
              <th key={i}>{displayName(m)}{scaleSuffix(m)}</th>
            ))}
            {targets.length > 1 && <th>Total TTK</th>}
          </tr>
        </thead>
        <tbody>
          {rows.map(({ setup, results, totalTtk }) => (
            <tr key={setup.uid}>
              <td>{setup.name}</td>
              {results.map((r, col) => {
                const isBest = bestDps[col] > 0 && r.dps >= bestDps[col] - 1e-9;
                const tip = `Max hit ${r.maxHit} | Accuracy ${(r.accuracy * 100).toFixed(1)}% | `
                  + `Avg ${r.avgDamagePerAttack.toFixed(2)}/attack | TTK ${formatSeconds(r.ttkSeconds)}`;
                return (
                  <td
                    key={col}
                    className={`clickable${isBest ? ' best' : ''}`}
                    title={tip}
                    onClick={() => showDetail(setup.name,
                      displayName(targets[col]) + scaleSuffix(targets[col]), r)}
                  >
                    {r.dps <= 0 ? '-' : r.dps.toFixed(2)}
                  </td>
                );
              })}
              {targets.length > 1 && (
                <td className={Number.isFinite(bestTotal) && totalTtk <= bestTotal + 1e-9 ? 'best' : ''}>
                  {formatSeconds(totalTtk)}
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </table>
      {detail && <div className="matrix-detail">{detail}</div>}
    </div>
  );
};
