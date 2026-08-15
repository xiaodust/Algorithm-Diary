package com.algodiary.service;

import com.algodiary.dto.GoalView;
import com.algodiary.dto.ListProgress;
import com.algodiary.model.ProblemList;
import com.algodiary.model.UserGoal;
import com.algodiary.store.AlgoStore;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GoalServiceTest {

    @Test
    void createsDefaultGoalWhenNoneExists() {
        AlgoStore store = mock(AlgoStore.class);
        ProblemListService listService = mock(ProblemListService.class);
        ProblemList list = new ProblemList("hot-100", "Hot 100", "BUILTIN", List.of("a", "b", "c", "d"));
        ListProgress progress = new ListProgress("hot-100", "Hot 100", 4, 1, 3, 0.5, 6);

        when(listService.getActiveList()).thenReturn(list);
        when(listService.getProgress(list)).thenReturn(progress);
        when(store.findGoal()).thenReturn(Optional.empty());

        GoalService service = new GoalService(store, listService);
        GoalView view = service.getGoalView();

        assertThat(view.targetType()).isEqualTo(GoalService.TARGET_COMPLETE_LIST);
        assertThat(view.target()).isEqualTo(4);
        assertThat(view.dailyTarget()).isEqualTo(3);
        assertThat(view.remaining()).isEqualTo(3);
        verify(store).saveGoal(any(UserGoal.class));
    }

    @Test
    void savesCustomSolveCountGoal() {
        AlgoStore store = mock(AlgoStore.class);
        ProblemListService listService = mock(ProblemListService.class);
        ProblemList list = new ProblemList("hot-100", "Hot 100", "BUILTIN", List.of("a", "b", "c", "d"));
        ListProgress progress = new ListProgress("hot-100", "Hot 100", 4, 1, 3, 0.5, 6);

        when(listService.getActiveList()).thenReturn(list);
        when(listService.getProgress(list)).thenReturn(progress);
        when(store.findGoal()).thenReturn(Optional.of(new UserGoal("hot-100", "SOLVE_COUNT", 20, 5)));

        GoalService service = new GoalService(store, listService);
        GoalView view = service.saveGoal("SOLVE_COUNT", 20, 5);

        assertThat(view.targetType()).isEqualTo("SOLVE_COUNT");
        assertThat(view.target()).isEqualTo(20);
        assertThat(view.dailyTarget()).isEqualTo(5);
        verify(store).saveGoal(any(UserGoal.class));
    }
}
