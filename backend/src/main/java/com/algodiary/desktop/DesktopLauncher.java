package com.algodiary.desktop;

import com.algodiary.AlgorithmDiaryApplication;
import java.awt.AWTException;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import org.springframework.context.ConfigurableApplicationContext;

public class DesktopLauncher extends Application {

    private static final String APP_URL = "http://localhost:8081/";

    private final CountDownLatch serverReady = new CountDownLatch(1);
    private volatile ConfigurableApplicationContext context;
    private volatile Throwable startupFailure;
    private Stage stage;
    private TrayIcon trayIcon;
    private boolean traySupported;
    private final ExternalLinkBridge externalLinkBridge = new ExternalLinkBridge();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() {
        Thread serverThread = new Thread(() -> {
            try {
                context = AlgorithmDiaryApplication.run(getParameters().getRaw().toArray(new String[0]));
            } catch (Throwable ex) {
                startupFailure = ex;
            } finally {
                serverReady.countDown();
            }
        }, "algorithm-diary-server");
        serverThread.setDaemon(false);
        serverThread.start();
    }

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        primaryStage.setTitle("算法刷题助手");
        loadWindowIcon(primaryStage);

        try {
            if (!serverReady.await(45, TimeUnit.SECONDS)) {
                showFatalError("服务启动超时，请检查端口 8081 是否被占用。");
                return;
            }
            if (startupFailure != null) {
                showFatalError("服务启动失败：" + rootMessage(startupFailure));
                return;
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            showFatalError("等待服务启动时被中断。");
            return;
        }

        WebView webView = new WebView();
        WebEngine engine = webView.getEngine();
        configureExternalLinks(engine);
        engine.load(APP_URL);

        Scene scene = new Scene(webView, 1280, 800);
        scene.setFill(Color.web("#f8fafc"));
        primaryStage.setMinWidth(960);
        primaryStage.setMinHeight(640);
        primaryStage.setScene(scene);

        setupTray();
        if (traySupported) {
            Platform.setImplicitExit(false);
            primaryStage.setOnCloseRequest(event -> {
                event.consume();
                primaryStage.hide();
            });
        } else {
            primaryStage.setOnCloseRequest(event -> shutdown());
        }

        primaryStage.show();
    }

    private void configureExternalLinks(WebEngine engine) {
        engine.setCreatePopupHandler(popupFeatures -> null);
        engine.getLoadWorker().stateProperty().addListener((observable, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                injectExternalLinkHandler(engine);
            }
        });
    }

    private void injectExternalLinkHandler(WebEngine engine) {
        try {
            JSObject window = (JSObject) engine.executeScript("window");
            window.setMember("desktopBridge", externalLinkBridge);
            engine.executeScript(
                    "(function () {"
                    + "if (window.__algodiaryLinkHandlerInstalled) { return; }"
                    + "window.__algodiaryLinkHandlerInstalled = true;"
                    + "document.addEventListener('click', function (event) {"
                    + "var node = event.target && event.target.closest ? event.target.closest('a') : null;"
                    + "if (!node) { return; }"
                    + "var href = node.href || '';"
                    + "if (node.target === '_blank' || href.indexOf('https://leetcode.cn/') === 0) {"
                    + "event.preventDefault();"
                    + "event.stopPropagation();"
                    + "if (window.desktopBridge) { window.desktopBridge.openExternal(href); }"
                    + "else { window.open(href, '_blank'); }"
                    + "}"
                    + "}, true);"
                    + "})();"
            );
            System.out.println("[desktop] External link bridge installed.");
        } catch (Exception ex) {
            System.err.println("[desktop] Failed to install external link bridge:");
            ex.printStackTrace(System.err);
        }
    }

    private void loadWindowIcon(Stage stage) {
        try (InputStream iconStream = DesktopLauncher.class.getResourceAsStream("/app-icon.png")) {
            if (iconStream != null) {
                stage.getIcons().add(new Image(iconStream));
            }
        } catch (Exception ignored) {
            // Fall back to the default Java icon.
        }
    }

    private void setupTray() {
        if (!SystemTray.isSupported()) {
            traySupported = false;
            return;
        }

        try {
            PopupMenu menu = new PopupMenu();
            MenuItem showItem = new MenuItem("显示主窗口");
            showItem.addActionListener(event -> Platform.runLater(() -> showStage()));
            MenuItem exitItem = new MenuItem("退出");
            exitItem.addActionListener(event -> shutdown());
            menu.add(showItem);
            menu.addSeparator();
            menu.add(exitItem);

            trayIcon = new TrayIcon(loadTrayImage(), "算法刷题助手", menu);
            trayIcon.setImageAutoSize(true);
            trayIcon.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent event) {
                    if (event.getClickCount() == 2) {
                        Platform.runLater(DesktopLauncher.this::showStage);
                    }
                }
            });

            SystemTray.getSystemTray().add(trayIcon);
            traySupported = true;
        } catch (AWTException ex) {
            traySupported = false;
        }
    }

    private java.awt.Image loadTrayImage() {
        try (InputStream iconStream = DesktopLauncher.class.getResourceAsStream("/app-icon.png")) {
            if (iconStream != null) {
                BufferedImage source = ImageIO.read(iconStream);
                if (source != null) {
                    return source.getScaledInstance(16, 16, java.awt.Image.SCALE_SMOOTH);
                }
            }
        } catch (Exception ignored) {
            // Fall back to the generated tray image below.
        }
        return createFallbackTrayImage();
    }

    private BufferedImage createFallbackTrayImage() {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        graphics.setColor(java.awt.Color.decode("#2563EB"));
        graphics.fillOval(0, 0, 16, 16);
        graphics.setColor(java.awt.Color.WHITE);
        graphics.drawString("A", 4, 12);
        graphics.dispose();
        return image;
    }

    private void showStage() {
        if (stage != null) {
            stage.show();
            stage.toFront();
        }
    }

    private void showFatalError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("算法刷题助手");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
            Platform.exit();
        });
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    private void shutdown() {
        if (trayIcon != null && SystemTray.isSupported()) {
            SystemTray.getSystemTray().remove(trayIcon);
        }
        if (context != null) {
            context.close();
        }
        Platform.exit();
    }

    public final class ExternalLinkBridge {

        public void openExternal(String url) {
            Platform.runLater(() -> openExternalOnFxThread(url));
        }

        private void openExternalOnFxThread(String url) {
            System.out.println("[desktop] openExternal requested: " + url);
            try {
                java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
            } catch (Exception ex) {
                System.err.println("[desktop] Desktop.browse failed, falling back to HostServices: " + ex.getMessage());
                try {
                    getHostServices().showDocument(url);
                } catch (Exception fallbackEx) {
                    System.err.println("[desktop] HostServices fallback also failed:");
                    fallbackEx.printStackTrace(System.err);
                }
            }
        }
    }
}
