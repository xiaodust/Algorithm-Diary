package com.algodiary.controller;

import com.algodiary.model.TutorMessage;
import com.algodiary.model.TutorSession;
import com.algodiary.service.TutorService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/tutor")
public class TutorController {

    private static final ExecutorService SSE_EXECUTOR = Executors.newCachedThreadPool();

    private final TutorService tutorService;

    public TutorController(TutorService tutorService) {
        this.tutorService = tutorService;
    }

    // ==================== 会话管理 ====================

    @GetMapping("/sessions")
    public List<TutorSession> sessions() {
        return tutorService.sessions();
    }

    @PostMapping("/sessions")
    public TutorSession createSession(@RequestBody(required = false) CreateSessionRequest request) {
        String name = request == null ? null : request.name();
        return tutorService.createSession(name);
    }

    @PostMapping("/sessions/{sessionId}/rename")
    public Map<String, Object> renameSession(@PathVariable String sessionId,
                                             @RequestBody RenameRequest request) {
        tutorService.renameSession(sessionId, request.name());
        return Map.of("ok", true);
    }

    @DeleteMapping("/sessions/{sessionId}")
    public Map<String, Object> deleteSession(@PathVariable String sessionId) {
        tutorService.deleteSession(sessionId);
        return Map.of("ok", true);
    }

    @PostMapping("/clear")
    public Map<String, Object> clearSession(@RequestBody SessionRequest request) {
        tutorService.clearSession(request.sessionId());
        return Map.of("ok", true);
    }

    @GetMapping("/history")
    public List<TutorMessage> history(@RequestParam String sessionId,
                                      @RequestParam(defaultValue = "20") int limit) {
        return tutorService.history(sessionId, limit);
    }

    // ==================== 对话 ====================

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(120_000L);
        SSE_EXECUTOR.execute(() -> {
            try {
                tutorService.chatStream(request.sessionId(), request.message(), delta -> {
                    try {
                        emitter.send(SseEmitter.event().data(Map.of("delta", delta)));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                emitter.send(SseEmitter.event().data(Map.of("delta", "[DONE]")));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody ChatRequest request) {
        String reply = tutorService.chat(request.sessionId(), request.message());
        return Map.of("reply", reply);
    }

    // ==================== 记忆沉淀 ====================

    @PostMapping("/remember")
    public Map<String, Object> remember(@RequestBody RememberRequest request) {
        List<String> facts = tutorService.rememberFact(request.fact());
        return Map.of("ok", true, "facts", facts);
    }

    @PostMapping("/sessions/{sessionId}/summarize")
    public Map<String, Object> summarize(@PathVariable String sessionId) {
        List<String> facts = tutorService.summarizeSession(sessionId);
        return Map.of("facts", facts);
    }

    // ==================== 画像快照 ====================

    @GetMapping("/profile")
    public Map<String, Object> profile() {
        return tutorService.profileSnapshot();
    }

    public record CreateSessionRequest(String name) {
    }

    public record RenameRequest(String name) {
    }

    public record SessionRequest(String sessionId) {
    }

    public record ChatRequest(String sessionId, String message) {
    }

    public record RememberRequest(String fact) {
    }
}
