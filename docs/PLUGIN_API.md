# 插件兼容协议

协议定义文件：`OpenCGL-Plugin-New/PluginApiModule/src/main/java/com/opencgl/api/PluginUI.java`。

旧插件不需要立即修改：所有新增方法都有默认值，默认使用实现类全名作为 ID、Plugin API 版本为 `1`，且不限制主程序版本。

新插件建议显式声明：

```java
@Override
public String pluginId() {
    // 发布后保持稳定；不要使用会随重构变化的显示名称。
    return "com.example.my-tool";
}

@Override
public String apiVersion() {
    // 当前主程序支持 Plugin API 1.x。
    return "1";
}

@Override
public String minimumHostVersion() {
    // 包含边界；空字符串表示不限制。
    return "2.2.2";
}

@Override
public String maximumHostVersion() {
    // 只有确认后续版本不兼容时才设置上限。
    return "";
}
```

加载规则：

1. 先扫描安装包内置插件，再扫描用户插件目录。
2. 相同 `pluginId` 只保留最后一个，因此用户插件可以覆盖内置插件。
3. API 主版本不匹配，或主程序版本不在声明范围内时，插件不会注册。
4. 跳过原因写入现有插件加载失败详情，其他插件继续加载。
5. `pluginId` 为空时回退到实现类全名，避免单个错误声明影响整个启动。

版本比较会忽略主程序版本前面的 `v` 和 `-构建时间` 后缀，例如 `v2.2.2-20260907` 按 `2.2.2` 比较。

## 插件启停与诊断

- 在已加载插件卡片上右键选择“禁用”，主程序会按 `pluginId` 持久化禁用状态。
- 禁用不会删除或重命名 JAR；内置版本和用户版本使用同一 ID 时会一起禁用，避免覆盖插件禁用后意外回退到内置版本。
- 可在“设置 → 插件加载状态”查看内置/用户插件数量、覆盖关系、禁用项、不兼容原因和 JAR 加载失败信息。
- 可单独重新启用某个插件，也可“重新启用全部”；操作后主程序会自动热刷新插件列表。
- 内置插件只能禁用，不能卸载；用户目录中的插件可以卸载，卸载覆盖版本后会恢复同 ID 的内置版本。

## 生命周期与资源释放

宿主会跟踪扫描实例和用户打开的运行实例，并在关闭 Tab、禁用、卸载、热重载和应用退出时
调用现有的 `PluginUI.dispose()`。同一个实例最多调用一次；某个插件清理失败会记录日志，
但不会阻止其他插件继续清理。宿主始终先调用 `dispose()`，再关闭插件 ClassLoader。

插件仍然负责关闭自己创建的资源。`dispose()` 必须停止线程池、定时任务和动画，关闭 Socket、
数据库或中间件客户端、文件句柄及子进程，并移除全局监听器。`System.gc()` 和关闭
`URLClassLoader` 都不能替代这些操作。使用 FXML 的插件入口必须保存
`FXMLLoader#getController()` 返回的 Controller，并在入口 `dispose()` 中调用 Controller 的
清理方法；否则宿主虽然调用了入口，实际资源仍不会释放。
