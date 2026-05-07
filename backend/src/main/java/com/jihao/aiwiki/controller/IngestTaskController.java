package com.jihao.aiwiki.controller;

import com.jihao.aiwiki.common.ApiResponse;
import com.jihao.aiwiki.common.PageResult;
import com.jihao.aiwiki.dto.task.IngestTaskPageQuery;
import com.jihao.aiwiki.service.IngestTaskService;
import com.jihao.aiwiki.vo.task.IngestTaskVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 摄入任务接口。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Validated
@RestController
@RequestMapping("/api/tasks")
@Tag(name = "Ingest Task", description = "摄入任务接口")
public class IngestTaskController {

    /** 摄入任务服务 */
    private final IngestTaskService ingestTaskService;

    /**
     * 创建任务接口。
     *
     * @param ingestTaskService 摄入任务服务
     */
    public IngestTaskController(IngestTaskService ingestTaskService) {
        this.ingestTaskService = ingestTaskService;
    }

    /**
     * 分页查询任务。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询任务")
    public ApiResponse<PageResult<IngestTaskVO>> pageTasks(@Valid IngestTaskPageQuery query) {
        return ApiResponse.success(ingestTaskService.pageTasks(query));
    }

    /**
     * 查询任务详情。
     *
     * @param taskId 任务 ID
     * @return 任务
     */
    @GetMapping("/detail")
    @Operation(summary = "查询任务详情")
    public ApiResponse<IngestTaskVO> getTask(@RequestParam @NotBlank(message = "taskId is required") String taskId) {
        return ApiResponse.success(ingestTaskService.getTask(taskId));
    }

    /**
     * 重试任务。
     *
     * @param taskId 任务 ID
     * @return 任务
     */
    @PostMapping("/{taskId}/retry")
    @Operation(summary = "重试任务")
    public ApiResponse<IngestTaskVO> retryTask(@PathVariable String taskId) {
        return ApiResponse.success(ingestTaskService.retryTask(taskId));
    }

    /**
     * 取消任务。
     *
     * @param taskId 任务 ID
     * @return 任务
     */
    @PostMapping("/{taskId}/cancel")
    @Operation(summary = "取消任务")
    public ApiResponse<IngestTaskVO> cancelTask(@PathVariable String taskId) {
        return ApiResponse.success(ingestTaskService.cancelTask(taskId));
    }

    /**
     * 订阅任务进度。
     *
     * @return SSE emitter
     */
    @GetMapping("/stream")
    @Operation(summary = "订阅任务进度")
    public SseEmitter streamTasks() {
        return ingestTaskService.streamTasks();
    }

    /**
     * 清除已终止任务（CANCELLED / FAILED / MANUAL_CHECK）。
     *
     * @param vaultId Vault ID
     * @return 删除条数
     */
    @DeleteMapping("/clear")
    @Operation(summary = "清除已终止任务")
    public ApiResponse<Integer> clearTerminated(@RequestParam Long vaultId) {
        return ApiResponse.success(ingestTaskService.clearTerminated(vaultId));
    }
}
