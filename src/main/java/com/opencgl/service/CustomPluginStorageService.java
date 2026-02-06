package com.opencgl.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencgl.base.utils.SqliteUtil;
import com.opencgl.model.CustomPluginEntry;

/**
 * 自定义插件持久化：Sqlite 表 custom_plugin，启动时校验表是否存在并建表。
 */
public class CustomPluginStorageService {
    private static final Logger log = LoggerFactory.getLogger(CustomPluginStorageService.class);
    private static final String TABLE = "custom_plugin";

    public CustomPluginStorageService() {
        initTable();
    }

    private void initTable() {
        try {
            boolean exist = SqliteUtil.checkTableExist(TABLE);
            if (!exist) {
                String sql = "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
                        "id TEXT PRIMARY KEY, " +
                        "name TEXT NOT NULL, " +
                        "url_or_path TEXT NOT NULL, " +
                        "icon_path TEXT, " +
                        "sort_order INTEGER DEFAULT 0, " +
                        "created_at TEXT" +
                        ")";
                SqliteUtil.update(sql);
                log.info("custom_plugin table created.");
            }
        } catch (Exception e) {
            log.error("Failed to init custom_plugin table", e);
        }
    }

    public List<CustomPluginEntry> loadAll() {
        try {
            String sql = "SELECT * FROM " + TABLE + " ORDER BY sort_order ASC, created_at ASC";
            List<CustomPluginEntry> list = SqliteUtil.queryForList(sql, CustomPluginEntry.class);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            log.error("Failed to load custom plugins", e);
            return new ArrayList<>();
        }
    }

    public void save(CustomPluginEntry entry) {
        if (entry.getId() == null || entry.getId().isBlank()) return;
        try {
            String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            if (entry.getCreatedAt() == null) entry.setCreatedAt(now);
            String sql = "INSERT INTO " + TABLE + " (id, name, url_or_path, icon_path, sort_order, created_at) VALUES (?, ?, ?, ?, ?, ?)";
            SqliteUtil.insert(sql,
                    entry.getId(),
                    entry.getName(),
                    entry.getUrlOrPath(),
                    entry.getIconPath() != null ? entry.getIconPath() : "",
                    entry.getSortOrder() != null ? entry.getSortOrder() : 0,
                    entry.getCreatedAt());
        } catch (Exception e) {
            log.error("Failed to save custom plugin: " + entry.getId(), e);
            throw new RuntimeException(e);
        }
    }

    public void update(CustomPluginEntry entry) {
        if (entry.getId() == null || entry.getId().isBlank()) return;
        try {
            String sql = "UPDATE " + TABLE + " SET name = ?, url_or_path = ?, icon_path = ?, sort_order = ? WHERE id = ?";
            SqliteUtil.update(sql,
                    entry.getName(),
                    entry.getUrlOrPath(),
                    entry.getIconPath() != null ? entry.getIconPath() : "",
                    entry.getSortOrder() != null ? entry.getSortOrder() : 0,
                    entry.getId());
        } catch (Exception e) {
            log.error("Failed to update custom plugin: " + entry.getId(), e);
            throw new RuntimeException(e);
        }
    }

    public void deleteById(String id) {
        if (id == null || id.isBlank()) return;
        try {
            String sql = "DELETE FROM " + TABLE + " WHERE id = ?";
            SqliteUtil.update(sql, id);
        } catch (Exception e) {
            log.error("Failed to delete custom plugin: " + id, e);
        }
    }

    public void deleteByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) return;
        for (String id : ids) {
            deleteById(id);
        }
    }
}
