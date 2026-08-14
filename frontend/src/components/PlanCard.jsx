import { leetCodeUrl, reasonLabel } from '../utils/format.js';

export default function PlanCard({ plan, checkin, problemTitles, onCompletePlan, onOpenExplain }) {
  const displayTitle = (slug) => problemTitles[slug] || slug;
  const dueCount = [...(plan?.coreTasks ?? []), ...(plan?.bonusTasks ?? [])].filter(
    (task) => task.reason === 'REVIEW' || task.reason === 'MISTAKE'
  ).length;

  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <div className="mb-4 flex items-center justify-between gap-3">
        <h2 className="text-lg font-semibold text-slate-900">今日计划</h2>
        <div className="flex items-center gap-2">
          {checkin && (
            <span className="rounded-full bg-orange-50 px-2 py-1 text-xs font-medium text-orange-600">
              🔥 连续 {checkin.streak} 天
            </span>
          )}
          {dueCount > 0 && (
            <span className="rounded-full bg-blue-50 px-2 py-1 text-xs font-medium text-blue-600">
              今日待复习 {dueCount} 题
            </span>
          )}
          <button
            type="button"
            onClick={onCompletePlan}
            disabled={checkin?.completed}
            className="cursor-pointer rounded-md bg-emerald-600 px-3 py-1.5 text-xs font-semibold text-white transition hover:bg-emerald-500 active:scale-95 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {checkin?.completed ? '今日已打卡' : '完成今日打卡'}
          </button>
        </div>
      </div>

      {plan && plan.coreTasks?.length === 0 && plan.bonusTasks?.length === 0 ? (
        <p className="text-slate-500">暂无任务，先同步 LeetCode 数据吧。</p>
      ) : (
        <div className="space-y-2">
          {plan?.coreTasks?.map((task) => (
            <div
              key={`${task.problemSlug}-core`}
              className="flex items-center justify-between gap-3 rounded-lg border border-indigo-100 bg-indigo-50 px-3 py-2 text-sm transition"
            >
              <a href={leetCodeUrl(task.problemSlug)} target="_blank" rel="noreferrer" className="min-w-0 flex-1">
                <span className="block truncate">{displayTitle(task.problemSlug)}</span>
                <span className="block text-xs text-slate-400">{task.problemSlug}</span>
              </a>
              <span className="text-xs font-medium text-indigo-600">{reasonLabel(task.reason)}</span>
              <button
                type="button"
                onClick={() => onOpenExplain(task.problemSlug)}
                className="cursor-pointer rounded-md bg-white px-2 py-1 text-xs text-indigo-600 transition hover:bg-indigo-100 active:scale-95"
              >
                解析
              </button>
            </div>
          ))}
          {plan?.bonusTasks?.map((task) => (
            <div
              key={`${task.problemSlug}-bonus`}
              className="flex items-center justify-between gap-3 rounded-lg border border-slate-200 px-3 py-2 text-sm transition"
            >
              <a href={leetCodeUrl(task.problemSlug)} target="_blank" rel="noreferrer" className="min-w-0 flex-1">
                <span className="block truncate">{displayTitle(task.problemSlug)}</span>
                <span className="block text-xs text-slate-400">{task.problemSlug}</span>
              </a>
              <span className="text-xs text-slate-500">{reasonLabel(task.reason)} · 冲刺</span>
              <button
                type="button"
                onClick={() => onOpenExplain(task.problemSlug)}
                className="cursor-pointer rounded-md border border-slate-200 bg-white px-2 py-1 text-xs text-slate-600 transition hover:bg-slate-50 active:scale-95"
              >
                解析
              </button>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}
