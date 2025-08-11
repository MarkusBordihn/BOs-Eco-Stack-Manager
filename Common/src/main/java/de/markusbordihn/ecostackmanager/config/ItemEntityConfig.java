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

package de.markusbordihn.ecostackmanager.config;

import de.markusbordihn.ecostackmanager.Constants;
import java.io.File;
import java.util.Properties;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ItemEntityConfig extends Config {

  public static final String CONFIG_FILE_NAME = "item_entity.cfg";
  public static final String CONFIG_FILE_HEADER = "Item Entity Configuration";
  private static final Logger log = LogManager.getLogger(Constants.LOG_NAME);
  public static Set<String> allowList = Set.of();
  public static Set<String> denyList = Set.of("minecraft:diamond", "minecraft:diamond_block");

  public static int collectRadius = 3;
  public static int maxNumberOfItemsPerWorld = 128;
  public static int maxNumberOfItemsPerType = 32;
  public static int maxStackSize = 64;
  public static boolean movePositionToLastDrop = false;
  public static int verificationCycle = 64;

  public static void registerConfig() {
    registerConfigFile(CONFIG_FILE_NAME, CONFIG_FILE_HEADER);
    parseConfigFile();
  }

  public static void parseConfigFile() {
    File configFile = getConfigFile(CONFIG_FILE_NAME);
    Properties properties = readConfigFile(configFile);
    Properties unmodifiedProperties = (Properties) properties.clone();

    // Config entries with validation
    allowList = parseConfigValue(properties, "allow_list", allowList);
    denyList = parseConfigValue(properties, "deny_list", denyList);

    collectRadius = validateAndParseRadius(properties, "collect_radius", collectRadius);
    maxNumberOfItemsPerWorld =
        validateAndParsePositiveInteger(
            properties, "max_number_of_items_per_world", maxNumberOfItemsPerWorld);
    maxNumberOfItemsPerType =
        validateAndParsePositiveInteger(
            properties, "max_number_of_items_per_type", maxNumberOfItemsPerType);
    maxStackSize = validateAndParseStackSize(properties, "max_stack_size", maxStackSize);
    movePositionToLastDrop =
        parseConfigValue(properties, "move_position_to_last_drop", movePositionToLastDrop);
    verificationCycle =
        validateAndParseVerificationCycle(properties, "verification_cycle", verificationCycle);

    // Update config file if needed
    updateConfigFileIfChanged(configFile, CONFIG_FILE_HEADER, properties, unmodifiedProperties);
  }

  private static int validateAndParseRadius(Properties properties, String key, int defaultValue) {
    int value = parseConfigValue(properties, key, defaultValue);
    if (value < 0) {
      log.warn("Invalid collect_radius value: {}. Using default: {}", value, defaultValue);
      return defaultValue;
    }
    if (value > 64) {
      log.warn(
          "Collect_radius value too large ({}), capping at 64 to prevent performance issues",
          value);
      return 64;
    }
    return value;
  }

  private static int validateAndParsePositiveInteger(
      Properties properties, String key, int defaultValue) {
    int value = parseConfigValue(properties, key, defaultValue);
    if (value < 0) {
      log.warn("Invalid {} value: {}. Using default: {}", key, value, defaultValue);
      return defaultValue;
    }
    return value;
  }

  private static int validateAndParseStackSize(
      Properties properties, String key, int defaultValue) {
    int value = parseConfigValue(properties, key, defaultValue);
    if (value < 1) {
      log.warn("Invalid max_stack_size value: {}. Using default: {}", value, defaultValue);
      return defaultValue;
    }
    if (value > 64) {
      log.warn(
          "Max_stack_size value too large ({}), capping at 64 to match Minecraft limits", value);
      return 64;
    }
    return value;
  }

  private static int validateAndParseVerificationCycle(
      Properties properties, String key, int defaultValue) {
    int value = parseConfigValue(properties, key, defaultValue);
    if (value < 1) {
      log.warn("Invalid verification_cycle value: {}. Using default: {}", value, defaultValue);
      return defaultValue;
    }
    if (value > 1000) {
      log.warn("Verification_cycle value too large ({}), capping at 1000 for performance", value);
      return 1000;
    }
    return value;
  }
}
