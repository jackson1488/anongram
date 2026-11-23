import { i18nStore } from '../../../core/i18n/i18nStore';
import { clear, copyToClipboard, el } from '../../../core/ui/dom';
import { icons, tgIcons } from '../../../core/ui/icons';
import { showToast } from '../../../core/ui/toast';
import { servicesT } from '../i18n';
import {
  AI_CATALOG,
  localAiStore,
  type AiReasoningLevel,
  type AiSystemPromptPreset,
} from './LocalAiStore';

export interface LocalAiChatOptions {
  onBack: () => void;
}

export function createLocalAiChatScreen(options: LocalAiChatOptions): HTMLElement {
  const root = el('div', { class: 'ag-ai-shell' });
  let drawerOpen = false;
  let modelMenuOpen = false;

  const topbar = el('div', { class: 'ag-ai-topbar' });
  const drawerSlot = el('div');
  const modelMenuSlot = el('div');
  const scrollArea = el('div', { class: 'ag-messages-scroll', style: 'flex:1;min-height:0;' });
  const controlsBar = el('div', { class: 'ag-ai-controls-bar' });

  const inputEl = el('input', {
    type: 'text',
    class: 'ag-msg-input',
    placeholder: servicesT().aiInputPlaceholder,
    onKeyDown: (e: Event) => {
      if ((e as KeyboardEvent).key === 'Enter') handleSend();
    },
  }) as HTMLInputElement;

  const handleSend = () => {
    const text = inputEl.value.trim();
    if (!text) return;
    inputEl.value = '';
    localAiStore.sendMessage(text);
  };

  const composer = el(
    'div',
    { class: 'ag-composer' },
    el('div', { class: 'ag-composer__pill' }, inputEl),
    el('button', { class: 'ag-send-fab', onClick: handleSend }, icons.send(18)),
  );

  const render = () => {
    const s = servicesT();
    const state = localAiStore.getState();
    const activeModel = localAiStore.activeModel;
    const activeSess = localAiStore.activeSession;
    inputEl.placeholder = s.aiInputPlaceholder;

    // Topbar
    clear(topbar);
    topbar.append(
      el(
        'div',
        { style: 'display:flex;align-items:center;gap:4px;' },
        el('button', { class: 'ag-icon-btn', onClick: options.onBack }, icons.back(20)),
        el(
          'button',
          {
            class: 'ag-icon-btn',
            title: s.aiSessions,
            onClick: () => {
              drawerOpen = !drawerOpen;
              modelMenuOpen = false;
              render();
            },
          },
          icons.menu(19),
        ),
      ),
      el(
        'button',
        {
          class: 'ag-ai-model-pill',
          onClick: () => {
            modelMenuOpen = !modelMenuOpen;
            drawerOpen = false;
            render();
          },
        },
        el('span', null, activeModel.displayName),
        tgIcons.chevronDown(14),
      ),
      el(
        'div',
        { style: 'display:flex;align-items:center;gap:4px;' },
        el(
          'button',
          {
            class: 'ag-icon-btn',
            title: s.aiClearChat,
            onClick: () => localAiStore.clearCurrentSession(),
          },
          icons.trash(17),
        ),
        el(
          'button',
          {
            class: 'ag-icon-btn',
            title: s.aiNewChat,
            onClick: () => {
              localAiStore.newSession();
              drawerOpen = false;
            },
          },
          tgIcons.plus(19),
        ),
      ),
    );

    // Drawer
    clear(drawerSlot);
    if (drawerOpen) {
      const sorted = [...state.sessions].sort(
        (a, b) => Number(b.isPinned) - Number(a.isPinned) || b.updatedAtMs - a.updatedAtMs,
      );
      drawerSlot.appendChild(
        el(
          'div',
          { class: 'ag-ai-drawer' },
          ...sorted.map((sess) =>
            el(
              'div',
              {
                class: `ag-ai-session-item ${sess.id === activeSess.id ? 'is-active' : ''}`,
                onClick: () => {
                  localAiStore.selectSession(sess.id);
                  drawerOpen = false;
                },
              },
              el(
                'div',
                { style: 'min-width:0;flex:1;' },
                el(
                  'div',
                  { style: 'font-size:13px;font-weight:600;color:var(--ag-text-primary);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;' },
                  `${sess.isPinned ? '📌 ' : ''}${sess.title}`,
                ),
                el(
                  'div',
                  { style: 'font-size:11px;color:var(--ag-text-secondary);' },
                  `${sess.messages.length} msg`,
                ),
              ),
              el(
                'button',
                {
                  class: 'ag-icon-btn',
                  style: 'width:32px;height:32px;',
                  onClick: (e: Event) => {
                    e.stopPropagation();
                    localAiStore.togglePinSession(sess.id);
                  },
                },
                icons.pin(14),
              ),
              el(
                'button',
                {
                  class: 'ag-icon-btn',
                  style: 'width:32px;height:32px;',
                  onClick: (e: Event) => {
                    e.stopPropagation();
                    localAiStore.deleteSession(sess.id);
                  },
                },
                icons.trash(14),
              ),
            ),
          ),
        ),
      );
    }

    // Model & Engine selector drawer
    clear(modelMenuSlot);
    if (modelMenuOpen) {
      modelMenuSlot.appendChild(
        el(
          'div',
          { class: 'ag-ai-drawer' },
          el(
            'div',
            { style: 'display:flex;gap:6px;flex-wrap:wrap;' },
            el(
              'button',
              {
                class: `ag-chip ${state.engineMode === 'local_gguf' ? 'is-active' : ''}`,
                onClick: () => localAiStore.setEngineMode('local_gguf'),
              },
              s.aiEngineLocal,
            ),
            el(
              'button',
              {
                class: `ag-chip ${state.engineMode === 'cloud_openai' ? 'is-active' : ''}`,
                onClick: () => localAiStore.setEngineMode('cloud_openai'),
              },
              s.aiEngineCloud,
            ),
          ),
          ...AI_CATALOG.map((m) =>
            el(
              'div',
              {
                class: `ag-ai-session-item ${m.id === state.activeModelId ? 'is-active' : ''}`,
                onClick: () => {
                  localAiStore.setModel(m.id);
                  modelMenuOpen = false;
                },
              },
              el(
                'div',
                null,
                el('div', { style: 'font-size:13px;font-weight:600;color:var(--ag-text-primary);' }, m.displayName),
                el('div', { style: 'font-size:11px;color:var(--ag-text-secondary);' }, `${m.sizeMb} MB · ${m.ramHint}`),
              ),
              m.id === state.activeModelId ? icons.check(16) : el('span', null),
            ),
          ),
        ),
      );
    }

    // Messages
    clear(scrollArea);
    if (activeSess.messages.length === 0) {
      scrollArea.appendChild(
        el(
          'div',
          { class: 'ag-ai-hero' },
          el('div', { class: 'ag-service-icon-badge', style: 'width:56px;height:56px;font-size:24px;' }, '🤖'),
          el('strong', { style: 'font-size:17px;color:var(--ag-text-primary);' }, s.aiEmptyTitle),
          el('span', { style: 'font-size:13px;color:var(--ag-text-secondary);max-width:300px;' }, s.aiEmptySub),
          el(
            'div',
            { class: 'ag-ai-suggestions' },
            ...s.aiSuggestions.map((prompt) =>
              el(
                'button',
                {
                  class: 'ag-ai-suggestion-btn',
                  onClick: () => localAiStore.sendMessage(prompt),
                },
                prompt,
              ),
            ),
          ),
        ),
      );
    } else {
      for (const m of activeSess.messages) {
        scrollArea.appendChild(
          el(
            'div',
            { class: `ag-msg-row ${m.isUser ? 'is-outgoing' : 'is-incoming'}` },
            el(
              'div',
              { class: `ag-bubble ${m.isUser ? 'is-outgoing' : 'is-incoming'}` },
              !m.isUser && m.thinking
                ? el(
                    'div',
                    { class: 'ag-ai-thinking-box' },
                    el('strong', { style: 'display:block;margin-bottom:2px;' }, s.aiThinking),
                    el('span', null, m.thinking),
                  )
                : el('span', null),
              el('div', { class: 'ag-bubble__text' }, m.content),
              !m.isUser
                ? el(
                    'div',
                    { style: 'display:flex;justify-content:flex-end;margin-top:4px;' },
                    el(
                      'button',
                      {
                        class: 'ag-icon-btn',
                        style: 'width:28px;height:28px;',
                        onClick: async () => {
                          await copyToClipboard(m.content);
                          showToast(s.aiCopied);
                        },
                      },
                      icons.copy(13),
                    ),
                  )
                : el('span', null),
            ),
          ),
        );
      }
      scrollArea.scrollTop = scrollArea.scrollHeight;
    }

    // Controls bar (reasoning level, persona, web tools)
    clear(controlsBar);
    const levels: AiReasoningLevel[] = ['off', 'low', 'medium', 'high'];
    const nextLevel = levels[(levels.indexOf(state.reasoningLevel) + 1) % levels.length];
    const presets: AiSystemPromptPreset[] = ['default', 'coder', 'analyst', 'translator', 'concise'];
    const nextPreset = presets[(presets.indexOf(state.systemPromptPreset) + 1) % presets.length];

    controlsBar.append(
      el(
        'button',
        {
          class: `ag-chip ${state.reasoningLevel !== 'off' ? 'is-active' : ''}`,
          onClick: () => localAiStore.setReasoning(nextLevel),
        },
        `${s.aiReasoningLabel}: ${state.reasoningLevel.toUpperCase()}`,
      ),
      el(
        'button',
        {
          class: 'ag-chip',
          onClick: () => localAiStore.setSystemPrompt(nextPreset),
        },
        `${s.aiPromptLabel}: ${state.systemPromptPreset}`,
      ),
      el(
        'button',
        {
          class: `ag-chip ${state.webToolsEnabled ? 'is-active' : ''}`,
          onClick: () => localAiStore.toggleWebTools(),
        },
        icons.globe(13),
        s.aiWebTools,
      ),
    );
  };

  root.append(topbar, drawerSlot, modelMenuSlot, scrollArea, controlsBar, composer);

  const unsubAi = localAiStore.subscribe(render);
  const unsubI18n = i18nStore.subscribe(render);
  render();

  (root as unknown as { __cleanup?: () => void }).__cleanup = () => {
    unsubAi();
    unsubI18n();
  };

  return root;
}
