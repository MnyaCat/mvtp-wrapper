package com.mnyacat.mvtpwrapper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandMap;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.mnyacat.mvtpwrapper.commands.TeleportCommand;
import com.mnyacat.mvtpwrapper.utils.MWBlockSafety;
import com.mnyacat.mvtpwrapper.utils.MWSafeTTeleporter;

public class MvtpWrapper extends JavaPlugin implements Listener {

    private DatabaseManager databaseManager;
    private MWSafeTTeleporter safeTTeleporter;
    private MWBlockSafety blockSafety;

    @Override
    public void onEnable() {
        getDataFolder().mkdir();
        try {
            this.databaseManager = new DatabaseManager();
        } catch (Exception e) {
            ComponentLogger logger = Bukkit.getPluginManager().getPlugin("MvtpWrapper").getComponentLogger();
            logger.warn(e.toString());
        }
        this.safeTTeleporter = new MWSafeTTeleporter(this);
        this.blockSafety = new MWBlockSafety(this);

        Bukkit.getPluginManager().registerEvents(this, this);

        // TODO: ワールドの追加/削除に応じてコマンドを追加/削除する
        // コマンドの実態はmwtp-worldName, エイリアスにworldNameを指定
        CommandMap commandMap = Bukkit.getServer().getCommandMap();
        // ネザーやエンドへ直接テレポートできないように、オーバーワールドのみに絞り込む
        List<World> worlds = Bukkit.getWorlds().stream().filter(world -> world.getEnvironment().getId() == 0).toList();
        for (World world : worlds) {
            final String worldName = world.getName();
            final String commandName = "mwtp-" + worldName;
            final ArrayList<String> aliases = new ArrayList<String>();
            aliases.add(worldName);
            boolean result = commandMap.register("",
                    new TeleportCommand(this, world.getUID(), commandName, commandName + "へテレポートします。",
                            "/<command>, /<command> [player], /<command> [player] [default|force|spawn]",
                            aliases));
            if (result) {
                getLogger().info(String.format("%1$sへのテレポートコマンドを[/%2$s, /%1$s]として登録しました。", worldName, commandName));
            } else {
                getLogger().warning(String.format("%sへのテレポートコマンドの登録に失敗しました。[commandName: %s]", worldName, commandName));
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().sendMessage(Component.text("Hello, " + event.getPlayer().getName() + "!"));
    }

    public DatabaseManager getDatabaseManager() {
        return this.databaseManager;
    }

    public MWSafeTTeleporter getSafeTTeleporter() {
        return this.safeTTeleporter;
    }

    public MWBlockSafety getBlockSafety() {
        return this.blockSafety;
    }

}
