import './core/ui/base.css';
import { i18nStore } from './core/i18n/i18nStore';
import { themeStore } from './core/theme/themeStore';
import { clear } from './core/ui/dom';
import { createAuthFlow, renderAuthShield } from './features/auth/AuthFlow';
import { authStore } from './features/auth/authStore';
import { createAppShell } from './features/navigation/AppShell';

const appEl = document.getElementById('app');
if (!appEl) {
  throw new Error('#app mount element not found');
}

// Initialize theme CSS variables immediately
void themeStore.scheme;
void i18nStore.language;

let activeCleanup: (() => void) | null = null;
let renderedStatus: string | null = null;

function mountRoot(): void {
  const status = authStore.authStatus;
  if (status === renderedStatus) return;
  renderedStatus = status;

  if (activeCleanup) {
    activeCleanup();
    activeCleanup = null;
  }
  clear(appEl!);

  let view: HTMLElement;
  if (status === 'loading') {
    view = renderAuthShield();
  } else if (status === 'unauthenticated') {
    view = createAuthFlow();
  } else {
    view = createAppShell();
  }
  activeCleanup = (view as unknown as { __cleanup?: () => void }).__cleanup ?? null;
  appEl!.appendChild(view);
}

authStore.subscribe(mountRoot);
mountRoot();
