import { useEffect, useRef, useState } from 'react';

export default function ActiveListCard({
  lists,
  active,
  goal,
  switching,
  loading,
  onSetActive,
  onRefreshLists,
  onOpenGoal,
  onNewList,
  onEditList,
  onDeleteList
}) {
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
                const custom = list.source === 'CUSTOM' || list.source === 'IMPORTED';
                return (
                  <li key={list.id}>
                    <div className="flex items-center gap-1">
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
                        <span className="flex shrink-0 items-center gap-1">
                          {custom && (
                            <span className="rounded-full bg-amber-50 px-1.5 py-0.5 text-[10px] text-amber-600">
                              {list.source === 'IMPORTED' ? '导入' : '自定义'}
                            </span>
                          )}
                          {selected && (
                            <svg className="h-4 w-4 shrink-0 text-indigo-600" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                              <path
                                fillRule="evenodd"
                                d="M16.704 4.153a.75.75 0 01.143 1.052l-8 10.5a.75.75 0 01-1.127.075l-4.5-4.5a.75.75 0 011.06-1.06l3.894 3.893 7.48-9.817a.75.75 0 011.05-.143z"
                                clipRule="evenodd"
                              />
                            </svg>
                          )}
                        </span>
                      </button>
                      {custom && (
                        <span className="flex shrink-0 items-center gap-0.5 pr-1">
                          <button
                            type="button"
                            title="编辑题单"
                            onClick={() => {
                              onEditList(list);
                              setOpen(false);
                            }}
                            className="cursor-pointer rounded p-1 text-slate-400 transition hover:bg-slate-100 hover:text-indigo-600"
                          >
                            <svg className="h-3.5 w-3.5" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                              <path d="M5.433 13.917l1.262-3.155A4 4 0 017.58 9.42l6.92-6.918a2.121 2.121 0 013 3l-6.92 6.918c-.383.383-.84.685-1.343.886l-3.154 1.262a.5.5 0 01-.65-.65z" />
                              <path d="M3.5 5.75c0-.69.56-1.25 1.25-1.25H10A.75.75 0 0010 3H4.75A2.75 2.75 0 002 5.75v9.5A2.75 2.75 0 004.75 18h9.5A2.75 2.75 0 0017 15.25V10a.75.75 0 00-1.5 0v5.25c0 .69-.56 1.25-1.25 1.25h-9.5c-.69 0-1.25-.56-1.25-1.25v-9.5z" />
                            </svg>
                          </button>
                          <button
                            type="button"
                            title="删除题单"
                            onClick={() => {
                              onDeleteList(list);
                              setOpen(false);
                            }}
                            className="cursor-pointer rounded p-1 text-slate-400 transition hover:bg-red-50 hover:text-red-600"
                          >
                            <svg className="h-3.5 w-3.5" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                              <path fillRule="evenodd" d="M8.75 1A2.75 2.75 0 006 3.75v.443c-.795.077-1.584.176-2.365.298a.75.75 0 10.23 1.482l.149-.022.841 10.518A2.75 2.75 0 007.596 19h4.807a2.75 2.75 0 002.742-2.53l.841-10.52.149.023a.75.75 0 00.23-1.482A41.03 41.03 0 0014 4.193V3.75A2.75 2.75 0 0011.25 1h-2.5zM10 4c.84 0 1.673.025 2.5.075V3.75c0-.69-.56-1.25-1.25-1.25h-2.5c-.69 0-1.25.56-1.25 1.25v.325C8.327 4.025 9.16 4 10 4zM8.58 7.72a.75.75 0 00-1.5.06l.3 7.5a.75.75 0 101.5-.06l-.3-7.5zm4.34.06a.75.75 0 10-1.5-.06l-.3 7.5a.75.75 0 101.5.06l.3-7.5z" clipRule="evenodd" />
                            </svg>
                          </button>
                        </span>
                      )}
                    </div>
                  </li>
                );
              })}
            </ul>
          )}
        </div>

        <button
          type="button"
          onClick={onNewList}
          className="cursor-pointer rounded-lg border border-indigo-200 bg-indigo-50 px-3 py-2 text-sm text-indigo-600 transition hover:bg-indigo-100 active:scale-95"
        >
          + 新建题单
        </button>
        <button
          type="button"
          onClick={onRefreshLists}
          className="cursor-pointer rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-600 transition hover:bg-slate-50 active:scale-95"
        >
          刷新题单
        </button>
        <button
          type="button"
          onClick={onOpenGoal}
          className="cursor-pointer rounded-lg border border-indigo-200 px-3 py-2 text-sm text-indigo-600 transition hover:bg-indigo-50 active:scale-95"
        >
          设置目标
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
            {goal && (
              <span className="ml-2 text-indigo-600">
                · 每日目标 {goal.dailyTarget} 题
              </span>
            )}
          </div>
        </div>
      )}
    </section>
  );
}
