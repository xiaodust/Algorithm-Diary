import { leetCodeUrl } from '../utils/format.js';

export default function MistakeCard({ mistakes, problemTitles, onReviewMistake, onOpenMistakeNote }) {
  const displayTitle = (slug) => problemTitles[slug] || slug;
  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <h2 className="mb-4 text-lg font-semibold text-slate-900">错题本</h2>
      {mistakes.length === 0 ? (
        <p className="text-slate-500">还没有错题，继续保持。</p>
      ) : (
        <ul className="space-y-3">
          {mistakes.map((mistake) => (
            <li key={mistake.problemSlug} className="flex items-start justify-between gap-3 text-sm">
              <div className="min-w-0">
                <a href={leetCodeUrl(mistake.problemSlug)} target="_blank" rel="noreferrer" className="font-medium text-slate-800 hover:underline">
                  {displayTitle(mistake.problemSlug)}
                </a>
                <span className="ml-1 text-xs text-slate-400">{mistake.problemSlug}</span>
                <span className="ml-2 text-slate-500">{mistake.errorType ?? '未分类'}</span>
                {mistake.stuckPoint && <p className="mt-1 text-xs text-slate-400">{mistake.stuckPoint}</p>}
              </div>
              <div className="flex shrink-0 gap-1">
                <button
                  type="button"
                  onClick={() => onOpenMistakeNote(mistake.problemSlug)}
                  className="cursor-pointer rounded-md border border-slate-200 px-2 py-1 text-xs text-slate-600 transition hover:bg-slate-50 active:scale-95"
                >
                  复盘
                </button>
                <button
                  type="button"
                  onClick={() => onReviewMistake(mistake.problemSlug, true)}
                  className="cursor-pointer rounded-md bg-emerald-50 px-2 py-1 text-xs text-emerald-600 transition hover:bg-emerald-100 active:scale-95"
                >
                  通过
                </button>
                <button
                  type="button"
                  onClick={() => onReviewMistake(mistake.problemSlug, false)}
                  className="cursor-pointer rounded-md bg-rose-50 px-2 py-1 text-xs text-rose-600 transition hover:bg-rose-100 active:scale-95"
                >
                  未通过
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
