package com.stardevllc.staritemgenerators.common.listener;

import com.stardevllc.staritemgenerators.common.model.*;
import com.stardevllc.staritemgenerators.common.model.ItemEntry.Flag;
import org.bukkit.entity.Item;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryPickupItemEvent;

public class GeneratorListener implements Listener {
    
    private GeneratorRegistry registry;
    
    public GeneratorListener(GeneratorRegistry registry) {
        this.registry = registry;
    }
    
    @EventHandler
    public void onItemMerge(ItemMergeEvent e) {
        for (ItemGenerator generator : registry.values()) {
            for (SpawnedItem item : generator.getSpawnedItems()) {
                if (e.getEntity().equals(item.item())) {
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }
    
    @EventHandler
    public void onInventoryItemPickup(InventoryPickupItemEvent e) {
        handleItemEvent(e.getItem(), Flag.INVENTORY_PICKUP, e);
    }
    
    @EventHandler
    public void onItemDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Item entity)) {
            return;
        }
        
        handleItemEvent(entity, Flag.INVULNERABLE, e);
    }
    
    private void handleItemEvent(Item itemEntity, Flag flag, Cancellable e) {
        for (ItemGenerator generator : registry.values()) {
            for (SpawnedItem spawnedItem : generator.getSpawnedItems()) {
                if (spawnedItem.item().equals(itemEntity)) {
                    if (spawnedItem.entry().hasFlag(flag)) {
                        e.setCancelled(true);
                    } else {
                        generator.removedSpawnedItem(itemEntity);
                    }
                    return;
                }
            }
        }
    }
    
    @EventHandler
    public void onItemDespawn(ItemDespawnEvent e) {
        handleItemEvent(e.getEntity(), Flag.PERSISTENT, e);
    }
}