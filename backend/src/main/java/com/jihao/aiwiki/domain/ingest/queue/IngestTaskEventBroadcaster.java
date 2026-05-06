package com.jihao.aiwiki.domain.ingest.queue;

import com.jihao.aiwiki.vo.task.IngestTaskEventVO;
import com.jihao.aiwiki.vo.task.IngestTaskVO;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 摄入任务 SSE 事件广播器。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Component
public class IngestTaskEventBroadcaster {

    /** SSE 连接超时时间 */
    private static final long SSE_TIMEOUT_MILLIS = 30L * 60L * 1000L;

    /** 已连接的 SSE 客户端 */
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /**
     * 注册 SSE 连接。
     *
     * @return SSE emitter
     */
    public SseEmitter register() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(error -> emitters.remove(emitter));
        return emitter;
    }

    /**
     * 广播任务事件。
     *
     * @param type 事件类型
     * @param task 任务
     */
    public void broadcast(IngestTaskEventType type, IngestTaskVO task) {
        IngestTaskEventVO event = new IngestTaskEventVO(type.name(), task);
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(type.name().toLowerCase()).data(event));
            } catch (IOException exception) {
                emitters.remove(emitter);
            }
        }
    }
}
