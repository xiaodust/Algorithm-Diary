import { useEffect, useRef, useState } from 'react';

export default function ActiveListCard({ lists, active, switching, loading, onSetActive, onRefreshLists }) {
  const [open, setOpen] = useState(false);
  const rootRef = useRef(null);
  const disabled = switching || loading;
  const selectedName = active?.listName ?? '选择题单';

  useEffect(() => {
    if (!open) {
      return undefined;
    }

    const handleClickOutside = (event) => {
      if (rootRef.current && !rootRef.current.contains(event.target)) {
        setOpen(false);
      }
    };
    const handleEscape = (event) => {
      if (event.key === 'Escape') {
        setOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    document.addEventListener('keydown', handleEscape);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleEscape);
    };
  }, [open]);

  return (
    <section className="mb-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <div className="flex flex-wrap items-center gap-4">
        <label className="text-sm font-medium text-slate-600">当前主线题单</label>

        <div ref={rootRef} className="relative w-64">
          <button
            type="button"
            onClick={() => !disabled && setOpen((value) => !value)}
            disabled={disabled}
            aria-haspopup="listbox"
            aria-expanded={open}
            className="flex w-full items-center justify-between gap-2 rounded-lg border border-slate-300 bg-white px-3 py-2 text-left text-sm text-slate-700 shadow-sm transition-colors hover:border-indigo-300 hover:bg-indigo-50/50 focus:border-indigo-400 focus:outline-none focus:ring-2 focus:ring-indigo-100 disabled:cursor-not-allowed disabled:opacity-60"
          >
            <span className="truncate">{selectedName}</span>
            <svg
              className={`h-4 w-4 shrink-0 text-slate-400 transition-transform ${open ? 'rotate-180' : ''}`}
              viewBox="0 0 20 20"
              fill="currentColor"
              aria-hidden="true"
            >
              <path
                fillRule="evenodd"
                d="M5.23 7.21a.75.75 0 011.06.02L10 11.06l3.71-3.83a.75.75 0 111.08 1.04l-4.25 4.39a.75.75 0 01-1.08 0L5.21 8.27a.75.75 0 01.02-1.06z"
                clipRule="evenodd"
              />
            </svg>
          </button>

          {open && (
            <ul
              role="listbox"
              className="absolute left-0 right-0 top-full z-20 mt-1 max-h-72 overflow-auto rounded-xl border border-slate-200 bg-white p-1 shadow-xl"
            >
              {lists.map((list) => {
                const selected = list.id === active?.listId;
                return (
                  <li key={list.id}>
                    <button
                      type="button"
                      role="option"
                      aria-selected={selected}
                      onClick={() => {
                        onSetActive(list.id);
                        setOpen(false);
                      }}
                      className={`flex w-full items-center justify-between gap-2 rounded-lg px-3 py-2 text-left text-sm transition-colors ${
                        selected
                          ? 'bg-indigo-50 font-medium text-indigo-700'
                          : 'text-slate-700 hover:bg-slate-50'
                      }`}
                    >
                      <span className="truncate">{list.name}</span>
                      {selected && (
                        <svg className="h-4 w-4 shrink-0 text-indigo-600" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                          <path
                            fillRule="evenodd"
                            d="M16.704 4.153a.75.75 0 01.143 1.052l-8 10.5a.75.75 0 01-1.127.075l-4.5-4.5a.75.75 0 011.06-1.06l3.894 3.893 7.48-9.817a.75.75 0 011.05-.143z"
                            clipRule="evenodd"
                          />
                        </svg>
                      )}
                    </button>
                  </li>
                );
              })}
            </ul>
          )}
        </div>

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
