package sirlich.plugin.TutorialToken;

import de.tr7zw.itemnbtapi.NBTItem;
import org.bukkit.*;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.List;
import java.util.Map;

public class TutorialToken extends JavaPlugin implements Listener , CommandExecutor
{
    private TutorialToken instance;

    private void loadConfigs(){
        try {
            if (!getDataFolder().exists()) {
                System.out.println("Data folder not found... creating!");
                getDataFolder().mkdirs();
                System.out.println(getDataFolder());
            } else {
                System.out.println("Data folder exists!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void spawnItems(){
        File dir = new File(this.getDataFolder().toString());
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            for (File arenaYml : directoryListing) {
                FileConfiguration config = YamlConfiguration.loadConfiguration(arenaYml);
                List<Map<?,?>> drops = config.getMapList( "drops");
                for(Map<?,?> drop : drops){
                    World world = Bukkit.getWorld(config.getString("default.world"));
                    boolean glowing = config.getBoolean("default.glowing");
                    Material item = Material.valueOf(config.getString("default.item").toUpperCase());
                    String message = config.getString("default.message");
                    Sound sound = Sound.valueOf(config.getString("default.sound").toUpperCase());
                    String name = config.getString("default.name");
                    boolean showName = Boolean.parseBoolean(config.getString("default.showName"));
                    int delay = config.getInt("default.delay");

                    if(drop.containsKey("world")){
                        world = Bukkit.getWorld((String) drop.get("world"));
                    } if(drop.containsKey("glowing")){
                        glowing = (Boolean) drop.get("glowing");
                    } if(drop.containsKey("item")){
                        item = Material.valueOf(drop.get("item").toString().toUpperCase());
                    } if(drop.containsKey("message")){
                        message = drop.get("message").toString();
                    } if(drop.containsKey("sound")){
                        sound = Sound.valueOf(drop.get("sound").toString().toUpperCase());
                    } if(drop.containsKey("name")){
                        name = drop.get("name").toString();
                    } if(drop.containsKey("showName")){
                        showName = Boolean.parseBoolean(drop.get("showName").toString());
                    } if(drop.containsKey("delay")){
                        delay = Integer.parseInt(drop.get("delay").toString());
                    }

                    String preLoc = drop.get("location").toString();
                    String[] locs = preLoc.split(" ");
                    int x = Integer.parseInt(locs[0]);
                    int y = Integer.parseInt(locs[1]);
                    int z = Integer.parseInt(locs[2]);

                    Location location = new Location(world,x,y,z);
                    spawnItem(location,glowing,item,message,sound,name,showName,delay);
                }
            }
        }
    }

    private void spawnItem(Location location, boolean glowing, Material item, String message, Sound sound, String name, boolean showName, int delay){
        ItemStack itemStack = new ItemStack(item);
        if(glowing){
            itemStack.addUnsafeEnchantment(Enchantment.THORNS,1);
        }

        NBTItem nbtItem = new NBTItem(itemStack);
        nbtItem.addCompound("tt.drop");
        nbtItem.setString("tt.message",message);
        nbtItem.setString("tt.sound",sound.toString());
        nbtItem.setString("tt.name",name);
        nbtItem.setInteger("tt.delay",delay);
        nbtItem.setBoolean("tt.showName",showName);
        itemStack = nbtItem.getItem();
        Item i = location.getWorld().dropItem(location,itemStack);
        i.setGravity(false);
        i.setVelocity(new Vector(0,0,0));
        i.setCustomName(ChatColor.translateAlternateColorCodes('&',name));
        i.setCustomNameVisible(showName);
    }

    private void removeItems(){

        //Loop through all worlds
        for(World world : Bukkit.getWorlds()){

            //Loop through all items
            for(Entity entity : Bukkit.getServer().getWorld("world").getEntities()){
                if(entity instanceof Item){
                    ItemStack itemStack = ((Item) entity).getItemStack();
                    NBTItem nbtItem = new NBTItem(itemStack);

                    if(nbtItem.hasKey("tt.drop")){
                        entity.remove();
                    }
                }
            }
        }
    }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        this.instance = this;
        loadConfigs();
        spawnItems();
    }

    @Override
    public void onDisable() {
        removeItems();
    }

    @EventHandler
    public void onEntityDespawn(final ItemDespawnEvent event){
        Item item = event.getEntity();
        ItemStack itemStack = item.getItemStack();
        NBTItem nbtItem = new NBTItem(itemStack);
        if(nbtItem.hasKey("tt.drop")){
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPickUpItem(final EntityPickupItemEvent event){
        if(event.getEntity() instanceof  Player){
            Player player = (Player) event.getEntity();
            ItemStack itemStack = event.getItem().getItemStack();
            NBTItem nbtItem = new NBTItem(itemStack);

            if(nbtItem.hasKey("tt.drop")){
                event.setCancelled(true);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', nbtItem.getString("tt.message")));
                player.playSound(player.getLocation(),Sound.valueOf(nbtItem.getString("tt.sound").toUpperCase()),1,1);
                final Item item = event.getItem();
                final Location location = event.getItem().getLocation();
                final boolean glowing = event.getItem().getItemStack().containsEnchantment(Enchantment.THORNS);
                final Material material = item.getItemStack().getType();
                final String message = nbtItem.getString("tt.message");
                final Sound sound = Sound.valueOf(nbtItem.getString("tt.sound").toUpperCase());
                final String name = nbtItem.getString("tt.name");
                final boolean showName = nbtItem.getBoolean("tt.showName");
                final int delay = nbtItem.getInteger("tt.delay");

                event.getItem().remove();

                new BukkitRunnable() {

                    @Override
                    public void run() {
                        spawnItem(location,glowing,material,message,sound,name,showName,delay);
                    }

                }.runTaskLater(instance, delay);
            }
        }
    }
}
