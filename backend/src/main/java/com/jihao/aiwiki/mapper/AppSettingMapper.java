package com.jihao.aiwiki.mapper;

import com.jihao.aiwiki.entity.AppSettingDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * app_setting 表访问接口。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Mapper
public interface AppSettingMapper {

    /**
     * 按配置键查询未删除配置。
     *
     * @param settingKey 配置键
     * @return 配置数据对象
     */
    @Select("""
            SELECT id, setting_key, setting_value, value_type, description, deleted, create_time, update_time
            FROM app_setting
            WHERE setting_key = #{settingKey} AND deleted = 0
            LIMIT 1
            """)
    AppSettingDO findByKey(@Param("settingKey") String settingKey);

    /**
     * 按配置键批量查询未删除配置。
     *
     * @param settingKeys 配置键列表
     * @return 配置数据对象列表
     */
    @Select("""
            <script>
            SELECT id, setting_key, setting_value, value_type, description, deleted, create_time, update_time
            FROM app_setting
            WHERE deleted = 0 AND setting_key IN
            <foreach collection="settingKeys" item="settingKey" open="(" separator="," close=")">
              #{settingKey}
            </foreach>
            </script>
            """)
    List<AppSettingDO> findByKeys(@Param("settingKeys") List<String> settingKeys);

    /**
     * 插入配置项。
     *
     * @param setting 配置数据对象
     * @return 影响行数
     */
    @Insert("""
            INSERT INTO app_setting(setting_key, setting_value, value_type, description, deleted)
            VALUES(#{settingKey}, #{settingValue}, #{valueType}, #{description}, 0)
            """)
    int insert(AppSettingDO setting);

    /**
     * 更新配置项值。
     *
     * @param settingKey 配置键
     * @param settingValue 配置值
     * @return 影响行数
     */
    @Update("""
            UPDATE app_setting
            SET setting_value = #{settingValue}
            WHERE setting_key = #{settingKey} AND deleted = 0
            """)
    int updateValue(@Param("settingKey") String settingKey, @Param("settingValue") String settingValue);
}
