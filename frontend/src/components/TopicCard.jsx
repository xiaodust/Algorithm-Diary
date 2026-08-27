export default function TopicCard({ weakTopics, topics, onSelectTopic, onShowAll }) {
  const topicName = (id) => topics.find((topic) => topic.id === id)?.name ?? id;
  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-lg font-semibold text-slate-900">薄弱题型</h2>
        <button
          type="button"
          onClick={onShowAll}
          className="cursor-pointer rounded-md border border-slate-200 px-2 py-1 text-xs text-slate-600 transition hover:bg-slate-50 active:scale-95"
        >
          查看全部题型
        </button>
      </div>
      {weakTopics.length === 0 ? (
        <p className="text-slate-500">暂无足够数据判断薄弱点，多刷几道题再看看。</p>
      ) : (
        <div className="flex flex-wrap gap-2">
          {weakTopics.map((stat) => (
            <button
              key={stat.topicId}
              type="button"
              onClick={() => onSelectTopic(stat.topicId)}
              className="cursor-pointer rounded-full bg-rose-50 px-3 py-1 text-sm text-rose-600 transition hover:bg-rose-100 active:scale-95"
            >
              {topicName(stat.topicId)} · AC {Math.round(stat.acRate * 100)}%
            </button>
          ))}
        </div>
      )}
    </section>
  );
}
