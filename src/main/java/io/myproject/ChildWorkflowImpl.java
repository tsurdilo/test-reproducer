package io.myproject;

import io.temporal.workflow.Workflow;

import java.time.Duration;

public class ChildWorkflowImpl implements ChildWorkflow {
    @Override
    public String executeChild() {
        Workflow.sleep(Duration.ofSeconds(50));
        return "Child workflow done";
    }
}
