export default function Header({
  leetcodeSettings,
  llmSettings,
  syncing,
  switching,
  tutorOpen,
  onOpenLeetCodeSettings,
  onOpenLlmSettings,
  onSync,
  onToggleTutor
}) {
  return (
    <header className="mb-8 flex items-center justify-between gap-4">
      <div>
        <h1 className="text-3xl font-bold text-slate-900">算法伴学助手</h1>
        <p className="mt-1 text-slate-500">在 LeetCode 刷题，这里负责记得住、练得准、坚持得下去</p>
      </div>
      <div className="flex items-center gap-3">
        <span
          className={`rounded-full px-3 py-1 text-xs font-medium ${
            leetcodeSettings?.configured ? 'bg-emerald-50 text-emerald-600' : 'bg-amber-50 text-amber-600'
          }`}
        >
          {leetcodeSettings?.configured ? '已配置登录态' : '未配置登录态'}
        </span>
        <button
          type="button"
          onClick={onOpenLeetCodeSettings}
          className="cursor-pointer rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50 active:scale-95"
        >
          配置 LeetCode
        </button>
        <button
          type="button"
          onClick={onToggleTutor}
          className={`cursor-pointer rounded-lg border px-4 py-2 text-sm font-medium transition active:scale-95 ${
            tutorOpen
              ? 'border-indigo-200 bg-indigo-50 text-indigo-700'
              : 'border-slate-300 text-slate-700 hover:bg-slate-50'
          }`}
        >
          🤖 AI 助教
        </button>
        <button
          type="button"
          onClick={onOpenLlmSettings}
          className={`cursor-pointer rounded-lg border px-4 py-2 text-sm font-medium transition active:scale-95 ${
            llmSettings?.configured
              ? 'border-emerald-200 bg-emerald-50 text-emerald-700 hover:bg-emerald-100'
              : 'border-slate-300 text-slate-700 hover:bg-slate-50'
          }`}
        >
          AI 配置
        </button>
        <button
          type="button"
          onClick={onSync}
          disabled={syncing || switching}
          className="cursor-pointer rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white shadow transition hover:bg-indigo-500 active:scale-95 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {syncing ? '同步中…' : '同步 LeetCode'}
        </button>
      </div>
    </header>
  );
}
