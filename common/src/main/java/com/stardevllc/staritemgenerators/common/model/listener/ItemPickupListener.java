package com.stardevllc.staritemgenerators.common.model.listener;

import com.stardevllc.staritemgenerators.common.model.ItemEntry;
import com.stardevllc.staritemgenerators.common.model.ItemGenerator;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;

@FunctionalInterface
public interface ItemPickupListener extends ItemEntryListener {
    void onPickup(LivingEntity entity, Item item, ItemEntry entry, ItemGenerator generator);
}
