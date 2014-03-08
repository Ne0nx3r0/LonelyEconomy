package com.ne0nx3r0.lonelyeconomy.commands;

import com.ne0nx3r0.economy.LonelyEconomyEconomy;
import com.ne0nx3r0.economy.LonelyEconomyResponse;
import com.ne0nx3r0.lonelyeconomy.LonelyEconomy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LonelyEconomyCommandExecutor implements CommandExecutor {
    private final LonelyEconomy plugin;
    private final LonelyEconomyEconomy economy;

    public LonelyEconomyCommandExecutor(LonelyEconomy plugin) {
        this.plugin = plugin;
        this.economy = plugin.economy;
    }

    @Override
    public boolean onCommand(CommandSender cs, Command cmnd, String label, String[] args) {
        /*
            /money pay <username> <amount>
            /money give <username> <amount>
            /money take <username> <amount>
            /money balance
            /money balance <username>
            /money top <amount>
            /money rank <username>
        */       
        
        if(args.length > 0)
        {
            switch(args[0])
            {
               /* case "balance":
                case "b":
                    return this._balance(cs,args);*/
                case "pay":
                    return this._pay(cs,args);
                case "give":
                    return this._give(cs,args);
                case "top":
                case "t":
                    return this._top(cs,args);
                case "rank":
                    return this._rank(cs,args);
                case "take":
                    return this._take(cs,args);
                case "gimme":
                    if(cs instanceof Player) {
                        Bukkit.getPlayer(cs.getName()).kickPlayer("/money: I TOLD YOU THAT IS NOT A COMMAND!!!");

                        Bukkit.getServer().broadcastMessage(ChatColor.LIGHT_PURPLE+"[Server] Guys, take it easy on him would you? /money is a busy guy.");
                    }
                    return true;
            }
        }
        
        return this._usage(cs,args);
    }

    private boolean hasCommandPermission(CommandSender cs,String sPerm,String sAction)
    {
        if(!(cs instanceof Player)) {
            return true;
        }
        
        if(cs.hasPermission(sPerm))
        {
            return true;
        }
        
        this.send(cs,"Permissions",
                ChatColor.RED+"You do not have permission to "+ChatColor.WHITE+sAction+ChatColor.RED+".",
                ChatColor.RED+"Permission node: "+ChatColor.WHITE+sPerm);
        
        return false;
    }

    private boolean _usage(CommandSender cs, String[] args) {
        List<String> availableCommands = new ArrayList<>();
        
        availableCommands.add("Usage");
        
        if(cs.hasPermission("lonelyeconomy.balance"))
        {
            availableCommands.add("/money");
        }
        if(cs.hasPermission("lonelyeconomy.balance.others"))
        {
            availableCommands.add("/money balance [playerName]");
        }
        if(cs.hasPermission("lonelyeconomy.pay"))
        {
            availableCommands.add("/money pay <username> <amount>");
        }
        if(cs.hasPermission("lonelyeconomy.top"))
        {
            availableCommands.add("/money top [#]");
        }
        if(cs.hasPermission("lonelyeconomy.rank"))
        {
            availableCommands.add("/money rank [username]");
        }
        if(cs.hasPermission("lonelyeconomy.give"))
        {
            availableCommands.add("/money give <username> <amount>");
        }
        if(cs.hasPermission("lonelyeconomy.take"))
        {
            availableCommands.add("/money take <username> <amount>");
        }
        
        availableCommands.add("");
        
        if(args.length > 0 && this.economy.hasAccount(args[0])) {
            BigDecimal playerBalance = this.plugin.economy.getBalance(args[0]);

            availableCommands.add(ChatColor.GRAY+args[0]+" has "+ChatColor.WHITE+this.economy.format(playerBalance));
        }
        else {
            BigDecimal playerBalance = this.plugin.economy.getBalance(cs.getName());

            availableCommands.add(ChatColor.GRAY+"You have "+ChatColor.WHITE+this.economy.format(playerBalance));
        }
        
        BigDecimal serverBalance = this.plugin.economy.getServerBalance();

        availableCommands.add(ChatColor.GRAY+"Server currently has: "+ChatColor.WHITE+this.economy.format(serverBalance)+ChatColor.GRAY+" in reserve.");
        
        this.send(cs,availableCommands.toArray(new String[availableCommands.size()]));
        
        return true;
    }

    private boolean _pay(CommandSender cs, String[] args) {
        if(!this.hasCommandPermission(cs, "lonelyeconomy.pay", "pay other users"))
        {
            return true;
        }
        
        if(args.length < 3)
        {
            this.send(cs,"Pay","/money pay <username> <amount>");

            return true;
        }
        
        String payTo = args[1];
        
        if(payTo.equalsIgnoreCase(cs.getName())) {
            String haveToGoReason;
            
            if(Bukkit.getOnlinePlayers().length > 1 && cs instanceof Player) {
                Random r = new Random();

                Player player = (Player) cs;
                
                while(player == cs) {
                    player = Bukkit.getOnlinePlayers()[r.nextInt(Bukkit.getOnlinePlayers().length)];
                }
                
                // get a random player name
                haveToGoReason = "I have to inform "+player.getDisplayName()+ChatColor.RED+" that /money gimme' is not a real command, even though everyone tries it.";
            }
            else {
                haveToGoReason = "The logging script just broke up with the file system and he's devastated.";
            }
            
            // TODO Async this into a timer
            this.send(cs,"Pay",
                    ChatColor.RED+"You can't give yourself money!",
                    "",
                    ChatColor.RED+"Well.. Okay, you can give yourself money, technically...",
                    "",
                    ChatColor.RED+"But seriously, I mean do you have any idea how much work that database has to do already? What if I just kept you here reading some stupid error message that really didn't say anything but was way longer than it needs to be, hrm?",
                    "",
                    ChatColor.RED+"It would be all:",
                    "",
                    ChatColor.RESET+"LonelyEconomy has thrown an exception of type ",
                    ChatColor.DARK_RED+"HOLY_CRAP_YOU_ARE_LAME_FOR_WASTING_MY_TIME",
                    "",
                    ChatColor.RED+"And you'd feed like an idiot wouldn't you?",
                    "",
                    ChatColor.RED+"Gah! I have to go. "+haveToGoReason);

            return true;
        }
        
        BigDecimal amount;
        
        try {
            amount = this.economy.getBigDecimal(args[2]);
        }
        catch(NumberFormatException e) {
            this.send(cs,"Pay",ChatColor.RED+"Invalid amount");
            
            return true;
        }
        
        LonelyEconomyResponse ler = economy.payPlayer(cs.getName(),payTo,amount);
        
        // responsible for checking balance, existence of accounts, etc...
        // This is done to prevent duplicate work
        if(!ler.wasSuccessful()) {
            this.send(cs,"Pay",ChatColor.RED+ler.getMessage());
            
            return true;
        }
        
        this.send(cs,"Pay","Sent "+amount+" to "+payTo);
        
        Player pPayTo = Bukkit.getPlayer(payTo);
        
        if(pPayTo != null) {
            pPayTo.sendMessage(cs.getName()+ChatColor.RESET+" has paid you "+this.economy.format(amount));
        }
        
        return true;
    }
    /*
    private boolean _balance(CommandSender cs, String[] args) {
        if(!this.hasCommandPermission(cs, "lonelyeconomy.balance", "check player's balances")) {
            return true;
        }
        
        if(args.length == 1)
        {
            this.send(cs,
                "Balance",
                "/money balance",
                "/money b <username>");
            
            BigDecimal playerBalance = this.plugin.economy.getBalance(cs.getName());
        
            cs.sendMessage(ChatColor.GREEN+"You have "+playerBalance.toPlainString()+playerBalance+economy.getCurrencyName(true));
            
            return true;
        }
        else {
            String playerName = args[1];
            
            if(!economy.hasAccount(playerName)) {
                this.send(cs,"Balance",playerName+" does not have an account!");
                
                return true;
            }
            
            BigDecimal playerBalance = this.plugin.economy.getBalance(playerName);
        
            this.send(cs,"Balance",playerName+" has "+this.economy.format(playerBalance));
        }
        
        return true;
    }*/

    private boolean _top(CommandSender cs, String[] args) {
        if(!this.hasCommandPermission(cs, "lonelyeconomy.top", "see top players")){ 
            return true;
        }
        
        int iTopAmount = 10;
        
        if(args.length == 2)
        {
            try
            {
                iTopAmount = Integer.parseInt(args[1]);
            }
            catch(NumberFormatException e)
            {
                this.send(cs,"Top",ChatColor.RED+args[1]+" is not a valid amount!");
                
                return true;
            }
        }
        
        if(iTopAmount > 50)
        {
            iTopAmount = 50;
        }

        List<String> toSend = new ArrayList<String>();

        toSend.add("Top "+iTopAmount+" players");
        
        int iRank = 0;
        
        LinkedHashMap<String, BigDecimal> topPlayers = this.economy.getTopPlayers(iTopAmount);
        Iterator it = topPlayers.entrySet().iterator();
        
        while (it.hasNext()) {
            iRank++;
            
            Map.Entry pairs = (Map.Entry) it.next();

            toSend.add(ChatColor.GOLD+"#"+iRank+" "+ChatColor.WHITE+pairs.getKey()+ChatColor.GRAY+" ("+this.economy.format((BigDecimal) pairs.getValue())+ChatColor.GRAY+")");
        }
        
        this.send(cs,toSend.toArray(new String[toSend.size()]));
        
        return true;
    }
    
    private boolean _rank(CommandSender cs, String[] args) {
        if(!this.hasCommandPermission(cs, "lonelyeconomy.rank", "see rank of players")){ 
            return true;
        }
        
        if(args.length == 1)
        {
            cs.sendMessage("You are ranked "+ChatColor.GOLD+"#"+this.economy.getRank(cs.getName()));
        }
        else if(this.economy.hasAccount(args[1]))
        {
            cs.sendMessage(args[1]+ " is ranked "+ChatColor.GOLD+"#"+this.economy.getRank(args[1]));
        }
        else
        {
            cs.sendMessage(ChatColor.RED+args[1]+" not found");
        }
        
        return true;
    }

    private boolean _give(CommandSender cs, String[] args) {
        if(!this.hasCommandPermission(cs, "lonelyeconomy.give", "give users money"))
        {
            return true;
        }

        if(args.length < 3)
        {
            cs.sendMessage("/money give <username> <amount>");
            
            return true;
        }
        
        String payTo = args[1];
        
        BigDecimal amount;
        
        try {
            amount = this.economy.getBigDecimal(args[2]);
        }
        catch(NumberFormatException e) {
            cs.sendMessage(ChatColor.RED+"Invalid amount");
            
            return true;
        }
        
        LonelyEconomyResponse ler = this.economy.giveMoneyToPlayer(payTo, amount);
        
        if(!ler.wasSuccessful())
        {
            cs.sendMessage(ChatColor.RED+ler.getMessage());
            
            return true;
        }
        
        this.send(cs,"Give",payTo+" was paid "+amount.toPlainString()+" from the server account.");
        
        return true;
    }


    private boolean _take(CommandSender cs, String[] args) {
        if(!this.hasCommandPermission(cs, "lonelyeconomy.take", "take money from players"))
        {
            return true;
        }
        
        if(args.length < 3)
        {
            cs.sendMessage("/money take <username> <amount>");
            
            return true;
        }
        
        String playerTakeFrom = args[1];
        
        BigDecimal amount;
        
        try {
            amount = plugin.economy.getBigDecimal(args[2]);
        }
        catch(NumberFormatException e) {
            cs.sendMessage(ChatColor.RED+"Invalid amount");
            
            return true;
        }
        
        LonelyEconomyResponse ler = this.economy.takeMoneyFromPlayer(playerTakeFrom, amount);
        
        if(!ler.wasSuccessful())
        {
            cs.sendMessage(ChatColor.RED+ler.getMessage());
            
            return true;
        }
        
        this.send(cs,"Take", playerTakeFrom+" lost "+amount.toPlainString()+" which was given to the server account.");
        
        return true;
    }
    
    private void send(CommandSender cs, String... args) {
        cs.sendMessage(ChatColor.GOLD+"--- "+ChatColor.WHITE+args[0]+ChatColor.GOLD+" ---");

        for(int i=1;i<args.length;i++) {
            cs.sendMessage(args[i]);
        }

        cs.sendMessage("");
    }
}