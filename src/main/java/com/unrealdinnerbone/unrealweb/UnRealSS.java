package com.unrealdinnerbone.unrealweb;

import com.unrealdinnerbone.config.ConfigCreator;
import com.unrealdinnerbone.config.ConfigManager;
import com.unrealdinnerbone.config.config.IntegerConfig;
import com.unrealdinnerbone.config.config.StringConfig;
import com.unrealdinnerbone.javalinutils.InfluxConfig;
import com.unrealdinnerbone.javalinutils.InfluxPlugin;
import com.unrealdinnerbone.unreallib.file.PathHelper;
import com.unrealdinnerbone.unreallib.json.JsonUtil;
import io.javalin.Javalin;
import io.javalin.http.UploadedFile;
import io.javalin.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Calendar;

public class UnRealSS {
    private static final Logger LOGGER = LoggerFactory.getLogger("SS");
    public static void main(String[] args) throws IOException {
        ConfigManager configManager = ConfigManager.createSimpleEnvPropertyConfigManger();
        Config config = configManager.loadConfig("config", Config::new);
        InfluxConfig influxConfig = configManager.loadConfig("influx", InfluxConfig::new);
        Javalin javalin = Javalin.create(javalinConfig -> javalinConfig.plugins.register(new InfluxPlugin(influxConfig))).start(config.port.getValue());
        javalin.get("/", ctx -> ctx.result("Website Online"));
        Path downloadsFolder = PathHelper.tryGetOrCreateFolder(Path.of(config.downloadsFolder.getValue()));
        javalin.post("/", ctx -> {
            String head = ctx.header("key");
            String apiKey = config.apiKey.getValue();
            if(head != null && head.equals(apiKey)) {
                Calendar cal = Calendar.getInstance();
                int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
                int month = cal.get(Calendar.MONTH) + 1;
                int year = cal.get(Calendar.YEAR);
                Path dayFolder = createFolder(createFolder(createFolder(downloadsFolder, String.valueOf(year)), String.valueOf(month)), String.valueOf(dayOfMonth));
                UploadedFile uploadedFile = ctx.uploadedFile("theFile");
                long time = System.currentTimeMillis();
                String name = time + "-" + uploadedFile.filename();
                String path = PathHelper.getOrCreateFile(dayFolder.resolve(name)).orElseThrow().toString();
                String url = ctx.queryParam("url") + path.substring(config.downloadsFolder.getValue().length() + 1).replace("\\", "/");
                LOGGER.info("New File! {} @ {}", name, url);
                FileUtil.streamToFile(uploadedFile.content(), path);
                ctx.result(JsonUtil.DEFAULT.toJson(new Return(uploadedFile.filename(), time, url)));

            } else {
                ctx.status(401);
            }
        });
        javalin.start(config.port.getValue());
    }
    public static Path createFolder(Path baseFolder, String subFolder) throws IOException {
        return PathHelper.tryGetOrCreateFolder(baseFolder.resolve(subFolder));
    }

    public static record Return(String fileName, long time, String location) {}

    public static class Config {

        public IntegerConfig port;
        public StringConfig apiKey;
        public StringConfig discord;
        public StringConfig downloadsFolder;

        public Config(ConfigCreator configCreator) {
            port = configCreator.createInteger("port", 9595);
            apiKey = configCreator.createString("apiKey", "");
            discord = configCreator.createString("discord", "");
            downloadsFolder = configCreator.createString("downloadsFolder", "downloads");
        }
    }
}