package dev.zerojdk.adapter.in.cli;

import dev.zerojdk.adapter.in.cli.mixin.HelpOption;
import dev.zerojdk.domain.service.catalog.storage.CatalogStorageService;
import dev.zerojdk.domain.service.catalog.CatalogUnchangedException;
import picocli.CommandLine;
import lombok.RequiredArgsConstructor;

@CommandLine.Command(
    header = "Download the latest catalog of available JDK releases",
    description = """
          %n  Downloads the latest catalog of available JDK releases from the remote
          source and stores it locally.
        
          The catalog contains metadata for all supported JDK versions across
          various distributions and platforms. It is used by commands such as
          'init', 'set version', 'list available', and 'sync'.
        
          If the catalog is already up to date, no changes are made.
        """,
    descriptionHeading = "%nDescription:",
    optionListHeading = "Options:%n",
    synopsisHeading = "%nSynopsis:%n  "
)
@RequiredArgsConstructor
public class ZjdkUpdate implements Runnable {
    private final CatalogStorageService catalogStorageService;

    @CommandLine.Mixin
    private HelpOption helpOption;

    @Override
    public void run() {
        try {
            catalogStorageService.updateCatalogIfNewer();
            System.out.println("Catalog updated.");
        } catch (CatalogUnchangedException e) {
            System.out.println("Catalog is already up-to-date.");
        } catch (Exception e) {
            System.out.println("Failed to update catalog: " + e.getMessage());
        }
    }
}
