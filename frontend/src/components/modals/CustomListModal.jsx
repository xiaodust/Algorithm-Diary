import { useEffect, useMemo, useState } from 'react';
import { api } from '../../services/api.js';

export default function CustomListModal({ list, onSave, onClose, saving }) {
  const isEdit = Boolean(list?.id);
  const [name, setName] = useState(list?.name ?? '');
  const [slugs, setSlugs] = useState(list?.problemSlugs ?? []);
  const [tab, setTab] = useState('manual');
  const [manualInput, setManualInput] = useState('');
  const [solved, setSolved] = useState([]);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [searching, setSearching] = useState(false);
  const [searchError, setSearchError] = useState('');
  const [plans, setPlans] = useState([]);
  const [plansLoading, setPlansLoading] = useState(false);
  const [plansError, setPlansError] = useState('');
  const [selectedPlan, setSelectedPlan] = useState('');
  const [importing, setImporting] = useState(false);

  useEffect(() => {
    if (tab === 'solved') {
      api.getSolvedProblems()
        .then(setSolved)
        .catch(() => setSolved([]));
    }
    if (tab === 'plans') {
      loadPlans();
    }
  }, [tab]);

  const loadPlans = async () => {
    setPlansLoading(true);
    setPlansError('');
    try {
      setPlans(await api.getStudyPlans());
    } catch (err) {
      setPlansError(err.message);
    } finally {
      setPlansLoading(false);
    }
  };

  const doSearch = async () => {
    if (!searchKeyword.trim()) return;
    setSearching(true);
    setSearchError('');
    try {
      setSearchResults(await api.searchProblems(searchKeyword.trim()));
    } catch (err) {
      setSearchError(err.message);
    } finally {
      setSearching(false);
    }
  };

  const addManual = () => {
    const parsed = manualInput
      .split(/[,\s，]+/)
      .map((s) => s.trim())
      .filter((s) => s);
    if (parsed.length === 0) return;
    setSlugs((prev) => [...prev, ...parsed.filter((s) => !prev.includes(s))]);
    setManualInput('');
  };

  const addSlug = (slug) => {
    if (!slug || slugs.includes(slug)) return;
    setSlugs((prev) => [...prev, slug]);
  };

  const removeSlug = (slug) => {
    setSlugs((prev) => prev.filter((s) => s !== slug));
  };

  const importPlan = async () => {
    if (!selectedPlan) return;
    setImporting(true);
    try {
      const imported = await api.importStudyPlan(selectedPlan);
      setSlugs((prev) => [...prev, ...imported.problemSlugs.filter((s) => !prev.includes(s))]);
      setSelectedPlan('');
      setNoticeText(`已导入「${imported.name}」共 ${imported.problemSlugs.length} 题`);
    } catch (err) {
      setPlansError(err.message);
    } finally {
      setImporting(false);
    }
  };

  const [noticeText, setNoticeText] = useState('');

  const submit = (event) => {
    event.preventDefault();
    onSave({ name, slugs });
  };

  const addable = name.trim().length > 0 && slugs.length > 0;

  const tabs = [
    { id: 'manual', label: '手动输入' },
    { id: 'solved', label: '从已做题目' },
    { id: 'search', label: '搜索题目' },
    { id: 'plans', label: '导入官方题单' }
  ];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4">
      <div className="max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-2xl bg-white p-6 shadow-xl">
        <div className="mb-5 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-slate-900">{isEdit ? '编辑自定义题单' : '新建自定义题单'}</h2>
          <button type="button" onClick={onClose} className="cursor-pointer text-slate-400 transition hover:text-slate-600">关闭</button>
        </div>

        <form onSubmit={submit} className="space-y-5">
          <div>
            <label className="mb-1 block text-sm font-medium text-slate-700">题单名称</label>
            <input
              value={name}
              onChange={(event) => setName(event.target.value)}
              maxLength={50}
              placeholder="例如：二分查找专项"
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
            />
          </div>

          <div>
            <div className="mb-2 flex items-center justify-between">
              <label className="block text-sm font-medium text-slate-700">题目（{slugs.length} 题）</label>
              <span className="text-xs text-slate-400">已选题目会直接加入题单</span>
            </div>

            <div className="mb-3 flex flex-wrap gap-1.5 rounded-lg border border-slate-200 bg-slate-50 p-2">
              {slugs.length === 0 ? (
                <span className="text-sm text-slate-400">还没有题目，从下方添加</span>
              ) : (
                slugs.map((slug) => (
                  <span key={slug} className="inline-flex items-center gap-1 rounded-full bg-indigo-50 py-0.5 pl-2.5 pr-1 text-xs text-indigo-700">
                    {slug}
                    <button
                      type="button"
                      onClick={() => removeSlug(slug)}
                      className="cursor-pointer rounded-full px-1 text-indigo-400 transition hover:bg-indigo-100 hover:text-indigo-700"
                      aria-label={`移除 ${slug}`}
                    >
                      ×
                    </button>
                  </span>
                ))
              )}
            </div>

            <div className="mb-3 flex flex-wrap gap-1.5 border-b border-slate-100 pb-3">
              {tabs.map((t) => (
                <button
                  key={t.id}
                  type="button"
                  onClick={() => setTab(t.id)}
                  className={`cursor-pointer rounded-lg px-3 py-1.5 text-sm transition ${
                    tab === t.id ? 'bg-indigo-600 text-white' : 'text-slate-500 hover:bg-slate-100'
                  }`}
                >
                  {t.label}
                </button>
              ))}
            </div>

            {tab === 'manual' && (
              <div className="flex gap-2">
                <input
                  value={manualInput}
                  onChange={(event) => setManualInput(event.target.value)}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter') {
                      event.preventDefault();
                      addManual();
                    }
                  }}
                  placeholder="输入题目 slug，逗号或空格分隔，如 two-sum, 3sum"
                  className="flex-1 rounded-lg border border-slate-300 px-3 py-2 text-sm"
                />
                <button
                  type="button"
                  onClick={addManual}
                  className="cursor-pointer rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-indigo-500 active:scale-95"
                >
                  添加
                </button>
              </div>
            )}

            {tab === 'solved' && (
              <div>
                {solved.length === 0 ? (
                  <p className="text-sm text-slate-400">暂无已做题目，先同步或刷几道题吧。</p>
                ) : (
                  <ul className="max-h-56 space-y-1 overflow-auto rounded-lg border border-slate-100 p-1">
                    {solved.map((p) => (
                      <li key={p.slug} className="flex items-center justify-between gap-2 rounded-md px-2 py-1.5 text-sm hover:bg-slate-50">
                        <span className="truncate text-slate-700">{p.title}</span>
                        <span className="shrink-0 text-xs text-slate-400">{p.slug}</span>
                        <button
                          type="button"
                          onClick={() => addSlug(p.slug)}
                          disabled={slugs.includes(p.slug)}
                          className={`shrink-0 cursor-pointer rounded-md px-2 py-1 text-xs transition ${
                            slugs.includes(p.slug)
                              ? 'cursor-not-allowed bg-slate-100 text-slate-300'
                              : 'bg-indigo-50 text-indigo-600 hover:bg-indigo-100'
                          }`}
                        >
                          {slugs.includes(p.slug) ? '已添加' : '添加'}
                        </button>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            )}

            {tab === 'search' && (
              <div className="space-y-2">
                <div className="flex gap-2">
                  <input
                    value={searchKeyword}
                    onChange={(event) => setSearchKeyword(event.target.value)}
                    onKeyDown={(event) => {
                      if (event.key === 'Enter') {
                        event.preventDefault();
                        doSearch();
                      }
                    }}
                    placeholder="输入题目名或关键词，如 two sum、二分"
                    className="flex-1 rounded-lg border border-slate-300 px-3 py-2 text-sm"
                  />
                  <button
                    type="button"
                    onClick={doSearch}
                    disabled={searching}
                    className="cursor-pointer rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-indigo-500 active:scale-95 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    {searching ? '搜索中…' : '搜索'}
                  </button>
                </div>
                {searchError && <p className="text-xs text-red-500">{searchError}</p>}
                {searchResults.length > 0 && (
                  <ul className="max-h-56 space-y-1 overflow-auto rounded-lg border border-slate-100 p-1">
                    {searchResults.map((p) => (
                      <li key={p.titleSlug} className="flex items-center justify-between gap-2 rounded-md px-2 py-1.5 text-sm hover:bg-slate-50">
                        <span className="truncate text-slate-700">
                          {p.translatedTitle || p.title || p.titleSlug}
                          <span className="ml-2 text-xs text-slate-400">{p.difficulty}</span>
                        </span>
                        <span className="shrink-0 text-xs text-slate-400">{p.titleSlug}</span>
                        <button
                          type="button"
                          onClick={() => addSlug(p.titleSlug)}
                          disabled={slugs.includes(p.titleSlug)}
                          className={`shrink-0 cursor-pointer rounded-md px-2 py-1 text-xs transition ${
                            slugs.includes(p.titleSlug)
                              ? 'cursor-not-allowed bg-slate-100 text-slate-300'
                              : 'bg-indigo-50 text-indigo-600 hover:bg-indigo-100'
                          }`}
                        >
                          {slugs.includes(p.titleSlug) ? '已添加' : '添加'}
                        </button>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            )}

            {tab === 'plans' && (
              <div className="space-y-2">
                {plansError && <p className="text-xs text-red-500">{plansError}</p>}
                {plansLoading ? (
                  <p className="text-sm text-slate-400">加载官方题单中…</p>
                ) : plans.length === 0 ? (
                  <p className="text-sm text-slate-400">未获取到官方题单，可点「重新加载」。</p>
                ) : (
                  <div className="flex flex-wrap gap-1.5">
                    {plans.map((plan) => (
                      <button
                        key={plan.slug}
                        type="button"
                        onClick={() => setSelectedPlan(plan.slug)}
                        className={`cursor-pointer rounded-lg border px-3 py-2 text-left text-sm transition ${
                          selectedPlan === plan.slug
                            ? 'border-indigo-500 bg-indigo-50 text-indigo-700'
                            : 'border-slate-200 text-slate-600 hover:bg-slate-50'
                        }`}
                      >
                        <span className="block font-medium">{plan.name}</span>
                        <span className="block text-xs text-slate-400">{plan.questionNum ?? '?'} 题</span>
                      </button>
                    ))}
                  </div>
                )}
                <div className="flex items-center gap-2 pt-1">
                  <button
                    type="button"
                    onClick={loadPlans}
                    disabled={plansLoading}
                    className="cursor-pointer rounded-lg border border-slate-300 px-3 py-1.5 text-xs text-slate-600 transition hover:bg-slate-50"
                  >
                    重新加载
                  </button>
                  <button
                    type="button"
                    onClick={importPlan}
                    disabled={!selectedPlan || importing}
                    className="cursor-pointer rounded-lg bg-indigo-600 px-3 py-1.5 text-xs font-medium text-white transition hover:bg-indigo-500 active:scale-95 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    {importing ? '导入中…' : '导入题目'}
                  </button>
                  {noticeText && <span className="text-xs text-emerald-600">{noticeText}</span>}
                </div>
              </div>
            )}
          </div>

          <div className="flex justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="cursor-pointer rounded-lg border border-slate-300 px-4 py-2 text-sm text-slate-700 transition hover:bg-slate-50 active:scale-95"
            >
              取消
            </button>
            <button
              type="submit"
              disabled={!addable || saving}
              className="cursor-pointer rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-indigo-500 active:scale-95 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {saving ? '保存中…' : isEdit ? '保存修改' : '创建题单'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
