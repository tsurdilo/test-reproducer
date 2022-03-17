package io.myproject;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionRequest;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionResponse;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.testing.TestWorkflowRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class ChildWorkflowPolicyTest {
    @Rule
    public TestWorkflowRule testWorkflowRule =
            TestWorkflowRule.newBuilder()
                    .setWorkflowTypes(ParentWorkflowImpl.class, ChildWorkflowImpl.class)
                    .build();

    @Test
    public void testAsyncChildWorkflow() {
        ParentWorkflow parentWorkflow =
                testWorkflowRule
                        .getWorkflowClient()
                        .newWorkflowStub(
                                ParentWorkflow.class,
                                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());

        // Start parent async
        WorkflowExecution parentExecution = WorkflowClient.start(parentWorkflow::executeParent);
        // Get untyped stub
        WorkflowStub untypedParent = WorkflowStub.fromTyped(parentWorkflow);
        // Wait for parent completion and get child execution
        WorkflowExecution childExecution = untypedParent.getResult(WorkflowExecution.class);

        assertNotNull(childExecution);

        Assert.assertEquals("WORKFLOW_EXECUTION_STATUS_COMPLETED", getExecutionStatus(parentExecution));
        // not really needed sleep but ok still lets do it...(can be removed)
        try {
            Thread.sleep(1 * 1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Child should have terminated when parent completed
        Assert.assertEquals("WORKFLOW_EXECUTION_STATUS_TERMINATED", getExecutionStatus(parentExecution));
    }

    private String getExecutionStatus(WorkflowExecution execution) {
        DescribeWorkflowExecutionResponse resp =
                testWorkflowRule
                        .getWorkflowClient()
                        .getWorkflowServiceStubs()
                        .blockingStub()
                        .describeWorkflowExecution(
                                DescribeWorkflowExecutionRequest.newBuilder()
                                        .setNamespace(testWorkflowRule.getTestEnvironment().getNamespace())
                                        .setExecution(execution)
                                        .build());

        return resp.getWorkflowExecutionInfo().getStatus().toString();
    }
}
