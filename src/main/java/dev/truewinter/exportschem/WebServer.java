package dev.truewinter.exportschem;

import io.javalin.Javalin;
import io.javalin.core.util.Header;

import java.io.File;
import java.io.FileInputStream;
import java.util.logging.Logger;

public class WebServer extends Thread {
    private int port;
    private String schemFolder;
    private Logger logger;
    private Javalin server;

    public WebServer(int port, String schemFolder, Logger logger) {
        this.port = port;
        this.schemFolder = schemFolder;
        this.logger = logger;
    }

    @Override
    public void run() {
        server = Javalin.create(config -> {
            config.showJavalinBanner = false;
        }).start(port);

        server.before(context -> {
            // No need to cache or index anything anywhere
            context.header(Header.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
            context.header(Header.EXPIRES, "0");
            context.header("X-Robots-Tag", "noindex");
        });

        // I know config.addStaticFiles() would be better, but I didn't
        // know how to bind a static directory to a specific web path
        // (e.g. /download/{file} -> %plugindir%/schematics/{file})
        server.get("/download/{file}", context -> {
            String reqFile = context.pathParam("file");
            File file = new File(schemFolder + File.separator + reqFile);

            if (!file.exists()) {
                context.result("File does not exist");
                return;
            }

            // just in case browsers try to show the file
            context.header(Header.CONTENT_TYPE, "application/octet-stream");
            context.result(new FileInputStream(file));
        });
    }

    public void stopServer() {
        logger.info("Stopping web server");
        server.stop();
        this.interrupt();
    }
}
