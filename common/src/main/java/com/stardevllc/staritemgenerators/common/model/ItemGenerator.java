package com.stardevllc.staritemgenerators.common.model;

import com.stardevllc.starlib.clock.ClockManager;
import com.stardevllc.starlib.injector.Inject;
import com.stardevllc.starlib.observable.collections.list.ObservableArrayList;
import com.stardevllc.starlib.observable.collections.list.ObservableList;
import com.stardevllc.starlib.observable.property.readonly.ReadOnlyBooleanProperty;
import com.stardevllc.starlib.observable.property.readwrite.ReadWriteBooleanProperty;
import com.stardevllc.starmclib.Cuboid;
import com.stardevllc.starmclib.Position;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;

import java.util.*;

public class ItemGenerator {
    /**
     * A unique identifier for the generator itself <br>
     * It is best to auto-generate this id and have the entry ids be readable
     */
    protected String id;
    
    /**
     * The entries that control what items spawn in the generator
     */
    protected final ObservableList<ItemEntry> itemEntries;
    
    /**
     * The min and max positions for the generator. This is Bukkit World independent, useful for minigames
     */
    protected Position boundsMin, boundsMax;
    
    @Inject
    protected ClockManager clockManager;
    
    protected World world;
    
    protected final ReadWriteBooleanProperty initProperty, runningProperty;
    
    protected final Set<SpawnedItem> spawnedItems = new HashSet<>();
    
    protected Cuboid region;
    
    public ItemGenerator(String id, List<ItemEntry> itemEntries, Position boundsMin, Position boundsMax) {
        this.id = id;
        this.itemEntries = new ObservableArrayList<>(itemEntries);
        this.initProperty = new ReadWriteBooleanProperty(this, "init", false);
        this.initProperty.addListener(c -> {
            if (c.newValue()) {
                for (ItemEntry itemEntry : this.itemEntries) {
                    itemEntry.init(this, world);
                }
            } else {
                for (ItemEntry itemEntry : this.itemEntries) {
                    itemEntry.reset();
                }
            }
        });
        this.runningProperty = new ReadWriteBooleanProperty(this, "running", false);
        this.runningProperty.addListener(c -> {
            if (c.newValue()) {
                for (ItemEntry itemEntry : this.itemEntries) {
                    itemEntry.unpause();
                }
            } else {
                for (ItemEntry itemEntry : this.itemEntries) {
                    itemEntry.pause();
                }
            }
        });
        
        this.itemEntries.addListener(c -> {
            if (c.added() != null) {
                if (initProperty.get()) {
                    c.added().init(this, world);
                }
            } else if (c.removed() != null) {
                c.removed().reset();
            }
        });
        
        this.boundsMin = boundsMin;
        this.boundsMax = boundsMax;
    }
    
    public void init(World world) {
        this.world = world;
        this.initProperty.set(true);
        this.region = new Cuboid(new Location(world, this.boundsMin.getBlockX(), this.boundsMin.getBlockY(), this.boundsMin.getBlockZ()), new Location(world, this.boundsMax.getBlockX(), this.boundsMax.getBlockY(), this.boundsMax.getBlockZ()));
    }
    
    public void start() {
        this.runningProperty.set(true);
    }
    
    public void stop() {
        this.runningProperty.set(false);
    }
    
    public Cuboid getRegion() {
        return region;
    }
    
    public boolean contains(Location location) {
        if (location == null) {
            return false;
        }
        
        if (region == null) {
            return false;
        }
        
        return region.contains(location);
    }
    
    public boolean contains(Entity entity) {
        if (entity == null) {
            return false;
        }
        
        return contains(entity.getLocation());
    }
    
    public Set<SpawnedItem> getSpawnedItems() {
        return new HashSet<>(spawnedItems);
    }
    
    public void addSpawnedItem(String entryId, Item item) {
        ItemEntry entry = getItemEntry(entryId);
        if (entry == null) {
            return;
        }
        
        SpawnedItem spawnedItem = new SpawnedItem(item, this, entry);
        this.spawnedItems.add(spawnedItem);
    }
    
    public void removedSpawnedItem(Item item) {
        this.spawnedItems.removeIf(spawnedItem -> spawnedItem.item().equals(item));
    }
    
    public int getSpawnedItemsCount(String entryId) {
        int count = 0;
        for (SpawnedItem spawnedItem : this.spawnedItems) {
            if (spawnedItem.entry().getId().equalsIgnoreCase(entryId)) {
                count++;
            }
        }
        
        return count;
    }
    
    public int getSpawnedItemsCount() {
        return this.spawnedItems.size();
    }
    
    public String getId() {
        return id;
    }
    
    public void addItemEntry(ItemEntry itemEntry) {
        this.itemEntries.add(itemEntry);
    }
    
    public List<ItemEntry> getItemEntries() {
        return new ArrayList<>(itemEntries);
    }
    
    public Position getBoundsMin() {
        return boundsMin;
    }
    
    public Position getBoundsMax() {
        return boundsMax;
    }
    
    public ClockManager getClockManager() {
        return clockManager;
    }
    
    public ReadOnlyBooleanProperty initProperty() {
        return initProperty.asReadOnly();
    }
    
    public ReadOnlyBooleanProperty runningProperty() {
        return runningProperty.asReadOnly();
    }
    
    public World getWorld() {
        return world;
    }
    
    public ItemEntry getItemEntry(String entryId) {
        for (ItemEntry itemEntry : this.itemEntries) {
            if (itemEntry.getId().equalsIgnoreCase(entryId)) {
                return itemEntry;
            }
        }
        
        return null;
    }
    
    public boolean hasEntry(String entryId) {
        for (ItemEntry itemEntry : this.itemEntries) {
            if (itemEntry.getId().equalsIgnoreCase(entryId)) {
                return true;
            }
        }
        
        return false;
    }
}