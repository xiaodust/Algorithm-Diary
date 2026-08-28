import { useEffect, useRef, useState } from 'react';
import { api } from '../../services/api.js';

const QUICK_QUESTIONS = [
  { label: '我的薄弱点', text: '根据我的做题数据，分析我最薄弱的 3 个题型，并给出针对性练习建议' },
  { label: '分析错题', text: '逐个分析我最近的错题，找出共性错误模式' },
  { label: '今天刷什么', text: '根据我的进度和薄弱点，推荐今天该刷的 3 道题' },
  { label: '制定计划', text: '结合我的目标和现有进度，制定未来两周的学习计划' }
];

const EXPLAIN_LEVELS = ['方向提示', '关键提示', '完整思路'];

let localSeq = 0;
const nextLocalId = () => `local-${Date.now()}-${localSeq++}`;

export default function TutorDrawer({ open, onClose, llmConfigured, onNotice, explainRequest, onExplainHandled }) {
  const [sessions, setSessions] = useState([]);
  const [currentId, setCurrentId] = useState(null);
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [thinking, setThinking] = useState(false);
  const [loadingHistory, setLoadingHistory] = useState(false);
  const [sessionsOpen, setSessionsOpen] = useState(false);
  const [summarizing, setSummarizing] = useState(false);
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
      // 若当前有流式输出中的消息，保留它，避免被历史覆盖导致索引错乱
      setMessages((prev) => {
        const streaming = prev.filter((m) => m.streaming);
        return [...history, ...streaming];
      });
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

    // 用稳定 id 定位流式消息，避免因历史加载/并发导致索引错位
    const userMsg = { id: nextLocalId(), role: 'user', content };
    const assistantMsg = { id: nextLocalId(), role: 'assistant', content: '', streaming: true };
    setMessages((prev) => [...prev, userMsg, assistantMsg]);

    let full = '';
    const patchAssistant = (patch) => {
      setMessages((prev) => prev.map((m) => (m.id === assistantMsg.id ? { ...m, ...patch } : m)));
    };

    try {
      await api.tutorChatStream(currentId, content, (delta) => {
        full += delta;
        patchAssistant({ content: full, streaming: true });
      });
      patchAssistant({ content: full, streaming: false });
    } catch (err) {
      patchAssistant({ content: full, streaming: false, error: err.message });
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
      onNotice?.('已沉淀到我的长期记忆 ✓');
    } catch (err) {
      onNotice?.(err.message);
    }
  };

  const summarize = async () => {
    if (!currentId || summarizing) return;
    setSummarizing(true);
    try {
      const result = await api.tutorSummarize(currentId);
      const count = result?.facts?.length ?? 0;
      onNotice?.(count > 0 ? `已提炼 ${count} 条重要信息存入长期记忆 ✓` : '本次对话暂无可提炼的重要信息');
    } catch (err) {
      onNotice?.(err.message);
    } finally {
      setSummarizing(false);
    }
  };

  return (
    <aside
      className={`flex h-full flex-col border-l border-slate-200 bg-white transition-all duration-300 ${
        open ? 'w-[420px] max-w-[420px] opacity-100' : 'w-0 max-w-0 overflow-hidden opacity-0'
      }`}
    >
      {/* 头部 */}
      <div className="flex items-center justify-between border-b border-slate-200 px-4 py-3">
        <div className="relative">
          <button
            type="button"
            onClick={() => setSessionsOpen((v) => !v)}
            className="flex items-center gap-2 rounded-lg px-2 py-1.5 text-sm font-semibold text-slate-800 transition hover:bg-slate-100"
          >
            <span>🤖 AI 算法助教</span>
            <span className="max-w-[120px] truncate text-xs font-normal text-slate-400">
              {sessions.find((s) => s.id === currentId)?.name ?? '新对话'}
            </span>
            <svg className={`h-4 w-4 shrink-0 text-slate-400 transition-transform ${sessionsOpen ? 'rotate-180' : ''}`} viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
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
            disabled={summarizing}
            title="从本对话中提炼值得长期记住的信息（如目标、薄弱模式、偏好），存入跨会话共享的长期记忆"
            className="flex cursor-pointer items-center gap-1 rounded-lg border border-amber-200 px-2 py-1 text-xs text-amber-600 transition hover:bg-amber-50 disabled:cursor-not-allowed disabled:opacity-50"
          >
            <svg className="h-3.5 w-3.5" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
              <path d="M10 2a6 6 0 00-6 6v3.586l-.707.707A1 1 0 003 14h14a1 1 0 00.707-1.707L17 11.586V8a6 6 0 00-6-6zM10 18a3 3 0 01-3-3h6a3 3 0 01-3 3z" />
            </svg>
            {summarizing ? '提炼中…' : '提炼记忆'}
          </button>
          <button
            type="button"
            onClick={onClose}
            className="cursor-pointer rounded-lg p-1.5 text-slate-400 transition hover:bg-slate-100 hover:text-slate-600"
            aria-label="收起侧边栏"
            title="收起侧边栏"
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
          messages.map((msg) => (
            <MessageBubble
              key={msg.id ?? `${msg.role}-${msg.content?.slice(0, 10)}`}
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
  );
}

function MessageBubble({ message, onRemember }) {
  const isUser = message.role === 'user';
  return (
    <div className={`flex ${isUser ? 'justify-end' : 'justify-start'}`}>
      <div
        className={`group relative max-w-[88%] rounded-2xl px-3 py-2 text-sm leading-6 shadow-sm ${
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
            title="记住这条（沉淀到长期记忆，跨对话共享）"
            className="absolute -left-8 top-1 hidden cursor-pointer rounded p-1 text-slate-300 transition hover:text-amber-500 group-hover:block"
          >
            <svg className="h-4 w-4" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
              <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
            </svg>
          </button>
        )}
        {isUser ? (
          <div className="whitespace-pre-wrap">{message.content}</div>
        ) : (
          <MarkdownContent
            content={message.content || (message.streaming ? '▋' : '')}
            streaming={message.streaming && Boolean(message.content)}
          />
        )}
        {message.error && (
          <p className="mt-1 text-xs text-red-500">请求失败，请重试。</p>
        )}
      </div>
    </div>
  );
}

// ==================== 轻量 Markdown 渲染 ====================

function MarkdownContent({ content, streaming }) {
  const blocks = splitBlocks(content);
  return (
    <div className="space-y-1.5">
      {blocks.map((block, index) => {
        if (block.type === 'code') {
          return (
            <CodeBlock key={index} code={block.code} lang={block.lang} />
          );
        }
        if (block.type === 'list') {
          return (
            <ul key={index} className="list-disc space-y-0.5 pl-5">
              {block.items.map((item, i) => (
                <li key={i}>
                  <InlineMarkdown text={item} />
                </li>
              ))}
            </ul>
          );
        }
        if (block.type === 'heading') {
          return (
            <p key={index} className="font-semibold text-slate-900">
              <InlineMarkdown text={block.text} />
            </p>
          );
        }
        if (block.type === 'hr') {
          return <hr key={index} className="my-1 border-slate-200" />;
        }
        return (
          <p key={index} className="whitespace-pre-wrap">
            <InlineMarkdown text={block.text} />
          </p>
        );
      })}
      {streaming && <span className="ml-0.5 inline-block h-4 w-0.5 animate-pulse bg-current align-middle" />}
    </div>
  );
}

function CodeBlock({ code, lang }) {
  const [copied, setCopied] = useState(false);
  const copy = async () => {
    try {
      await navigator.clipboard.writeText(code);
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    } catch {
      // 忽略复制失败
    }
  };
  return (
    <div className="relative my-1 overflow-hidden rounded-lg bg-slate-900">
      <div className="flex items-center justify-between border-b border-slate-700/60 px-3 py-1">
        <span className="text-[10px] uppercase tracking-wide text-slate-400">{lang || 'code'}</span>
        <button
          type="button"
          onClick={copy}
          className="cursor-pointer rounded px-1.5 py-0.5 text-[10px] text-slate-400 transition hover:bg-slate-700 hover:text-slate-200"
        >
          {copied ? '已复制 ✓' : '复制'}
        </button>
      </div>
      <pre className="overflow-x-auto p-3 text-xs leading-5 text-slate-100">
        <code>{code}</code>
      </pre>
    </div>
  );
}

function InlineMarkdown({ text }) {
  // 行内渲染：`code`、**bold**、*italic*、[link](url)
  const parts = [];
  const regex = /(`[^`]+`|\*\*[^*]+\*\*|\*[^*]+\*|\[[^\]]+\]\([^)]+\))/g;
  let lastIndex = 0;
  let match;
  let key = 0;
  while ((match = regex.exec(text)) !== null) {
    if (match.index > lastIndex) {
      parts.push(<span key={key++}>{text.slice(lastIndex, match.index)}</span>);
    }
    const token = match[0];
    if (token.startsWith('`') && token.endsWith('`')) {
      parts.push(
        <code key={key++} className="rounded bg-slate-200/70 px-1 py-0.5 font-mono text-[0.85em] text-rose-600">
          {token.slice(1, -1)}
        </code>
      );
    } else if (token.startsWith('**') && token.endsWith('**')) {
      parts.push(<strong key={key++} className="font-semibold">{token.slice(2, -2)}</strong>);
    } else if (token.startsWith('*') && token.endsWith('*') && token.length > 2) {
      parts.push(<em key={key++}>{token.slice(1, -1)}</em>);
    } else if (token.startsWith('[')) {
      const m = /\[([^\]]+)\]\(([^)]+)\)/.exec(token);
      if (m) {
        parts.push(
          <a key={key++} href={m[2]} target="_blank" rel="noreferrer" className="text-indigo-600 underline">
            {m[1]}
          </a>
        );
      } else {
        parts.push(<span key={key++}>{token}</span>);
      }
    } else {
      parts.push(<span key={key++}>{token}</span>);
    }
    lastIndex = match.index + token.length;
  }
  if (lastIndex < text.length) {
    parts.push(<span key={key++}>{text.slice(lastIndex)}</span>);
  }
  return <>{parts}</>;
}

