import { leetCodeUrl } from '../../utils/format.js';

export default function ExplainModal({
  slug,
  title,
  level,
  text,
  explaining,
  onLevelChange,
  onClose
}) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4" onClick={onClose}>
      <div className="max-h-[90vh] w-full max-w-xl overflow-y-auto rounded-2xl bg-white p-6 shadow-xl" onClick={(event) => event.stopPropagation()}>
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-slate-900">题目解析</h2>
          <button type="button" onClick={onClose} className="cursor-pointer text-slate-400 transition hover:text-slate-600">关闭</button>
        </div>
        <div className="mb-4">
          <p className="font-medium text-slate-800">{title}</p>
          <p className="text-xs text-slate-400">{slug}</p>
        </div>
        <div className="mb-4 flex flex-wrap gap-2">
          {['方向提示', '关键提示', '完整思路'].map((label, index) => (
            <button
              key={label}
              type="button"
              onClick={() => onLevelChange(index)}
              disabled={explaining}
              className={`cursor-pointer rounded-lg px-3 py-2 text-sm transition active:scale-95 disabled:cursor-not-allowed disabled:opacity-50 ${
                level === index ? 'bg-indigo-600 text-white' : 'border border-slate-200 text-slate-600 hover:bg-slate-50'
              }`}
            >
              {label}
            </button>
          ))}
        </div>
        {explaining ? (
          <p className="text-sm text-slate-400">解析中…</p>
        ) : (
          text && <div className="whitespace-pre-wrap rounded-lg bg-slate-50 p-3 text-sm leading-6 text-slate-700">{text}</div>
        )}
        <a href={leetCodeUrl(slug)} target="_blank" rel="noreferrer" className="mt-4 inline-block text-sm font-medium text-indigo-600 hover:underline">
          去 leetcode.cn 做这道题 →
        </a>
      </div>
    </div>
  );
}
