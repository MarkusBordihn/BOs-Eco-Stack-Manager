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
import de.markusbordihn.ecostackmanager.config.ItemEntityConfig;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ItemEntityManager {

  private static final Logger log = LogManager.getLogger(Constants.LOG_NAME);

  private static final Map<String, Set<ItemEntity>> itemTypeEntityMap = new ConcurrentHashMap<>();
  private static final Map<String, Set<ItemEntity>> itemWorldEntityMap = new ConcurrentHashMap<>();

  private static int itemEntityVerificationCounter = 0;

  private ItemEntityManager() {}

  public static boolean handleItemJoinWorldEvent(ItemEntity itemEntity, ServerLevel serverLevel) {

    // Check if we got a relevant and valid item entity.
    String itemName = getNameFromRelevantItemEntity(itemEntity);
    if (itemName == null || itemName.isEmpty()) {
      return false;
    }

    // Check if item is allowed to be optimized.
    if (!ItemEntityConfig.allowList.isEmpty() && !ItemEntityConfig.allowList.contains(itemName)) {
      log.debug(
          "[Item Allow List] {} is not on the allow list: {}",
          itemName,
          ItemEntityConfig.allowList);
      return false;
    }

    // Check if item is denied to be optimized.
    if (!ItemEntityConfig.denyList.isEmpty() && ItemEntityConfig.denyList.contains(itemName)) {
      log.debug(
          "[Item Deny List] {} will not be optimized: {}", itemName, ItemEntityConfig.denyList);
      return false;
    }

    // Get world name and start processing of data
    String levelName = serverLevel.dimension().location().toString();
    log.debug("[Item Entity joined {}] {} {}", levelName, itemName, itemEntity);

    // Check if items could be merged with other items
    String itemTypeEntityMapKey = '[' + levelName + ']' + itemName;
    itemTypeEntityMap.computeIfAbsent(itemTypeEntityMapKey, k -> new LinkedHashSet<>());
    Set<ItemEntity> itemTypeEntities = itemTypeEntityMap.get(itemTypeEntityMapKey);

    ItemStack itemStack = itemEntity.getItem();
    if (itemStack.isStackable()
        && itemStack.getCount() < itemStack.getMaxStackSize()
        && itemStack.getMaxStackSize() > 1) {
      // Get basic information about the current item.
      double x = itemEntity.getX();
      double y = itemEntity.getY();
      double z = itemEntity.getZ();
      int xStart = (int) x - ItemEntityConfig.collectRadius;
      int yStart = (int) y - ItemEntityConfig.collectRadius;
      int zStart = (int) z - ItemEntityConfig.collectRadius;
      int xEnd = (int) x + ItemEntityConfig.collectRadius;
      int yEnd = (int) y + ItemEntityConfig.collectRadius;
      int zEnd = (int) z + ItemEntityConfig.collectRadius;
      boolean itemCanSeeSky = serverLevel.canSeeSky(itemEntity.blockPosition());

      // Compare information with known items.
      Set<ItemEntity> itemEntities = new HashSet<>(itemTypeEntities);
      for (ItemEntity existingItemEntity : itemEntities) {
        ItemStack existingItemStack = existingItemEntity.getItem();

        // Check if they are in an equal position, if both could see the sky, ignore the y values.
        if (shouldMerge(
            itemEntity,
            itemStack,
            itemCanSeeSky,
            existingItemEntity,
            existingItemStack,
            xStart,
            yStart,
            zStart,
            xEnd,
            yEnd,
            zEnd,
            serverLevel)) {
          mergeItemStacks(itemEntity, itemStack, existingItemEntity, existingItemStack, x, y, z);
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
    if (ItemEntityConfig.maxNumberOfItemsPerWorld > 0
        && numberOfItemWorldEntities > ItemEntityConfig.maxNumberOfItemsPerWorld) {
      ItemEntity firsItemWorldEntity = itemWorldEntities.iterator().next();
      log.debug(
          "[Item Entity World Limit {}] Removing first item {}",
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
    int numberOfItemTypeEntities = itemTypeEntities.size();
    if (ItemEntityConfig.maxNumberOfItemsPerType > 0
        && numberOfItemTypeEntities > ItemEntityConfig.maxNumberOfItemsPerType) {
      ItemEntity firstItemEntity = itemTypeEntities.iterator().next();
      log.debug(
          "[Item Entity Type Limit {}] Removing first item {}",
          numberOfItemTypeEntities,
          firstItemEntity);
      firstItemEntity.discard();
      itemTypeEntities.remove(firstItemEntity);
      itemWorldEntities.remove(firstItemEntity);
    }

    // Verify item entities after a specific number of tracked items.
    if (itemEntityVerificationCounter++ >= ItemEntityConfig.verificationCycle) {
      verifyItemEntities();
      itemEntityVerificationCounter = 0;
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
      if (log.isDebugEnabled()) {
        log.debug("[Item Entity leaved {}] {} {}.", levelName, itemName, itemEntity);
      }
      itemTypeEntities.remove(itemEntity);
    } else {
      log.warn("[Item Entity leaved {}] {} {} was not tracked!", levelName, itemName, itemEntity);
    }
  }

  private static String getNameFromRelevantItemEntity(final ItemEntity itemEntity) {
    if (itemEntity == null || itemEntity.isRemoved() || itemEntity.hasCustomName()) {
      return null;
    }

    // All items have the entity minecraft.item, so we are using the translation key
    // to better distinguish the different types of items and minecraft.item as backup.
    String itemName = Registry.ITEM.getKey(itemEntity.getItem().getItem()).toString();
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
    if (Constants.MOD_CREATE_LOADED && itemName.startsWith("create")) {
      return null;
    }

    return itemName;
  }

  private static void verifyItemEntities() {
    log.debug("[Verification] Start verification of tracked item entities ...");

    // Verify Entities in overall overview
    int removedItemsType = getRemovedItemEntities(itemTypeEntityMap);

    // Verify Entities from world specific overview
    int removedItemsWorld = getRemovedItemEntities(itemWorldEntityMap);

    if (removedItemsType > 0 || removedItemsWorld > 0) {
      log.debug(
          "[Verification] Removed {} items ({} items per type / {} items per world)",
          removedItemsType + removedItemsWorld,
          removedItemsType,
          removedItemsWorld);
    }
  }

  private static int getRemovedItemEntities(Map<String, Set<ItemEntity>> itemTypeEntityMap) {
    int removedItemsType = 0;
    for (Set<ItemEntity> entities : itemTypeEntityMap.values()) {
      Iterator<ItemEntity> entityIterator = entities.iterator();
      while (entityIterator.hasNext()) {
        Entity entity = entityIterator.next();
        if (entity != null && entity.isRemoved()) {
          entityIterator.remove();
          removedItemsType++;
        }
      }
    }
    return removedItemsType;
  }

  private static boolean shouldMerge(
      final ItemEntity itemEntity,
      final ItemStack itemStack,
      final boolean itemCanSeeSky,
      final ItemEntity existingItemEntity,
      final ItemStack existingItemStack,
      final int xStart,
      final int yStart,
      final int zStart,
      final int xEnd,
      final int yEnd,
      final int zEnd,
      final ServerLevel serverLevel) {
    boolean existingItemCanSeeSky = serverLevel.canSeeSky(existingItemEntity.blockPosition());
    int x = (int) existingItemEntity.getX();
    int y = (int) existingItemEntity.getY();
    int z = (int) existingItemEntity.getZ();
    return itemEntity.getId() != existingItemEntity.getId()
        && existingItemEntity.isAlive()
        && ItemEntity.areMergable(itemStack, existingItemStack)
        && (xStart < x && x < xEnd)
        && ((itemCanSeeSky && existingItemCanSeeSky) || (yStart < y && y < yEnd))
        && (zStart < z && z < zEnd);
  }

  private static void mergeItemStacks(
      ItemEntity itemEntity,
      ItemStack itemStack,
      ItemEntity existingItemEntity,
      ItemStack existingItemStack,
      final double x,
      final double y,
      final double z) {
    // Combine item stacks and update the existing item entity.
    ItemStack combinedItemStack =
        ItemEntity.merge(existingItemStack, itemStack, ItemEntityConfig.maxStackSize);
    log.debug(
        "[Merging Item Entity] {} with {} and {} items",
        itemEntity,
        existingItemEntity,
        combinedItemStack);

    // Set the combined item stack to the existing item entity.
    existingItemEntity.setItem(combinedItemStack);

    // Remove item entity before moving the existing item entity to the new position.
    if (!itemEntity.isRemoved()) {
      itemEntity.discard();
    }

    // Update position of the item entity to the new position, but adjust the z position.
    if (ItemEntityConfig.movePositionToLastDrop) {
      existingItemEntity.setPos(
          x, existingItemEntity.getY() + ((y - existingItemEntity.getY()) / 4), z);
    }
  }
}
