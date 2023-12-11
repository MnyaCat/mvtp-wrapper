package com.mnyacat.mvtpwrapper.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.PortalType;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.mnyacat.mvtpwrapper.DatabaseManager;
import com.mnyacat.mvtpwrapper.LastPlayerDimension;
import com.mnyacat.mvtpwrapper.MWLocation;
import com.mnyacat.mvtpwrapper.MvtpWrapper;
import com.mnyacat.mvtpwrapper.utils.MWSafeTTeleporter;
import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.enums.TeleportResult;
import com.onarandombox.MultiverseNetherPortals.MultiverseNetherPortals;

public class TeleportCommand extends MvtpWrapperCommand {

    private UUID targetWorldUUID;

    public TeleportCommand(@NotNull MvtpWrapper plugin, @NotNull UUID targetWorldUUID, @NotNull String name,
            @NotNull String description, @NotNull String usageMessage, @NotNull List<String> aliases) {
        super(plugin, name, description, usageMessage, aliases);
        this.targetWorldUUID = targetWorldUUID;

        World targetWorld = Bukkit.getWorld(targetWorldUUID);
        this.setPermission("mvtpwrapper.teleportworld." + targetWorld.getName());
    }

    public enum TeleportMode {
        DEFAULT,
        FORCE,
        SPAWN;
    }

