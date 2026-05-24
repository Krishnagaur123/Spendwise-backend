package com.krishna.Spendwise.controller.api;

import com.krishna.Spendwise.domain.dto.api.GoalRequest;
import com.krishna.Spendwise.domain.dto.api.GoalResponse;
import com.krishna.Spendwise.domain.dto.api.GoalUpdateRequest;
import com.krishna.Spendwise.domain.dto.api.MessageResponse;
import com.krishna.Spendwise.service.GoalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

    @PostMapping
    public ResponseEntity<GoalResponse> createGoal(@Valid @RequestBody GoalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(goalService.createGoal(request));
    }

    @GetMapping
    public ResponseEntity<Map<String, List<GoalResponse>>> getAllGoals() {
        return ResponseEntity.ok(Map.of("goals", goalService.getAllGoals()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GoalResponse> getGoal(@PathVariable Long id) {
        return ResponseEntity.ok(goalService.getGoalById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GoalResponse> updateGoal(@PathVariable Long id, @Valid @RequestBody GoalUpdateRequest request) {
        return ResponseEntity.ok(goalService.updateGoal(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteGoal(@PathVariable Long id) {
        goalService.deleteGoal(id);
        return ResponseEntity.ok(new MessageResponse("Goal deleted successfully"));
    }
}
