import MarkdownText from './MarkdownText.jsx';

export default function InsightCard({ insight, onRefreshInsight }) {
  if (!insight) {
    return null;
  }
  return (
    <section className="mb-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <div className="mb-2 flex items-center justify-between">
        <h2 className="text-lg font-semibold text-slate-900">AI 洞察 / 周报</h2>
        <button
          type="button"
          onClick={onRefreshInsight}
          className="cursor-pointer rounded-md border border-slate-200 px-2 py-1 text-xs text-slate-600 transition hover:bg-slate-50 active:scale-95"
        >
          刷新周报
        </button>
      </div>
      <MarkdownText content={insight} />
    </section>
  );
}
