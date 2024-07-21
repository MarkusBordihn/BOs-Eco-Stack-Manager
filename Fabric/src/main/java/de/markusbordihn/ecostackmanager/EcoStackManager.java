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

import de.markusbordihn.ecostackmanager.entity.EntityWorldEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EcoStackManager implements ModInitializer {

  private static final Logger log = LogManager.getLogger(Constants.LOG_NAME);

  public EcoStackManager() {}

  @Override
  public void onInitialize() {
    FabricLoaderImpl fabricLoader = FabricLoaderImpl.INSTANCE;
    Constants.GAME_DIR = fabricLoader.getGameDir().toFile();
    log.info("Initializing {} (Fabric) with {} ...", Constants.MOD_NAME, Constants.GAME_DIR);

    log.info("{} Entity events ...", Constants.LOG_REGISTER_PREFIX);
    EntityWorldEvents.register();
  }
}
