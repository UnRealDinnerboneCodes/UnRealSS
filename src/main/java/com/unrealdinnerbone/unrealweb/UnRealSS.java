package com.unrealdinnerbone.unrealweb;

import com.unrealdinnerbone.config.ConfigManager;
import com.unrealdinnerbone.config.IConfigCreator;
import com.unrealdinnerbone.config.config.IntegerConfig;
import com.unrealdinnerbone.config.config.StringConfig;
import com.unrealdinnerbone.unreallib.ExceptionHelper;
import com.unrealdinnerbone.unreallib.TaskScheduler;
import com.unrealdinnerbone.unreallib.discord.DiscordWebhook;
import com.unrealdinnerbone.unreallib.discord.EmbedObject;
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
    public static Javalin javalin;

    public static void main(String[] args) {
        ConfigManager configManager = ConfigManager.createSimpleEnvPropertyConfigManger();
        Config config = configManager.loadConfig("config", Config::new);
        javalin = Javalin.create(theConfig -> {});
        javalin.get("/", ctx -> ctx.result("Website Online"));
        ExceptionHelper.handle(() -> PathHelper.tryGetOrCreateFolder(Path.of(config.getDownloadsFolder())),
                downloadsFolder -> javalin.post("/", ctx -> {
                    String head = ctx.header("key");
                    if(head != null && head.equals(config.getApiKey())) {
                        Calendar cal = Calendar.getInstance();
                        int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
                        int month = cal.get(Calendar.MONTH) + 1;
                        int year = cal.get(Calendar.YEAR);
                        Path dayFolder = createFolder(createFolder(createFolder(downloadsFolder, String.valueOf(year)), String.valueOf(month)), String.valueOf(dayOfMonth));
                        UploadedFile uploadedFile = ctx.uploadedFile("theFile");
                        long time = System.currentTimeMillis();
                        String addRandom = ctx.header("time");
                        String name = uploadedFile.filename();
                        if(addRandom != null) {
                            name = uploadedFile.filename();
                        }
                        String path = PathHelper.getOrCreateFile(dayFolder.resolve(name)).toString();
                        String url = ctx.queryParam("url") + path.substring(config.getDownloadsFolder().length() + 1).replace("\\", "/");
                        LOGGER.info("[{}] New File! {} @ {}", cal, name, url);
                        FileUtil.streamToFile(uploadedFile.content(), path);
                        ctx.result(JsonUtil.DEFAULT.toJson(Return.class, new Return(uploadedFile.filename(), time, url)));

                        TaskScheduler.handleTaskOnThread(() -> {
                            try {
                                DiscordWebhook.of(config.getDiscord()).addEmbed(EmbedObject.builder().image(url).build()).execute();
                            }catch(IOException | InterruptedException e) {
                                LOGGER.error("Error with webhook", e);
                            }
                        });
                    }else {
                        ctx.status(401);
                    }
                }), (IOException e) -> LOGGER.error("Error creating downloads folder", e));
        javalin.start(config.getPort());
    }

    public static Path createFolder(Path baseFolder, String subFolder) throws IOException {
        return PathHelper.tryGetOrCreateFolder(baseFolder.resolve(subFolder));
    }

    public static record Return(String fileName, long time, String location) {}

    public static class Config {

        private final IntegerConfig port;
        private final StringConfig apiKey;
        private final StringConfig discord;
        private final StringConfig downloadsFolder;

        public Config(IConfigCreator creator) {
            port = creator.createInteger("port", 9595);
            apiKey = creator.createString("apiKey", "key");
            discord = creator.createString("discord", "webhook");
            downloadsFolder = creator.createString("downloadsFolder", "downloads");
        }

        public int getPort() {
            return port.getValue();
        }

        public String getApiKey() {
            return apiKey.getValue();
        }

        public String getDiscord() {
            return discord.getValue();
        }

        public String getDownloadsFolder() {
            return downloadsFolder.getValue();
        }
    }
}