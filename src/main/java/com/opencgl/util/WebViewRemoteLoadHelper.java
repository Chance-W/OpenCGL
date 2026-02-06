package com.opencgl.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * 远程网页按正确编码拉取后供 WebView loadContent，避免 GBK 等页面在引擎内按 UTF-8 解析产生乱码。
 */
public final class WebViewRemoteLoadHelper {

    private static final Pattern META_CHARSET = Pattern.compile(
        "<meta[^>]+charset\\s*=\\s*['\"]?([a-zA-Z0-9_-]+)['\"]?",
        Pattern.CASE_INSENSITIVE);
    private static final Pattern HTTP_EQUIV_CHARSET = Pattern.compile(
        "<meta[^>]+http-equiv\\s*=\\s*['\"]?Content-Type['\"]?[^>]+content\\s*=\\s*[^>]*charset\\s*=\\s*([a-zA-Z0-9_-]+)",
        Pattern.CASE_INSENSITIVE);

    private WebViewRemoteLoadHelper() {
    }

    /**
     * 用 Java HttpClient 拉取 URL，按响应头或 HTML meta 检测编码并解码为字符串。
     * 失败返回 empty。
     */
    public static Optional<String> fetchAsString(String url) {
        try {
            HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml;q=0.9,*/*;q=0.8")
                .GET()
                .build();
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                return Optional.empty();
            }
            byte[] body = response.body();
            Charset charset = detectCharset(response, body);
            String html = new String(body, charset);
            return Optional.of(html);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static Charset detectCharset(HttpResponse<byte[]> response, byte[] body) {
        String contentType = response.headers().firstValue("Content-Type").orElse("");
        Charset fromHeader = parseCharsetFromContentType(contentType);
        String declared = fromHeader != null ? fromHeader.name() : null;
        if (declared == null) {
            String fromMeta = detectCharsetFromHtml(body);
            if (fromMeta != null) {
                try {
                    fromHeader = Charset.forName(fromMeta);
                    declared = fromHeader.name();
                } catch (Exception ignored) {
                }
            }
        }
        if (fromHeader == null) {
            fromHeader = StandardCharsets.UTF_8;
            declared = "UTF-8";
        }
        if ("UTF-8".equalsIgnoreCase(declared)) {
            Charset tryGbk = tryGbk();
            if (tryGbk != null) {
                String asUtf8 = new String(body, StandardCharsets.UTF_8);
                String asGbk = new String(body, tryGbk);
                if (replacementCount(asUtf8) > replacementCount(asGbk)
                    || (replacementCount(asUtf8) > 0 && cjkCount(asGbk) > cjkCount(asUtf8))) {
                    return tryGbk;
                }
            }
        }
        return fromHeader;
    }

    private static Charset tryGbk() {
        try {
            return Charset.forName("GBK");
        } catch (Exception e) {
            try {
                return Charset.forName("GB2312");
            } catch (Exception e2) {
                return null;
            }
        }
    }

    private static int replacementCount(String s) {
        return (int) IntStream.range(0, s.length()).filter(i -> s.charAt(i) == '\uFFFD').count();
    }

    private static int cjkCount(String s) {
        return (int) IntStream.range(0, s.length())
            .filter(i -> isCjk(s.charAt(i)))
            .count();
    }

    private static boolean isCjk(char c) {
        return (c >= 0x4E00 && c <= 0x9FFF)
            || (c >= 0x3400 && c <= 0x4DBF)
            || (c >= 0xF900 && c <= 0xFAFF);
    }

    private static Charset parseCharsetFromContentType(String contentType) {
        if (contentType == null) return null;
        int i = contentType.toLowerCase().indexOf("charset=");
        if (i == -1) return null;
        int start = i + 8;
        int end = start;
        while (end < contentType.length() && (Character.isLetterOrDigit(contentType.charAt(end)) || contentType.charAt(end) == '-' || contentType.charAt(end) == '_')) {
            end++;
        }
        String name = contentType.substring(start, end).trim();
        if (name.isEmpty()) return null;
        try {
            return Charset.forName(name);
        } catch (Exception e) {
            return null;
        }
    }

    private static String detectCharsetFromHtml(byte[] body) {
        String head = new String(body, 0, Math.min(body.length, 32 * 1024), StandardCharsets.UTF_8);
        Matcher m = META_CHARSET.matcher(head);
        if (m.find()) return m.group(1);
        m = HTTP_EQUIV_CHARSET.matcher(head);
        if (m.find()) return m.group(1);
        return null;
    }

    /** 在 HTML 的 head 内注入 &lt;base href="baseUrl"&gt;，使相对路径相对到 baseUrl */
    public static String injectBaseUrl(String html, String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) return html;
        String escaped = baseUrl.replace("&", "&amp;").replace("\"", "&quot;");
        String baseTag = "<base href=\"" + escaped + "\"/>";
        int headStart = html.toLowerCase().indexOf("<head");
        if (headStart == -1) {
            return "<head>" + baseTag + "</head>" + html;
        }
        int insertAfter = html.indexOf('>', headStart) + 1;
        return html.substring(0, insertAfter) + baseTag + html.substring(insertAfter);
    }
}
