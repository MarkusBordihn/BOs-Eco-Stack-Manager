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

package de.markusbordihn.ecostackmanager;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.IEnvironment;
import de.markusbordihn.ecostackmanager.debug.DebugManager;
import de.markusbordihn.ecostackmanager.mods.AdditionalModsMessages;
import java.util.Optional;
import net.neoforged.fml.IExtensionPoint;
import net.neoforged.fml.IExtensionPoint.DisplayTest;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SuppressWarnings("unused")
@Mod(Constants.MOD_ID)
public class EcoStackManager {

  private static final Logger log = LogManager.getLogger(Constants.LOG_NAME);

  public EcoStackManager() {
    Constants.GAME_DIR = FMLPaths.GAMEDIR.get().toFile();
    log.info("Initializing {} (Forge) with {} ...", Constants.MOD_NAME, Constants.GAME_DIR);

    log.info("{} Debug Manager ...", Constants.LOG_REGISTER_PREFIX);
    Optional<String> version =
        Launcher.INSTANCE.environment().getProperty(IEnvironment.Keys.VERSION.get());
    if (version.isPresent() && "MOD_DEV".equals(version.get())) {
      DebugManager.setDevelopmentEnvironment(true);
    }
    DebugManager.checkForDebugLogging(Constants.LOG_NAME);

    log.info("Detecting additional mods ...");
    Constants.MOD_CLUMPS_LOADED = ModList.get().isLoaded(Constants.MOD_CLUMPS_ID);
    Constants.MOD_CREATE_LOADED = ModList.get().isLoaded(Constants.MOD_CREATE_ID);
    Constants.MOD_GET_IT_TOGETHER_DROPS_LOADED =
        ModList.get().isLoaded(Constants.MOD_GET_IT_TOGETHER_DROPS_ID);
    AdditionalModsMessages.checkForIncompatibility();

    // Make sure the mod being absent on the other network side does not cause the client to display
    // the server as incompatible
    ModLoadingContext.get()
        .registerExtensionPoint(
            IExtensionPoint.DisplayTest.class,
            () ->
                new IExtensionPoint.DisplayTest(
                    () -> DisplayTest.IGNORESERVERONLY, (a, b) -> true));
  }
}
