export default function LlmSettingsModal({
  apiKey,
  baseUrl,
  model,
  onApiKeyChange,
  onBaseUrlChange,
  onModelChange,
  onSave,
  onClose,
  saving
}) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4" onClick={onClose}>
      <div className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-2xl bg-white p-6 shadow-xl" onClick={(event) => event.stopPropagation()}>
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-slate-900">配置 AI 模型</h2>
          <button type="button" onClick={onClose} className="cursor-pointer text-slate-400 transition hover:text-slate-600">关闭</button>
        </div>
        <div className="space-y-3">
          <div>
            <label className="mb-1 block text-sm text-slate-600">API Key</label>
            <input type="password" value={apiKey} onChange={(event) => onApiKeyChange(event.target.value)} placeholder="sk-..." className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm" />
          </div>
          <div>
            <label className="mb-1 block text-sm text-slate-600">Base URL</label>
            <input value={baseUrl} onChange={(event) => onBaseUrlChange(event.target.value)} placeholder="https://api.deepseek.com/v1" className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm" />
          </div>
          <div>
            <label className="mb-1 block text-sm text-slate-600">模型</label>
            <input list="model-options" value={model} onChange={(event) => onModelChange(event.target.value)} placeholder="deepseek-v4-flash" className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm" />
            <datalist id="model-options">
              <option value="deepseek-v4-flash" />
              <option value="deepseek-chat" />
              <option value="deepseek-reasoner" />
              <option value="gpt-4o-mini" />
              <option value="gpt-4o" />
            </datalist>
          </div>
        </div>
        <p className="mt-3 text-xs text-slate-400">API Key 仅保存在本机 SQLite，不会写入 git。</p>
        <div className="mt-5 flex justify-end gap-2">
          <button type="button" onClick={onClose} className="cursor-pointer rounded-lg border border-slate-300 px-4 py-2 text-sm text-slate-700 transition hover:bg-slate-50 active:scale-95">取消</button>
          <button type="button" onClick={onSave} disabled={saving} className="cursor-pointer rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-indigo-500 active:scale-95 disabled:cursor-not-allowed disabled:opacity-50">{saving ? '保存中…' : '保存'}</button>
        </div>
      </div>
    </div>
  );
}
