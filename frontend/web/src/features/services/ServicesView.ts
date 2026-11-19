import { clear, el } from '../../core/ui/dom';
import { icons } from '../../core/ui/icons';
import { showToast } from '../../core/ui/toast';
import { createCallsView } from '../calls/CallsView';

export function createServicesView(): HTMLElement {
  const root = el('div', { class: 'ag-list-pane' });
  let subView: 'hub' | 'calls' | 'ai' = 'hub';
  const aiMessages: Array<{ isUser: boolean; text: string }> = [
    {
      isUser: false,
      text: 'Привет! Я локальный AI-ассистент AnonGram. Все запросы обрабатываются приватно без отправки метаданных.',
    },
  ];

  const render = () => {
    clear(root);

    if (subView === 'calls') {
      const callsPane = createCallsView();
      root.appendChild(
        el(
          'div',
          { class: 'ag-chat-header' },
          el(
            'button',
            {
              class: 'ag-icon-btn',
              onClick: () => {
                subView = 'hub';
                render();
              },
            },
            icons.back(20),
          ),
          el('div', { class: 'ag-chat-header__title' }, 'Звонки'),
        ),
      );
      root.appendChild(callsPane);
      return;
    }

    if (subView === 'ai') {
      const aiInput = el('input', {
        type: 'text',
        class: 'ag-msg-input',
        placeholder: 'Спросите локальный AI...',
        onKeyDown: (e: Event) => {
          if ((e as KeyboardEvent).key === 'Enter') sendAi();
        },
      }) as HTMLInputElement;

      const sendAi = () => {
        const text = aiInput.value.trim();
        if (!text) return;
        aiMessages.push({ isUser: true, text });
        aiMessages.push({
          isUser: false,
          text: `Локальный узел принял запрос: «${text}». Ответ сформирован в изолированной песочнице без сети.`,
        });
        render();
      };

      root.appendChild(
        el(
          'div',
          { class: 'ag-chat-header' },
          el(
            'button',
            {
              class: 'ag-icon-btn',
              onClick: () => {
                subView = 'hub';
                render();
              },
            },
            icons.back(20),
          ),
          el('div', { class: 'ag-chat-header__title' }, 'Local AI (Private)'),
        ),
      );

      root.appendChild(
        el(
          'div',
          { class: 'ag-messages-scroll' },
          ...aiMessages.map((m) =>
            el(
              'div',
              { class: `ag-msg-row ${m.isUser ? 'is-outgoing' : 'is-incoming'}` },
              el(
                'div',
                { class: `ag-bubble ${m.isUser ? 'is-outgoing' : 'is-incoming'}` },
                el('div', { class: 'ag-bubble__text' }, m.text),
              ),
            ),
          ),
        ),
      );

      root.appendChild(
        el(
          'div',
          { class: 'ag-composer' },
          el('div', { class: 'ag-composer__pill' }, aiInput),
          el('button', { class: 'ag-send-fab', onClick: sendAi }, icons.send(18)),
        ),
      );
      return;
    }

    root.appendChild(
      el(
        'div',
        { class: 'ag-pane-header' },
        el('h1', { class: 'ag-pane-title' }, 'Сервисы'),
      ),
    );

    const services = [
      {
        emoji: '🤖',
        title: 'Local AI Assistant',
        sub: 'Приватный ИИ без облачных логов и телеметрии',
        action: () => {
          subView = 'ai';
          render();
        },
      },
      {
        emoji: '📞',
        title: 'Анонимные звонки',
        sub: 'Зашифрованные P2P-вызовы и журнал звонков',
        action: () => {
          subView = 'calls';
          render();
        },
      },
      {
        emoji: '🔥',
        title: 'OTP-комнаты (Сгораемые)',
        sub: 'Одноразовые диалоги с автоуничтожением ключей',
        action: () => showToast('OTP-комната готова к созданию'),
      },
      {
        emoji: '🧅',
        title: 'Onion v3 Blind Relay',
        sub: 'Активная цепочка: 3 хопа • Задержка ~42 мс',
        action: () => showToast('Цепочка Onion v3 стабильна'),
      },
    ];

    root.appendChild(
      el(
        'div',
        { class: 'ag-scroll-list' },
        ...services.map((s) =>
          el(
            'div',
            { class: 'ag-card ag-service-card', onClick: s.action },
            el('div', { class: 'ag-service-card__icon' }, s.emoji),
            el(
              'div',
              { class: 'ag-service-card__body' },
              el('strong', null, s.title),
              el('span', null, s.sub),
            ),
            icons.chevronRight(18),
          ),
        ),
      ),
    );
  };

  render();
  return root;
}
