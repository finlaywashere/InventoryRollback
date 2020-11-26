package xyz.finlaym.invrollback;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener{
	
	private static int PRUNE_COUNT = 20;
	
	private File saveFolder;
	
	@Override
	public void onEnable() {
		super.onEnable();
		saveFolder = getDataFolder();
		if(!saveFolder.exists())
			saveFolder.mkdirs();
		Bukkit.getPluginManager().registerEvents(this, this);
	}
	@Override
	public void onDisable() {
		super.onDisable();
	}
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(command.getName().equalsIgnoreCase("invrollback")) {
			if(args.length == 0) {
				sender.sendMessage("Error: Usage is /rollback <player> [deathindex]");
				return true;
			}
			Player p = sender.getServer().getPlayer(args[0]);
			File playerFolder = new File(saveFolder,p.getUniqueId().toString());
			File[] files = getFiles(playerFolder);
			Arrays.sort(files);
			File f;
			if(args.length >= 2) {
				int index = Integer.valueOf(args[1]);
				if(files.length-1-index < 0) {
					sender.sendMessage("Error: No deaths available with that death offset! Highest available offset it "+(files.length-1));
					return true;
				}
				f = files[files.length-1-index];
			}else {
				f = files[files.length-1];
			}
			if(!f.exists()) {
				sender.sendMessage("Error: No deaths on file for that player!");
				return true;
			}
			FileConfiguration conf = new YamlConfiguration();
			try {
				conf.load(f);
			} catch (Exception e) {
				e.printStackTrace();
			}
			PlayerInventory inv = p.getInventory();
			for(int i = 0; i < 36; i++) {
				ItemStack item = conf.getItemStack(""+i, null);
				inv.setItem(i, item);
			}
			inv.setHelmet(conf.getItemStack("helmet",null));
			inv.setChestplate(conf.getItemStack("chestplate",null));
			inv.setLeggings(conf.getItemStack("leggings",null));
			inv.setBoots(conf.getItemStack("boots",null));
			inv.setItemInOffHand(conf.getItemStack("offhand",null));
			
			sender.sendMessage("Successfully restored player's inventory!");
			
			return true;
		}
		return super.onCommand(sender, command, label, args);
	}
	@EventHandler
	public void onDeath(PlayerDeathEvent e) {
		Player p = e.getEntity();
		File playerFolder = new File(saveFolder,p.getUniqueId().toString());
		if(!playerFolder.exists())
			playerFolder.mkdirs();
		// Prune files
		File[] files = null;
		while((files = getFiles(playerFolder)).length >= PRUNE_COUNT) {
			Arrays.sort(files);
			try {
				Files.delete(files[0].toPath());
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		long time = System.currentTimeMillis();
		File saveFile = new File(playerFolder,"save-"+time+".save");
		PlayerInventory inv = p.getInventory();
		FileConfiguration conf = new YamlConfiguration();
		try {
			conf.set("helmet", inv.getHelmet());
			conf.set("chestplate", inv.getChestplate());
			conf.set("leggings", inv.getLeggings());
			conf.set("boots", inv.getBoots());
			conf.set("offhand", inv.getItemInOffHand());
			for(int i = 0; i < 36; i++) {
				conf.set(""+i, inv.getItem(i));
			}
			conf.save(saveFile);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
	}
	private static File[] getFiles(File parent) {
		return parent.listFiles(new FileFilter(){
			@Override
			public boolean accept(File f) {
				if(f.isFile())
					return true;
				return false;
			}
		});
	}
}
