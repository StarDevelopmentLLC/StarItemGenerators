package com.stardevllc.staritemgenerators.common.model;

import com.stardevllc.itembuilder.common.ItemBuilder;
import com.stardevllc.staritemgenerators.common.model.listener.ItemPickupListener;
import com.stardevllc.staritemgenerators.common.model.listener.ItemSpawnListener;
import com.stardevllc.starlib.clock.clocks.Timer;
import com.stardevllc.starmclib.Position;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.*;

public class ItemEntry {
    
    public enum Flag {
        /**
         * Controls if the item can despawn
         */
        PERSISTENT,
        
        /**
         * Controls if the item can be destroyed
         */
        INVULNERABLE,
        
        /**
         * Controls if the item can be picked up by inventories
         */
        INVENTORY_PICKUP
    }
    
    /**
     * A unique identifier for the item entry. This is per generator
     */
    protected String id;
    
    /**
     * The builder used to create items
     */
    protected ItemBuilder<?, ?> builder;
    
    /**
     * The cooldown in milliseconds before the next item spawns
     */
    protected long cooldown;
    
    /**
     * The max amount of items that can be within the generator bounds
     */
    protected int maxItems;
    
    /**
     * The base spawn position for the items
     */
    protected Position spawnPosition;
    
    /**
     * The timer instance for controlling when items spawn
     */
    protected Timer timer;
    
    /**
     * This is the generator instance
     */
    protected ItemGenerator generator;
    
    /**
     * This is the world that the items are to be spawned in
     */
    protected World world;
    
    /**
     * The boolean based flags for the entry
     */
    protected final Set<Flag> flags = EnumSet.noneOf(Flag.class);
    
    private final List<ItemPickupListener> itemPickupListeners = new ArrayList<>();
    private final List<ItemSpawnListener> itemSpawnListeners = new ArrayList<>();
    
    public ItemEntry(String id, ItemBuilder<?, ?> builder, long cooldown, int maxItems, Position spawnPosition, Flag... flags) {
        this.id = id;
        this.builder = builder;
        this.cooldown = cooldown;
        this.maxItems = maxItems;
        this.spawnPosition = spawnPosition;
        if (flags != null) {
            this.flags.addAll(List.of(flags));
        }
    }
    
    public ItemEntry(String id, ItemBuilder<?, ?> builder, long cooldown, int maxItems, Position spawnPosition, List<Flag> flags) {
        this.id = id;
        this.builder = builder;
        this.cooldown = cooldown;
        this.maxItems = maxItems;
        this.spawnPosition = spawnPosition;
        if (flags != null) {
            this.flags.addAll(flags);
        }
    }
    
    public void init(ItemGenerator generator, World world) {
        this.generator = generator;
        this.world = world;
        
        if (this.timer != null) {
            this.timer.cancel();
        }
        
        this.timer = generator.getClockManager().createTimer(cooldown);
        this.timer.addRepeatingCallback(snapshot -> {
            if (this.world != null && this.generator != null) {
                if (generator.runningProperty.get()) {
                    int currentCount = generator.getSpawnedItemsCount(getId());
                    if (currentCount < this.maxItems) {
                        generator.addSpawnedItem(getId(), spawnItem(world));
                    }
                }
            }
            
            this.timer.setLengthAndReset(cooldown);
        }, cooldown);
        
        this.timer.start();
    }
    
    public void addSpawnListener(ItemSpawnListener listener) {
        this.itemSpawnListeners.add(listener);
    }
    
    public void handleItemSpawn(Item item, ItemEntry itemEntry, ItemGenerator generator) {
        for (ItemSpawnListener listener : this.itemSpawnListeners) {
            listener.onSpawn(item, itemEntry, generator);
        }
    }
    
    public void addPickupListener(ItemPickupListener listener) {
        this.itemPickupListeners.add(listener);
    }
    
    public void handleItemPickup(LivingEntity entity, Item item, ItemEntry itemEntry) {
        for (ItemPickupListener listener : this.itemPickupListeners) {
            listener.onPickup(entity, item, itemEntry, generator);
        }
    }
    
    public void start() {
        if (this.timer != null) {
            this.timer.start();
        }
    }
    
    public void stop() {
        if (this.timer != null) {
            this.timer.setLengthAndReset(this.cooldown);
            this.timer.pause();
        }
    }
    
    public void pause() {
        if (this.timer != null) {
            this.timer.pause();
        }
    }
    
    public void unpause() {
        if (this.timer != null) {
            this.timer.unpause();
        }
    }
    
    public void reset() {
        this.generator = null;
        this.world = null;
        if (this.timer != null) {
            this.timer.cancel();
        }
        this.timer = null;
    }
    
    public Timer getTimer() {
        return timer;
    }
    
    public Item spawnItem(World world) {
        Location location = spawnPosition.toBlockLocation(world).add(0.5, 0, 0.5);
        Item item = world.dropItem(location, createItemStack());
        item.setVelocity(new Vector());
        handleItemSpawn(item, this, this.generator);
        return item;
    }
    
    public String getId() {
        return id;
    }
    
    public ItemBuilder<?, ?> getBuilder() {
        return builder;
    }
    
    public ItemStack createItemStack() {
        return builder.build();
    }
    
    public long getCooldown() {
        return cooldown;
    }
    
    public int getMaxItems() {
        return maxItems;
    }
    
    public void setCooldown(long cooldown) {
        this.cooldown = cooldown;
        if (this.timer != null) {
            this.timer.setLengthAndReset(cooldown);
        }
    }
    
    public void setMaxItems(int maxItems) {
        this.maxItems = maxItems;
    }
    
    public Position getSpawnPosition() {
        return spawnPosition;
    }
    
    public World getWorld() {
        return world;
    }
    
    public Set<Flag> getFlags() {
        return EnumSet.copyOf(this.flags);
    }
    
    public boolean hasFlag(Flag flag) {
        return this.flags.contains(flag);
    }
}