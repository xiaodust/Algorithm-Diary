import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import TutorDrawer from './TutorDrawer.jsx';

const mockApi = vi.hoisted(() => ({
  tutorSessions: vi.fn().mockResolvedValue([
    { id: 's1', name: '会话一', createdAt: '2026-01-01', updatedAt: '2026-01-02' }
  ]),
  tutorCreateSession: vi.fn().mockResolvedValue({ id: 's-new', name: '新对话' }),
  tutorDeleteSession: vi.fn().mockResolvedValue({ ok: true }),
  tutorHistory: vi.fn().mockResolvedValue([
    { role: 'user', content: '我的薄弱点' },
    { role: 'assistant', content: '从数据看，DP 较弱' }
  ]),
  tutorChatStream: vi.fn(async (sessionId, message, onDelta) => {
    for (const ch of '好的') onDelta(ch);
  }),
  tutorSummarize: vi.fn().mockResolvedValue({ facts: ['用户目标'] })
}));

vi.mock('../../services/api.js', () => ({ api: mockApi }));

describe('TutorDrawer', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders drawer when open', async () => {
    render(<TutorDrawer open onClose={() => {}} llmConfigured onNotice={() => {}} />);
    await waitFor(() => {
      expect(screen.getByText(/AI 算法助教/)).toBeTruthy();
    });
  });

  it('loads sessions and history', async () => {
    render(<TutorDrawer open onClose={() => {}} llmConfigured onNotice={() => {}} />);
    await waitFor(() => {
      expect(mockApi.tutorSessions).toHaveBeenCalled();
      expect(mockApi.tutorHistory).toHaveBeenCalled();
    });
    expect(screen.getByText(/DP 较弱/)).toBeTruthy();
    expect(screen.getByText('会话一')).toBeTruthy();
  });

  it('sends message via stream and renders reply', async () => {
    render(<TutorDrawer open onClose={() => {}} llmConfigured onNotice={() => {}} />);
    await waitFor(() => expect(screen.getByPlaceholderText(/输入问题/)).toBeTruthy());

    const textarea = screen.getByPlaceholderText(/输入问题/);
    fireEvent.change(textarea, { target: { value: '你好' } });
    fireEvent.click(screen.getByText('发送'));

    await waitFor(() => {
      expect(mockApi.tutorChatStream).toHaveBeenCalledWith('s1', '你好', expect.any(Function));
    });
    await waitFor(() => {
      expect(screen.getByText(/好的/)).toBeTruthy();
    });
  });

  it('sends quick question', async () => {
    render(<TutorDrawer open onClose={() => {}} llmConfigured onNotice={() => {}} />);
    await waitFor(() => expect(screen.getByText('分析错题')).toBeTruthy());
    fireEvent.click(screen.getByText('分析错题'));
    await waitFor(() => {
      expect(mockApi.tutorChatStream).toHaveBeenCalledWith('s1', expect.stringContaining('错题'), expect.any(Function));
    });
  });

  it('shows warning when llm not configured', () => {
    render(<TutorDrawer open onClose={() => {}} llmConfigured={false} onNotice={() => {}} />);
    expect(screen.getByText(/未配置 AI 模型/)).toBeTruthy();
  });

  it('handles explain request automatically', async () => {
    render(
      <TutorDrawer
        open
        onClose={() => {}}
        llmConfigured
        onNotice={() => {}}
        explainRequest={{ slug: 'two-sum', title: '两数之和', level: 0 }}
        onExplainHandled={() => {}}
      />
    );
    await waitFor(() => {
      expect(mockApi.tutorChatStream).toHaveBeenCalledWith(
        's1',
        expect.stringContaining('请解析题目 two-sum'),
        expect.any(Function)
      );
    });
  });

  it('summarizes session', async () => {
    render(<TutorDrawer open onClose={() => {}} llmConfigured onNotice={() => {}} />);
    await waitFor(() => expect(screen.getByText('提炼记忆')).toBeTruthy());
    fireEvent.click(screen.getByText('提炼记忆'));
    await waitFor(() => {
      expect(mockApi.tutorSummarize).toHaveBeenCalledWith('s1');
    });
  });

  it('does not crash when history loads during streaming (S.id regression)', async () => {
    // 模拟：流式回复进行中，历史请求延迟返回并覆盖消息数组
    let resolveHistory;
    mockApi.tutorHistory.mockReturnValueOnce(
      new Promise((resolve) => {
        resolveHistory = resolve;
      })
    );
    // 流式回调分批推送，制造"流式中历史覆盖"的竞态
    mockApi.tutorChatStream.mockImplementationOnce(async (sessionId, message, onDelta) => {
      onDelta('第一条');
      // 历史加载完成，覆盖消息数组（旧代码此处产生稀疏数组）
      resolveHistory([{ role: 'user', content: '旧历史' }]);
      await new Promise((r) => setTimeout(r, 10));
      onDelta('第二条');
    });

    render(<TutorDrawer open onClose={() => {}} llmConfigured onNotice={() => {}} />);
    await waitFor(() => expect(screen.getByPlaceholderText(/输入问题/)).toBeTruthy());

    const textarea = screen.getByPlaceholderText(/输入问题/);
    fireEvent.change(textarea, { target: { value: '你好' } });
    fireEvent.click(screen.getByText('发送'));

    // 关键断言：不抛异常，且流式内容正常渲染（而非 S.id undefined 崩溃）
    await waitFor(() => {
      expect(screen.getByText(/第一条/)).toBeTruthy();
    });
    await waitFor(() => {
      expect(screen.getByText(/第二条/)).toBeTruthy();
    });
    // 旧历史保留
    expect(screen.getByText('旧历史')).toBeTruthy();
  });

  it('renders markdown and code blocks', async () => {
    mockApi.tutorHistory.mockResolvedValue([
      {
        role: 'assistant',
        content: '**加粗文字**\n```java\nint x = 1;\n```\n- 列表项一\n- 列表项二'
      }
    ]);
    render(<TutorDrawer open onClose={() => {}} llmConfigured onNotice={() => {}} />);
    await waitFor(() => {
      expect(screen.getByText('加粗文字')).toBeTruthy();
    });
    expect(screen.getByText('int x = 1;')).toBeTruthy();
    expect(screen.getByText('列表项一')).toBeTruthy();
    expect(screen.getByText('列表项二')).toBeTruthy();
    expect(screen.getByText('java')).toBeTruthy();
  });
});
