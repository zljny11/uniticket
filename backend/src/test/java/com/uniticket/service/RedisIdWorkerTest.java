package com.uniticket.service;

import com.uniticket.utils.RedisIdWorker;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
public class RedisIdWorkerTest {

    @Resource
    private RedisIdWorker redisIdWorker;

    private ExecutorService es = Executors.newFixedThreadPool(500);

    /**
     * 测试分布式ID生成器的性能，以及可用性
     */
    @Test
    public void testNextId() throws InterruptedException {
        // 使用CountDownLatch让主线程等待所有任务完成，否则只能测到“提交任务”的耗时
        CountDownLatch latch = new CountDownLatch(300);
        // 创建线程任务：每个任务生成100个ID，完成后countDown
        Runnable task = () -> {
            for (int i = 0; i < 100; i++) {
                long id = redisIdWorker.nextId("order");
                System.out.println("id = " + id);
            }
            // 当前任务结束，计数器-1
            latch.countDown();
        };
        long begin = System.currentTimeMillis();
        // 创建300个任务并发执行，每个任务创建100个id，总计生成3w个id
        for (int i = 0; i < 300; i++) {
            es.submit(task);
        }
        // 主线程阻塞，直到计数器归0，确保统计的是“全部任务完成”的耗时
        latch.await();
        long end = System.currentTimeMillis();
        System.out.println("生成3w个id共耗时" + (end - begin) + "ms");
    }
}
