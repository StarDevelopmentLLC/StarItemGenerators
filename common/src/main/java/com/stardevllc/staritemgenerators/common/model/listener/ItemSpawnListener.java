package com.stardevllc.staritemgenerators.common.model.listener;

import com.stardevllc.staritemgenerators.common.model.ItemEntry;
import com.stardevllc.staritemgenerators.common.model.ItemGenerator;
import org.bukkit.entity.Item;

@FunctionalInterface
public interface ItemSpawnListener extends ItemEntryListener {
    void onSpawn(Item item, ItemEntry entry, ItemGenerator generator);
}