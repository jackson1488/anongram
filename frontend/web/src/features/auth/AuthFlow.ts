import type { UserIdentity } from '../../core/crypto/identity';
import { clear, el } from '../../core/ui/dom';
import './auth.css';
import { createRestoreAccountScreen } from './ui/login/RestoreAccountScreen';
import { createBackupSeedScreen } from './ui/registration/BackupSeedScreen';
import { createCreateIdentityScreen } from './ui/registration/CreateIdentityScreen';
import { createWelcomeScreen, renderAuthShield } from './ui/welcome/WelcomeScreen';

export { renderAuthShield };

type AuthStep = 'welcome' | 'create' | 'backup' | 'restore';
type Cleanable = HTMLElement & { __cleanup?: () => void };

/** Mirrors Android AuthNavGraph: welcome -> create -> backup, welcome -> restore. */
export function createAuthFlow(): HTMLElement {
  const root = el('div', { class: 'ag-auth-screen' }) as Cleanable;
  let childCleanup: (() => void) | null = null;
  let draft: UserIdentity | null = null;

  const go = (step: AuthStep) => {
    childCleanup?.();
    childCleanup = null;
    clear(root);
    let screen: Cleanable;
    switch (step) {
      case 'create':
        screen = createCreateIdentityScreen({
          onBack: () => go('welcome'),
          onDone: (identity) => {
            draft = identity;
            go('backup');
          },
        });
        break;
      case 'backup':
        if (!draft) return go('create');
        screen = createBackupSeedScreen(draft, () => go('create'));
        break;
      case 'restore':
        screen = createRestoreAccountScreen(() => go('welcome'));
        break;
      default:
        screen = createWelcomeScreen(() => go('create'), () => go('restore'));
    }
    childCleanup = screen.__cleanup ?? null;
    root.appendChild(screen);
  };

  go('welcome');
  root.__cleanup = () => childCleanup?.();
  return root;
}
