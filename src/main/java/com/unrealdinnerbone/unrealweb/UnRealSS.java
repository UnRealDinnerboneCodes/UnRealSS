package com.unrealdinnerbone.unrealweb;

import com.unrealdinnerbone.config.api.ConfigCreator;
import com.unrealdinnerbone.config.api.exception.ConfigException;
import com.unrealdinnerbone.config.config.ConfigValue;
import com.unrealdinnerbone.config.impl.provider.EnvProvider;
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
    public static void main(String[] args) {
        EnvProvider<?> envProvider = new EnvProvider<>();
        Config config = envProvider.loadConfig("config", Config::new);
        try {
            envProvider.read();
        } catch (ConfigException e) {
            LOGGER.error("Failed to read config", e);
        }
        try (Javalin javalin = Javalin.create(javalinConfig -> {}).start(config.port.get())) {
            javalin.get("/", ctx -> ctx.result("Website Online"));
            Path downloadsFolder = PathHelper.tryGetOrCreateFolder(Path.of(config.downloadsFolder.get()));
            javalin.post("/", ctx -> {
                String head = ctx.header("key");
                String apiKey = config.apiKey.get();
                if (head != null && head.equals(apiKey)) {
                    Calendar cal = Calendar.getInstance();
                    int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
                    int month = cal.get(Calendar.MONTH) + 1;
                    int year = cal.get(Calendar.YEAR);
                    Path dayFolder = createFolder(createFolder(createFolder(downloadsFolder, String.valueOf(year)), String.valueOf(month)), String.valueOf(dayOfMonth));
                    UploadedFile uploadedFile = ctx.uploadedFile("theFile");
                    long time = System.currentTimeMillis();
                    String name = time + "-" + uploadedFile.filename();
                    String path = PathHelper.getOrCreateFile(dayFolder.resolve(name)).orElseThrow().toString();
                    String url = ctx.queryParam("url") + path.substring(config.downloadsFolder.get().length() + 1).replace("\\", "/");
                    LOGGER.info("New File! {} @ {}", name, url);
                    FileUtil.streamToFile(uploadedFile.content(), path);
                    ctx.result(JsonUtil.DEFAULT.toJson(new Return(uploadedFile.filename(), time, url)));

                } else {
                    ctx.status(401);
                }
            });
        }catch (Exception e) {
            LOGGER.error("Failed to start javalin", e);
        }
    }
    public static Path createFolder(Path baseFolder, String subFolder) throws IOException {
        return PathHelper.tryGetOrCreateFolder(baseFolder.resolve(subFolder));
    }

    public static record Return(String fileName, long time, String location) {}

    public static class Config {

        public ConfigValue<Integer> port;
        public ConfigValue<String> apiKey;
        public ConfigValue<String> discord;
        public ConfigValue<String> downloadsFolder;

        public Config(ConfigCreator configCreator) {
            port = configCreator.createInteger("port", 9595);
            apiKey = configCreator.createString("apiKey", "");
            discord = configCreator.createString("discord", "");
            downloadsFolder = configCreator.createString("downloadsFolder", "downloads");
        }
    }
}