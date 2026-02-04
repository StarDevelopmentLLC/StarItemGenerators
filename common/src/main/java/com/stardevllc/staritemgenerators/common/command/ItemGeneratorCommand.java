package com.stardevllc.staritemgenerators.common.command;

import com.stardevllc.itembuilder.ItemBuilders;
import com.stardevllc.smaterial.SMaterial;
import com.stardevllc.staritemgenerators.common.model.*;
import com.stardevllc.staritemgenerators.common.model.ItemEntry.Flag;
import com.stardevllc.starlib.objects.registry.RegistryObject;
import com.stardevllc.starlib.time.TimeFormat;
import com.stardevllc.starlib.time.TimeParser;
import com.stardevllc.starmclib.Position;
import com.stardevllc.starmclib.StarColorsV2;
import com.stardevllc.starmclib.command.flags.CmdFlags;
import com.stardevllc.starmclib.command.flags.FlagResult;
import com.stardevllc.starmclib.command.flags.type.ComplexFlag;
import com.stardevllc.starmclib.command.flags.type.PresenceFlag;
import com.stardevllc.starmclib.plugin.ExtendedJavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;

public class ItemGeneratorCommand implements CommandExecutor, Listener {
    
    private ExtendedJavaPlugin plugin;
    
    private GeneratorRegistry registry;
    
    /**
     * The generator selections based on each player <br>
     * It is stored as the generator id to make sure that memory leak chances are minimal
     */
    private Map<UUID, String> selectedGenerators = new HashMap<>();
    private Map<UUID, Selection> positionSelection = new HashMap<>();
    
    private final CmdFlags cmdFlags;
    
    private static final TimeParser timeParser = new TimeParser();
    private static final TimeFormat timeFormat = new TimeFormat("%*##h%%*##m%%*##s%%*##ms%");
    
    private static final PresenceFlag DEBUG_FLAG = new PresenceFlag("d", "Debug");
    
    private static final PresenceFlag SELECT_FLAG = new PresenceFlag("se", "Select");
    private static final PresenceFlag INIT_FLAG = new PresenceFlag("i", "Init");
    private static final PresenceFlag START_FLAG = new PresenceFlag("st", "start");
    
    //Item specific flags
    private static final PresenceFlag INVULNERABLE_FLAG = new PresenceFlag("in", "Invulnerable");
    private static final PresenceFlag INVENTORY_PICKUP_FLAG = new PresenceFlag("ip", "Inventory Pickup");
    private static final PresenceFlag PERSISTENT_FLAG = new PresenceFlag("pe", "Persistent");
    private static final ComplexFlag MAX_ITEMS_FLAG = new ComplexFlag("mi", "Max Items", Integer.MAX_VALUE);
    
