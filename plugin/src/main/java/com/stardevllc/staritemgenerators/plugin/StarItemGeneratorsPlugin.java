package com.stardevllc.staritemgenerators.plugin;

import com.stardevllc.staritemgenerators.common.StarItemGenerators;
import com.stardevllc.staritemgenerators.common.command.ItemGeneratorCommand;
import com.stardevllc.staritemgenerators.common.listener.GeneratorListener;
import com.stardevllc.starmclib.plugin.ExtendedJavaPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPickupItemEvent;

public class StarItemGeneratorsPlugin extends ExtendedJavaPlugin implements Listener {
    
    public void onEnable() {
        super.onEnable();
        
        StarItemGenerators.init(this);
        
        registerCommand("itemgenerator", new ItemGeneratorCommand(this, StarItemGenerators.getGeneratorRegistry()));
        registerListeners(this, new GeneratorListener(StarItemGenerators.getGeneratorRegistry()));
    }
    
    @EventHandler 
    public void onItemPickup(PlayerPickupItemEvent e) {
        StarItemGenerators.handleItemPickup(e.getPlayer(), e.getItem(), e.getRemaining());
    }
}