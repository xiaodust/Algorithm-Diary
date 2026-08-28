async function request(path, options = {}) {
  const response = await fetch(path, {
    headers: { 'Content-Type': 'application/json' },
    ...options
  });
  if (!response.ok) {
    let detail = '';
    try {
      const body = await response.json();
      detail = body?.message || body?.error || '';
    } catch {
      // 忽略非 JSON 错误响应
    }
    throw new Error(`请求失败: ${response.status}${detail ? ` · ${detail}` : ''}`);
  }
  const text = await response.text();
  return text ? JSON.parse(text) : null;
}

export const api = {
  getLists: () => request('/api/lists'),
  refreshLists: () => request('/api/lists/refresh', { method: 'POST' }),
  getActiveList: () => request('/api/lists/active'),
  createList: (payload) =>
    request('/api/lists', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  updateList: (listId, payload) =>
    request(`/api/lists/${listId}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),
  deleteList: (listId) => request(`/api/lists/${listId}`, { method: 'DELETE' }),
  searchProblems: (keyword, limit = 20) =>
    request(`/api/lists/search?keyword=${encodeURIComponent(keyword)}&limit=${limit}`),
  getStudyPlans: () => request('/api/lists/study-plans'),
  importStudyPlan: (planSlug) =>
    request('/api/lists/import', {
      method: 'POST',
      body: JSON.stringify({ planSlug })
    }),
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
  getSolvedProblems: () => request('/api/problems/solved'),
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
    }),

  // ===== AI 助教 =====
  tutorSessions: () => request('/api/tutor/sessions'),
  tutorCreateSession: (name) =>
    request('/api/tutor/sessions', {
      method: 'POST',
      body: JSON.stringify({ name })
    }),
  tutorRenameSession: (sessionId, name) =>
    request(`/api/tutor/sessions/${sessionId}/rename`, {
      method: 'POST',
      body: JSON.stringify({ name })
    }),
  tutorDeleteSession: (sessionId) =>
    request(`/api/tutor/sessions/${sessionId}`, { method: 'DELETE' }),
  tutorHistory: (sessionId, limit = 20) =>
    request(`/api/tutor/history?sessionId=${sessionId}&limit=${limit}`),
  tutorClear: (sessionId) =>
    request('/api/tutor/clear', {
      method: 'POST',
      body: JSON.stringify({ sessionId })
    }),
  tutorRemember: (fact) =>
    request('/api/tutor/remember', {
      method: 'POST',
      body: JSON.stringify({ fact })
    }),
  tutorSummarize: (sessionId) =>
    request(`/api/tutor/sessions/${sessionId}/summarize`, { method: 'POST' }),
  tutorProfile: () => request('/api/tutor/profile'),
  tutorChat: (sessionId, message) =>
    request('/api/tutor/chat', {
      method: 'POST',
      body: JSON.stringify({ sessionId, message })
    }),
  tutorChatStream: async (sessionId, message, onDelta) => {
    const response = await fetch('/api/tutor/chat/stream', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ sessionId, message })
    });
    if (!response.ok) {
      let detail = '';
      try {
        const body = await response.json();
        detail = body?.message || body?.error || '';
      } catch {
        // 忽略
      }
      throw new Error(`请求失败: ${response.status}${detail ? ` · ${detail}` : ''}`);
    }
    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';
    for (;;) {
      const { done, value } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split('\n');
      buffer = lines.pop() ?? '';
      for (const line of lines) {
        const trimmed = line.trim();
        if (!trimmed.startsWith('data:')) continue;
        const payload = trimmed.slice(5).trim();
        if (!payload) continue;
        try {
          const parsed = JSON.parse(payload);
          const delta = parsed?.delta ?? '';
          if (delta === '[DONE]') return;
          if (delta) onDelta(delta);
        } catch {
          // 忽略解析失败的行
        }
      }
    }
  }
};
