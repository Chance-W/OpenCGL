package com.opencgl.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @author Chance.W
 */
@Data
@ToString
@Builder
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class MenuInfo {
    private String fatherMenuName;
    private String menuName;
    private String iconPath;
    private String fxmlPath;
    private String clazz;
    private Boolean enable;
    private String jarName;
    private String pluginInfo;
}
