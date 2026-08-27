import { useEffect, useState } from 'react';

export default function GoalModal({ goal, saving, onSave, onClose }) {
  const [targetType, setTargetType] = useState(goal?.targetType ?? 'COMPLETE_LIST');
  const [target, setTarget] = useState(goal?.target ?? goal?.total ?? 100);
  const [dailyTarget, setDailyTarget] = useState(goal?.dailyTarget ?? 3);

  useEffect(() => {
    if (goal) {
      setTargetType(goal.targetType ?? 'COMPLETE_LIST');
      setTarget(goal.target ?? goal.total ?? 100);
      setDailyTarget(goal.dailyTarget ?? 3);
    }
  }, [goal]);

  const submit = (event) => {
    event.preventDefault();
    onSave({
      targetType,
      target: Number(target),
      dailyTarget: Number(dailyTarget)
    });
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4">
      <div className="w-full max-w-lg rounded-2xl bg-white p-6 shadow-xl" onClick={(event) => event.stopPropagation()}>
        <div className="mb-5 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-slate-900">设置刷题目标</h2>
          <button type="button" onClick={onClose} className="cursor-pointer text-slate-400 transition hover:text-slate-600">关闭</button>
        </div>

        <form onSubmit={submit} className="space-y-5">
          <div>
            <label className="mb-2 block text-sm font-medium text-slate-700">长期目标</label>
            <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
              <button
                type="button"
                onClick={() => setTargetType('COMPLETE_LIST')}
                className={`rounded-lg border px-3 py-2 text-left text-sm transition ${
                  targetType === 'COMPLETE_LIST'
                    ? 'border-indigo-500 bg-indigo-50 text-indigo-700'
                    : 'border-slate-300 text-slate-600 hover:bg-slate-50'
                }`}
              >
                完成当前题单
              </button>
              <button
                type="button"
                onClick={() => setTargetType('SOLVE_COUNT')}
                className={`rounded-lg border px-3 py-2 text-left text-sm transition ${
                  targetType === 'SOLVE_COUNT'
                    ? 'border-indigo-500 bg-indigo-50 text-indigo-700'
                    : 'border-slate-300 text-slate-600 hover:bg-slate-50'
                }`}
              >
                自定义题数
              </button>
            </div>
          </div>

          <div>
            <label className="mb-1 block text-sm font-medium text-slate-700">目标题数</label>
            <input
              type="number"
              min="1"
              max="10000"
              value={target}
              onChange={(event) => setTarget(event.target.value)}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
            />
            <p className="mt-1 text-xs text-slate-400">完成当前题单时，会自动使用该题单总题数。</p>
          </div>

          <div>
            <label className="mb-1 block text-sm font-medium text-slate-700">每日目标题数</label>
            <input
              type="number"
              min="1"
              max="50"
              value={dailyTarget}
              onChange={(event) => setDailyTarget(event.target.value)}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
            />
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
              disabled={saving}
              className="cursor-pointer rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-indigo-500 active:scale-95 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {saving ? '保存中…' : '保存'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
