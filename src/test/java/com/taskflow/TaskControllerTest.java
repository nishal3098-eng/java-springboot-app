package com.taskflow;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TaskControllerTest {
    @Test void taskLifecycle() {
        TaskController controller = new TaskController();
        var task = controller.add(new TaskController.NewTask("Build application"));
        assertEquals(1, controller.list().size());
        assertTrue(controller.toggle(task.id()).completed());
        controller.delete(task.id());
        assertTrue(controller.list().isEmpty());
    }
}
