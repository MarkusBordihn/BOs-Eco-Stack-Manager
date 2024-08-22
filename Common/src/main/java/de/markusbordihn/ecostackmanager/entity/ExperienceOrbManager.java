/*
 * Copyright 2024 Markus Bordihn
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.markusbordihn.ecostackmanager.entity;

import de.markusbordihn.ecostackmanager.Constants;
import de.markusbordihn.ecostackmanager.config.ExperienceOrbConfig;
import de.markusbordihn.ecostackmanager.utils.ReflectionUtils;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ExperienceOrbManager {

  private static final Logger log = LogManager.getLogger(Constants.LOG_NAME);

  private static final Map<String, Set<ExperienceOrb>> levelExperienceOrbMap =
      new ConcurrentHashMap<>();

  private static boolean raiseExpectationErrorOnce = true;

  private ExperienceOrbManager() {}

  public static boolean handleExperienceOrbJoinWorldEvent(
      ExperienceOrb experienceOrb, ServerLevel serverLevel) {

    // Check if experience orb is already removed.
    if (experienceOrb.isRemoved()) {
      return false;
    }

    // Get world name and ignore orb if it has 0 xp.
    String levelName = serverLevel.dimension().location().toString();
    if (experienceOrb.getValue() <= 0) {
      log.debug(
          "[Removed Ghost Experience Orb] {} with {} xp from {}.",
          experienceOrb,
          experienceOrb.getValue(),
          levelName);
      experienceOrb.discard();
      return true;
    } else {
      log.debug(
          "Experience Orb {} with {} xp joined {}.",
          experienceOrb,
          experienceOrb.getValue(),
          levelName);
    }

    // Handle experience orb merge, if radius is set.
    if (ExperienceOrbConfig.collectRadius > 0) {
      return handleExperienceOrbMerge(experienceOrb, levelName);
    }

    return false;
  }

  public static void handleExperienceOrbLeaveWorldEvent(
      ExperienceOrb experienceOrb, ServerLevel serverLevel) {

    // Get level name and start processing of data
    String levelName = serverLevel.dimension().location().toString();
    log.debug(
        "Experience Orb {} with {} xp left {}.",
        experienceOrb,
        experienceOrb.getValue(),
        levelName);

    // Remove item from level type map.
    Set<ExperienceOrb> experienceOrbWorldEntities = levelExperienceOrbMap.get(levelName);
    if (experienceOrbWorldEntities != null) {
      experienceOrbWorldEntities.remove(experienceOrb);
    }
  }

  public static boolean handleExperienceOrbMerge(ExperienceOrb experienceOrb, String levelName) {
    levelExperienceOrbMap.computeIfAbsent(levelName, k -> new LinkedHashSet<>());
    Set<ExperienceOrb> experienceOrbWorldEntities = levelExperienceOrbMap.get(levelName);

    // Early exit if no experience orbs are available.
    if (experienceOrbWorldEntities.isEmpty()) {
      experienceOrbWorldEntities.add(experienceOrb);
      return false;
    }

    // Get basic information about the experience orb and the surrounding area.
    double x = experienceOrb.getX();
    double y = experienceOrb.getY();
    double z = experienceOrb.getZ();
    int xStart = (int) x - ExperienceOrbConfig.collectRadius;
    int yStart = (int) y - ExperienceOrbConfig.collectRadius;
    int zStart = (int) z - ExperienceOrbConfig.collectRadius;
    int xEnd = (int) x + ExperienceOrbConfig.collectRadius;
    int yEnd = (int) y + ExperienceOrbConfig.collectRadius;
    int zEnd = (int) z + ExperienceOrbConfig.collectRadius;

    // Compare information with known items.
    for (ExperienceOrb existingExperienceOrb : new HashSet<>(experienceOrbWorldEntities)) {
      if (shouldMerge(
          experienceOrb, existingExperienceOrb, xStart, yStart, zStart, xEnd, yEnd, zEnd)) {
        mergeExperienceOrbs(experienceOrb, existingExperienceOrb, x, y, z);
        return true;
      }
    }

    experienceOrbWorldEntities.add(experienceOrb);
    return false;
  }

  private static boolean shouldMerge(
      final ExperienceOrb experienceOrb,
      final ExperienceOrb existingExperienceOrb,
      final int xStart,
      final int yStart,
      final int zStart,
      final int xEnd,
      final int yEnd,
      final int zEnd) {
    return experienceOrb.getId() != existingExperienceOrb.getId()
        && experienceOrb.isAlive()
        && existingExperienceOrb.isAlive()
        && xStart < existingExperienceOrb.getX()
        && existingExperienceOrb.getX() < xEnd
        && yStart < existingExperienceOrb.getY()
        && existingExperienceOrb.getY() < yEnd
        && zStart < existingExperienceOrb.getZ()
        && existingExperienceOrb.getZ() < zEnd;
  }

  private static void mergeExperienceOrbs(
      ExperienceOrb experienceOrb,
      ExperienceOrb existingExperienceOrb,
      final double x,
      final double y,
      final double z) {
    // Combine experience orb values.
    int newExperienceValue = existingExperienceOrb.getValue() + experienceOrb.getValue();
    log.debug(
        "[Merging Experience Orb] {} with {} and {} xp.",
        experienceOrb,
        existingExperienceOrb,
        newExperienceValue);

    // Merge experience orbs values and check if it was successful.
    if (ReflectionUtils.changeIntValueField(
        existingExperienceOrb,
        new String[] {"value", "amount", "field_6159", "f_20770_"},
        newExperienceValue)) {

      // Discard experience orb if merge was successful, before moving the existing experience orb.
      if (!experienceOrb.isRemoved()) {
        experienceOrb.discard();
      }

      // Move existing experience orb to the new location, but adjust the z position.
      if (ExperienceOrbConfig.movePositionToLastDrop) {
        existingExperienceOrb.moveTo(
            x, existingExperienceOrb.getY() + ((y - existingExperienceOrb.getY()) / 4), z);
      }

    } else {
      // Move experience orb closer to existing experience orb, if merge was not successful.
      if (raiseExpectationErrorOnce) {
        log.error(
            "Reflection failed: Unable to merge experience orbs {} with {} and {} xp.",
            experienceOrb,
            existingExperienceOrb,
            newExperienceValue);
        raiseExpectationErrorOnce = false;
      }
      experienceOrb.moveTo(
          existingExperienceOrb.getBlockX(),
          existingExperienceOrb.getBlockY(),
          existingExperienceOrb.getBlockZ());
    }
  }
}
