package dev.zerojdk.adapter.in.cli.bootstrap;

import dev.zerojdk.adapter.out.SystemPropertyBasedPlatformDetection;
import dev.zerojdk.adapter.out.catalog.JsonCatalogRepository;
import dev.zerojdk.adapter.out.catalog.provider.CatalogStorageProvider;
import dev.zerojdk.adapter.out.catalog.provider.JsonCatalogStorageProvider;
import dev.zerojdk.adapter.out.catalog.storage.FsCatalogStorageMetadataRepository;
import dev.zerojdk.adapter.out.config.FsJdkConfigRepository;
import dev.zerojdk.adapter.out.download.HttpDownloadService;
import dev.zerojdk.adapter.out.event.InMemoryDomainEventPublisher;
import dev.zerojdk.adapter.out.github.client.GitHubReleaseClient;
import dev.zerojdk.adapter.out.layout.*;
import dev.zerojdk.adapter.out.release.FsJavaHomeDetector;
import dev.zerojdk.adapter.out.release.FsJdkInstaller;
import dev.zerojdk.adapter.out.release.FsJdkRegistrationRepository;
import dev.zerojdk.adapter.out.shell.FsShellExtensionStorage;
import dev.zerojdk.adapter.out.unarchiver.DetectingUnarchiverFactory;
import dev.zerojdk.adapter.out.wrapper.FsWrapperConfigRepository;
import dev.zerojdk.adapter.out.wrapper.FsWrapperScriptRepository;
import dev.zerojdk.adapter.out.wrapper.WrapperGitHubArtifactNameResolver;
import dev.zerojdk.adapter.out.wrapper.WrapperGitHubReleaseResolver;
import dev.zerojdk.domain.port.out.PlatformDetection;
import dev.zerojdk.domain.port.out.catalog.CatalogProviderService;
import dev.zerojdk.domain.port.out.catalog.CatalogStorageMetadataRepository;
import dev.zerojdk.domain.port.out.download.DownloadService;
import dev.zerojdk.domain.port.out.layout.*;
import dev.zerojdk.domain.port.out.release.JdkInstaller;
import dev.zerojdk.domain.service.catalog.CatalogService;
import dev.zerojdk.domain.service.catalog.storage.CatalogStorageService;
import dev.zerojdk.domain.service.catalog.storage.RemoteCatalogProviderService;
import dev.zerojdk.domain.service.config.JdkConfigService;
import dev.zerojdk.domain.service.install.JdkInstallService;
import dev.zerojdk.domain.service.release.JdkReleaseService;
import dev.zerojdk.domain.service.shell.ShellExtensionWriter;
import dev.zerojdk.domain.service.sync.ManifestSyncService;
import dev.zerojdk.domain.service.unarchiving.ArchiveExtractionService;
import dev.zerojdk.domain.service.wrapper.BinaryInstaller;
import dev.zerojdk.domain.service.wrapper.WrapperInstaller;
import dev.zerojdk.domain.service.wrapper.WrapperScriptGenerator;
import dev.zerojdk.infrastructure.VersionProvider;

public class ZjdkBootstrapper {
    public static ZjdkRuntime bootstrap() {
        // Common
        BaseLayout baseLayout = new FsBaseLayout();
        PlatformDetection platformDetection = new SystemPropertyBasedPlatformDetection();
        DownloadService downloadService = new HttpDownloadService();
        VersionProvider versionProvider = new VersionProvider();

        // Event Management
        InMemoryDomainEventPublisher eventPublisher = new InMemoryDomainEventPublisher();

        ArchiveExtractionService archiveExtractionService =
            new ArchiveExtractionService(new DetectingUnarchiverFactory(), eventPublisher);

        CatalogProviderService providerService = new RemoteCatalogProviderService(
            new GitHubReleaseClient(downloadService,
                "https://api.github.com/repos/zero-jdk/zero-jdk-catalog/releases/latest"),
            archiveExtractionService, eventPublisher);

        // Catalog Storage setup
        CatalogStorageService catalogStorageService = createCatalogStorageService(
            baseLayout, providerService);

        // Catalog
        CatalogService catalogService = createCatalogService(
            baseLayout, providerService);

        // JDK Config
        JdkConfigService jdkConfigService = createJdkConfigService(baseLayout, catalogService);

        // Jdk Release
        JdkReleaseService jdkReleaseService = createJdkReleaseService(baseLayout, catalogService);
        JdkInstallService jdkInstallService = createJdkInstallService(baseLayout, downloadService,
            archiveExtractionService, eventPublisher, catalogService, jdkReleaseService);

        // Sync aggregation
        ManifestSyncService manifestSyncService = new ManifestSyncService(jdkConfigService, jdkInstallService);

        // Wrapper
        WrapperInstaller wrapperInstaller = createWrapperInstaller(baseLayout);

        // Shell Extensions
        ShellExtensionWriter shellExtensionWriter = createShellExtensionWriter(baseLayout);

        return new ZjdkRuntime(platformDetection, versionProvider, catalogService, catalogStorageService,
            jdkConfigService, jdkReleaseService, jdkInstallService, manifestSyncService, wrapperInstaller,
            shellExtensionWriter, eventPublisher
        );
    }

