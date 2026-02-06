package com.opencgl.util;

import com.opencgl.api.PluginUI;

/** Validates plugin protocol and host-version declarations before registration. */
public final class PluginCompatibility {
    public static final int SUPPORTED_API_MAJOR = 1;

    public record Result(boolean compatible, String reason) {
        static Result accepted() { return new Result(true, ""); }
        static Result rejected(String reason) { return new Result(false, reason); }
    }

    private PluginCompatibility() {
    }

    public static Result check(PluginUI plugin, String hostVersion) {
        if (!isVersion(plugin.apiVersion())) {
            return Result.rejected("invalid plugin API version: " + plugin.apiVersion());
        }
        if (!isVersion(hostVersion)) {
            return Result.rejected("invalid host OpenCGL version: " + hostVersion);
        }
        String minimumHostVersion = plugin.minimumHostVersion();
        String maximumHostVersion = plugin.maximumHostVersion();
        if (!isOptionalVersion(minimumHostVersion)) {
            return Result.rejected("invalid minimum OpenCGL version: " + minimumHostVersion);
        }
        if (!isOptionalVersion(maximumHostVersion)) {
            return Result.rejected("invalid maximum OpenCGL version: " + maximumHostVersion);
        }
        int apiMajor = major(plugin.apiVersion());
        if (apiMajor != SUPPORTED_API_MAJOR) {
            return Result.rejected("requires plugin API " + plugin.apiVersion()
                + ", host supports API " + SUPPORTED_API_MAJOR);
        }
        if (!minimumHostVersion.isBlank()
            && compareVersions(hostVersion, minimumHostVersion) < 0) {
            return Result.rejected("requires OpenCGL >= " + minimumHostVersion);
        }
        if (!maximumHostVersion.isBlank()
            && compareVersions(hostVersion, maximumHostVersion) > 0) {
            return Result.rejected("requires OpenCGL <= " + maximumHostVersion);
        }
        return Result.accepted();
    }

    static int compareVersions(String left, String right) {
        int[] a = components(left);
        int[] b = components(right);
        for (int index = 0; index < Math.max(a.length, b.length); index++) {
            int av = index < a.length ? a[index] : 0;
            int bv = index < b.length ? b[index] : 0;
            if (av != bv) {
                return Integer.compare(av, bv);
            }
        }
        return 0;
    }

    private static int major(String version) {
        int[] values = components(version);
        return values.length == 0 ? SUPPORTED_API_MAJOR : values[0];
    }

    private static boolean isOptionalVersion(String version) {
        return version != null && (version.isBlank() || isVersion(version));
    }

    private static boolean isVersion(String version) {
        if (version == null) return false;
        return version.trim().matches("[vV]?\\d+(?:\\.\\d+)*(?:[-+][0-9A-Za-z.-]+)?");
    }

    private static int[] components(String version) {
        if (version == null) {
            return new int[0];
        }
        String normalized = version.trim().replaceFirst("^[vV]", "").split("[-+]", 2)[0];
        if (normalized.isBlank()) {
            return new int[0];
        }
        String[] parts = normalized.split("\\.");
        int[] values = new int[parts.length];
        for (int index = 0; index < parts.length; index++) {
            values[index] = Integer.parseInt(parts[index]);
        }
        return values;
    }
}
