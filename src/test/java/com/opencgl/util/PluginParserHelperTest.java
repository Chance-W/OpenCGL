package com.opencgl.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PluginParserHelperTest {

    @TempDir
    Path tempDir;

    @Test
    void onlyUserPluginFilesAreUninstallable() {
        PluginParserHelper.PluginLoadInfo bundled = new PluginParserHelper.PluginLoadInfo(
            "id", "name", "1", "/app/ext-plugin/plugin.jar",
            PluginParserHelper.PluginSource.BUNDLED, null);
        PluginParserHelper.PluginLoadInfo user = new PluginParserHelper.PluginLoadInfo(
            "id", "name", "1", "/user/ext-plugin/plugin.jar",
            PluginParserHelper.PluginSource.USER, null);

        assertFalse(bundled.uninstallable());
        assertTrue(user.uninstallable());
    }

    @Test
    void explicitValidDirectoryHasPriority() throws Exception {
        Path configured = Files.createDirectory(tempDir.resolve("configured"));
        Path bundled = Files.createDirectories(tempDir.resolve("app/ext-plugin"));

        assertEquals(
            configured.toFile(),
            PluginParserHelper.resolvePluginDirectory(
                configured.toString(), tempDir.resolve("OpenCGL-Tool.exe").toString()
            )
        );
        // Ensure the fixture really contains a competing packaged directory.
        assertEquals(true, Files.isDirectory(bundled));
    }

    @Test
    void loadsBundledThenConfiguredSoUserPluginsCanOverride() throws Exception {
        Path configured = Files.createDirectory(tempDir.resolve("configured"));
        Path bundled = Files.createDirectories(tempDir.resolve("app/ext-plugin"));

        assertEquals(
            java.util.List.of(bundled.toFile(), configured.toFile()),
            PluginParserHelper.resolvePluginDirectories(
                configured.toString(), tempDir.resolve("OpenCGL-Tool.exe").toString()
            )
        );
    }

    @Test
    void findsWindowsPackagedPluginDirectoryWhenConfigurationIsEmpty() throws Exception {
        Path bundled = Files.createDirectories(tempDir.resolve("app/ext-plugin"));
        assertEquals(
            bundled.toFile(),
            PluginParserHelper.resolvePluginDirectory("", tempDir.resolve("OpenCGL-Tool.exe").toString())
        );
    }

    @Test
    void findsMacPackagedPluginDirectory() throws Exception {
        Path contents = tempDir.resolve("OpenCGL-Tool.app/Contents");
        Path bundled = Files.createDirectories(contents.resolve("app/ext-plugin"));
        Path executable = contents.resolve("MacOS/OpenCGL-Tool");
        assertEquals(bundled.toFile(), PluginParserHelper.resolvePluginDirectory(null, executable.toString()));
    }

    @Test
    void findsLinuxPackagedPluginDirectory() throws Exception {
        Path bundled = Files.createDirectories(tempDir.resolve("lib/app/ext-plugin"));
        Path executable = tempDir.resolve("bin/OpenCGL-Tool");
        assertEquals(bundled.toFile(), PluginParserHelper.resolvePluginDirectory(null, executable.toString()));
    }
}
