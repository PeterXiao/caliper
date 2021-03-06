/*
 * Copyright (C) 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.caliper.runner.config;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.google.caliper.api.ResultProcessor;
import com.google.caliper.model.Trial;
import com.google.caliper.runner.options.CaliperOptions;
import com.google.caliper.runner.options.ParsedOptions;
import com.google.caliper.runner.target.Device;
import com.google.caliper.runner.target.LocalDevice;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.lang.management.ManagementFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests {@link CaliperConfig}. */
@RunWith(JUnit4.class)
public class CaliperConfigTest {
  @Rule public TemporaryFolder folder = new TemporaryFolder();

  private static final CaliperConfig DEVICE_TEST_CONFIG =
      new CaliperConfig(
          ImmutableMap.of(
              "device.local.type", "local",
              "device.local.options.defaultVmType", "jvm",
              "device.android.type", "adb",
              "device.android.options.defaultVmType", "android"));

  private static CaliperOptions options(String... args) {
    return ParsedOptions.from(args, false);
  }

  @Test
  public void getDefaultDeviceConfig_noAndroidWorkerClasspath() {
    DeviceConfig deviceConfig = DEVICE_TEST_CONFIG.getDeviceConfig(options());
    assertThat(deviceConfig.name()).isEqualTo("local");
    assertThat(deviceConfig.type()).isEqualTo(DeviceType.LOCAL);
  }

  @Test
  public void getDefaultDeviceConfig_androidWorkerClasspath() {
    CaliperOptions options = options("--worker-classpath-android=foo");
    DeviceConfig deviceConfig = DEVICE_TEST_CONFIG.getDeviceConfig(options);
    assertThat(deviceConfig.name()).isEqualTo("android");
    assertThat(deviceConfig.type()).isEqualTo(DeviceType.ADB);
  }

  @Test
  public void getDeviceConfig() {
    DeviceConfig deviceConfig = DEVICE_TEST_CONFIG.getDeviceConfig(options("-e", "local"));
    assertThat(deviceConfig.name()).isEqualTo("local");
    assertThat(deviceConfig.type()).isEqualTo(DeviceType.LOCAL);
    assertThat(deviceConfig.options()).containsEntry("defaultVmType", "jvm");
    assertThat(deviceConfig.options()).doesNotContainKey("vmBaseDirectory");
  }

  @Test
  public void testGetDeviceConfig_nonLocalDevice() {
    DeviceConfig deviceConfig = DEVICE_TEST_CONFIG.getDeviceConfig(options("-e", "android"));
    assertThat(deviceConfig.name()).isEqualTo("android");
    assertThat(deviceConfig.type()).isEqualTo(DeviceType.ADB);
    assertThat(deviceConfig.options()).containsEntry("defaultVmType", "android");
    assertThat(deviceConfig.options()).doesNotContainKey("vmBaseDirectory");
  }

  @Test
  public void testGetDeviceConfig_missingType() {
    CaliperConfig config =
        new CaliperConfig(
            ImmutableMap.of(
                "device.local.typo", "local",
                "device.local.options.defaultVmType", "jvm"));
    try {
      config.getDeviceConfig(options("-e", "local"));
      fail();
    } catch (InvalidConfigurationException expected) {
    }
  }

  @Test
  public void getDefaultVmConfig() throws Exception {
    CaliperConfig configuration =
        new CaliperConfig(
            ImmutableMap.of(
                "device.local.type", "local",
                "vm.args", "-very -special=args"));
    Device device = LocalDevice.builder().caliperConfig(configuration).build();
    VmConfig defaultVmConfig = device.defaultVmConfig();
    assertEquals(System.getProperty("java.home"), defaultVmConfig.home().get());
    ImmutableList<String> expectedArgs =
        new ImmutableList.Builder<String>()
            .addAll(ManagementFactory.getRuntimeMXBean().getInputArguments())
            .add("-very")
            .add("-special=args")
            .build();
    assertEquals(expectedArgs, defaultVmConfig.args());
  }

  @Test
  public void getVmConfig_baseDirectoryAndName() throws Exception {
    File tempBaseDir = folder.newFolder();
    CaliperConfig configuration =
        new CaliperConfig(ImmutableMap.of("vm.baseDirectory", tempBaseDir.getAbsolutePath()));
    assertEquals(VmConfig.builder().name("test").build(), configuration.getVmConfig("test"));
  }

