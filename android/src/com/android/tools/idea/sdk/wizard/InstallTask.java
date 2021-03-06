/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.idea.sdk.wizard;

import com.android.annotations.VisibleForTesting;
import com.android.repository.api.*;
import com.android.repository.io.FileOp;
import com.android.sdklib.repository.AndroidSdkHandler;
import com.android.tools.idea.sdk.StudioDownloader;
import com.android.tools.idea.sdk.progress.StudioLoggerProgressIndicator;
import com.android.tools.idea.wizard.model.ModelWizardDialog;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.intellij.notification.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.HyperlinkEvent;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * {@link Task} that installs SDK packages.
 */
class InstallTask extends Task.Backgroundable {
  private final com.android.repository.api.ProgressIndicator myLogger;
  private Collection<UpdatablePackage> myInstallRequests;
  private Collection<LocalPackage> myUninstallRequests;
  private final RepoManager myRepoManager;
  private final FileOp myFileOp;
  private final InstallerFactory myInstallerFactory;
  private boolean myBackgrounded;
  @Nullable
  private Runnable myPrepareCompleteCallback;
  @Nullable
  private Function<List<RepoPackage>, Void> myCompleteCallback;
  private final SettingsController mySettingsController;

  public InstallTask(@NotNull InstallerFactory installerFactory,
                     @NotNull AndroidSdkHandler sdkHandler,
                     @NotNull SettingsController settings,
                     @NotNull com.android.repository.api.ProgressIndicator logger) {
    super(null, "Installing Android SDK", true, PerformInBackgroundOption.ALWAYS_BACKGROUND);
    myLogger = logger;
    myRepoManager = sdkHandler.getSdkManager(logger);
    myFileOp = sdkHandler.getFileOp();
    myInstallerFactory = installerFactory;
    mySettingsController = settings;
  }

  @Override
  public void onCancel() {
    myLogger.cancel();
  }

  @Override
  public void processSentToBackground() {
    myBackgrounded = true;
  }

  @Override
  public void run(@NotNull ProgressIndicator indicator) {
    final List<RepoPackage> failures = Lists.newArrayList();

    LinkedHashMap<RepoPackage, PackageOperation> operations = new LinkedHashMap<>();
    for (UpdatablePackage install : myInstallRequests) {
      operations.put(install.getRemote(), getOrCreateInstaller(install.getRemote(), indicator));
    }
    for (LocalPackage uninstall : myUninstallRequests) {
      operations.put(uninstall, getOrCreateUninstaller(uninstall));
    }
    try {
      while (!operations.isEmpty()) {
        preparePackages(operations, failures);
        if (myPrepareCompleteCallback != null) {
          myPrepareCompleteCallback.run();
        }
        if (!myBackgrounded) {
          completePackages(operations, failures);
        }
        else {
          // Otherwise show a notification that we're ready to complete.
          showPrepareCompleteNotification(operations.keySet());
          return;
        }
      }
    }
    finally {
      if (!failures.isEmpty()) {
        myLogger.logInfo("Failed packages:");
        for (RepoPackage p : failures) {
          myLogger.logInfo(String.format("- %1$s (%2$s)", p.getDisplayName(), p.getPath()));
        }
      }
    }
    // Use a simple progress indicator here so we don't pick up the log messages from the reload.
    StudioLoggerProgressIndicator progress = new StudioLoggerProgressIndicator(getClass());
    myRepoManager.loadSynchronously(RepoManager.DEFAULT_EXPIRATION_PERIOD_MS, progress, null, mySettingsController);
    if (myCompleteCallback != null) {
      myCompleteCallback.apply(failures);
    }
  }

  /**
   * Complete installation of the given packages using the given operations. If a package is completed successfully, it is removed from
   * {@code operations}. If a package fails to be installed and has a fallback operation, the fallback is added to {@code operations}, and
   * it is the responsibility of the caller to retry. If a package fails to be installed and has no fallback, it is added to
   * {@code failures}.
   */
  @VisibleForTesting
  void completePackages(@NotNull Map<RepoPackage, PackageOperation> operations, @NotNull List<RepoPackage> failures) {
    for (RepoPackage p : ImmutableSet.copyOf(operations.keySet())) {
      PackageOperation installer = operations.get(p);
      // If we're not backgrounded, go on to the final part immediately.
      if (!installer.complete(myLogger)) {
        PackageOperation fallback = installer.getFallbackOperation();
        if (fallback != null) {
          // retry the whole thing with the fallback
          myLogger.logWarning(String.format("Failed to complete operation using %s, retrying with %s", installer.getClass().getName(),
                                            fallback.getClass().getName()));
          operations.put(p, fallback);
        }
        else {
          failures.add(p);
          operations.remove(p);
        }
      }
      else {
        operations.remove(p);
      }
    }
  }

