package com.algodiary.controller;

import com.algodiary.model.MistakeNote;
import com.algodiary.model.ProblemState;
import com.algodiary.model.Review;
import com.algodiary.service.MistakeService;
import com.algodiary.store.AlgoStore;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/mistakes")
public class MistakeController {

    private final AlgoStore store;
    private final MistakeService mistakeService;

    public MistakeController(AlgoStore store, MistakeService mistakeService) {
        this.store = store;
        this.mistakeService = mistakeService;
    }

    @GetMapping
    public List<MistakeNote> getAll() {
        return store.findAllMistakes();
    }

    @PostMapping("/{slug}/note")
    public MistakeNote saveNote(@PathVariable String slug, @RequestBody MistakeNote note) {
        MistakeNote saved = new MistakeNote(slug, note.errorType(), note.stuckPoint(), note.lesson(), note.similarProblems());
        store.saveMistake(saved);
        return saved;
    }

    @PostMapping("/{slug}/review")
    public void review(@PathVariable String slug, @RequestBody ReviewRequest request) {
        Instant now = Instant.now();
        ProblemState current = store.findState(slug).orElse(ProblemState.empty(slug));
        Instant next = mistakeService.nextReview(current, request.passed(), now);
        store.saveReview(new Review(slug, now, request.passed(), request.notes()));
        List<Review> reviews = store.findReviews(slug);
        boolean graduate = request.passed() && mistakeService.shouldGraduate(reviews);

        ProblemState updated = new ProblemState(
                slug,
                request.passed() ? Math.min(4, current.masteryLevel() + 1) : Math.max(0, current.masteryLevel() - 1),
                current.acCount(),
                current.attemptCount(),
                !request.passed() && !graduate,
                request.passed() ? null : current.mistakeType(),
                now,
                next,
                current.reviewCount() + 1,
                current.firstAcAt()
        );
        store.saveState(updated);
        if (graduate) {
            store.deleteMistake(slug);
        }
    }

    public record ReviewRequest(boolean passed, String notes) {
    }
}
