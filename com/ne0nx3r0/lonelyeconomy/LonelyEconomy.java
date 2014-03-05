package com.ne0nx3r0.lonelyeconomy;

import com.ne0nx3r0.lonelyeconomy.commands.LonelyEconomyCommandExecutor;
import com.ne0nx3r0.economy.LonelyEconomyEconomy;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class LonelyEconomy extends JavaPlugin {
    public LonelyEconomyEconomy economy;
    
    @Override
    public void onEnable() {
        try {
            getDataFolder().mkdirs();

            File configFile = new File(getDataFolder(),"config.yml");

            if(!configFile.exists())
            {
                copy(getResource("config.yml"), configFile);
            }
        } 
        catch (IOException ex) {
            this.getLogger().log(Level.INFO, "Unable to load config! Disabling");
            
            this.disable();
        }
        
        this.economy = new LonelyEconomyEconomy(this);
        
        this.getCommand("money").setExecutor(new LonelyEconomyCommandExecutor(this));
    }
    
    public void disable() {
        Bukkit.getPluginManager().disablePlugin(this);
    }
        
// Public helper methods
    public void copy(InputStream in, File file) throws IOException
    {
        OutputStream out = new FileOutputStream(file);
        byte[] buf = new byte[1024];
        int len;
        while((len=in.read(buf))>0)
        {
            out.write(buf,0,len);
        }
        out.close();
        in.close();
    }
}
