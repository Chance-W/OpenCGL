package com.opencgl.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @author Chance.W
 * @version 1.0
 * @CreateDate 2023/06/05 20:04
 * @since v9.0
 */
@Data
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class PluginInfo {
    private String pluginName;
    private String fatherName;
    private String controllerType;
    private String className;
    private String fxmlPath;
    private String iconPath;
    private Boolean enable;
    private String pluginInfo;
}