    public ItemGeneratorCommand(ExtendedJavaPlugin plugin, GeneratorRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
        
        this.cmdFlags = new CmdFlags(DEBUG_FLAG, SELECT_FLAG, INIT_FLAG, START_FLAG, INVENTORY_PICKUP_FLAG, INVULNERABLE_FLAG, PERSISTENT_FLAG, MAX_ITEMS_FLAG);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        StarColorsV2 colors = plugin.getColors();
        if (!sender.hasPermission("staritemgenerators.command.itemgenerators")) {
            colors.coloredLegacy(sender, "You do not have permission to perform that command.");
            return true;
        }
        
        if (registry == null) {
            colors.coloredLegacy(sender, "&4The Registry for Item Generators is not present for the command logic. This is a bug");
            return true;
        }
        
        FlagResult flagResults = cmdFlags.parse(args);
        args = flagResults.args();
        
        if (!(args.length > 0)) {
            colors.coloredLegacy(sender, "&cYou must provide a sub command.");
            return true;
        }
        
        if (!(sender instanceof Player player)) {
            colors.coloredLegacy(sender, "&cOnly players can use that command.");
            return true;
        }
        
        Location location = player.getLocation();
        
        if (args[0].toLowerCase().startsWith("pos")) {
            this.positionSelection.computeIfAbsent(player.getUniqueId(), u -> new Selection());
            Selection selection = this.positionSelection.get(player.getUniqueId());
            Location loc = player.getLocation().clone();
            int positionNumber;
            if (args[0].equalsIgnoreCase("pos1")) {
                selection.setPos1(loc);
                positionNumber = 1;
            } else if (args[0].equalsIgnoreCase("pos2")) {
                selection.setPos2(loc);
                positionNumber = 2;
            } else {
                colors.coloredLegacy(sender, "&cInvalid position command.");
                return true;
            }
            
            colors.coloredLegacy(sender, "&eSet position &b" + positionNumber + " &eto &b" + loc.getBlockX() + "&e, &b" + loc.getBlockY() + "&e, &b" + loc.getBlockZ() + "&e.");
            return true;
        } else if (args[0].equalsIgnoreCase("create")) {
            if (!(args.length > 1)) {
                colors.coloredLegacy(sender, "&cYou must provide an id for the generator");
                return true;
            }
            
            String id = args[1];
            
            if (this.registry.containsKey(id)) {
                colors.coloredLegacy(sender, "&cA generator with that name already exists.");
                return true;
            }
            
            Selection selection = this.positionSelection.get(player.getUniqueId());
            if (selection == null) {
                colors.coloredLegacy(sender, "&cYou do not have a selection. Use /" + label + " pos<#> to create one.");
                return true;
            }
            
            if (selection.getPos1() == null || selection.getPos2() == null) {
                colors.coloredLegacy(sender, "&cYour selection does not have both postions defined.");
                return true;
            }
            
            if (!selection.getPos1().getWorld().getName().equalsIgnoreCase(selection.getPos2().getWorld().getName())) {
                colors.coloredLegacy(sender, "&cThe positions are not in the same world.");
                return true;
            }
            
            Position pos1 = new Position(selection.getPos1().getBlockX(), selection.getPos1().getBlockY(), selection.getPos1().getBlockZ());
            Position pos2 = new Position(selection.getPos2().getBlockX(), selection.getPos2().getBlockY(), selection.getPos2().getBlockZ());
            
            ItemGenerator itemGenerator = new ItemGenerator(id, new ArrayList<>(), pos1, pos2);
            
            RegistryObject<String, ItemGenerator> object = registry.register(itemGenerator);
            if (object == null) {
                colors.coloredLegacy(sender, "&cFailed to register the new item generator");
                return true;
            }
            
            colors.coloredLegacy(sender, "&eCreated an Item Generator with the id &b" + object.getKey());
            
            if (flagResults.isPresent(INIT_FLAG)) {
                itemGenerator.init(player.getWorld());
                colors.coloredLegacy(sender, "&eInitialized the Item Generator &b" + object.getKey() + " &ein world &b" + player.getWorld().getName());
            }
            
            if (flagResults.isPresent(START_FLAG)) {
                itemGenerator.start();
                colors.coloredLegacy(sender, "&eStarted the Item Generator &b" + object.getKey());
            }
            
            if (flagResults.isPresent(SELECT_FLAG)) {
                selectedGenerators.put(player.getUniqueId(), object.getKey());
                colors.coloredLegacy(sender, "&eSelected the Item Generator &b" + object.getKey());
            }
            
            return true;
        } else if (args[0].equalsIgnoreCase("select")) {
            if (!(args.length > 1)) {
                colors.coloredLegacy(sender, "&cYou must provide an id for the generator to select");
                return true;
            }
            
            ItemGenerator generator = registry.get(args[1]);
            if (generator == null) {
                colors.coloredLegacy(sender, "&cAn Item Generator with the id " + args[1] + " does not exist.");
                return true;
            }
            
            selectedGenerators.put(player.getUniqueId(), generator.getId());
            colors.coloredLegacy(sender, "&eSelected the Item Generator &b" + generator.getId());
            return true;
        } else if (args[0].equalsIgnoreCase("status")) {
            String genId;
            if (args.length > 1) {
                genId = args[1];
            } else {
                genId = this.selectedGenerators.get(player.getUniqueId());
            }
            
            if (genId == null && genId.isBlank()) {
                colors.coloredLegacy(sender, "&cYou must provide a generator id or select a generator to use this command.");
                return true;
            }
            
            ItemGenerator generator = registry.get(genId);
            if (generator == null) {
                colors.coloredLegacy(sender, "&cThe id " + genId + " does not match a valid generator.");
                return true;
            }
            
            List<String> lines = new LinkedList<>();
            lines.add("&6Information for Item Generator &b" + generator.getId());
            lines.add("&eBounds: ");
            Position min = generator.getBoundsMin();
            lines.add("  &eMin: &b(" + min.getBlockX() + ", " + min.getBlockY() + ", " + min.getBlockZ() + ")");
            Position max = generator.getBoundsMin();
            lines.add("  &eMax: &b(" + max.getBlockX() + ", " + max.getBlockY() + ", " + max.getBlockZ() + ")");
            lines.add("&eInitialized: " + formatBoolean(generator.initProperty().get()));
            lines.add("&eRunning: " + formatBoolean(generator.runningProperty().get()));
            lines.add("&eWorld: &b" + (generator.getWorld() != null ? generator.getWorld().getName() : "None"));
            lines.add("&eSpawned Items: &b" + generator.getSpawnedItemsCount());
            lines.add("&eEntries: ");
            for (ItemEntry entry : generator.getItemEntries()) {
                lines.add("  &e" + entry.getId() + ":");
                lines.add("    &eCooldown: &b" + timeFormat.format(entry.getCooldown()));
                lines.add("    &eMax items: &b" + entry.getMaxItems());
                List<String> flags = new ArrayList<>();
                for (Flag flag : entry.getFlags()) {
                    flags.add(flag.name().toLowerCase());
                }
                
                String flagsString = String.join(", ", flags);
                lines.add("    &eFlags: &b" + (flagsString.isBlank() ? "None" : flagsString));
                lines.add("    &eSpawned Items: &b" + generator.getSpawnedItemsCount(entry.getId()));
                Position pos = entry.getSpawnPosition();
                lines.add("    &ePos: &b(" + pos.getBlockX() + ", " + pos.getBlockY() + ", " + pos.getBlockZ() + ")");
                lines.add("    &eWorld: &b" + (entry.getWorld() != null ? entry.getWorld().getName() : "None"));
                lines.add("    &eNext Spawn: &b" + (entry.getTimer() != null ? timeFormat.format(entry.getTimer().getTime())/* + " (" + entry.getTimer().statusProperty().get().name() + ")" */: "0s"));
            }
            
            lines.forEach(line -> colors.coloredLegacy(sender, line));
            return true;
        }
        
        String genId = this.selectedGenerators.get(player.getUniqueId());
        if (genId == null && genId.isBlank()) {
            colors.coloredLegacy(sender, "&cYou must have a generator selection to use that command.");
            return true;
        }
        
        ItemGenerator generator = registry.get(genId);
        if (generator == null) {
            colors.coloredLegacy(sender, "&cThe id " + genId + " does not match a valid generator."); 
            return true;
        }
        
        if (args[0].equalsIgnoreCase("init")) {
            if (generator.initProperty().get()) {
                colors.coloredLegacy(sender, "&cGenerator " + generator.getId() + " is already initialized.");
                return true;
            }
            
            generator.init(player.getWorld());
            colors.coloredLegacy(sender, "&eInitialized the Item Generator &b" + generator.getId() + " &ein world &b" + player.getWorld().getName());
        } else if (args[0].equalsIgnoreCase("start")) {
            if (generator.runningProperty().get()) {
                colors.coloredLegacy(sender, "&cGenerator " + generator.getId() + " is already running.");
                return true;
            }
            
            generator.start();
            colors.coloredLegacy(sender, "&eStarted the Item Generator &b" + generator.getId());
        } else if (args[0].equalsIgnoreCase("stop")) {
            if (!generator.runningProperty().get()) {
                colors.coloredLegacy(sender, "&cGenerator " + generator.getId() + " is not running.");
                return true;
            }
            
            generator.stop();
            colors.coloredLegacy(sender, "&eStopped the Item Generator &b" + generator.getId());
        } else if (args[0].equalsIgnoreCase("additem")) {
            if (!(args.length > 2)) {
                colors.coloredLegacy(sender, "&cUsage: /" + label + " " + args[0] + " <material> <cooldown>");
                return true;
            }
            
            if (!generator.initProperty().get()) {
                colors.coloredLegacy(sender, "&cThe generator must be initialized to add an item");
                return true;
            }
            
            if (!generator.contains(player)) {
                colors.coloredLegacy(sender, "&cYou are not within the generator bounds.");
                return true;
            }
            
            Optional<SMaterial> matOpt = SMaterial.matchXMaterial(args[1].toUpperCase());
            if (matOpt.isEmpty()) {
                colors.coloredLegacy(sender, "&cYou provided an invalid material " + args[1]);
                return true;
            }
            
            SMaterial material = matOpt.get();
            
            if (!material.isSupported()) {
                colors.coloredLegacy(sender, "&cYou provided an invalid material " + args[1]);
                return true;
            }
            
            if (generator.hasEntry(material.name().toLowerCase())) {
                colors.coloredLegacy(sender, "&cAn item already exists by the name " + material.name().toLowerCase());
                return true;
            }
            
            long cooldown = timeParser.parseTime(args[2]);
            
            int maxItems;
            Object rawMaxItems = flagResults.getValue(MAX_ITEMS_FLAG);
            try {
                maxItems = Integer.parseInt(rawMaxItems.toString());
            } catch (NumberFormatException e) {
                colors.coloredLegacy(sender, "&cInvalid number provided: " + args[3]);
                return true;
            }
            
            List<Flag> flags = new ArrayList<>();
            
            if (flagResults.isPresent(PERSISTENT_FLAG)) {
                flags.add(Flag.PERSISTENT);
            }
            
            if (flagResults.isPresent(INVULNERABLE_FLAG)) {
                flags.add(Flag.INVULNERABLE);
            }
            
            if (flagResults.isPresent(INVENTORY_PICKUP_FLAG)) {
                flags.add(Flag.INVENTORY_PICKUP);
            }
            
            ItemEntry itemEntry = new ItemEntry(material.name().toLowerCase(), ItemBuilders.of(material), cooldown, maxItems, new Position(location.getBlockX(), location.getBlockY(), location.getBlockZ()), flags);
            generator.addItemEntry(itemEntry);
            
            if (flagResults.isPresent(DEBUG_FLAG)) {
                itemEntry.addPickupListener((entity, item, entry, gen) -> colors.coloredLegacy(sender, "&8Picked up " + entry.getId()));
                itemEntry.addSpawnListener((item, entry, gen) -> colors.coloredLegacy(sender, "&8Spawned " + entry.getId()));
            }
            
            List<String> msgLines = new LinkedList<>();
            msgLines.add("&eYou added the item &b" + itemEntry.getId() + " &eto the generator &b" + generator.getId());
            
            if (maxItems == Integer.MAX_VALUE) {
                msgLines.add("  &eMax Items: &bInfinite");
            } else {
                msgLines.add("  &eMax Items: &b" + maxItems);
            }
            
            msgLines.add("  &ePersistent: &b" + formatBoolean(itemEntry.hasFlag(Flag.PERSISTENT)));
            msgLines.add("  &eInventory Pickup: &b" + formatBoolean(itemEntry.hasFlag(Flag.INVENTORY_PICKUP)));
            msgLines.add("  &eInvulnerable: &b" + formatBoolean(itemEntry.hasFlag(Flag.INVULNERABLE)));
            
            msgLines.forEach(line -> colors.coloredLegacy(sender, line));
        } 
        
        return true;
    }
    
    private static String formatBoolean(boolean v) {
        return v ? "&atrue" : "&cfalse";
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        selectedGenerators.remove(e.getPlayer().getUniqueId());
    }
    
    private static class Selection {
        private Location pos1, pos2;
        
        public Location getPos1() {
            return pos1;
        }
        
        public void setPos1(Location pos1) {
            this.pos1 = pos1;
        }
        
        public Location getPos2() {
            return pos2;
        }
        
        public void setPos2(Location pos2) {
            this.pos2 = pos2;
        }
    }
}