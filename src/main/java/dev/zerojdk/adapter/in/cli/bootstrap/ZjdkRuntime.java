package dev.zerojdk.adapter.in.cli.bootstrap;

import dev.zerojdk.adapter.out.event.InMemoryDomainEventPublisher;
import dev.zerojdk.domain.port.out.PlatformDetection;
import dev.zerojdk.domain.service.catalog.CatalogService;
import dev.zerojdk.domain.service.catalog.storage.CatalogStorageService;
import dev.zerojdk.domain.service.config.JdkConfigService;
import dev.zerojdk.domain.service.install.JdkInstallService;
import dev.zerojdk.domain.service.release.JdkReleaseService;
import dev.zerojdk.domain.service.shell.ShellExtensionWriter;
import dev.zerojdk.domain.service.sync.ManifestSyncService;
import dev.zerojdk.domain.service.wrapper.WrapperInstaller;
import dev.zerojdk.infrastructure.VersionProvider;

public record ZjdkRuntime(PlatformDetection platformDetection, VersionProvider versionProvider,
                          CatalogService catalogService, CatalogStorageService catalogStorageService,
                          JdkConfigService jdkConfigService, JdkReleaseService jdkReleaseService,
                          JdkInstallService jdkInstallService, ManifestSyncService manifestSyncService,
                          WrapperInstaller wrapperInstaller, ShellExtensionWriter shellExtensionWriter,
                          InMemoryDomainEventPublisher eventPublisher) { }
