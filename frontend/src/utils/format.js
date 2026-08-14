export function leetCodeUrl(slug) {
  return `https://leetcode.cn/problems/${slug}/`;
}

export function reasonLabel(reason) {
  const labels = {
    MISTAKE: '错题',
    REVIEW: '复习',
    WEAK_TOPIC: '薄弱题型',
    LIST_NEW: '新题',
    DAILY: '每日一题',
    FREE: '自由'
  };
  return labels[reason] ?? reason;
}
