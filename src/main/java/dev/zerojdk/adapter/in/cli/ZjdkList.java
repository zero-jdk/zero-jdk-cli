package dev.zerojdk.adapter.in.cli;

import dev.zerojdk.adapter.in.cli.mixin.HelpOption;
import dev.zerojdk.adapter.in.cli.renderer.JdkReleaseRenderer;
import dev.zerojdk.adapter.in.cli.renderer.JdkVersionRenderer;
import dev.zerojdk.domain.model.JdkVersion;
import dev.zerojdk.domain.model.Platform;
import dev.zerojdk.domain.model.release.JdkRelease;
import dev.zerojdk.domain.port.out.PlatformDetection;
import dev.zerojdk.domain.service.catalog.CatalogService;
import dev.zerojdk.domain.service.release.JdkReleaseService;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@CommandLine.Command(
    header = "List installed or available JDK releases",
    description = """
          %n  Lists installed or available JDK releases for the current platform.
        
          Use the 'installed' subcommand to display JDKs that are currently installed
          on the system and available for use by Zero-JDK.
        
          Use the 'available' subcommand to explore the JDK releases available from
          the catalog for the current platform. This includes multiple distributions
          such as Temurin, GraalVM, or others.
        
          Each listing is specific to the platform that zjdk is running on.
        """,
    descriptionHeading = "%nDescription:",
    optionListHeading = "Options:%n",
    commandListHeading = "%nCommands:%n",
    synopsisHeading = "%nSynopsis:%n  ",
    synopsisSubcommandLabel = "<command>"
)
public class ZjdkList {
    @CommandLine.Mixin
    private HelpOption helpOption;

    @RequiredArgsConstructor
    @CommandLine.Command(
        header = "List JDKs installed by zjdk on this system",
        description = """
              %n  Lists JDK releases installed for the current platform using Zero-JDK.

              Only releases matching the platform zjdk is running on are included.
              Output is grouped by distribution and sorted by version.
            """,
        descriptionHeading = "%nDescription:",
        optionListHeading = "Options:%n",
        synopsisHeading = "%nSynopsis:%n  "
    )
    public static class Installed implements Runnable {
        private final PlatformDetection platformDetection;
        private final JdkReleaseService jdkReleaseService;

        @CommandLine.Mixin
        private HelpOption helpOption;

        @Override
        public void run() {
            Platform platform = platformDetection.detect();

            List<JdkRelease> releases = jdkReleaseService.findInstalledJdkReleases(platform);

            new JdkReleaseRenderer().render(
                releases,
                0,
                Comparator
                    .comparing((JdkRelease release) -> release.jdkVersion().getDistribution())
                    .thenComparing((JdkRelease release) -> release.jdkVersion().getDistributionVersion())
                );
        }
    }

    @RequiredArgsConstructor
    @CommandLine.Command(
        header = "List available JDK versions from the catalog",
        description = """
              %n  Displays JDK versions available for download from the local catalog,
              filtered by the current platform.
            
              By default, shows the latest available version for each distribution.
              To filter by distribution, use the '--dist' option.
            
              Use '--all' to display all available versions for the selected distribution.
            
              To refresh the catalog used by this command, run 'zjdk update'.
            """,
        descriptionHeading = "%nDescription:",
        optionListHeading = "Options:%n",
        synopsisHeading = "%nSynopsis:%n  "
    )
    public static class Available implements Runnable {
        private final PlatformDetection platformDetection;
        private final CatalogService catalogService;

        @CommandLine.Mixin
        private HelpOption helpOption;

        @CommandLine.Option(names = {"-d", "--dist"}, description = "The name of the JDK distribution")
        private String distribution;
        @CommandLine.Option(names = {"-a", "--all"}, description = "Shows all available version of a distribution (requires --dist)")
        private boolean all;

        @Override
        public void run() {
            if (all && distribution == null) {
                throw new CommandLine.ParameterException(
                    new CommandLine(this),
                    "'--all' requires '--dist' to be specified");
            }

            Platform platform = platformDetection.detect();

            JdkVersionRenderer jdkVersionRenderer = new JdkVersionRenderer();

            if (distribution == null) {
                Map<String, List<JdkVersion>> latest = catalogService.findLatest(platform);

                latest.keySet().stream().sorted().forEach(dist -> {
                    System.out.println(dist);
                    jdkVersionRenderer.render(latest.get(dist), 2, groupBySupportThenSort());
                });
            } else {
                List<JdkVersion> versions = all
                    ? catalogService.findAllByDistribution(platform, distribution).stream()
                        .sorted(Comparator.comparing(JdkVersion::getDistributionVersion))
                        .toList()
                    : catalogService.findLatestByDistribution(platform, distribution);

                Comparator<JdkVersion> ordering = all
                    ? Comparator.comparing(JdkVersion::getDistributionVersion).reversed()
                    : this::compareLtsFirst;

                jdkVersionRenderer.render(versions, 0, ordering);
            }
        }

        private int compareLtsFirst(JdkVersion a, JdkVersion b) {
            return (a.getSupport() == JdkVersion.Support.LTS ? 0 : 1)
                 - (b.getSupport() == JdkVersion.Support.LTS ? 0 : 1);
        }

        private Comparator<JdkVersion> groupBySupportThenSort() {
            return Comparator.comparingInt(v -> v.getSupport() == JdkVersion.Support.LTS ? 0 : 1);
        }
    }
}
