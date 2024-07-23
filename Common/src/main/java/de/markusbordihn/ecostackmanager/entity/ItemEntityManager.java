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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ItemEntityManager {

  private static final Logger log = LogManager.getLogger(Constants.LOG_NAME);

  private static final Map<String, Set<ItemEntity>> itemTypeEntityMap = new ConcurrentHashMap<>();
  private static final Map<String, Set<ItemEntity>> itemWorldEntityMap = new ConcurrentHashMap<>();

  private ItemEntityManager() {}

  public static boolean handleItemJoinWorldEvent(ItemEntity itemEntity, ServerLevel serverLevel) {

    // Check if we got a relevant and valid item entity.
    String itemName = getNameFromRelevantItemEntity(itemEntity);
    if (itemName == null || itemName.isEmpty()) {
      return false;
    }

    // Get world name and start processing of data
    String levelName = serverLevel.dimension().location().toString();
    log.debug("[Item joined {}] {} {}", levelName, itemName, itemEntity);

    // Check if items could be merged with other items
    String itemTypeEntityMapKey = '[' + levelName + ']' + itemName;
    itemTypeEntityMap.computeIfAbsent(itemTypeEntityMapKey, k -> new LinkedHashSet<>());
    Set<ItemEntity> itemTypeEntities = itemTypeEntityMap.get(itemTypeEntityMapKey);

    ItemStack itemStack = itemEntity.getItem();
    if (itemStack.isStackable()
        && itemStack.getCount() < itemStack.getMaxStackSize()
        && itemStack.getMaxStackSize() > 1) {
      // Get basic information about the current item.
      int x = (int) itemEntity.getX();
      int y = (int) itemEntity.getY();
      int z = (int) itemEntity.getZ();
      int xStart = x - EcoStackManagerConfig.ITEM_ENTITY_COLLECT_RADIUS;
      int yStart = y - EcoStackManagerConfig.ITEM_ENTITY_COLLECT_RADIUS;
      int zStart = z - EcoStackManagerConfig.ITEM_ENTITY_COLLECT_RADIUS;
      int xEnd = x + EcoStackManagerConfig.ITEM_ENTITY_COLLECT_RADIUS;
      int yEnd = y + EcoStackManagerConfig.ITEM_ENTITY_COLLECT_RADIUS;
      int zEnd = z + EcoStackManagerConfig.ITEM_ENTITY_COLLECT_RADIUS;
      boolean itemCanSeeSky = serverLevel.canSeeSky(itemEntity.blockPosition());

      // Compare information with known items.
      Set<ItemEntity> itemEntities = new HashSet<>(itemTypeEntities);
      for (ItemEntity existingItemEntity : itemEntities) {
        int xSub = (int) existingItemEntity.getX();
        int ySub = (int) existingItemEntity.getY();
        int zSub = (int) existingItemEntity.getZ();
        boolean existingItemCanSeeSky = serverLevel.canSeeSky(existingItemEntity.blockPosition());
        ItemStack existingItemStack = existingItemEntity.getItem();

        // Check if they are in an equal position, if both could see the sky, ignore the y values.
        if (itemEntity.getId() != existingItemEntity.getId()
            && existingItemEntity.isAlive()
            && ItemEntity.areMergable(itemStack, existingItemStack)
            && (xStart < xSub && xSub < xEnd)
            && ((itemCanSeeSky && existingItemCanSeeSky) || (yStart < ySub && ySub < yEnd))
            && (zStart < zSub && zSub < zEnd)) {
          if (log.isDebugEnabled()) {
            int newItemCount = existingItemStack.getCount() + itemStack.getCount();
            log.debug(
                "[Merge Item] {} + {} = {} items", itemEntity, existingItemEntity, newItemCount);
          }
          ItemStack combinedItemStack = ItemEntity.merge(existingItemStack, itemStack, 64);
          existingItemEntity.setItem(combinedItemStack);
          return true;
        }
      }
    }

    // Storing items per world regardless of item type
    itemWorldEntityMap.computeIfAbsent(levelName, k -> new LinkedHashSet<>());
    Set<ItemEntity> itemWorldEntities = itemWorldEntityMap.get(levelName);
    itemWorldEntities.add(itemEntity);

    // Optimized items per world regardless of type if they're exceeding maxNumberOfItems limit.
    int numberOfItemWorldEntities = itemWorldEntities.size();
    if (numberOfItemWorldEntities > EcoStackManagerConfig.ITEM_ENTITY_MAX_NUMBER_OF_ITEMS) {
      ItemEntity firsItemWorldEntity = itemWorldEntities.iterator().next();
      log.debug(
          "[Item World Limit {}] Removing first item {}",
          numberOfItemWorldEntities,
          firsItemWorldEntity);
      firsItemWorldEntity.discard();
      itemWorldEntities.remove(firsItemWorldEntity);
      Set<ItemEntity> itemEntities = itemTypeEntityMap.get('[' + levelName + ']' + itemName);
      if (itemEntities != null) {
        itemEntities.remove(firsItemWorldEntity);
      }
    }

    // Storing items per type and world
    itemTypeEntities.add(itemEntity);

    // Optimized items per type and world if exceeding numberOfItemsPerType limit.
    int numberOfItemEntities = itemTypeEntities.size();
    if (numberOfItemEntities > EcoStackManagerConfig.ITEM_ENTITY_MAX_NUMBER_OF_ITEMS_PER_TYPE) {
      ItemEntity firstItemEntity = itemTypeEntities.iterator().next();
      log.debug(
          "[Item Type Limit {}] Removing first item {}", numberOfItemEntities, firstItemEntity);
      firstItemEntity.discard();
      itemTypeEntities.remove(firstItemEntity);
      itemWorldEntities.remove(firstItemEntity);
    }

    return false;
  }

  public static void handleItemLeaveWorldEvent(ItemEntity itemEntity, ServerLevel serverLevel) {
    // Check if we got a relevant and valid item entity.
    String itemName = getNameFromRelevantItemEntity(itemEntity);
    if (itemName == null || itemName.isEmpty()) {
      return;
    }

    // Get world name and start processing of data
    String levelName = serverLevel.dimension().location().toString();

    // Remove item from world map.
    Set<ItemEntity> itemWorldEntities = itemWorldEntityMap.get(levelName);
    if (itemWorldEntities != null) {
      itemWorldEntities.remove(itemEntity);
    }

    // Remove item from world type map.
    Set<ItemEntity> itemTypeEntities = itemTypeEntityMap.get('[' + levelName + ']' + itemName);
    if (itemTypeEntities != null) {
      itemTypeEntities.remove(itemEntity);
      if (log.isDebugEnabled()) {
        log.debug(
            "[Item leaved {}] {} {}.",
            levelName,
            itemName,
            itemEntity.getDisplayName().getString());
      }
    } else {
      log.warn(
          "Item {} {} in {} was not tracked by item entity manager!",
          itemName,
          itemEntity.getDisplayName().getString(),
          levelName);
    }
  }

  public static String getNameFromRelevantItemEntity(ItemEntity itemEntity) {
    if (itemEntity == null || itemEntity.isRemoved() || itemEntity.hasCustomName()) {
      return null;
    }

    // All items have the entity minecraft.item, so we are using the translation key
    // to better distinguish the different types of items and minecraft.item as backup.
    String itemName = BuiltInRegistries.ITEM.getKey(itemEntity.getItem().getItem()).toString();
    if (itemName.isEmpty()) {
      return null;
    }

    // Ignore dropped air blocks because these are not used at all by the players.
    // Warning: Removing the air block is a bad idea, because it's used to pre-reserve the space.
    if (itemName.equals("block.minecraft.air") || itemName.equals("minecraft:air")) {
      return null;
    }

    // Ignore specific entities from mods which implements their own spawn handling, logic or
    // using pseudo mobs for interactive blocks.
    if (itemName.startsWith("create")) {
      return null;
    }

    return itemName;
  }
}
