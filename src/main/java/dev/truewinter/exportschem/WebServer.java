package dev.truewinter.exportschem;

import io.javalin.Javalin;
import io.javalin.core.util.Header;
import io.javalin.http.HttpCode;
import io.javalin.http.UploadedFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class WebServer extends Thread {
    private int port;
    private String schemFolder;
    private Logger logger;
    private Javalin server;
    private ExportSchem exportSchem;

    public WebServer(int port, String schemFolder, Logger logger) {
        this.port = port;
        this.schemFolder = schemFolder;
        this.logger = logger;
        this.exportSchem = ExportSchem.getInstance();
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

        server.get("/upload/{key}", context -> {
            String key = context.pathParam("key");

            if (!exportSchem.hasImportKey(key)) {
                context.status(HttpCode.FORBIDDEN);
                context.result("Invalid upload key");
                return;
            }

            ImportKey importKey = exportSchem.getImportKey(key);

            if (!importKey.getPlayer().isOnline()) {
                context.status(HttpCode.UNPROCESSABLE_ENTITY);
                context.result("Player not online");
                return;
            }

            InputStream inputStream = exportSchem.getResource("web/upload.html");

            if (inputStream == null) {
                context.status(HttpCode.INTERNAL_SERVER_ERROR);
                context.result("Unable to find upload page");
                return;
            }

            StringBuilder html = new StringBuilder();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(inputStreamReader);
            for (String line; (line = reader.readLine()) != null;) {
                html.append(line
                        .replaceAll(Pattern.quote("{{player}}"), importKey.getPlayer().getName())
                        .replaceAll(Pattern.quote("{{version}}"), exportSchem.getDescription().getVersion())
                );
            }

            context.header(Header.CONTENT_TYPE, "text/html");
            context.result(html.toString());
        });

        server.post("/upload/{key}", context -> {
            String key = context.pathParam("key");

            if (!exportSchem.hasImportKey(key)) {
                context.status(HttpCode.FORBIDDEN);
                context.result("Invalid upload key");
                return;
            }

            ImportKey importKey = exportSchem.getImportKey(key);

            if (!importKey.getPlayer().isOnline()) {
                context.status(HttpCode.UNPROCESSABLE_ENTITY);
                context.result("Player not online");
                return;
            }

            UploadedFile uploadedFile = context.uploadedFile("file");

            if (uploadedFile == null) {
                context.status(HttpCode.BAD_REQUEST);
                context.result("File is required");
                return;
            }

            if (uploadedFile.getFilename().isBlank()) {
                context.status(HttpCode.BAD_REQUEST);
                context.result("File is required");
                return;
            }

            long uploadSize = uploadedFile.getSize() / 1000;
            long maxUploadSize = exportSchem.getConfig().getLong("import-max-size");

            if (uploadSize > maxUploadSize) {
                context.status(HttpCode.PAYLOAD_TOO_LARGE);
                context.result("File size exceeds max size of " + maxUploadSize + "KB");
                return;
            }

            List<String> allowedExts = Arrays.asList(".schem", ".schematic");
            if (!allowedExts.contains(uploadedFile.getExtension())) {
                context.status(HttpCode.BAD_REQUEST);
                context.result("Invalid file type");
                return;
            }

            try {
                exportSchem.importSchem(uploadedFile.getFilename(), uploadedFile.getContent(), importKey);
                context.result("Imported schematic into " + importKey.getPlayer().getName() + "'s WorldEdit clipboard");
            } catch (Exception e) {
                context.status(HttpCode.INTERNAL_SERVER_ERROR);
                context.result("An error occurred: " + e.getMessage());
            }
        });

        // I know config.addStaticFiles() would be better, but I didn't
        // know how to bind a static directory to a specific web path
        // (e.g. /download/{file} -> %plugindir%/schematics/{file})
        server.get("/download/{file}", context -> {
            String reqFile = context.pathParam("file");
            File file = new File(schemFolder + File.separator + reqFile);

            if (!file.exists()) {
                context.status(HttpCode.NOT_FOUND);
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
