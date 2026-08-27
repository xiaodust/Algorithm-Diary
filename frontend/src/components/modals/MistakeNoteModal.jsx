export default function MistakeNoteModal({
  errorType,
  stuckPoint,
  lesson,
  similarProblems,
  onErrorTypeChange,
  onStuckPointChange,
  onLessonChange,
  onSimilarChange,
  onSave,
  onClose,
  saving
}) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4">
      <div className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-2xl bg-white p-6 shadow-xl" onClick={(event) => event.stopPropagation()}>
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-slate-900">错题复盘</h2>
          <button type="button" onClick={onClose} className="cursor-pointer text-slate-400 transition hover:text-slate-600">关闭</button>
        </div>
        <div className="space-y-3">
          <div>
            <label className="mb-1 block text-sm text-slate-600">错误类型</label>
            <input value={errorType} onChange={(event) => onErrorTypeChange(event.target.value)} placeholder="例如：wrong_answer / timeout / 思路错" className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm" />
          </div>
          <div>
            <label className="mb-1 block text-sm text-slate-600">卡在哪一步</label>
            <textarea value={stuckPoint} onChange={(event) => onStuckPointChange(event.target.value)} rows={3} className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm" />
          </div>
          <div>
            <label className="mb-1 block text-sm text-slate-600">下次怎么做</label>
            <textarea value={lesson} onChange={(event) => onLessonChange(event.target.value)} rows={3} className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm" />
          </div>
          <div>
            <label className="mb-1 block text-sm text-slate-600">相似题</label>
            <input value={similarProblems} onChange={(event) => onSimilarChange(event.target.value)} placeholder="例如：two-sum、3sum" className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm" />
          </div>
        </div>
        <div className="mt-5 flex justify-end gap-2">
          <button type="button" onClick={onClose} className="cursor-pointer rounded-lg border border-slate-300 px-4 py-2 text-sm text-slate-700 transition hover:bg-slate-50 active:scale-95">取消</button>
          <button type="button" onClick={onSave} disabled={saving} className="cursor-pointer rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-indigo-500 active:scale-95 disabled:cursor-not-allowed disabled:opacity-50">{saving ? '保存中…' : '保存'}</button>
        </div>
      </div>
    </div>
  );
}
