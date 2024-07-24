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

import java.io.File;
import java.util.Properties;
import java.util.Set;

public class ItemEntityConfig extends Config {

  public static final String CONFIG_FILE_NAME = "item_entity.cfg";
  public static final String CONFIG_FILE_HEADER = "Item Entity Configuration";

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

    // Config entries
    allowList = parseConfigValue(properties, "allow_list", allowList);
    denyList = parseConfigValue(properties, "deny_list", denyList);

    collectRadius = parseConfigValue(properties, "collect_radius", collectRadius);
    maxNumberOfItemsPerWorld =
        parseConfigValue(properties, "max_number_of_items_per_world", maxNumberOfItemsPerWorld);
    maxNumberOfItemsPerType =
        parseConfigValue(properties, "max_number_of_items_per_type", maxNumberOfItemsPerType);
    maxStackSize = parseConfigValue(properties, "max_stack_size", maxStackSize);
    movePositionToLastDrop =
        parseConfigValue(properties, "move_position_to_last_drop", movePositionToLastDrop);
    verificationCycle = parseConfigValue(properties, "verification_cycle", verificationCycle);

    // Update config file if needed
    updateConfigFileIfChanged(configFile, CONFIG_FILE_HEADER, properties, unmodifiedProperties);
  }
}
