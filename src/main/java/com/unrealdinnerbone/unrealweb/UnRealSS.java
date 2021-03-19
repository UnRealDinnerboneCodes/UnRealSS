package com.unrealdinnerbone.unrealweb;

import com.unrealdinnerbone.config.ConfigManager;
import com.unrealdinnerbone.config.api.IConfig;
import com.unrealdinnerbone.unreallib.JsonUtil;
import com.unrealdinnerbone.unreallib.TaskScheduler;
import com.unrealdinnerbone.unreallib.discord.DiscordWebhook;
import com.unrealdinnerbone.unreallib.discord.EmbedObject;
import com.unrealdinnerbone.unreallib.file.PathHelper;
import io.javalin.Javalin;
import io.javalin.core.JavalinConfig;
import io.javalin.core.util.FileUtil;
import io.javalin.http.UploadedFile;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Calendar;

import static io.javalin.Javalin.log;

public class UnRealSS {

    private static final Logger LOGGER = LoggerFactory.getLogger("SS");
    public static Javalin javalin;

    public static void main(String[] args) throws IOException {
        ConfigManager configManager = ConfigManager.createSimpleEnvPropertyConfigManger();
        Config config = configManager.loadConfig(new Config());
        javalin = Javalin.create(JavalinConfig::enableCorsForAllOrigins);
        javalin.get("/", ctx -> ctx.result("Website Online"));
        Path downloadsFolder = PathHelper.getOrCreateFolder(Path.of(config.downloadsFolder));
        javalin.post("/", ctx -> {
            if(ctx.header("key").equals(config.apiKey)) {
                Calendar cal = Calendar.getInstance();
                int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
                int month = cal.get(Calendar.MONTH) +1;
                int year = cal.get(Calendar.YEAR);
                Path dayFolder = createFolder(createFolder(createFolder(downloadsFolder, String.valueOf(year)), String.valueOf(month)), String.valueOf(dayOfMonth));
                UploadedFile uploadedFile = ctx.uploadedFile("theFile");
                long time = System.currentTimeMillis();
                String name = RandomStringUtils.randomAlphanumeric(5).toUpperCase() + time + "-" + uploadedFile.getFilename();
                String path = PathHelper.getOrCreateFile(dayFolder.resolve(name)).toString();
                String url = ctx.queryParam("url") + path.substring(config.downloadsFolder.length() + 1).replace("\\", "/");
                log.info("[{}] New File! {} @ {}", cal, name, url);
                FileUtil.streamToFile(uploadedFile.getContent(), path);
                ctx.result(JsonUtil.GSON.toJson(new Return(uploadedFile.getFilename(), time, url)));
                TaskScheduler.handleTaskOnThread(() -> {
                    DiscordWebhook discordWebhook = DiscordWebhook.webhookFor(config.discord);
                    discordWebhook.addEmbed(EmbedObject.builder().image(url).build());
                    try {
                        discordWebhook.execute();
                    } catch (IOException e) {
                        log.error("Error with webhook", e);
                    }
                });
            }
        });
        javalin.start(Integer.parseInt(config.port));
    }

    public static Path createFolder(Path baseFolder, String subFolder) throws IOException {
        return createGoAwayPage(PathHelper.getOrCreateFolder(baseFolder.resolve(subFolder)));
    }

    public static Path createGoAwayPage(Path location) {
        try {
            Files.writeString(location, "GO AWAY", StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        } catch(IOException e) {
            LOGGER.error("Error while writing to file", e);
        }
        return location;
    }

    public static record Return(String fileName, long time, String location) {}

    public static class Config implements IConfig {

        public String port = "9595";
        public String apiKey = "";
        public String discord = "";
        public String downloadsFolder = "downloads";

        @Override
        public String getName() {
            return "config";
        }

        @Override
        public String getFolderName() {
            return "config";
        }
    }




}


