import { leetCodeUrl } from '../../utils/format.js';

export default function TopicsModal({
  stats,
  topics,
  selectedTopicId,
  problems,
  trend = [],
  problemTitles,
  onSelectTopic,
  onClose
}) {
  const topicName = (id) => topics.find((topic) => topic.id === id)?.name ?? id;
  const displayTitle = (slug) => problemTitles[slug] || slug;
  const selected = stats.find((stat) => stat.topicId === selectedTopicId);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4" onClick={onClose}>
      <div className="max-h-[90vh] w-full max-w-4xl overflow-y-auto rounded-2xl bg-white p-6 shadow-xl" onClick={(event) => event.stopPropagation()}>
        <div className="mb-5 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-slate-900">题型详情</h2>
          <button type="button" onClick={onClose} className="cursor-pointer text-slate-400 transition hover:text-slate-600">关闭</button>
        </div>

        <div className="grid grid-cols-1 gap-6 md:grid-cols-[240px_1fr]">
          <div className="space-y-2">
            {stats.map((stat) => (
              <button
                key={stat.topicId}
                type="button"
                onClick={() => onSelectTopic(stat.topicId)}
                className={`w-full cursor-pointer rounded-lg px-3 py-2 text-left text-sm transition active:scale-95 ${
                  stat.topicId === selectedTopicId ? 'bg-indigo-50 text-indigo-700' : 'text-slate-600 hover:bg-slate-50'
                }`}
              >
                <span className="block font-medium">{topicName(stat.topicId)}</span>
                <span className="block text-xs text-slate-400">
                  AC {Math.round(stat.acRate * 100)}% · {stat.problemCount} 题
                </span>
              </button>
            ))}
          </div>

          <div>
            {selected ? (
              <>
                <div className="mb-4 grid grid-cols-2 gap-3 sm:grid-cols-4">
                  <Stat label="题目数" value={selected.problemCount} />
                  <Stat label="AC 率" value={`${Math.round(selected.acRate * 100)}%`} />
                  <Stat label="熟练度" value={selected.masteryAvg.toFixed(1)} />
                  <Stat label="遗忘率" value={`${Math.round(selected.forgetRate * 100)}%`} />
                </div>
                <TrendChart data={trend} />
                <ul className="space-y-2">
                  {problems.map((problem) => (
                    <li key={problem.slug} className="flex items-center justify-between gap-3 text-sm">
                      <a href={leetCodeUrl(problem.slug)} target="_blank" rel="noreferrer" className="font-medium text-slate-800 hover:underline">
                        {displayTitle(problem.slug)}
                      </a>
                      <span className={`shrink-0 rounded-full px-2 py-0.5 text-xs ${problem.solved ? 'bg-emerald-50 text-emerald-600' : 'bg-slate-100 text-slate-500'}`}>
                        {problem.solved ? '已做' : '未做'}
                      </span>
                    </li>
                  ))}
                </ul>
              </>
            ) : (
              <p className="text-slate-400">从左侧选择题型查看详情。</p>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

function Stat({ label, value }) {
  return (
    <div className="rounded-lg bg-slate-50 px-3 py-2">
      <div className="text-xs text-slate-400">{label}</div>
      <div className="text-sm font-semibold text-slate-700">{value}</div>
    </div>
  );
}

function TrendChart({ data }) {
  if (!data || data.length === 0) {
    return <p className="mb-4 text-xs text-slate-400">暂无趋势数据。</p>;
  }

  const width = 600;
  const height = 150;
  const padding = 20;
  const innerWidth = width - padding * 2;
  const innerHeight = height - padding * 2;

  const point = (index, value) => {
    const x = padding + (innerWidth * index) / Math.max(1, data.length - 1);
    const y = padding + innerHeight * (1 - Math.max(0, Math.min(1, value)));
    return `${x.toFixed(2)},${y.toFixed(2)}`;
  };

  const acLine = data.map((item, index) => point(index, item.acRate)).join(' ');
  const forgetLine = data.map((item, index) => point(index, item.forgetRate)).join(' ');

  return (
    <div className="mb-4 rounded-xl border border-slate-100 bg-slate-50 p-3">
      <div className="mb-2 flex items-center gap-4 text-xs">
        <span className="flex items-center gap-1 text-slate-500">
          <span className="inline-block h-1.5 w-4 rounded-full bg-emerald-500" />
          AC 率
        </span>
        <span className="flex items-center gap-1 text-slate-500">
          <span className="inline-block h-1.5 w-4 rounded-full bg-rose-400" />
          遗忘率
        </span>
      </div>
      <svg viewBox={`0 0 ${width} ${height}`} className="h-28 w-full" role="img" aria-label="题型趋势">
        <line x1={padding} y1={padding} x2={padding} y2={height - padding} stroke="#e2e8f0" strokeWidth="1" />
        <line x1={padding} y1={height - padding} x2={width - padding} y2={height - padding} stroke="#e2e8f0" strokeWidth="1" />
        <polyline points={acLine} fill="none" stroke="#10b981" strokeWidth="2" />
        <polyline points={forgetLine} fill="none" stroke="#fb7185" strokeWidth="2" />
      </svg>
      <div className="mt-1 flex justify-between text-[10px] text-slate-400">
        <span>{data[0]?.date}</span>
        <span>{data[data.length - 1]?.date}</span>
      </div>
    </div>
  );
}
