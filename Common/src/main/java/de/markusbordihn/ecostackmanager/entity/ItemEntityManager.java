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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
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

    // Check if item is allowed to be processed
    if (!isItemAllowedForOptimization(itemName)) {
      return false;
    }

    // Get world name and start processing of data
    String levelName = serverLevel.dimension().identifier().toString();
    log.debug("[Item Entity joined {}] {} {}", levelName, itemName, itemEntity);

    // Try to merge with existing items
    if (tryMergeWithExistingItems(itemEntity, serverLevel, levelName, itemName)) {
      return true;
    }

    // Add item to tracking and enforce limits
    addItemToTrackingAndEnforceLimits(itemEntity, levelName, itemName);

    // Verify item entities after a specific number of tracked items.
    if (itemEntityVerificationCounter++ >= ItemEntityConfig.verificationCycle) {
      verifyItemEntities();
      itemEntityVerificationCounter = 0;
    }

    return false;
  }

  private static boolean isItemAllowedForOptimization(String itemName) {
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

    return true;
  }

  private static boolean tryMergeWithExistingItems(
      ItemEntity itemEntity, ServerLevel serverLevel, String levelName, String itemName) {
    String itemTypeEntityMapKey = '[' + levelName + ']' + itemName;
    itemTypeEntityMap.computeIfAbsent(itemTypeEntityMapKey, k -> new LinkedHashSet<>());
    Set<ItemEntity> itemTypeEntities = itemTypeEntityMap.get(itemTypeEntityMapKey);

    ItemStack itemStack = itemEntity.getItem();
    if (!isItemStackMergeable(itemStack)) {
      return false;
    }

    return findAndMergeWithSuitableItem(itemEntity, itemStack, itemTypeEntities, serverLevel);
  }

  private static boolean isItemStackMergeable(ItemStack itemStack) {
    return itemStack.isStackable()
        && itemStack.getCount() < itemStack.getMaxStackSize()
        && itemStack.getMaxStackSize() > 1;
  }

  private static boolean findAndMergeWithSuitableItem(
      ItemEntity itemEntity,
      ItemStack itemStack,
      Set<ItemEntity> itemTypeEntities,
      ServerLevel serverLevel) {
    // Get basic information about the current item.
    double x = itemEntity.getX();
    double y = itemEntity.getY();
    double z = itemEntity.getZ();
    MergeAreaBounds bounds = new MergeAreaBounds(x, y, z, ItemEntityConfig.collectRadius);
    boolean itemCanSeeSky = serverLevel.canSeeSky(itemEntity.blockPosition());

    // Create defensive copy to prevent concurrent modification issues
    ItemEntity[] entityArray;
    try {
      entityArray = itemTypeEntities.toArray(new ItemEntity[0]);
    } catch (Exception e) {
      log.debug("Failed to create entity array for merge operation: {}", e.getMessage());
      return false;
    }

    for (ItemEntity existingItemEntity : entityArray) {
      if (existingItemEntity == null || existingItemEntity.isRemoved()) {
        continue;
      }

      ItemStack existingItemStack = existingItemEntity.getItem();

      if (shouldMergeItems(
          itemEntity,
          itemStack,
          itemCanSeeSky,
          existingItemEntity,
          existingItemStack,
          bounds,
          serverLevel)) {
        mergeItemStacks(itemEntity, itemStack, existingItemEntity, existingItemStack, x, y, z);
        return true;
      }
    }
    return false;
  }

  private static void addItemToTrackingAndEnforceLimits(
      ItemEntity itemEntity, String levelName, String itemName) {
    itemWorldEntityMap.computeIfAbsent(levelName, k -> new LinkedHashSet<>());
    Set<ItemEntity> itemWorldEntities = itemWorldEntityMap.get(levelName);
    itemWorldEntities.add(itemEntity);

    enforceWorldItemLimit(itemWorldEntities, levelName, itemName);

    String itemTypeEntityMapKey = '[' + levelName + ']' + itemName;
    Set<ItemEntity> itemTypeEntities = itemTypeEntityMap.get(itemTypeEntityMapKey);
    if (itemTypeEntities != null) {
      itemTypeEntities.add(itemEntity);
      enforceTypeItemLimit(itemTypeEntities, itemWorldEntities);
    }
  }

  private static void enforceWorldItemLimit(
      Set<ItemEntity> itemWorldEntities, String levelName, String itemName) {
    int numberOfItemWorldEntities = itemWorldEntities.size();
    if (ItemEntityConfig.maxNumberOfItemsPerWorld > 0
        && numberOfItemWorldEntities > ItemEntityConfig.maxNumberOfItemsPerWorld) {
      ItemEntity firstItemWorldEntity = itemWorldEntities.iterator().next();
      log.debug(
          "[Item Entity World Limit {}] Removing first item {}",
          numberOfItemWorldEntities,
          firstItemWorldEntity);
      firstItemWorldEntity.discard();
      itemWorldEntities.remove(firstItemWorldEntity);
      Set<ItemEntity> itemEntities = itemTypeEntityMap.get('[' + levelName + ']' + itemName);
      if (itemEntities != null) {
        itemEntities.remove(firstItemWorldEntity);
      }
    }
  }

  private static void enforceTypeItemLimit(
      Set<ItemEntity> itemTypeEntities, Set<ItemEntity> itemWorldEntities) {
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
  }

  public static void handleItemLeaveWorldEvent(ItemEntity itemEntity, ServerLevel serverLevel) {
    // Check if we got a relevant and valid item entity.
    String itemName = getNameFromRelevantItemEntity(itemEntity);
    if (itemName == null || itemName.isEmpty()) {
      return;
    }

    // Get world name and start processing of data
    String levelName = serverLevel.dimension().identifier().toString();

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

    // Clean up empty sets to prevent memory leaks
    cleanupEmptySets();

    if (removedItemsType > 0 || removedItemsWorld > 0) {
      log.debug(
          "[Verification] Removed {} items ({} items per type / {} items per world)",
          removedItemsType + removedItemsWorld,
          removedItemsType,
          removedItemsWorld);
    }
  }

  private static void cleanupEmptySets() {
    // Remove empty sets from itemTypeEntityMap to prevent memory leaks
    itemTypeEntityMap.entrySet().removeIf(entry -> entry.getValue().isEmpty());

    // Remove empty sets from itemWorldEntityMap to prevent memory leaks
    itemWorldEntityMap.entrySet().removeIf(entry -> entry.getValue().isEmpty());
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

  private static boolean shouldMergeItems(
      final ItemEntity itemEntity,
      final ItemStack itemStack,
      final boolean itemCanSeeSky,
      final ItemEntity existingItemEntity,
      final ItemStack existingItemStack,
      final MergeAreaBounds bounds,
      final ServerLevel serverLevel) {
    // Add defensive null checks and synchronization to prevent race conditions
    if (existingItemEntity == null || existingItemStack == null || existingItemEntity.isRemoved()) {
      return false;
    }

    boolean existingItemCanSeeSky = serverLevel.canSeeSky(existingItemEntity.blockPosition());
    int x = (int) existingItemEntity.getX();
    int y = (int) existingItemEntity.getY();
    int z = (int) existingItemEntity.getZ();

    // Check if entity is still alive before proceeding with merge logic
    return itemEntity.getId() != existingItemEntity.getId()
        && existingItemEntity.isAlive()
        && !existingItemEntity.isRemoved()
        && ItemEntity.areMergable(itemStack, existingItemStack)
        && (bounds.xStart < x && x < bounds.xEnd)
        && ((itemCanSeeSky && existingItemCanSeeSky) || (bounds.yStart < y && y < bounds.yEnd))
        && (bounds.zStart < z && z < bounds.zEnd);
  }

  private static void mergeItemStacks(
      ItemEntity itemEntity,
      ItemStack itemStack,
      ItemEntity existingItemEntity,
      ItemStack existingItemStack,
      final double x,
      final double y,
      final double z) {
    // Additional safety check before merging
    if (existingItemEntity == null || existingItemEntity.isRemoved() || itemEntity.isRemoved()) {
      log.warn("Attempted to merge removed or null item entities");
      return;
    }

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

  // Helper class to encapsulate merge area bounds
  private static class MergeAreaBounds {
    final int xStart, yStart, zStart, xEnd, yEnd, zEnd;

    MergeAreaBounds(double x, double y, double z, int radius) {
      this.xStart = (int) x - radius;
      this.yStart = (int) y - radius;
      this.zStart = (int) z - radius;
      this.xEnd = (int) x + radius;
      this.yEnd = (int) y + radius;
      this.zEnd = (int) z + radius;
    }
  }
}
