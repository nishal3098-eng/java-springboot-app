package com.taskflow;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    public record Task(long id, String title, boolean completed) {}
    public record NewTask(String title) {}
    private final Map<Long, Task> tasks = new ConcurrentSkipListMap<>();
    private final AtomicLong ids = new AtomicLong();
    @GetMapping public List<Task> list() { return List.copyOf(tasks.values()); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Task add(@RequestBody NewTask input) {
        if (input.title() == null || input.title().isBlank() || input.title().length() > 200)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a title of 1-200 characters");
        Task task = new Task(ids.incrementAndGet(), input.title().trim(), false);
        tasks.put(task.id(), task); return task;
    }
    @PutMapping("/{id}") public Task toggle(@PathVariable long id) {
        Task task = tasks.computeIfPresent(id, (key, old) -> new Task(key, old.title(), !old.completed()));
        if (task == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return task;
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) { tasks.remove(id); }
}
