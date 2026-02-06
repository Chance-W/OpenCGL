package com.opencgl.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencgl.base.utils.SqliteUtil;
import com.opencgl.model.PluginClickRecord;

/**
 * 插件点击频次记录服务
 */
public class PluginClickStore {
    private static final Logger log = LoggerFactory.getLogger(PluginClickStore.class);

    public PluginClickStore() {
        initTable();
    }

    private void initTable() {
        try {
            boolean exist = SqliteUtil.checkTableExist("plugin_click_record");
            if (!exist) {
                String sql = "CREATE TABLE IF NOT EXISTS plugin_click_record (" +
                             "plugin_name TEXT PRIMARY KEY, " +
                             "click_count INTEGER DEFAULT 0, " +
                             "last_click_at TEXT" +
                             ")";
                SqliteUtil.update(sql);
                log.info("plugin_click_record table created.");
            }
        } catch (Exception e) {
            log.error("Failed to init plugin_click_record table", e);
        }
    }

    /**
     * 点击 +1
     */
    public void increment(String pluginName) {
        try {
            String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            
            // 查询是否已存在
            String querySql = "SELECT * FROM plugin_click_record WHERE plugin_name = ?";
            List<PluginClickRecord> list = SqliteUtil.queryForList(querySql, PluginClickRecord.class, pluginName);
            
            if (list == null || list.isEmpty()) {
                String insertSql = "INSERT INTO plugin_click_record (plugin_name, click_count, last_click_at) VALUES (?, 1, ?)";
                SqliteUtil.insert(insertSql, pluginName, now);
            } else {
                String updateSql = "UPDATE plugin_click_record SET click_count = click_count + 1, last_click_at = ? WHERE plugin_name = ?";
                SqliteUtil.update(updateSql, now, pluginName);
            }
        } catch (Exception e) {
            log.error("Failed to increment plugin click count for: " + pluginName, e);
        }
    }

    /**
     * 获取单个插件的点击次数
     */
    public long getCount(String pluginName) {
        try {
            String querySql = "SELECT * FROM plugin_click_record WHERE plugin_name = ?";
            List<PluginClickRecord> list = SqliteUtil.queryForList(querySql, PluginClickRecord.class, pluginName);
            if (list != null && !list.isEmpty()) {
                Long count = list.get(0).getClickCount();
                return count == null ? 0L : count;
            }
        } catch (Exception e) {
            log.error("Failed to get plugin click count for: " + pluginName, e);
        }
        return 0L;
    }

    /**
     * 获取所有插件的点击记录
     */
    public Map<String, Long> loadAll() {
        Map<String, Long> map = new HashMap<>();
        try {
            String querySql = "SELECT * FROM plugin_click_record";
            List<PluginClickRecord> list = SqliteUtil.queryForList(querySql, PluginClickRecord.class);
            if (list != null) {
                for (PluginClickRecord record : list) {
                    Long count = record.getClickCount();
                    map.put(record.getPluginName(), count == null ? 0L : count);
                }
            }
        } catch (Exception e) {
            log.error("Failed to load all plugin click records", e);
        }
        return map;
    }
}
