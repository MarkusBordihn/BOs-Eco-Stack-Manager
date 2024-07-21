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
import de.markusbordihn.ecostackmanager.config.EcoStackManagerConfig;
import java.lang.reflect.Field;
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
          "Remove Experience Orb {} with {} xp from {}.",
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
    return handleExperienceOrbMerge(experienceOrb, levelName);
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

    if (experienceOrbWorldEntities.isEmpty()) {
      experienceOrbWorldEntities.add(experienceOrb);
      return false;
    }

    int x = (int) experienceOrb.getX();
    int y = (int) experienceOrb.getY();
    int z = (int) experienceOrb.getZ();
    int xStart = x - EcoStackManagerConfig.EXPERIENCE_ORB_COLLECT_RADIUS;
    int yStart = y - EcoStackManagerConfig.EXPERIENCE_ORB_COLLECT_RADIUS;
    int zStart = z - EcoStackManagerConfig.EXPERIENCE_ORB_COLLECT_RADIUS;
    int xEnd = x + EcoStackManagerConfig.EXPERIENCE_ORB_COLLECT_RADIUS;
    int yEnd = y + EcoStackManagerConfig.EXPERIENCE_ORB_COLLECT_RADIUS;
    int zEnd = z + EcoStackManagerConfig.EXPERIENCE_ORB_COLLECT_RADIUS;

    for (ExperienceOrb existingExperienceOrb : new HashSet<>(experienceOrbWorldEntities)) {
      if (shouldMerge(
          experienceOrb, existingExperienceOrb, xStart, yStart, zStart, xEnd, yEnd, zEnd)) {
        mergeExperienceOrbs(experienceOrb, existingExperienceOrb);
        return true;
      }
    }

    experienceOrbWorldEntities.add(experienceOrb);
    return false;
  }

  private static boolean shouldMerge(
      ExperienceOrb experienceOrb,
      ExperienceOrb existingExperienceOrb,
      int xStart,
      int yStart,
      int zStart,
      int xEnd,
      int yEnd,
      int zEnd) {
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
      ExperienceOrb experienceOrb, ExperienceOrb existingExperienceOrb) {
    int newExperienceValue = existingExperienceOrb.getValue() + experienceOrb.getValue();
    log.debug(
        "Merged experience orb {} with {} and {} xp.",
        experienceOrb,
        existingExperienceOrb,
        newExperienceValue);
    try {
      Field experienceValue = existingExperienceOrb.getClass().getDeclaredField("value");
      experienceValue.setAccessible(true);
      experienceValue.setInt(existingExperienceOrb, newExperienceValue);
      experienceOrb.moveTo(
          existingExperienceOrb.getX(), existingExperienceOrb.getY(), existingExperienceOrb.getZ());
      experienceOrb.discard();
    } catch (NoSuchFieldException | IllegalAccessException e) {
      log.error(
          "Unable to merge experience orbs {} with {} due to {}",
          experienceOrb,
          existingExperienceOrb,
          e);
    }
  }
}