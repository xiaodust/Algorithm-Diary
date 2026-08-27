package com.algodiary.controller;

import com.algodiary.dto.GoalView;
import com.algodiary.service.GoalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GoalController.class)
class GoalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GoalService goalService;

    @Test
    void returnsCurrentGoal() throws Exception {
        when(goalService.getGoalView()).thenReturn(new GoalView(
                "hot-100",
                "Hot 100",
                "COMPLETE_LIST",
                100,
                3,
                100,
                42,
                58,
                12,
                42.0
        ));

        mockMvc.perform(get("/api/goal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyTarget").value(3))
                .andExpect(jsonPath("$.target").value(100));
    }

    @Test
    void rejectsInvalidGoalPayload() throws Exception {
        mockMvc.perform(post("/api/goal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetType\":\"\",\"target\":0,\"dailyTarget\":0}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
