package com.jihao.aiwiki.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * app_setting 表数据对象。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Data
public class AppSettingDO {

    /** 主键 ID */
    private Long id;

    /** 配置键 */
    private String settingKey;

    /** 配置值 */
    private String settingValue;

    /** 配置值类型 */
    private String valueType;

    /** 配置说明 */
    private String description;

    /** 逻辑删除标记 */
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