    @Override
    public @NotNull boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel,
            @NotNull String[] args) {
        Player targetPlayer = null;
        TeleportMode teleportMode;

        @Nullable
        String rawTargetPlayerName = null;
        @Nullable
        String rawTeleportMode = null;

        if (args.length == 1) {
            if (Arrays.asList(Arrays.stream(TeleportMode.values()).map(mode -> mode.name()).toArray())
                    .contains(args[0].toUpperCase())) {
                rawTeleportMode = args[0];
            } else {
                rawTargetPlayerName = args[0];
            }
        } else if (args.length == 2) {
            rawTargetPlayerName = args[0];
            rawTeleportMode = args[1];
        } else if (args.length > 2) {
            sender.sendRichMessage("<red>引数が多すぎます。このコマンドの引数は2つまでです。");
            return false;
        }

        if (Objects.nonNull(rawTargetPlayerName)) {
            Player nullablePlayer = Bukkit.getPlayerExact(rawTargetPlayerName);
            if (Objects.nonNull(nullablePlayer)) {
                targetPlayer = nullablePlayer;
            } else {
                sender.sendRichMessage("<red>プレイヤーが見つかりませんでした。");
                return false;
            }
        } else {
            if (sender instanceof Player) {
                targetPlayer = (Player) sender;
            } else {
                sender.sendRichMessage("<red>プレイヤー以外が実行する場合は、引数にプレイヤーを指定する必要があります。");
                return false;
            }
        }

        if (Objects.nonNull(rawTeleportMode)) {
            try {
                teleportMode = TeleportMode.valueOf(rawTeleportMode.toUpperCase());
            } catch (IllegalArgumentException e) {
                sender.sendRichMessage(String.format("<red>テレポートモード[%s]は存在しません。", rawTeleportMode));
                return false;
            }
        } else {
            teleportMode = TeleportMode.DEFAULT;
        }

        if (Objects.isNull(teleportMode)) {
            teleportMode = TeleportMode.DEFAULT;
        }

        // TODO: OP権限を自動でパスできるようにする
        if (sender instanceof Player) {
            Player sendPlayer = (Player) sender;
            if (sendPlayer.getUniqueId() == targetPlayer.getUniqueId()
                    && !sender.hasPermission("mvtpwrapper.teleport.self")) {
                sender.sendRichMessage("<red>自身をテレポートするにはパーミッション[mvtpwrapper.teleport.self]が必要です。");
                return false;
            } else if (sendPlayer.getUniqueId() != targetPlayer.getUniqueId()
                    && !sender.hasPermission("mvtpwrapper.teleport.other")) {
                sender.sendRichMessage("<red>他のプレイヤーをテレポートするにはパーミッション[mvtpwrapper.teleport.other]が必要です。");
                return false;
            }
        }

        MWSafeTTeleporter safeTTeleporter = plugin.getSafeTTeleporter();
        DatabaseManager db = plugin.getDatabaseManager();

        MultiverseNetherPortals netherportals = (MultiverseNetherPortals) Bukkit.getServer().getPluginManager()
                .getPlugin("Multiverse-NetherPortals");

        MultiverseCore.addPlayerToTeleportQueue(sender.getName(), targetPlayer.getName());

        UUID playerUUID = targetPlayer.getUniqueId();
        World playerWorld = targetPlayer.getWorld();
        UUID playerWorldUUID = playerWorld.getUID();
        World targetWorld = Bukkit.getWorld(targetWorldUUID);

        // 現在いるワールド、紐付けられたオーバーワールドへのテレポートを拒否する
        if (playerWorldUUID == targetWorldUUID) {
            sender.sendRichMessage("<red>現在いるワールドにテレポートすることはできません!");
            return false;
        } else if (playerWorld.getEnvironment() == Environment.NETHER
                || playerWorld.getEnvironment() == Environment.THE_END) {
            PortalType type;
            String dimensionName;
            if (playerWorld.getEnvironment() == Environment.NETHER) {
                type = PortalType.NETHER;
                dimensionName = "ネザー";
            } else {
                type = PortalType.ENDER;
                dimensionName = "ジ・エンド";
            }
            @Nullable
            String overWorldName = netherportals.getWorldLink(playerWorld.getName(), type);
            @Nullable
            World linkedOverWorld = Objects.nonNull(overWorldName) ? Bukkit.getWorld(overWorldName)
                    : null;
            if (Objects.nonNull(linkedOverWorld) && linkedOverWorld.getUID() == targetWorldUUID) {
                sender.sendRichMessage(String.format("<red>現在いる%sに紐付けられたオーバーワールドへテレポートすることはできません!", dimensionName));
                return false;
            }
        }

        // DBへ現在の座標を保存
        db.upsertLocation(playerUUID, playerWorldUUID, targetPlayer.getX(),
                targetPlayer.getY(), targetPlayer.getZ(), targetPlayer.getYaw(), targetPlayer.getPitch());
        db.upsertLastPlayerDimension(playerUUID, playerWorld);
        // DBからテレポート先のワールドの座標を取得
        // ネザーやエンドの場合は紐付けられたオーバーワールドの初期スポーンにテレポートさせる
        LastPlayerDimension lastPlayerDimension = db.getLastPlayerDimension(playerUUID, targetWorldUUID);
        @Nullable
        World destinationWorld;
        if (Objects.nonNull(lastPlayerDimension)) {
            switch (lastPlayerDimension.dimension) {
                case NETHER:
                    @Nullable
                    String netherName = netherportals.getWorldLink(targetWorld.getName(), PortalType.NETHER);
                    destinationWorld = Objects.nonNull(netherName) ? Bukkit.getWorld(netherName)
                            : Bukkit.getWorld(targetWorldUUID);
                    break;
                case THE_END:
                    @Nullable
                    String theEndName = netherportals.getWorldLink(targetWorld.getName(), PortalType.ENDER);
                    destinationWorld = Objects.nonNull(theEndName) ? Bukkit.getWorld(theEndName)
                            : Bukkit.getWorld(targetWorldUUID);
                    break;
                default:
                    destinationWorld = Bukkit.getWorld(targetWorldUUID);
                    break;
            }
        } else {
            destinationWorld = Bukkit.getWorld(targetWorldUUID);
        }

        MWLocation mwLocation = db.getLastLocation(playerUUID, destinationWorld.getUID());

        // 座標が登録されていない場合は初期スポーンへテレポートする
        Location targetLocation = Objects.nonNull(mwLocation) ? mwLocation.toLocation()
                : Bukkit.getWorld(targetWorldUUID).getSpawnLocation();
        TeleportResult result;
        switch (teleportMode) {
            case FORCE:
                result = safeTTeleporter.safelyTeleport(sender, targetPlayer, targetLocation, false);
                break;
            case SPAWN:
                result = safeTTeleporter.safelyTeleport(sender, targetPlayer,
                        Bukkit.getWorld(targetWorldUUID).getSpawnLocation(), true);
                break;
            default:
                result = safeTTeleporter.safelyTeleport(sender, targetPlayer, targetLocation, true);
                break;
        }
        switch (result) {
            case FAIL_PERMISSION:
                sender.sendRichMessage("テレポートに必要な権限がありません");
                return false;
            case FAIL_UNSAFE:
                sender.sendRichMessage(
                        "テレポート先が安全ではありません。強制的にテレポートする場合はテレポートモードをforceに、初期スポーンにテレポートする場合はspawnに設定してください。");
                return false;
            case FAIL_TOO_POOR:
                int rest = 0;
                sender.sendRichMessage(String.format("通貨が足りません。テレポートするにはあと%d必要です", rest));
                return false;
            case FAIL_INVALID:
                sender.sendRichMessage("テレポートが無効です");
                return false;
            case FAIL_OTHER:
                sender.sendRichMessage("不明なエラーが発生したため、テレポートが失敗しました");
                return false;
            default:
                break;
        }

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args)
            throws IllegalArgumentException {
        Preconditions.checkArgument(sender != null, "Sender cannot be null");
        Preconditions.checkArgument(args != null, "Arguments cannot be null");
        Preconditions.checkArgument(alias != null, "Alias cannot be null");

        if (args.length == 0 || !sender.getServer().suggestPlayerNamesWhenNullTabCompletions()) {
            return ImmutableList.of();
        }

        String lastWord = args[args.length - 1];

        if (args.length < 2) {

            Player senderPlayer = sender instanceof Player ? (Player) sender : null;

            ArrayList<String> matchedPlayers = new ArrayList<String>();
            for (Player player : sender.getServer().getOnlinePlayers()) {
                String name = player.getName();
                if ((senderPlayer == null || senderPlayer.canSee(player))
                        && StringUtil.startsWithIgnoreCase(name, lastWord)) {
                    matchedPlayers.add(name);
                }
            }

            Collections.sort(matchedPlayers, String.CASE_INSENSITIVE_ORDER);
            return matchedPlayers;
        } else {
            ArrayList<String> matchedModes = new ArrayList<String>();
            for (TeleportMode mode : TeleportMode.values()) {
                String name = mode.name().toLowerCase();
                if (StringUtil.startsWithIgnoreCase(name, lastWord)) {
                    matchedModes.add(name);
                }
            }

            Collections.sort(matchedModes, String.CASE_INSENSITIVE_ORDER);
            return matchedModes;
        }
    }
}
