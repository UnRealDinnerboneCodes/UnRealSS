package com.unrealdinnerbone.unrealweb;

import com.unrealdinnerbone.unrealweb.discord.DiscordWebhook;

import java.io.IOException;

public class Test
{
    public static void main(String[] args) throws IOException {
        DiscordWebhook discordWebhook = new DiscordWebhook("https://discordapp.com/api/webhooks/672136412513959936/ZfyNHRt1MgGk_MRIRWH112tU3fg08vP7YxLLrzrDDD5kL8zZZNbFQlohLg4tHaoJmJe3");
        discordWebhook.setUsername("Cake");
        discordWebhook.setContent("Cake");
        discordWebhook.execute();
    }
}
