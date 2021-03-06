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
package com.android.tools.idea.sdk.install.patch;

import com.android.repository.Revision;
import com.android.repository.api.Installer;
import com.android.repository.api.LocalPackage;
import com.android.repository.api.RepoManager;
import com.android.repository.impl.installer.BasicInstallerFactory;
import com.android.repository.impl.meta.RepositoryPackages;
import com.android.repository.testframework.FakeDownloader;
import com.android.repository.testframework.FakeRepoManager;
import com.android.repository.testframework.MockFileOp;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static com.android.repository.testframework.FakePackage.FakeLocalPackage;
import static com.android.repository.testframework.FakePackage.FakeRemotePackage;
import static org.junit.Assert.*;

/**
 * Tests for {@link PatchInstallerFactory}
 */

public class PatchInstallerFactoryTest {
  private PatchInstallerFactory myInstallerFactory;
  private RepoManager myRepoManager;
  private RepositoryPackages myRepositoryPackages;
  private MockFileOp myFileOp;
  private static final LocalPackage PATCHER_4 = new FakeLocalPackage("patcher;v4");
  private static final LocalPackage PATCHER_2 = new FakeLocalPackage("patcher;v2");

  @Before
  public void setUp() {
    myFileOp = new MockFileOp();
    myInstallerFactory = new PatchInstallerFactory();
    myInstallerFactory.setFallbackFactory(new BasicInstallerFactory());
    myRepositoryPackages = new RepositoryPackages();
    File root = new File("/sdk");
    myRepoManager = new FakeRepoManager(root, myRepositoryPackages);
  }

  @Test
  public void cantHandleLinuxUninstallWithPatcher() {
    LocalPackage p = new FakeLocalPackage("foo");
    myRepositoryPackages.setLocalPkgInfos(ImmutableList.of(p, PATCHER_4));
    assertFalse(myInstallerFactory.canHandlePackage(p, myRepoManager, myFileOp));
  }

  @Test
  public void canHandleWindowsUninstallWithPatcher() {
    myFileOp.setIsWindows(true);
    LocalPackage p = new FakeLocalPackage("foo");
    myRepositoryPackages.setLocalPkgInfos(ImmutableList.of(p, PATCHER_4));
    assertTrue(myInstallerFactory.canHandlePackage(p, myRepoManager, myFileOp));
  }

  @Test
  public void cantHandleWindowsUninstallWithoutPatcher() {
    myFileOp.setIsWindows(true);
    LocalPackage p = new FakeLocalPackage("foo");
    myRepositoryPackages.setLocalPkgInfos(ImmutableList.of(p));
    assertFalse(myInstallerFactory.canHandlePackage(p, myRepoManager, myFileOp));
  }

  @Test
  public void cantHandleWindowsUninstallOfLatestPatcher() {
    myFileOp.setIsWindows(true);
    myRepositoryPackages.setLocalPkgInfos(ImmutableList.of(PATCHER_4));
    assertFalse(myInstallerFactory.canHandlePackage(PATCHER_4, myRepoManager, myFileOp));
  }

  @Test
  public void cantHandleNoPatchOnLinux() {
    FakeRemotePackage p = new FakeRemotePackage("foo");
    myRepositoryPackages.setLocalPkgInfos(ImmutableList.of(PATCHER_4));
    myRepositoryPackages.setRemotePkgInfos(ImmutableList.of(p));
    assertFalse(myInstallerFactory.canHandlePackage(p, myRepoManager, myFileOp));
  }

  @Test
  public void canHandleOnLinux() {
    FakeRemotePackage remote = new FakeRemotePackage("foo");
    remote.setRevision(new Revision(2));
    remote.setPatchInfo("foo", new Revision(1));
    FakeLocalPackage local = new FakeLocalPackage("foo");
    local.setRevision(new Revision(1));
    myRepositoryPackages.setLocalPkgInfos(ImmutableList.of(local, PATCHER_4));
    myRepositoryPackages.setRemotePkgInfos(ImmutableList.of(remote));
    assertTrue(myInstallerFactory.canHandlePackage(remote, myRepoManager, myFileOp));
  }

  @Test
  public void cantHandleWrongPatch() {
    FakeRemotePackage remote = new FakeRemotePackage("foo");
    remote.setRevision(new Revision(2));
    remote.setPatchInfo("foo", new Revision(1));
    FakeLocalPackage local = new FakeLocalPackage("foo");
    local.setRevision(new Revision(1, 1));
    myRepositoryPackages.setLocalPkgInfos(ImmutableList.of(local, PATCHER_4));
    myRepositoryPackages.setRemotePkgInfos(ImmutableList.of(remote));
    assertFalse(myInstallerFactory.canHandlePackage(remote, myRepoManager, myFileOp));
  }

  @Test
  public void canHandleOnWindows() {
    myFileOp.setIsWindows(true);
    FakeRemotePackage remote = new FakeRemotePackage("foo");
    remote.setRevision(new Revision(2));
    remote.setPatchInfo("foo", new Revision(1));
    FakeLocalPackage local = new FakeLocalPackage("foo");
    local.setRevision(new Revision(1));
    myRepositoryPackages.setLocalPkgInfos(ImmutableList.of(local, PATCHER_4));
    myRepositoryPackages.setRemotePkgInfos(ImmutableList.of(remote));
    assertTrue(myInstallerFactory.canHandlePackage(remote, myRepoManager, myFileOp));
  }

