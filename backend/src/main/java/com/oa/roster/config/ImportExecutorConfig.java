package com.oa.roster.config;

import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Excel 导入专用线程池：手动 new ThreadPoolExecutor，不用 Executors 工厂方法。
 *
 * 参数设计（面试可讲）：
 * 1. 导入是 IO 密集型任务（读文件、走网络、等数据库），线程大部分时间在等待而非烧 CPU，
 *    所以线程数可以远高于 CPU 核数：core = 核数 * 2，max = 核数 * 4；
 *    （对比 CPU 密集型：线程数 = 核数 + 1 即可，多了反而上下文切换浪费）
 * 2. 队列必须【有界】(50)：10 个 HR 反复上传时任务排队执行而非无界堆积——
 *    Executors.newFixedThreadPool 的无界队列会在高峰期把内存拖垮（OOM 隐患），
 *    这也是阿里规约禁止用 Executors 工厂方法的核心原因；
 * 3. 拒绝策略选 AbortPolicy：队列满直接抛 RejectedExecutionException，
 *    由提交方 catch 后返回"队列已满"的友好提示——
 *    不选 CallerRunsPolicy，因为它会让 Tomcat 工作线程亲自执行导入，
 *    把"异步"退化回"同步"，占用 Web 线程 1-2 分钟；
 * 4. 优雅停机：应用关闭时先 shutdown()（拒绝新任务、执行完已排队任务），
 *    最多等待 30 秒再 shutdownNow()（中断剩余任务），避免部署重启把导入腰斩。
 *    注：原生 ThreadPoolExecutor 没有 Spring ThreadPoolTaskExecutor 的
 *    setWaitForTasksToCompleteOnShutdown 那套 API，需要在销毁回调里手动编排。
 */
@Configuration
public class ImportExecutorConfig {

    public static final String IMPORT_EXECUTOR = "importExecutor";

    private ThreadPoolExecutor importExecutorInstance;

    @Bean(IMPORT_EXECUTOR)
    public ThreadPoolExecutor importExecutor() {
        int cores = Runtime.getRuntime().availableProcessors();
        importExecutorInstance = new ThreadPoolExecutor(
                cores * 2,                          // corePoolSize：IO 密集型取核数 * 2
                cores * 4,                          // maximumPoolSize：高峰扩容上限
                60L, TimeUnit.SECONDS,              // 超出 core 的空闲线程 60s 后回收
                new LinkedBlockingQueue<>(50),      // 有界队列：排队上限，防无界堆积
                new CustomizableThreadFactory("excel-import-"), // 线程命名，排查日志用
                new ThreadPoolExecutor.AbortPolicy());          // 队列满抛异常，提交方友好提示
        return importExecutorInstance;
    }

    /** 优雅停机：等在跑/排队的导入任务收尾，最多 30 秒后强制中断 */
    @PreDestroy
    public void shutdownGracefully() throws InterruptedException {
        if (importExecutorInstance == null) {
            return;
        }
        importExecutorInstance.shutdown(); // 停止接收新任务，已提交的继续执行
        if (!importExecutorInstance.awaitTermination(30, TimeUnit.SECONDS)) {
            importExecutorInstance.shutdownNow(); // 超时：中断剩余任务（已入库批次凭幂等重跑）
        }
    }
}
