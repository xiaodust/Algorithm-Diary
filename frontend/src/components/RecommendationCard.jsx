export default function RecommendationCard({ recommendations, problemTitles }) {
  const displayTitle = (slug) => problemTitles[slug] || slug;
  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <h2 className="mb-4 text-lg font-semibold text-slate-900">推荐题目</h2>
      {recommendations.length === 0 ? (
        <p className="text-slate-500">暂无推荐，先同步数据或完成题单内题目。</p>
      ) : (
        <ul className="space-y-3">
          {recommendations.map((item) => (
            <li key={item.problemSlug} className="text-sm">
              <a href={item.url} target="_blank" rel="noreferrer" className="font-medium text-indigo-600 hover:underline">
                {displayTitle(item.problemSlug)}
              </a>
              <span className="ml-1 text-xs text-slate-400">{item.problemSlug}</span>
              <span className="ml-2 text-slate-500">{item.reason}</span>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
