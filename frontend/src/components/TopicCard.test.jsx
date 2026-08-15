import { render, screen, fireEvent } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import TopicCard from './TopicCard.jsx';

describe('TopicCard', () => {
  it('renders weak topics and supports topic selection', () => {
    const onSelectTopic = vi.fn();
    const onShowAll = vi.fn();

    render(
      <TopicCard
        weakTopics={[{ topicId: 'dp', acRate: 0.4, problemCount: 5 }]}
        topics={[{ id: 'dp', name: '动态规划' }]}
        onSelectTopic={onSelectTopic}
        onShowAll={onShowAll}
      />
    );

    expect(screen.getByText('动态规划 · AC 40%')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /动态规划/ }));
    expect(onSelectTopic).toHaveBeenCalledWith('dp');

    fireEvent.click(screen.getByRole('button', { name: '查看全部题型' }));
    expect(onShowAll).toHaveBeenCalled();
  });
});