  @Test
  public void getVmConfig_baseDirectoryAndHome() throws Exception {
    File tempBaseDir = folder.newFolder();
    CaliperConfig configuration =
        new CaliperConfig(
            ImmutableMap.of(
                "vm.baseDirectory", tempBaseDir.getAbsolutePath(), "vm.test.home", "test-home"));
    assertEquals(
        VmConfig.builder().name("test").home("test-home").build(),
        configuration.getVmConfig("test"));
  }

  @Test
  public void getVmConfig() throws Exception {
    File jdkHome = folder.newFolder();
    CaliperConfig configuration =
        new CaliperConfig(
            ImmutableMap.of(
                "vm.args", "-a -b   -c",
                "vm.test.home", jdkHome.getAbsolutePath(),
                "vm.test.args", " -d     -e     "));
    assertEquals(
        VmConfig.builder()
            .name("test")
            .home(jdkHome.getPath())
            .addArg("-a")
            .addArg("-b")
            .addArg("-c")
            .addArg("-d")
            .addArg("-e")
            .build(),
        configuration.getVmConfig("test"));
  }

  @Test
  public void getVmConfig_escapedSpacesInArgs() throws Exception {
    File jdkHome = folder.newFolder();
    CaliperConfig configuration =
        new CaliperConfig(
            ImmutableMap.of(
                "vm.args",
                "-a=string\\ with\\ spa\\ces -b -c",
                "vm.test.home",
                jdkHome.getAbsolutePath()));
    assertEquals(
        VmConfig.builder()
            .name("test")
            .home(jdkHome.getPath())
            .addArg("-a=string with spaces")
            .addArg("-b")
            .addArg("-c")
            .build(),
        configuration.getVmConfig("test"));
  }

  @Test
  public void getInstrumentConfig() throws Exception {
    CaliperConfig configuration =
        new CaliperConfig(
            ImmutableMap.of(
                "instrument.test.class", "test.ClassName",
                "instrument.test.options.a", "1",
                "instrument.test.options.b", "excited b b excited"));
    assertEquals(
        new InstrumentConfig.Builder()
            .className("test.ClassName")
            .addOption("a", "1")
            .addOption("b", "excited b b excited")
            .build(),
        configuration.getInstrumentConfig("test"));
  }

  @Test
  public void getInstrumentConfig_notConfigured() throws Exception {
    CaliperConfig configuration =
        new CaliperConfig(
            ImmutableMap.of(
                "instrument.test.options.a", "1",
                "instrument.test.options.b", "excited b b excited"));
    try {
      configuration.getInstrumentConfig("test");
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void getConfiguredInstruments() throws Exception {
    CaliperConfig configuration =
        new CaliperConfig(
            ImmutableMap.of(
                "instrument.test.class", "test.ClassName",
                "instrument.test2.class", "test.ClassName",
                "instrument.test3.options.a", "1",
                "instrument.test4.class", "test.ClassName",
                "instrument.test4.options.b", "excited b b excited"));
    assertEquals(
        ImmutableSet.of("test", "test2", "test4"), configuration.getConfiguredInstruments());
  }

  @Test
  public void getConfiguredResultProcessors() throws Exception {
    assertEquals(
        ImmutableSet.of(),
        new CaliperConfig(ImmutableMap.<String, String>of()).getConfiguredResultProcessors());
    CaliperConfig configuration =
        new CaliperConfig(
            ImmutableMap.of("results.test.class", TestResultProcessor.class.getName()));
    assertEquals(
        ImmutableSet.of(TestResultProcessor.class), configuration.getConfiguredResultProcessors());
  }

  @Test
  public void getResultProcessorConfig() throws Exception {
    CaliperConfig configuration =
        new CaliperConfig(
            ImmutableMap.of(
                "results.test.class", TestResultProcessor.class.getName(),
                "results.test.options.g", "ak",
                "results.test.options.c", "aliper"));
    assertEquals(
        new ResultProcessorConfig.Builder()
            .className(TestResultProcessor.class.getName())
            .addOption("g", "ak")
            .addOption("c", "aliper")
            .build(),
        configuration.getResultProcessorConfig(TestResultProcessor.class));
  }

  private static final class TestResultProcessor implements ResultProcessor {
    @Override
    public void close() {}

    @Override
    public void processTrial(Trial trial) {}
  }
}
