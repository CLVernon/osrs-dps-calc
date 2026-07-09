import { useEffect } from 'react';
import { useStore } from './state/store';
import { loadRepository } from './data/repository';
import { CharacterPanel } from './ui/CharacterPanel';
import { SetupsPanel } from './ui/SetupsPanel';
import { SetupEditor } from './ui/SetupEditor';
import { TargetsPanel } from './ui/TargetsPanel';
import { ComparisonMatrix } from './ui/ComparisonMatrix';

export const App = () => {
  const repo = useStore((s) => s.repo);
  const dataStatus = useStore((s) => s.dataStatus);
  const setRepo = useStore((s) => s.setRepo);

  useEffect(() => {
    if (!repo) {
      loadRepository()
        .then(setRepo)
        .catch((e) => console.error('Failed to load data', e));
    }
  }, [repo, setRepo]);

  if (!repo) {
    return (
      <div className="app-loading">
        <div>Loading OSRS wiki data...</div>
      </div>
    );
  }

  return (
    <>
      <div className="topbar">
        <h1>OSRS DPS Calculator</h1>
        <span className="subtle">
          Compare gear setups (rows) against target monsters (columns)
        </span>
        <span className="status">{dataStatus}</span>
      </div>
      <div className="main">
        <div className="column left">
          <CharacterPanel />
          <hr style={{ border: 'none', borderTop: '1px solid var(--border)', margin: '12px 0' }} />
          <SetupsPanel />
        </div>
        <div className="column center">
          <SetupEditor />
        </div>
        <div className="column right">
          <TargetsPanel />
        </div>
      </div>
      <ComparisonMatrix />
    </>
  );
};
