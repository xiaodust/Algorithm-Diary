export default function LeetCodeSettingsModal({
  sessionInput,
  csrfInput,
  cfInput,
  onSessionChange,
  onCsrfChange,
  onCfChange,
  onSave,
  onClose,
  saving
}) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4">
      <div className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-2xl bg-white p-6 shadow-xl" onClick={(event) => event.stopPropagation()}>
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-slate-900">配置 LeetCode 登录态</h2>
          <button type="button" onClick={onClose} className="cursor-pointer text-slate-400 transition hover:text-slate-600">关闭</button>
        </div>
        <ol className="mb-5 list-decimal space-y-2 pl-5 text-sm text-slate-600">
          <li>打开并登录 <span className="font-medium">leetcode.cn</span></li>
          <li>按 <span className="font-medium">F12</span> 打开开发者工具</li>
          <li>切到「应用 / Application」→「Cookies」→ leetcode.cn</li>
          <li>复制 <span className="font-medium">LEETCODE_SESSION</span> 的值</li>
          <li>复制 <span className="font-medium">csrftoken</span> 的值（推荐）</li>
          <li>如遇 Cloudflare 拦截，再复制 <span className="font-medium">cf_clearance</span>（可选）</li>
        </ol>
        <div className="space-y-3">
          <input value={sessionInput} onChange={(event) => onSessionChange(event.target.value)} placeholder="LEETCODE_SESSION（必填）" className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm" />
          <input value={csrfInput} onChange={(event) => onCsrfChange(event.target.value)} placeholder="csrftoken（推荐）" className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm" />
          <input value={cfInput} onChange={(event) => onCfChange(event.target.value)} placeholder="cf_clearance（可选）" className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm" />
        </div>
        <div className="mt-5 flex justify-end gap-2">
          <button type="button" onClick={onClose} className="cursor-pointer rounded-lg border border-slate-300 px-4 py-2 text-sm text-slate-700 transition hover:bg-slate-50 active:scale-95">取消</button>
          <button type="button" onClick={onSave} disabled={saving} className="cursor-pointer rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-indigo-500 active:scale-95 disabled:cursor-not-allowed disabled:opacity-50">{saving ? '保存中…' : '保存'}</button>
        </div>
      </div>
    </div>
  );
}
