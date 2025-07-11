package dev.zerojdk;

import dev.zerojdk.adapter.in.cli.*;

import dev.zerojdk.adapter.in.cli.bootstrap.ZjdkBootstrapper;
import dev.zerojdk.adapter.in.cli.bootstrap.ZjdkRuntime;
import dev.zerojdk.adapter.in.cli.handler.CliExecutionExceptionHandler;
import dev.zerojdk.adapter.in.cli.renderer.CommandGroupRenderer;
import picocli.CommandLine;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static picocli.CommandLine.Model.UsageMessageSpec.SECTION_KEY_OPTION_LIST;

@CommandLine.Command(name = "zjdk",
    footer = "%nSee 'zjdk help <command>' to read about a specific subcommand",
    customSynopsis = "zjdk [-v | --version] [-h | --help] <command>"
)
public class Application implements Runnable {
    @SuppressWarnings("unused")
    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true)
    private boolean helpRequested;

    @SuppressWarnings("unused")
    @CommandLine.Option(names = {"-v", "--version"}, versionHelp = true)
    private boolean versionRequested;

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    public static void main(String[] args) {
        ZjdkRuntime runtime = ZjdkBootstrapper.bootstrap();

        // CLI setup
        CommandLine commandLine = new CommandLine(new Application())
            .addSubcommand("init", new ZjdkInit(
                runtime.platformDetection(), runtime.jdkConfigService(), runtime.manifestSyncService(), runtime.eventPublisher()))
            .addSubcommand("sync", new ZjdkSync(
                runtime.platformDetection(), runtime.manifestSyncService(), runtime.eventPublisher()))
            .addSubcommand("wrapper", new ZjdkWrapper(
                runtime.platformDetection(), runtime.wrapperInstaller(), runtime.versionProvider()))

            .addSubcommand("list", new CommandLine(new ZjdkList())
                .addSubcommand("available", new ZjdkList.Available(
                    runtime.platformDetection(), runtime.catalogService()))
                .addSubcommand("installed", new ZjdkList.Installed(
                    runtime.platformDetection(), runtime.jdkReleaseService())))
            .addSubcommand("set", new CommandLine(new ZjdkSet())
                .addSubcommand("version", new ZjdkSet.Version(
                    runtime.platformDetection(), runtime.jdkConfigService(), runtime.manifestSyncService(), runtime.eventPublisher())))
            .addSubcommand("info", new ZjdkInfo(
                runtime.platformDetection(), runtime.jdkConfigService(), runtime.catalogService()))

            .addSubcommand("env", new ZjdkEnv(
                runtime.platformDetection(), runtime.jdkConfigService(), runtime.jdkReleaseService()))
            .addSubcommand("shell", new CommandLine(new ZjdkShell())
                .addSubcommand("install", new CommandLine(new ZjdkShell.Install())
                    .addSubcommand("zsh", new ZjdkShell.Install.Zsh(
                        runtime.shellExtensionWriter()))
                    .addSubcommand("bash", new ZjdkShell.Install.Bash(
                        runtime.shellExtensionWriter()))))

            .addSubcommand("update", new ZjdkUpdate(
                runtime.catalogStorageService()))
            .addSubcommand(new CommandLine.HelpCommand())
            .setExecutionExceptionHandler(new CliExecutionExceptionHandler());

        // Help page rendering
        Map<String, List<String>> sections = new LinkedHashMap<>();
        sections.put("%nBootstrap%n", List.of("init", "sync", "wrapper"));
        sections.put("%nVersion Management%n", List.of("list", "set", "info"));
        sections.put("%nEnvironment%n", List.of("env", "shell"));
        sections.put("%nMaintenance%n", List.of("update"));

        new CommandGroupRenderer(sections).apply(commandLine);

        // remove default help/version options
        commandLine.getHelpSectionMap().remove(SECTION_KEY_OPTION_LIST);

        // Version
        commandLine.getCommandSpec()
            .versionProvider(() -> new String[] { runtime.versionProvider().getVersion() != null
                    ? runtime.versionProvider().getVersion()
                    : "unknown" });

        // Help Command
        if (args.length >= 2 && args[0].equals("help")) {
            CommandLine current = commandLine;

            for (int i = 1; i < args.length; i++) {
                Map<String, CommandLine> subs = current.getSubcommands();
                String next = args[i];

                if (!subs.containsKey(next)) {
                    System.err.printf("Unknown subcommand: '%s'%n", next);
                    current.usage(System.err);
                    return;
                }
                current = subs.get(next);
            }

            current.usage(System.out);
            return;
        }

        System.exit(commandLine.execute(args));
    }

    @Override
    public void run() {
        spec
            .commandLine()
            .usage(System.err);
    }
}
