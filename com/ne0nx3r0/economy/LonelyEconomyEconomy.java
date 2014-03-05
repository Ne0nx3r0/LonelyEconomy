package com.ne0nx3r0.economy;

import com.ne0nx3r0.lonelyeconomy.LonelyEconomy;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import lib.PatPeter.SQLibrary.SQLite;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public final class LonelyEconomyEconomy {
    private final LonelyEconomy plugin;
    private final SQLite sqlite;
    private final String SERVER_ACCOUNT_USERNAME = "!*~server~*!";
    private boolean enabled;
    private Logger logger;
    
    public LonelyEconomyEconomy(LonelyEconomy plugin) {
        this.plugin = plugin;
        
        this.logger = Logger.getLogger("LonelyEconomyEconomy");  
        FileHandler fh;  
          
        try {  
            this.logger.setUseParentHandlers(false);
            // This block configure the logger with handler and formatter  
            fh = new FileHandler(new File(plugin.getDataFolder(),"transactions.log").getAbsolutePath());  
            logger.addHandler(fh);  
            //logger.setLevel(Level.ALL);  
            SimpleFormatter formatter = new SimpleFormatter();  
            fh.setFormatter(formatter);  

            
              
        } catch (SecurityException | IOException ex) {  
            Logger.getLogger(LonelyEconomyEconomy.class.getName()).log(Level.SEVERE, null, ex);
            
            this.sqlite = null;
            
            plugin.disable();
            
            return;
        }  
        
        this.sqlite = new SQLite(
            plugin.getLogger(),
            "LonelyEconomy",
            "lonelyeconomy",
            plugin.getDataFolder().getAbsolutePath()
        );
        
        try {
            sqlite.open();
        } 
        catch (SQLException ex) {
            Logger.getLogger(LonelyEconomyEconomy.class.getName()).log(Level.SEVERE, null, ex);
            
            plugin.disable();
            
            return;
        }
        
        if(!sqlite.checkTable("accounts"))
        {
            // 1,000,000,000.00000001
            sqlite.query("CREATE TABLE accounts("
                + "username VARCHAR(16) PRIMARY KEY,"   // Minecraft username
                + "balance VARCHAR(32),"                // String <> BigDecimal
                + "sorting_balance INT"                // Never read from, only used to sort for /top, etc.
            + ");"); 
            
            plugin.getLogger().log(Level.INFO, "Database & accounts table created.");
            
            this.log("Database & accounts table created.");
            
            BigDecimal serverStartingBalance = new BigDecimal(plugin.getConfig().getInt("server_starting_balance",0)); 
        
            this.createAccount(SERVER_ACCOUNT_USERNAME, serverStartingBalance);
        }
        
        this.enabled = true;
    }
    
    public boolean isEnabled() {
        return this.enabled;
    }

    public synchronized LonelyEconomyResponse createAccount(String playerName,BigDecimal startingBalance) {
        playerName = playerName.toLowerCase();
        
        try
        {
            PreparedStatement statement = sqlite.prepare("SELECT balance FROM accounts WHERE username=? LIMIT 1;");

            statement.setString(1, playerName.toLowerCase());

            ResultSet result = statement.executeQuery();

            if(result.next())
            {
                return new LonelyEconomyResponse(LonelyEconomyResponseType.FAILURE)
                     .setMessage(playerName+" already has an account!");
            }
        } 
        catch (SQLException ex) {
            Logger.getLogger(LonelyEconomyEconomy.class.getName()).log(Level.SEVERE, null, ex);

            return new LonelyEconomyResponse(LonelyEconomyResponseType.FAILURE_DATABASE)
                 .setMessage("A database error occurred!");
        }
        
        try {
            PreparedStatement statement = sqlite.prepare("INSERT INTO accounts(username,balance,sorting_balance) VALUES(?,?,?);");

            statement.setString(1, playerName.toLowerCase());
            statement.setString(2, this.toDatabaseValue(startingBalance));
            statement.setInt(3, startingBalance.intValue());

            statement.execute();
        }
        catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, null, ex);

            this.log("Failed to create account for "+playerName);
        
            return new LonelyEconomyResponse(LonelyEconomyResponseType.FAILURE)
                 .setMessage("A database error occurred!");
        }

        this.log("Created account for "+playerName+ " with a starting balance of "+startingBalance);
        
        return new LonelyEconomyResponse(LonelyEconomyResponseType.SUCCESS);
    }

    public synchronized boolean hasAccount(String playerName) {
        try
        {
            PreparedStatement statement = sqlite.prepare("SELECT balance FROM accounts WHERE username=? LIMIT 1;");

            statement.setString(1, playerName.toLowerCase());

            ResultSet result = statement.executeQuery();

            if(result.next())
            {
                return true;
            }
            
            // If the player is online create an account for them
            if(Bukkit.getPlayer(playerName) != null) {
                LonelyEconomyResponse ler = this.createAccount(playerName, new BigDecimal(0));
                
                if(ler.wasSuccessful()) {
                    return true;
                }
            }
        } 
        catch (SQLException ex) {
            Logger.getLogger(LonelyEconomyEconomy.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return false;
    }
    
    public synchronized BigDecimal getBalance(String playerName) {
        try
        {
            PreparedStatement statement = sqlite.prepare("SELECT balance FROM accounts WHERE username=? LIMIT 1;");

            statement.setString(1, playerName.toLowerCase());

            ResultSet result = statement.executeQuery();

            if(result.next())
            {
                return this.fromDatabaseValue(result.getString("balance"));
            }
            
            // If the player is online create an account for them
            if(Bukkit.getPlayer(playerName) != null) {
                BigDecimal startingBalance = new BigDecimal(0);
                
                LonelyEconomyResponse ler = this.createAccount(playerName, startingBalance);
                
                if(ler.wasSuccessful()) {
                    return startingBalance;
                }
            }
        } 
        catch (SQLException ex) {
            Logger.getLogger(LonelyEconomyEconomy.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return new BigDecimal(0);
    }
    
    public BigDecimal getServerBalance() {
        return this.getBalance(SERVER_ACCOUNT_USERNAME);
    }
    
    public boolean hasBalance(String player, BigDecimal amount) {
        // amount >= playerBalance
        return this.getBalance(player).compareTo(amount) > -1;
    }

    private synchronized LonelyEconomyResponse setPlayerBalance(String playerName,BigDecimal amount) {    
        long startTime = System.nanoTime();    
        try
        {
            PreparedStatement statement = sqlite.prepare("UPDATE accounts SET balance=?,sorting_balance=? WHERE username=?");

            statement.setString(1, this.toDatabaseValue(amount));
            statement.setInt(2, amount.intValue());
            statement.setString(3, playerName.toLowerCase());

            statement.execute();
            
            this.log("Set "+playerName+" balance to "+amount.toPlainString());
        } 
        catch (Exception ex) {
            Logger.getLogger(LonelyEconomyEconomy.class.getName()).log(Level.SEVERE, null, ex);
            
            return new LonelyEconomyResponse(LonelyEconomyResponseType.FAILURE_DATABASE)
                .setMessage("A database error occurred!");
        }
        
        System.out.println((System.nanoTime()-startTime)+" setPlayerBalance()");
        
        return new LonelyEconomyResponse(LonelyEconomyResponseType.SUCCESS)
            .setBalance(amount);
    }
    
    public LonelyEconomyResponse takeMoneyFromPlayer(String playerName, BigDecimal amount) {
        long startTime = System.nanoTime();
// Disable accessing the server account
        if(playerName.equalsIgnoreCase(SERVER_ACCOUNT_USERNAME)) {
            return new LonelyEconomyResponse(LonelyEconomyResponseType.FAILURE)
                .setMessage("Server account cannot be accessed this way");
        }
        
        if(!this.hasAccount(playerName)) {
            return new LonelyEconomyResponse(LonelyEconomyResponseType.FAILURE_NO_ACCOUNT_EXISTS)
                .setMessage("No account exists for "+playerName+"!");
        }
        
        this.log("Attempting to take "+amount.toPlainString()+" from "+playerName);

// Grab player's balance to see if the player has enough 
        BigDecimal playerBalance = this.getBalance(playerName);
      
// Check if the player's balance is lower than the amount
        if(playerBalance.compareTo(amount) == -1) {
            return new LonelyEconomyResponse(LonelyEconomyResponseType.FAILURE_INSUFFICIENT_FUNDS)
                .setMessage(playerName+" doesn't have "+amount+"!");
        }
        
// Take the money from the player   
        LonelyEconomyResponse ler = this.setPlayerBalance(playerName, playerBalance.subtract(amount));
        
// Error occurred 
        if(!ler.wasSuccessful()) {
            this.log("Failed to take "+amount.toPlainString()+" from "+playerName+" to give to server");
            
            return ler;
        }
        
// Get the server's balance to give the new amount to it
        BigDecimal serverBalance = this.getBalance(this.SERVER_ACCOUNT_USERNAME);
        
// Give the money to the server
        ler = this.setPlayerBalance(this.SERVER_ACCOUNT_USERNAME, serverBalance.add(amount));
        
// Error occurred 
        if(!ler.wasSuccessful()) {
            this.log("Failed to give "+amount.toPlainString()+" to server from "+playerName);
            
            return ler;
        }
        
        this.log("Took "+amount.toPlainString()+" from "+playerName);
        
        System.out.println((System.nanoTime()-startTime)+" takeMoneyFromPlayer()");
// Return success  
        return ler;
    }

    public LonelyEconomyResponse giveMoneyToPlayer(String playerName, BigDecimal amount) {
        long startTime = System.nanoTime();
// Disable accessing the server account
        if(playerName.equalsIgnoreCase(SERVER_ACCOUNT_USERNAME)) {
            return new LonelyEconomyResponse(LonelyEconomyResponseType.FAILURE)
                .setMessage("Server account cannot be accessed this way");
        }
        
        if(!this.hasAccount(playerName)) {
            return new LonelyEconomyResponse(LonelyEconomyResponseType.FAILURE_NO_ACCOUNT_EXISTS)
                .setMessage("No account exists for "+playerName+"!");
        }

        this.log("Attempting to give "+amount.toPlainString()+" to "+playerName);
       
// Grab server's balance to see if the server has enough
        BigDecimal serverBalance = this.getBalance(this.SERVER_ACCOUNT_USERNAME);

// Check if the server balance is lower than the amount
        if(serverBalance.compareTo(amount) == -1) {
            return new LonelyEconomyResponse(LonelyEconomyResponseType.FAILURE_INSUFFICIENT_FUNDS)
                .setMessage("Server does not have "+amount+" to give to "+playerName+"!");
        }

// Take the money from the server        
        LonelyEconomyResponse ler = this.setPlayerBalance(
                this.SERVER_ACCOUNT_USERNAME, 
                serverBalance.subtract(amount));

// Error occurred        
        if(!ler.wasSuccessful()) {
            this.log("Failed to take "+amount.toPlainString()+" from server to give to "+playerName);
            
            return ler;
        }

// Get the player's balance to give the new amount to him        
        BigDecimal playerBalance = this.getBalance(playerName);

// Give the money to the player
        ler = this.setPlayerBalance(
                playerName, 
                playerBalance.add(amount));
        
// Error occurred
        if(!ler.wasSuccessful()) {
            this.log("Failed to give "+amount.toPlainString()+" to "+playerName);
            
            return ler;
        }
        
        this.log("Gave "+amount.toPlainString()+" to "+playerName);

        System.out.println((System.nanoTime()-startTime)+" giveMoneyToPlayer()");
// Return success        
        return ler;
    }

    public LonelyEconomyResponse payPlayer(String payFrom, String payTo, BigDecimal amount) {
        
        long startTime = System.nanoTime();
        
        if(payFrom.equalsIgnoreCase(payTo)) {
            // no need to do anything...
            return new LonelyEconomyResponse(LonelyEconomyResponseType.SUCCESS);
        }
        
        if(!this.hasAccount(payTo)) {
            return new LonelyEconomyResponse(LonelyEconomyResponseType.FAILURE_NO_ACCOUNT_EXISTS)
                .setMessage("No account exists for "+payTo+"!");
        }
        
        LonelyEconomyResponse ler = this.takeMoneyFromPlayer(payFrom,amount);
        
        if(!ler.wasSuccessful()) {
            return ler;
        }
        
        System.out.println((System.nanoTime()-startTime)+" payPlayer()");
        return this.giveMoneyToPlayer(payTo,amount);
    }

    public LinkedHashMap<String,BigDecimal> getTopPlayers(int iTopAmount) {
        try
        {
            PreparedStatement statement = sqlite.prepare(""
                + "SELECT username,balance "
                + "FROM accounts WHERE username != ? "
                + "ORDER BY sorting_balance DESC,username ASC LIMIT "+iTopAmount+";");
            
            statement.setString(1, SERVER_ACCOUNT_USERNAME);
            
            ResultSet result = statement.executeQuery();
            
            LinkedHashMap <String,BigDecimal> topPlayers = new LinkedHashMap <>();
            
            while(result.next())
            {
                topPlayers.put(result.getString("username"),this.fromDatabaseValue(result.getString("balance")));
            }
            
            result.close();
            
            return topPlayers;
        }
        catch (Exception ex)
        {
            Logger.getLogger(LonelyEconomyEconomy.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return null;
    }

    public int getRank(String playerName) {
        try
        {
            PreparedStatement statement = sqlite.prepare(""
                + "SELECT COUNT(username) as rank "
                + "FROM accounts "
                + "WHERE username != ? AND sorting_balance > ?");
            
            statement.setString(1,this.SERVER_ACCOUNT_USERNAME);
            statement.setInt(2, this.getBalance(playerName).intValue());
            
            ResultSet result = statement.executeQuery();

            if(result.next())
            {
                int iRank = result.getInt("rank")+1;
                
                result.close();
                
                return iRank;
            }
            
            result.close();
        }
        catch (Exception ex)
        {
            Logger.getLogger(LonelyEconomyEconomy.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return -1;
    }
    
    public BigDecimal getBigDecimal(double amount) {
        BigDecimal bd = new BigDecimal(amount);
        
        return bd.setScale(2, BigDecimal.ROUND_HALF_UP);
    }
    
    public BigDecimal getBigDecimal(String amount) throws NumberFormatException {
        long startTime = System.nanoTime();
        BigDecimal bd;

        bd = new BigDecimal(amount);

        System.out.println((System.nanoTime()-startTime)+" from getBigDecimal()");
        return bd.setScale(2, BigDecimal.ROUND_HALF_UP);
    }
    
    public double getDouble(BigDecimal bd) {
        return bd.doubleValue();
    }
    
    public String getCurrencyName(boolean plural) {
        return "ß";
    }
    
    public String format(BigDecimal amount) {
        return ChatColor.WHITE+amount.toPlainString()+ChatColor.GOLD+this.getCurrencyName(true)+ChatColor.RESET;
    }
    
    // internal helper methods
    private String toDatabaseValue(BigDecimal bd) {
        return bd.toPlainString();
    }
    
    private BigDecimal fromDatabaseValue(String databaseValue) {
        return new BigDecimal(databaseValue);
    }
    
    private void log(String message)
    {
        logger.info(message);
    }
}
