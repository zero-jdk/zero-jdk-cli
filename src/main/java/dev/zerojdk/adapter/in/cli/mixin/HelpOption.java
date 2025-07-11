package dev.zerojdk.adapter.in.cli.mixin;

import picocli.CommandLine;

public class HelpOption {
    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit")
    private boolean helpRequested;
}
