import { useEffect, useRef, useState } from 'react';
import { api } from '../../services/api.js';

const QUICK_QUESTIONS = [
  { label: '我的薄弱点', text: '根据我的做题数据，分析我最薄弱的 3 个题型，并给出针对性练习建议' },
  { label: '分析错题', text: '逐个分析我最近的错题，找出共性错误模式' },
  { label: '今天刷什么', text: '根据我的进度和薄弱点，推荐今天该刷的 3 道题' },
  { label: '制定计划', text: '结合我的目标和现有进度，制定未来两周的学习计划' }
];

const EXPLAIN_LEVELS = ['方向提示', '关键提示', '完整思路'];

export default function TutorDrawer({ open, onClose, llmConfigured, onNotice, explainRequest, onExplainHandled }) {
  const [sessions, setSessions] = useState([]);
  const [currentId, setCurrentId] = useState(null);
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [thinking, setThinking] = useState(false);
  const [loadingHistory, setLoadingHistory] = useState(false);
  const [sessionsOpen, setSessionsOpen] = useState(false);
  const messagesRef = useRef(null);
  const explainedRef = useRef(null);

  // 打开时加载会话列表
  useEffect(() => {
    if (!open) return;
    loadSessions();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  // 切换会话时加载历史
  useEffect(() => {
    if (!open || !currentId) return;
    loadHistory(currentId);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentId, open]);

  // 收到解析请求时自动发起
  useEffect(() => {
    if (!open || !currentId || !explainRequest || explainedRef.current === explainRequest.slug) return;
    explainedRef.current = explainRequest.slug;
    const { slug, title, level = 0 } = explainRequest;
    send(`请解析题目 ${slug}（${title ?? slug}），等级：${EXPLAIN_LEVELS[level] ?? '方向提示'}`);
    onExplainHandled?.();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, currentId, explainRequest]);

  // 消息变化自动滚底
  useEffect(() => {
    const el = messagesRef.current;
    if (el) el.scrollTop = el.scrollHeight;
  }, [messages]);

  const loadSessions = async () => {
    try {
      const list = await api.tutorSessions();
      setSessions(list);
      if (list.length === 0) {
        const created = await api.tutorCreateSession('新对话');
        setSessions([created]);
        setCurrentId(created.id);
      } else if (!currentId || !list.some((s) => s.id === currentId)) {
        setCurrentId(list[0].id);
      }
    } catch (err) {
      onNotice?.(err.message);
    }
  };

  const loadHistory = async (sessionId) => {
    setLoadingHistory(true);
    try {
      const history = await api.tutorHistory(sessionId);
      setMessages(history);
    } catch (err) {
      onNotice?.(err.message);
    } finally {
      setLoadingHistory(false);
    }
  };

  const createSession = async () => {
    try {
      const created = await api.tutorCreateSession('新对话');
      setSessions((prev) => [created, ...prev]);
      setCurrentId(created.id);
      setMessages([]);
      setSessionsOpen(false);
    } catch (err) {
      onNotice?.(err.message);
    }
  };

  const switchSession = (id) => {
    if (id === currentId) return;
    setCurrentId(id);
    setSessionsOpen(false);
  };

  const deleteSession = async (id) => {
    if (!window.confirm('确定删除这个对话吗？此操作不可恢复。')) return;
    try {
      await api.tutorDeleteSession(id);
      const remaining = sessions.filter((s) => s.id !== id);
      setSessions(remaining);
      if (id === currentId) {
        if (remaining.length > 0) {
          setCurrentId(remaining[0].id);
        } else {
          const created = await api.tutorCreateSession('新对话');
          setSessions([created]);
          setCurrentId(created.id);
        }
      }
    } catch (err) {
      onNotice?.(err.message);
    }
  };

  const renameSession = async (id, name) => {
    if (!name || !name.trim()) return;
    try {
      await api.tutorRenameSession(id, name.trim());
      setSessions((prev) => prev.map((s) => (s.id === id ? { ...s, name: name.trim() } : s)));
    } catch (err) {
      onNotice?.(err.message);
    }
  };

  const send = async (text) => {
    const content = (text ?? input).trim();
    if (!content || !currentId || thinking) return;
    setInput('');
    setThinking(true);
    setMessages((prev) => [...prev, { role: 'user', content }]);
    const assistantIndex = messages.length + 1; // user 消息占一位
    setMessages((prev) => [...prev, { role: 'assistant', content: '', streaming: true }]);

    let full = '';
    try {
      await api.tutorChatStream(currentId, content, (delta) => {
        full += delta;
        setMessages((prev) => {
          const next = [...prev];
          next[assistantIndex] = { role: 'assistant', content: full, streaming: true };
          return next;
        });
      });
      setMessages((prev) => {
        const next = [...prev];
        if (next[assistantIndex]) {
          next[assistantIndex] = { role: 'assistant', content: full, streaming: false };
        }
        return next;
      });
    } catch (err) {
      setMessages((prev) => {
        const next = [...prev];
        if (next[assistantIndex]) {
          next[assistantIndex] = { role: 'assistant', content: full, streaming: false, error: err.message };
        }
        return next;
      });
      onNotice?.(err.message);
    } finally {
      setThinking(false);
      // 刷新会话列表（自动命名可能已更新）
      api.tutorSessions().then(setSessions).catch(() => {});
    }
  };

  const rememberFact = async (content) => {
    if (!content || !content.trim()) return;
    try {
      await api.tutorRemember(content.trim());
      onNotice?.('已沉淀到我的长期记忆');
    } catch (err) {
      onNotice?.(err.message);
    }
  };

  const summarize = async () => {
    if (!currentId) return;
    try {
      const result = await api.tutorSummarize(currentId);
      const count = result?.facts?.length ?? 0;
      onNotice?.(count > 0 ? `已提炼 ${count} 条重要信息存入长期记忆` : '本次对话暂无可提炼的重要信息');
    } catch (err) {
      onNotice?.(err.message);
    }
  };

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-40">
      <div className="absolute inset-0 bg-slate-900/40" onClick={onClose} />
      <aside className="absolute inset-y-0 right-0 flex w-[420px] max-w-full flex-col bg-white shadow-2xl">
        {/* 头部 */}
        <div className="flex items-center justify-between border-b border-slate-200 px-4 py-3">
          <div className="relative">
            <button
              type="button"
              onClick={() => setSessionsOpen((v) => !v)}
              className="flex items-center gap-2 rounded-lg px-2 py-1.5 text-sm font-semibold text-slate-800 transition hover:bg-slate-100"
            >
              <span>🤖 AI 算法助教</span>
              <span className="text-xs font-normal text-slate-400">
                {sessions.find((s) => s.id === currentId)?.name ?? '新对话'}
              </span>
              <svg className={`h-4 w-4 text-slate-400 transition-transform ${sessionsOpen ? 'rotate-180' : ''}`} viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                <path fillRule="evenodd" d="M5.23 7.21a.75.75 0 011.06.02L10 11.06l3.71-3.83a.75.75 0 111.08 1.04l-4.25 4.39a.75.75 0 01-1.08 0L5.21 8.27a.75.75 0 01.02-1.06z" clipRule="evenodd" />
              </svg>
            </button>

            {sessionsOpen && (
              <div className="absolute left-0 right-0 top-full z-50 mt-1 max-h-72 overflow-auto rounded-xl border border-slate-200 bg-white p-1 shadow-xl">
                <button
                  type="button"
                  onClick={createSession}
                  className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left text-sm font-medium text-indigo-600 transition hover:bg-indigo-50"
                >
                  <span className="text-base leading-none">＋</span> 新对话
                </button>
                {sessions.map((s) => (
                  <div key={s.id} className="group flex items-center gap-1">
                    <button
                      type="button"
                      onClick={() => switchSession(s.id)}
                      className={`flex-1 truncate rounded-lg px-3 py-2 text-left text-sm transition ${
                        s.id === currentId ? 'bg-indigo-50 font-medium text-indigo-700' : 'text-slate-700 hover:bg-slate-50'
                      }`}
                    >
                      {s.name}
                    </button>
                    <button
                      type="button"
                      title="删除对话"
                      onClick={() => deleteSession(s.id)}
                      className="mr-1 hidden cursor-pointer rounded p-1 text-slate-400 hover:bg-red-50 hover:text-red-600 group-hover:block"
                    >
                      <svg className="h-3.5 w-3.5" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                        <path fillRule="evenodd" d="M8.75 1A2.75 2.75 0 006 3.75v.443c-.795.077-1.584.176-2.365.298a.75.75 0 10.23 1.482l.149-.022.841 10.518A2.75 2.75 0 007.596 19h4.807a2.75 2.75 0 002.742-2.53l.841-10.52.149.023a.75.75 0 00.23-1.482A41.03 41.03 0 0014 4.193V3.75A2.75 2.75 0 0011.25 1h-2.5zM10 4c.84 0 1.673.025 2.5.075V3.75c0-.69-.56-1.25-1.25-1.25h-2.5c-.69 0-1.25.56-1.25 1.25v.325C8.327 4.025 9.16 4 10 4zM8.58 7.72a.75.75 0 00-1.5.06l.3 7.5a.75.75 0 101.5-.06l-.3-7.5zm4.34.06a.75.75 0 10-1.5-.06l-.3 7.5a.75.75 0 101.5.06l.3-7.5z" clipRule="evenodd" />
                      </svg>
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={summarize}
              title="提炼本对话重要信息存入长期记忆"
              className="cursor-pointer rounded-lg border border-amber-200 px-2 py-1 text-xs text-amber-600 transition hover:bg-amber-50"
            >
              提炼记忆
            </button>
            <button
              type="button"
              onClick={onClose}
              className="cursor-pointer rounded-lg p-1.5 text-slate-400 transition hover:bg-slate-100 hover:text-slate-600"
              aria-label="收起"
            >
              <svg className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                <path fillRule="evenodd" d="M4.72 3.97a.75.75 0 011.06 0l4.22 4.22 4.22-4.22a.75.75 0 111.06 1.06L11.06 10l4.22 4.22a.75.75 0 11-1.06 1.06L10 11.06l-4.22 4.22a.75.75 0 01-1.06-1.06L8.94 10 4.72 5.78a.75.75 0 010-1.06z" clipRule="evenodd" />
              </svg>
            </button>
          </div>
        </div>

        {/* 未配置提示 */}
        {!llmConfigured && (
          <div className="border-b border-amber-100 bg-amber-50 px-4 py-2 text-xs text-amber-700">
            未配置 AI 模型，当前仅能使用规则回复。建议在右上角「AI 配置」填入 API Key。
          </div>
        )}

        {/* 消息区 */}
        <div ref={messagesRef} className="flex-1 space-y-3 overflow-y-auto px-4 py-4">
          {loadingHistory ? (
            <p className="text-center text-sm text-slate-400">加载中…</p>
          ) : messages.length === 0 ? (
            <div className="pt-6 text-center">
              <p className="text-4xl">🤖</p>
              <p className="mt-3 text-sm text-slate-500">
                你好！我是你的算法助教，可以随时问我关于刷题、错题、薄弱点的问题。
              </p>
            </div>
          ) : (
            messages.map((msg, index) => (
              <MessageBubble
                key={`${msg.id ?? index}-${index}`}
                message={msg}
                onRemember={rememberFact}
              />
            ))
          )}
          {thinking && !messages.some((m) => m.streaming) && (
            <p className="text-center text-xs text-slate-400">助教思考中…</p>
          )}
        </div>

        {/* 快捷提问 */}
        <div className="flex flex-wrap gap-1.5 border-t border-slate-100 px-4 py-2">
          {QUICK_QUESTIONS.map((q) => (
            <button
              key={q.label}
              type="button"
              onClick={() => send(q.text)}
              disabled={thinking}
              className="cursor-pointer rounded-full border border-indigo-100 bg-indigo-50 px-3 py-1 text-xs text-indigo-600 transition hover:bg-indigo-100 active:scale-95 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {q.label}
            </button>
          ))}
        </div>

        {/* 输入区 */}
        <div className="flex items-end gap-2 border-t border-slate-200 px-4 py-3">
          <textarea
            value={input}
            onChange={(event) => setInput(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === 'Enter' && !event.shiftKey) {
                event.preventDefault();
                send();
              }
            }}
            rows={2}
            placeholder="输入问题，Enter 发送，Shift+Enter 换行"
            className="flex-1 resize-none rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-indigo-400 focus:outline-none focus:ring-2 focus:ring-indigo-100"
          />
          <button
            type="button"
            onClick={() => send()}
            disabled={thinking || !input.trim()}
            className="cursor-pointer rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-indigo-500 active:scale-95 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {thinking ? '…' : '发送'}
          </button>
        </div>
      </aside>
    </div>
  );
}

function MessageBubble({ message, onRemember }) {
  const isUser = message.role === 'user';
  return (
    <div className={`flex ${isUser ? 'justify-end' : 'justify-start'}`}>
      <div
        className={`group relative max-w-[85%] rounded-2xl px-3 py-2 text-sm leading-6 shadow-sm ${
          isUser
            ? 'rounded-br-md bg-indigo-600 text-white'
            : message.error
              ? 'rounded-bl-md border border-red-200 bg-red-50 text-red-700'
              : 'rounded-bl-md border border-slate-100 bg-slate-50 text-slate-700'
        }`}
      >
        {!isUser && (
          <button
            type="button"
            onClick={() => onRemember(message.content)}
            title="记住这条（沉淀到长期记忆）"
            className="absolute -left-8 top-1 hidden cursor-pointer rounded p-1 text-slate-300 transition hover:text-amber-500 group-hover:block"
          >
            <svg className="h-4 w-4" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
              <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
            </svg>
          </button>
        )}
        <div className="whitespace-pre-wrap">
          {message.content || (message.streaming ? '▋' : '')}
          {message.streaming && message.content && <span className="ml-0.5 inline-block h-4 w-0.5 animate-pulse bg-current align-middle" />}
        </div>
        {message.error && (
          <p className="mt-1 text-xs text-red-500">请求失败，请重试。</p>
        )}
      </div>
    </div>
  );
}
