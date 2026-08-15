async function request(path, options = {}) {
  const response = await fetch(path, {
    headers: { 'Content-Type': 'application/json' },
    ...options
  });
  if (!response.ok) {
    throw new Error(`请求失败: ${response.status}`);
  }
  const text = await response.text();
  return text ? JSON.parse(text) : null;
}

export const api = {
  getLists: () => request('/api/lists'),
  refreshLists: () => request('/api/lists/refresh', { method: 'POST' }),
  getActiveList: () => request('/api/lists/active'),
  getGoal: () => request('/api/goal'),
  saveGoal: (payload) =>
    request('/api/goal', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  setActiveList: (listId) =>
    request('/api/lists/active', {
      method: 'POST',
      body: JSON.stringify({ listId })
    }),
  sync: () => request('/api/sync', { method: 'POST' }),
  getPlan: () => request('/api/plan/today'),
  getPlanStatus: () => request('/api/plan/status'),
  completePlan: () => request('/api/plan/complete', { method: 'POST' }),
  getTopics: () => request('/api/topics'),
  getTopicStats: () => request('/api/topics/stats'),
  getTopicProblems: (topicId) => request(`/api/topics/${topicId}/problems`),
  getTopicTrend: (topicId, days = 30) =>
    request(`/api/topics/${topicId}/trend?days=${days}`),
  getRecommendations: () => request('/api/recommendations'),
  getMistakes: () => request('/api/mistakes'),
  getInsight: () => request('/api/insights/summary'),
  refreshInsight: () => request('/api/insights/refresh', { method: 'POST' }),
  getProblemTitles: () => request('/api/problems/titles'),
  reviewMistake: (slug, passed, notes = '') =>
    request(`/api/mistakes/${slug}/review`, {
      method: 'POST',
      body: JSON.stringify({ passed, notes })
    }),
  saveMistakeNote: (slug, note) =>
    request(`/api/mistakes/${slug}/note`, {
      method: 'POST',
      body: JSON.stringify(note)
    }),
  getLeetCodeSettings: () => request('/api/settings/leetcode'),
  saveLeetCodeSettings: (payload) =>
    request('/api/settings/leetcode', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  getLlmSettings: () => request('/api/settings/llm'),
  saveLlmSettings: (payload) =>
    request('/api/settings/llm', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  explain: (problemSlug, hintLevel) =>
    request('/api/explain', {
      method: 'POST',
      body: JSON.stringify({ problemSlug, hintLevel })
    })
};
