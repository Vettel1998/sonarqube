/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.scan.filesystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.batch.ResourceFilter;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.scan.filesystem.FileFilter;
import org.sonar.api.scan.filesystem.ModuleExclusions;
import org.sonar.api.utils.WildcardPattern;

import java.io.File;

public class ExclusionFileFilter implements FileFilter, ResourceFilter, BatchComponent {
  private final WildcardPattern[] sourceInclusions;
  private final WildcardPattern[] testInclusions;
  private final WildcardPattern[] sourceExclusions;
  private final WildcardPattern[] testExclusions;

  public ExclusionFileFilter(ModuleExclusions exclusions) {
    sourceInclusions = WildcardPattern.create(exclusions.sourceInclusions());
    log("Included sources: ", sourceInclusions);

    sourceExclusions = WildcardPattern.create(exclusions.sourceExclusions());
    log("Excluded sources: ", sourceExclusions);

    testInclusions = WildcardPattern.create(exclusions.testInclusions());
    log("Included tests: ", testInclusions);

    testExclusions = WildcardPattern.create(exclusions.testExclusions());
    log("Excluded tests: ", testExclusions);
  }

  private void log(String title, WildcardPattern[] patterns) {
    if (patterns.length > 0) {
      Logger log = LoggerFactory.getLogger(ExclusionFileFilter.class);
      log.info(title);
      for (WildcardPattern pattern : patterns) {
        log.info("  " + pattern);
      }
    }
  }

  public boolean accept(File file, Context context) {
    WildcardPattern[] inclusionPatterns = (context.fileType() == FileType.TEST ? testInclusions : sourceInclusions);
    if (inclusionPatterns.length > 0) {
      boolean matchInclusion = false;
      for (WildcardPattern pattern : inclusionPatterns) {
        matchInclusion |= pattern.match(context.fileRelativePath());
      }
      if (!matchInclusion) {
        return false;
      }
    }
    WildcardPattern[] exclusionPatterns = (context.fileType() == FileType.TEST ? testExclusions : sourceExclusions);
    for (WildcardPattern pattern : exclusionPatterns) {
      if (pattern.match(context.fileRelativePath())) {
        return false;
      }
    }
    return true;
  }

  public boolean isIgnored(Resource resource) {
    if (ResourceUtils.isFile(resource)) {
      return isIgnoredFileResource(resource);
    }
    return false;
  }

  private boolean isIgnoredFileResource(Resource resource) {
    WildcardPattern[] inclusionPatterns = (ResourceUtils.isUnitTestClass(resource) ? testInclusions : sourceInclusions);
    if (inclusionPatterns.length > 0) {
      boolean matchInclusion = false;
      for (WildcardPattern pattern : inclusionPatterns) {
        matchInclusion |= resource.matchFilePattern(pattern.toString());
      }
      if (!matchInclusion) {
        return true;
      }
    }
    WildcardPattern[] exclusionPatterns = (ResourceUtils.isUnitTestClass(resource) ? testExclusions : sourceExclusions);
    for (WildcardPattern pattern : exclusionPatterns) {
      if (resource.matchFilePattern(pattern.toString())) {
        return true;
      }
    }
    return false;
  }

  WildcardPattern[] sourceInclusions() {
    return sourceInclusions;
  }

  WildcardPattern[] testInclusions() {
    return testInclusions;
  }

  WildcardPattern[] sourceExclusions() {
    return sourceExclusions;
  }

  WildcardPattern[] testExclusions() {
    return testExclusions;
  }
}
