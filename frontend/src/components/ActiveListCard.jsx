export default function ActiveListCard({ lists, active, switching, loading, onSetActive, onRefreshLists }) {
  return (
    <section className="mb-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <div className="flex flex-wrap items-center gap-4">
        <label className="text-sm font-medium text-slate-600">当前主线题单</label>
        <select
          value={active?.listId ?? ''}
          onChange={(event) => onSetActive(event.target.value)}
          disabled={switching || loading}
          className="cursor-pointer rounded-lg border border-slate-300 px-3 py-2 text-sm transition hover:border-indigo-300 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {lists.map((list) => (
            <option key={list.id} value={list.id}>
              {list.name}
            </option>
          ))}
        </select>
        <button
          type="button"
          onClick={onRefreshLists}
          className="cursor-pointer rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-600 transition hover:bg-slate-50 active:scale-95"
        >
          刷新题单
        </button>
        {switching && <span className="text-xs text-slate-400">切换中…</span>}
      </div>
      {active && (
        <div className="mt-4">
          <div className="mb-2 flex items-center justify-between text-sm">
            <span className="font-medium text-slate-700">{active.listName} 进度</span>
            <span className="text-slate-500">{active.solved} / {active.total}</span>
          </div>
          <div className="h-3 overflow-hidden rounded-full bg-slate-100">
            <div
              className="h-full rounded-full bg-emerald-500 transition-all duration-300"
              style={{ width: `${active.total > 0 ? (active.solved / active.total) * 100 : 0}%` }}
            />
          </div>
          <div className="mt-2 text-xs text-slate-500">
            还差 {active.remaining ?? active.total - active.solved} 题
            {active.estimatedDays != null ? ` · 按当前节奏预计 ${active.estimatedDays} 天完成` : ' · 预计时间待积累数据'}
          </div>
        </div>
      )}
    </section>
  );
}
