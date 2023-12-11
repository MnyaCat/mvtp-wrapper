package com.mnyacat.mvtpwrapper.commands;

import java.util.List;

import org.bukkit.command.Command;
import org.jetbrains.annotations.NotNull;

import com.mnyacat.mvtpwrapper.MvtpWrapper;

public abstract class MvtpWrapperCommand extends Command {
    protected MvtpWrapper plugin;

    public MvtpWrapperCommand(@NotNull MvtpWrapper plugin, @NotNull String name, @NotNull String description,
            @NotNull String usageMessage, @NotNull List<String> aliases) {
        super(name, description, usageMessage, aliases);
        this.plugin = plugin;
    }
}
