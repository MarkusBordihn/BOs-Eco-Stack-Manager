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

package de.markusbordihn.ecostackmanager.mods;

import de.markusbordihn.ecostackmanager.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AdditionalModsMessages {

  private static final Logger log = LogManager.getLogger(Constants.LOG_NAME);

  private AdditionalModsMessages() {}

  public static void checkForIncompatibility() {
    log.info("Checking for additional mods for avoiding incompatibility ...");

    if (Constants.MOD_CLUMPS_LOADED) {
      log.error(
          "ERROR: {} groups XP orbs together into a new single entity, which will conflict with the XP Orb feature of this mod!",
          Constants.MOD_CLUMPS_NAME);
      log.warn(
          "Don't use both optimizations together! Clustering of Experience Orbs will be automatically disabled!");
    }

    if (Constants.MOD_CREATE_LOADED) {
      log.warn(
          "WARNING: {} items will be automatically exclude from optimizations!",
          Constants.MOD_CREATE_NAME);
    }

    if (Constants.MOD_GET_IT_TOGETHER_DROPS_LOADED) {
      log.error(
          "ERROR: {} groups items together, which will conflict with the item merging feature of this mod!",
          Constants.MOD_GET_IT_TOGETHER_DROPS_NAME);
      log.warn(
          "Don't use both optimizations together! Item merging will be automatically disabled!");
    }
  }
}
