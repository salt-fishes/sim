package com.trhsy.sim.util;

import com.trhsy.sim.loader.ModSimLoader;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * P15(S1): 单线程异步文件写入。
 *
 * <p>建筑/农场/矿场/蓝图的保存都是很小的文本文件，但旧实现直接在服务端
 * tick 线程或客户端渲染线程里做磁盘 I/O；城市一大就会卡服/卡画面。
 * 这里把“拼字符串”留在调用线程（保证读到一致的数据快照），
 * 真正的写文件与原子替换放到守护线程。</p>
 *
 * <p>世界保存/服务器关闭时调用 {@link #flush(long)}，保证数据落盘。</p>
 */
public final class AsyncFileWriter {
    private static final AtomicLong SEQ = new AtomicLong();
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Sim-Async-Save");
        thread.setDaemon(true);
        return thread;
    });

    private AsyncFileWriter() {
    }

    /**
     * 异步写入文本；内容在调用时就已经确定，之后不会再被修改。
     */
    public static void write(File target, String content, Charset charset) {
        if (target == null || content == null) {
            return;
        }
        final Charset cs = charset != null ? charset : Charset.defaultCharset();
        EXECUTOR.execute(() -> doWrite(target, content, cs));
    }

    private static void doWrite(File target, String content, Charset charset) {
        File tmp = new File(target.getAbsolutePath() + ".tmp" + SEQ.incrementAndGet());
        try {
            File parent = target.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                ModSimLoader.log.warn("无法创建保存目录: {}", parent.getAbsolutePath());
                return;
            }
            Files.write(tmp.toPath(), content.getBytes(charset));
            try {
                Files.move(tmp.toPath(), target.toPath(),
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            ModSimLoader.log.error("异步保存失败 {}: {}", target.getAbsolutePath(), e.getMessage());
            if (tmp.exists()) {
                tmp.delete();
            }
        }
    }

    /** 等待已排队的写入完成；服务器关闭时调用。 */
    public static void flush(long timeoutMs) {
        try {
            EXECUTOR.submit(() -> {
            }).get(Math.max(1L, timeoutMs), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            ModSimLoader.log.warn("等待异步保存超时/失败: {}", e.getMessage());
        }
    }

    /**
     * P15(S1): 删除也进同一个队列，保证“先排队的保存”不会在删除之后
     * 又把文件写回来（拆楼后重建幽灵建筑）。
     */
    public static void delete(File target) {
        if (target == null) {
            return;
        }
        EXECUTOR.execute(() -> {
            try {
                Files.deleteIfExists(target.toPath());
            } catch (Exception e) {
                ModSimLoader.log.warn("异步删除失败 {}: {}", target.getAbsolutePath(), e.getMessage());
            }
        });
    }
}
