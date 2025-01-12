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
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;

public class EntityWorldEvents {

  private EntityWorldEvents() {}

  public static void register() {
    ServerEntityEvents.ENTITY_LOAD.register(EntityWorldEvents::handleEntityJoinWorldEvent);
    ServerEntityEvents.ENTITY_UNLOAD.register(EntityWorldEvents::handleEntityLeaveWorldEvent);
  }

  public static void handleEntityJoinWorldEvent(
      final Entity entity, final ServerLevel serverLevel) {
    if (entity instanceof ExperienceOrb experienceOrb && !Constants.MOD_CLUMPS_LOADED) {
      ExperienceOrbManager.handleExperienceOrbJoinWorldEvent(experienceOrb, serverLevel);
    } else if (entity instanceof ItemEntity itemEntity
        && !Constants.MOD_GET_IT_TOGETHER_DROPS_LOADED) {
      ItemEntityManager.handleItemJoinWorldEvent(itemEntity, serverLevel);
    }
  }

  public static void handleEntityLeaveWorldEvent(
      final Entity entity, final ServerLevel serverLevel) {
    if (entity instanceof ExperienceOrb experienceOrb && !Constants.MOD_CLUMPS_LOADED) {
      ExperienceOrbManager.handleExperienceOrbLeaveWorldEvent(experienceOrb, serverLevel);
    } else if (entity instanceof ItemEntity itemEntity
        && !Constants.MOD_GET_IT_TOGETHER_DROPS_LOADED) {
      ItemEntityManager.handleItemLeaveWorldEvent(itemEntity, serverLevel);
    }
  }
}
