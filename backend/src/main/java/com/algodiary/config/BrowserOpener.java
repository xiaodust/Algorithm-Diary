package com.algodiary.config;

import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class BrowserOpener implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BrowserOpener.class);

    private final boolean enabled;
    private final int port;

    public BrowserOpener(
            @Value("${algodiary.open-browser-on-startup:false}") boolean enabled,
            @Value("${server.port:8081}") int port) {
        this.enabled = enabled;
        this.port = port;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!enabled) {
            return;
        }
        String url = new URI("http://localhost:" + port).toString();
        String os = System.getProperty("os.name", "").toLowerCase();
        ProcessBuilder builder;
        if (os.contains("win")) {
            builder = new ProcessBuilder("cmd", "/c", "start", "", url);
        } else if (os.contains("mac")) {
            builder = new ProcessBuilder("open", url);
        } else {
            builder = new ProcessBuilder("xdg-open", url);
        }

        try {
            builder.start();
        } catch (Exception ex) {
            log.warn("自动打开浏览器失败，请手动访问 {}", url, ex);
        }
    }
}
