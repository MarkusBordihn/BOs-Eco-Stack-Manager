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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.EntityLeaveWorldEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@SuppressWarnings("unused")
@EventBusSubscriber
public class EntityWorldEvents {

  private EntityWorldEvents() {}

  @SubscribeEvent(priority = EventPriority.HIGH)
  public static void handleEntityJoinWorldEvent(final EntityJoinWorldEvent event) {
    if (event.isCanceled() || !(event.getWorld() instanceof ServerLevel serverLevel)) {
      return;
    }

    if (event.getEntity() instanceof ExperienceOrb experienceOrb && !Constants.MOD_CLUMPS_LOADED) {
      if (ExperienceOrbManager.handleExperienceOrbJoinWorldEvent(experienceOrb, serverLevel)
          && event.isCancelable()) {
        event.setCanceled(true);
      }
    } else if (event.getEntity() instanceof ItemEntity itemEntity && !Constants.MOD_CLUMPS_LOADED) {
      if (ItemEntityManager.handleItemJoinWorldEvent(itemEntity, serverLevel)
          && event.isCancelable()) {
        event.setCanceled(true);
      }
    }
  }

  @SubscribeEvent(priority = EventPriority.HIGH)
  public static void handleEntityLeaveWorldEvent(final EntityLeaveWorldEvent event) {
    if (event.isCanceled() || !(event.getWorld() instanceof ServerLevel serverLevel)) {
      return;
    }

    if (event.getEntity() instanceof ExperienceOrb experienceOrb && !Constants.MOD_CLUMPS_LOADED) {
      ExperienceOrbManager.handleExperienceOrbLeaveWorldEvent(experienceOrb, serverLevel);
    } else if (event.getEntity() instanceof ItemEntity itemEntity && !Constants.MOD_CLUMPS_LOADED) {
      ItemEntityManager.handleItemLeaveWorldEvent(itemEntity, serverLevel);
    }
  }
}