    private static CatalogService createCatalogService(BaseLayout baseLayout, CatalogProviderService providerService) {
        CatalogStorageLayout catalogStorageLayout = new FsCatalogStorageLayout(baseLayout);
        CatalogStorageMetadataRepository metadataRepository =
            new FsCatalogStorageMetadataRepository(catalogStorageLayout);

        CatalogStorageProvider storageProvider = new JsonCatalogStorageProvider(
            new CatalogStorageService(providerService, metadataRepository));

        return new CatalogService(new JsonCatalogRepository(storageProvider));
    }

    private static JdkConfigService createJdkConfigService(BaseLayout baseLayout, CatalogService catalogService) {
        return new JdkConfigService(
            new FsJdkConfigRepository(baseLayout), catalogService);
    }

    private static JdkReleaseService createJdkReleaseService(BaseLayout baseLayout, CatalogService catalogService) {
        return new JdkReleaseService(catalogService,
            new FsJdkRegistrationRepository(
                new FsJdkReleaseLayout(baseLayout)));
    }

    private static JdkInstallService createJdkInstallService(BaseLayout baseLayout, DownloadService downloadService,
        ArchiveExtractionService archiveExtractionService, InMemoryDomainEventPublisher eventPublisher, CatalogService
        catalogService, JdkReleaseService jdkReleaseService) {

        JdkReleaseLayout jdkReleaseLayout = new FsJdkReleaseLayout(baseLayout);

        JdkInstaller installer = new FsJdkInstaller(jdkReleaseLayout,
            new FsJdkRegistrationRepository(jdkReleaseLayout),
            new FsJavaHomeDetector());

        return new JdkInstallService(eventPublisher, jdkReleaseLayout, downloadService,
            archiveExtractionService, catalogService, installer, jdkReleaseService);
    }

    private static WrapperInstaller createWrapperInstaller(BaseLayout baseLayout) {
        WrapperLayout wrapperLayout = new FsWrapperLayout(baseLayout);

        return new WrapperInstaller(
            new FsWrapperConfigRepository(wrapperLayout),
            new FsWrapperScriptRepository(wrapperLayout),
            new WrapperGitHubReleaseResolver(
                "https://github.com/zero-jdk/zero-jdk-cli/releases/download/v%s/%s",
                new WrapperGitHubArtifactNameResolver()
            ),
            new WrapperScriptGenerator(baseLayout, wrapperLayout),
            new BinaryInstaller(wrapperLayout)
        );
    }

    private static ShellExtensionWriter createShellExtensionWriter(BaseLayout baseLayout) {
        ShellExtensionLayout layout = new FsShellExtensionLayout(baseLayout);

        return new ShellExtensionWriter
            (new FsShellExtensionStorage(layout));
    }

    private static CatalogStorageService createCatalogStorageService(BaseLayout baseLayout,
        CatalogProviderService providerService) {

        CatalogStorageLayout layout = new FsCatalogStorageLayout(baseLayout);
        CatalogStorageMetadataRepository metadata =
            new FsCatalogStorageMetadataRepository(layout);

        return new CatalogStorageService(providerService, metadata);
    }
}