  @NotNull
  private PackageOperation getOrCreateInstaller(@NotNull RepoPackage p, @NotNull ProgressIndicator indicator) {
    // If there's already an installer in progress for this package, reuse it.
    PackageOperation op = myRepoManager.getInProgressInstallOperation(p);
    if (op == null || !(op instanceof Installer)) {
      op = myInstallerFactory.createInstaller((RemotePackage)p, myRepoManager, new StudioDownloader(indicator), myFileOp);
    }
    return op;
  }

  @NotNull
  private PackageOperation getOrCreateUninstaller(@NotNull RepoPackage p) {
    // If there's already an uninstaller in progress for this package, reuse it.
    PackageOperation op = myRepoManager.getInProgressInstallOperation(p);
    if (op == null || !(op instanceof Uninstaller) || op.getInstallStatus() == PackageOperation.InstallStatus.FAILED) {
      op = myInstallerFactory.createUninstaller((LocalPackage)p, myRepoManager, myFileOp);
    }
    return op;
  }

  /**
   * Prepare the given packages using the given operations. If preparation for a package fails, it is retried with the
   * {@link PackageOperation#getFallbackOperation() fallback operation}. If fallbacks also fail, the package is removed from
   * {@code packageOperationMap} and added to {@code failures}.
   */
  @VisibleForTesting
  void preparePackages(@NotNull Map<RepoPackage, PackageOperation> packageOperationMap,
                       @NotNull List<RepoPackage> failures) {
    for (RepoPackage pack : ImmutableSet.copyOf(packageOperationMap.keySet())) {
      PackageOperation op = packageOperationMap.get(pack);
      boolean success = false;
      while (op != null) {
        try {
          success = op.prepare(myLogger);
        }
        catch (Exception e) {
          Logger.getInstance(getClass()).warn(e);
        }
        if (success) {
          packageOperationMap.put(pack, op);
          break;
        }
        op = op.getFallbackOperation();
      }
      if (!success) {
        failures.add(pack);
        packageOperationMap.remove(pack);
      }
    }
  }

  private void showPrepareCompleteNotification(@NotNull final Collection<RepoPackage> packages) {
    final NotificationListener notificationListener = new NotificationListener.Adapter() {
      @Override
      protected void hyperlinkActivated(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
        if ("install".equals(event.getDescription())) {
          ModelWizardDialog dialogForPaths =
            SdkQuickfixUtils.createDialogForPackages(null, myInstallRequests, myUninstallRequests, false);
          if (dialogForPaths != null) {
            dialogForPaths.show();
          }
        }
        notification.expire();
      }
    };
    final NotificationGroup group = new NotificationGroup("SDK Installer", NotificationDisplayType.STICKY_BALLOON, false);
    Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
    final Project[] openProjectsOrNull = openProjects.length == 0 ? new Project[]{null} : openProjects;
    ApplicationManager.getApplication().invokeLater(
      () -> {
        for (Project p : openProjectsOrNull) {
          String message;
          if (packages.size() == 1) {
            RepoPackage pack = packages.iterator().next();
            PackageOperation op = myRepoManager.getInProgressInstallOperation(pack);
            // op shouldn't be null. But just in case, we assume it's an install.
            String opName = op == null || op instanceof Installer ? "Install" : "Uninstall";
            message = String.format("%1$sation of '%2$s' is ready to continue<br/><a href=\"install\">%1$s Now</a>",
                                    opName, pack.getDisplayName());
          }
          else {
            message = packages.size() + " packages are ready to install or uninstall<br/><a href=\"install\">Continue</a>";
          }
          group.createNotification(
            "SDK Install", message, NotificationType.INFORMATION, notificationListener).notify(p);
        }
      },
      ModalityState.NON_MODAL,  // Don't show while we're in a modal context (e.g. sdk manager)
      o -> {
        for (RepoPackage pack : packages) {
          PackageOperation installer = myRepoManager.getInProgressInstallOperation(pack);
          if (installer != null && installer.getInstallStatus() == PackageOperation.InstallStatus.PREPARED) {
            return false;
          }
        }
        return true;
      });
  }

  public void setCompleteCallback(Function<List<RepoPackage>, Void> completeCallback) {
    myCompleteCallback = completeCallback;
  }

  public void setPrepareCompleteCallback(@Nullable Runnable prepareCompleteCallback) {
    myPrepareCompleteCallback = prepareCompleteCallback;
  }

  public void setInstallRequests(List<UpdatablePackage> installRequests) {
    myInstallRequests = installRequests;
  }

  public void setUninstallRequests(Collection<LocalPackage> uninstallRequests) {
    myUninstallRequests = uninstallRequests;
  }
}
