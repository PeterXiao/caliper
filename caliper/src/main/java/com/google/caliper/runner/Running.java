/*
 * Copyright (C) 2013 Google Inc.
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

package com.google.caliper.runner;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * A collection of annotations for bindings pertaining to the currently running experiment.
 */
public class Running {
  private Running() {}

  @Retention(RUNTIME)
  @Target({FIELD, PARAMETER, METHOD})
  @BindingAnnotation
  public @interface Benchmark {}

  @Retention(RUNTIME)
  @Target({FIELD, PARAMETER, METHOD})
  @BindingAnnotation
  public @interface BenchmarkMethod {}

  @Retention(RUNTIME)
  @Target({FIELD, PARAMETER, METHOD})
  @BindingAnnotation
  public @interface BenchmarkClass {}

  @Retention(RUNTIME)
  @Target({FIELD, PARAMETER, METHOD})
  @BindingAnnotation
  public @interface BeforeExperimentMethods {}

  @Retention(RUNTIME)
  @Target({FIELD, PARAMETER, METHOD})
  @BindingAnnotation
  public @interface AfterExperimentMethods {}
}