  @Test
  public void canHandleNoPatchOnWindowsWithNewPatcher() {
    myFileOp.setIsWindows(true);
    FakeRemotePackage remote = new FakeRemotePackage("foo");
    remote.setRevision(new Revision(2));
    FakeLocalPackage local = new FakeLocalPackage("foo");
    local.setRevision(new Revision(1));
    myRepositoryPackages.setLocalPkgInfos(ImmutableList.of(local, PATCHER_4));
    myRepositoryPackages.setRemotePkgInfos(ImmutableList.of(remote));
    assertTrue(myInstallerFactory.canHandlePackage(remote, myRepoManager, myFileOp));
  }

  @Test
  public void cantHandleNoSrcOnWindows() {
    myFileOp.setIsWindows(true);
    FakeRemotePackage remote = new FakeRemotePackage("foo");
    remote.setRevision(new Revision(2));
    myRepositoryPackages.setLocalPkgInfos(ImmutableList.of(PATCHER_4));
    myRepositoryPackages.setRemotePkgInfos(ImmutableList.of(remote));
    assertFalse(myInstallerFactory.canHandlePackage(remote, myRepoManager, myFileOp));
  }

  @Test
  public void cantHandleNoPatchOnWindowsWithOldPatcher() {
    myFileOp.setIsWindows(true);
    FakeRemotePackage remote = new FakeRemotePackage("foo");
    remote.setRevision(new Revision(2));
    FakeLocalPackage local = new FakeLocalPackage("foo");
    local.setRevision(new Revision(1));
    myRepositoryPackages.setLocalPkgInfos(ImmutableList.of(local, PATCHER_2));
    myRepositoryPackages.setRemotePkgInfos(ImmutableList.of(remote));
    assertFalse(myInstallerFactory.canHandlePackage(remote, myRepoManager, myFileOp));
  }

  @Test
  public void cantHandleNoPatchOnWindowsWithNoPatcher() {
    myFileOp.setIsWindows(true);
    FakeRemotePackage remote = new FakeRemotePackage("foo");
    remote.setRevision(new Revision(2));
    FakeLocalPackage local = new FakeLocalPackage("foo");
    local.setRevision(new Revision(1));
    myRepositoryPackages.setLocalPkgInfos(ImmutableList.of(local));
    myRepositoryPackages.setRemotePkgInfos(ImmutableList.of(remote));
    assertFalse(myInstallerFactory.canHandlePackage(remote, myRepoManager, myFileOp));
  }

  @Test
  public void createPatchUninstaller() {
    myFileOp.setIsWindows(true);
    FakeLocalPackage p = new FakeLocalPackage("foo");
    myRepositoryPackages.setLocalPkgInfos(ImmutableList.of(p, PATCHER_4));
    assertTrue(myInstallerFactory.createUninstaller(p, myRepoManager, myFileOp) instanceof PatchUninstaller);
  }

  @Test
  public void createFallbackUninstaller() {
    FakeLocalPackage p = new FakeLocalPackage("foo");
    assertFalse(myInstallerFactory.createUninstaller(p, myRepoManager, myFileOp) instanceof PatchUninstaller);
  }

  @Test
  public void createInstallerWithPatch() {
    FakeRemotePackage remote = new FakeRemotePackage("foo");
    remote.setRevision(new Revision(2));
    remote.setPatchInfo("foo", new Revision(1));
    FakeLocalPackage local = new FakeLocalPackage("foo");
    local.setRevision(new Revision(1));
    myRepositoryPackages.setLocalPkgInfos(ImmutableList.of(local, PATCHER_4));
    myRepositoryPackages.setRemotePkgInfos(ImmutableList.of(remote));
    Installer installer = myInstallerFactory.createInstaller(remote, myRepoManager, new FakeDownloader(myFileOp), myFileOp);
    assertTrue(installer instanceof PatchInstaller);
  }

  @Test
  public void createInstallerWithoutPatch() {
    myFileOp.setIsWindows(true);
    FakeRemotePackage remote = new FakeRemotePackage("foo");
    remote.setRevision(new Revision(2));
    FakeLocalPackage local = new FakeLocalPackage("foo");
    local.setRevision(new Revision(1));
    myRepositoryPackages.setLocalPkgInfos(ImmutableList.of(local, PATCHER_4));
    myRepositoryPackages.setRemotePkgInfos(ImmutableList.of(remote));
    Installer installer = myInstallerFactory.createInstaller(remote, myRepoManager, new FakeDownloader(myFileOp), myFileOp);
    assertTrue(installer instanceof FullInstaller);
  }

  @Test
  public void createFallbackInstaller() {
    FakeRemotePackage remote = new FakeRemotePackage("foo");
    remote.setRevision(new Revision(2));
    FakeLocalPackage local = new FakeLocalPackage("foo");
    local.setRevision(new Revision(1));
    myRepositoryPackages.setLocalPkgInfos(ImmutableList.of(local, PATCHER_4));
    myRepositoryPackages.setRemotePkgInfos(ImmutableList.of(remote));
    Installer installer = myInstallerFactory.createInstaller(remote, myRepoManager, new FakeDownloader(myFileOp), myFileOp);
    assertNotNull(installer);
    assertFalse(installer instanceof PatchOperation);
  }
}