function splitBlocks(content) {
  const blocks = [];
  const lines = content.split('\n');
  let i = 0;

  while (i < lines.length) {
    const line = lines[i];

    // 代码块 ```lang ... ```
    const codeStart = /^```(\w*)\s*$/.exec(line);
    if (codeStart) {
      const lang = codeStart[1] || '';
      const codeLines = [];
      i++;
      while (i < lines.length && !/^```\s*$/.test(lines[i])) {
        codeLines.push(lines[i]);
        i++;
      }
      i++; // 跳过结尾 ```
      blocks.push({ type: 'code', code: codeLines.join('\n'), lang });
      continue;
    }

    // 空行
    if (line.trim() === '') {
      i++;
      continue;
    }

    // 标题 #
    const heading = /^(#{1,4})\s+(.*)$/.exec(line);
    if (heading) {
      blocks.push({ type: 'heading', text: heading[2] });
      i++;
      continue;
    }

    // 分割线
    if (/^(-{3,}|\*{3,}|_{3,})$/.test(line.trim())) {
      blocks.push({ type: 'hr' });
      i++;
      continue;
    }

    // 无序列表
    if (/^\s*[-*+]\s+/.test(line) || /^\s*\d+[.)]\s+/.test(line)) {
      const items = [];
      while (i < lines.length && (/^\s*[-*+]\s+/.test(lines[i]) || /^\s*\d+[.)]\s+/.test(lines[i]))) {
        items.push(lines[i].replace(/^\s*[-*+]\s+/, '').replace(/^\s*\d+[.)]\s+/, ''));
        i++;
      }
      blocks.push({ type: 'list', items });
      continue;
    }

    // 普通段落：合并连续非空行
    const paraLines = [line];
    i++;
    while (i < lines.length && lines[i].trim() !== '' && !/^```/.test(lines[i]) && !/^\s*[-*+]\s+/.test(lines[i])) {
      paraLines.push(lines[i]);
      i++;
    }
    blocks.push({ type: 'text', text: paraLines.join('\n') });
  }
  return blocks;
}
