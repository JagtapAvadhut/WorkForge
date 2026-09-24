package com.avadhoot.workforge.issue;

import com.avadhoot.workforge.issue.dto.IssueDtos.CreateIssueRequest;
import com.avadhoot.workforge.issue.dto.IssueResponse;
import com.avadhoot.workforge.project.ProjectService;
import com.avadhoot.workforge.project.dto.ProjectDtos.CreateProjectRequest;
import com.avadhoot.workforge.project.dto.ProjectDtos.ProjectResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class IssueKeyGenerationTest {

    @Autowired
    private ProjectService projectService;
    @Autowired
    private IssueService issueService;

    private CreateIssueRequest issueRequest(String projectKey, String summary) {
        return new CreateIssueRequest(
                projectKey, summary, null, "TASK", "MEDIUM", null,
                null, null, null, null, null, null);
    }

    @Test
    void concurrentIssueCreation_producesUniqueSequentialKeys() throws Exception {
        String key = "CON" + (System.nanoTime() % 100000);
        ProjectResponse project = projectService.create(
                new CreateProjectRequest(key, "Concurrency Project", null, "1", null));

        int threads = 8;
        int perThread = 5;
        int total = threads * perThread;

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        ConcurrentLinkedQueue<String> keys = new ConcurrentLinkedQueue<>();

        List<Callable<Void>> tasks = IntStream.range(0, threads).<Callable<Void>>mapToObj(t -> () -> {
            startGate.await();
            for (int i = 0; i < perThread; i++) {
                IssueResponse issue = issueService.create(issueRequest(project.key(), "Concurrent issue"));
                keys.add(issue.key());
            }
            return null;
        }).collect(Collectors.toList());

        List<Future<Void>> futures = tasks.stream().map(executor::submit).toList();
        startGate.countDown();
        for (Future<Void> future : futures) {
            future.get();
        }
        executor.shutdown();

        Set<String> uniqueKeys = Set.copyOf(keys);
        assertThat(keys).hasSize(total);
        assertThat(uniqueKeys).hasSize(total);

        Set<Integer> sequences = keys.stream()
                .map(k -> Integer.parseInt(k.substring(k.indexOf('-') + 1)))
                .collect(Collectors.toSet());
        Set<Integer> expected = IntStream.rangeClosed(1, total).boxed().collect(Collectors.toSet());
        assertThat(sequences).isEqualTo(expected);
    }
}
