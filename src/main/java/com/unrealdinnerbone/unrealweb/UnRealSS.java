package com.unrealdinnerbone.unrealweb;

import com.unrealdinnerbone.config.ConfigManager;
import com.unrealdinnerbone.config.api.IConfig;
import com.unrealdinnerbone.unreallib.JsonUtil;
import com.unrealdinnerbone.unreallib.TaskScheduler;
import com.unrealdinnerbone.unreallib.discord.DiscordWebhook;
import com.unrealdinnerbone.unreallib.discord.EmbedObject;
import com.unrealdinnerbone.unreallib.file.FileHelper;
import io.javalin.Javalin;
import io.javalin.core.JavalinConfig;
import io.javalin.core.util.FileUtil;
import io.javalin.http.UploadedFile;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.jetty.util.MathUtils;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

@Slf4j
public class UnRealSS {

    public static Javalin javalin;

    public static void main(String[] args) {
        ConfigManager configManager = ConfigManager.createSimpleEnvPropertyConfigManger();
        Config config = configManager.loadConfig(new Config());
        javalin = Javalin.create(JavalinConfig::enableCorsForAllOrigins);
        javalin.get("/", ctx -> ctx.result("Website Online"));
        File downloadsFolder = FileHelper.getOrCreateFolder(config.downloadsFolder);
        javalin.post("/", ctx -> {
            Calendar cal = Calendar.getInstance();
            int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
            int month = cal.get(Calendar.MONTH) +1;
            int year = cal.get(Calendar.YEAR);
            File dayFolder = createFolder(createFolder(createFolder(downloadsFolder, String.valueOf(year)), String.valueOf(month)), String.valueOf(dayOfMonth));
            UploadedFile uploadedFile = ctx.uploadedFile("theFile");
            long time = System.currentTimeMillis();
            String name = RandomStringUtils.randomAlphanumeric(5).toUpperCase() + time + "-" + uploadedFile.getFilename();
            String path = FileHelper.getOrCreateFile(dayFolder, name).getPath();
            String url = ctx.queryParam("url") + path.substring(config.downloadsFolder.length() + 1).replace("\\", "/");
            log.info("[{}] New File! {} @ {}", cal, name, url);
            FileUtil.streamToFile(uploadedFile.getContent(), path);
            ctx.result(JsonUtil.getBasicGson().toJson(new Return(uploadedFile.getFilename(), time, url)));
            TaskScheduler.handleTaskOnThread(() -> {
                DiscordWebhook discordWebhook = new DiscordWebhook(config.discord);
                discordWebhook.addEmbed(EmbedObject.builder().image(url).build());
                try {
                    discordWebhook.execute();
                } catch (IOException e) {
                    log.error("Error with webhook", e);
                }
            });
        });
        javalin.start(Integer.parseInt(config.port));
    }

    public static File createFolder(File baseFolder, String subFolder) {
        return createGoAwayPage(FileHelper.getOrCreateFolder(baseFolder, subFolder));
    }

    public static File createGoAwayPage(File location) {
        FileHelper.writeStringToFile("GO AWAY", FileHelper.getOrCreateFile(location, "index.html"), false);
        return location;
    }

    @AllArgsConstructor
    public static class Return {
        public String fileName;
        public long time;
        public String location;
    }

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


