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
});
