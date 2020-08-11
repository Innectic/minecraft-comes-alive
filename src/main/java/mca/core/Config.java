package mca.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.config.IConfigElement;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public final class Config implements Serializable {
    @Getter private transient final Configuration config;

    @Getter private boolean overwriteOriginalVillagers;
    @Getter private boolean enableDiminishingReturns;
    @Getter private boolean enableInfection;
    @Getter private int infectionChance;
    @Getter private boolean allowGrimReaper;
    @Getter private int guardSpawnRate;
    @Getter private int chanceToHaveTwins;
    @Getter private int marriageHeartsRequirement;
    @Getter private int roseGoldSpawnWeight;
    @Getter private int babyGrowUpTime;
    @Getter private int childGrowUpTime;
    @Getter private int villagerSpawnerCap;
    @Getter private int villagerSpawnerRateMinutes;
    @Getter private int villagerMaxHealth;
    @Getter private boolean allowTrading;
    @Getter private boolean logVillagerDeaths;
    @Getter private boolean enableRevivals;
    @Getter private String villagerChatPrefix;
    @Getter private boolean allowPlayerMarriage;
    @Getter private boolean enableAdminCommands;
    @Getter private boolean allowCrashReporting;
    @Getter private boolean allowUpdateChecking;
    @Getter private boolean allowRoseGoldGeneration;

    public Config(FMLPreInitializationEvent event) {
        config = new Configuration(event.getSuggestedConfigurationFile());
        addConfigValues();
    }

    private void addConfigValues() {
        overwriteOriginalVillagers = config.get("General", "Overwrite Original Villagers?", true, "Should original villagers be overwritten by MCA villagers?").getBoolean();
        enableDiminishingReturns = config.get("General", "Enable Interaction Fatigue?", true, "Should interactions yield diminishing returns over time?").getBoolean();
        enableInfection = config.get("General", "Enable Zombie Infection?", true, "Should zombies be able to infect villagers?").getBoolean();
        infectionChance = config.get("General", "Chance of Infection", 5, "Chance that a villager will be infected on hit from a zombie. Default is 5 for 5%.").getInt();
        allowGrimReaper = config.get("General", "Allow Grim Reaper?", true, "Should the Grim Reaper boss be enabled?").getBoolean();
        guardSpawnRate = config.get("General", "Guard Spawn Rate", 6, "How many villagers that should be in a village before a guard spawns.").getInt();
        chanceToHaveTwins = config.get("General", "Chance to Have Twins", 2, "Chance that you will have twins. Default is 2 for 2%.").getInt();
        marriageHeartsRequirement = config.get("General", "Marriage Hearts Requirement", 100, "Number of hearts required to get married.").getInt();
        roseGoldSpawnWeight = config.get("General", "Rose Gold Spawn Weight", 6, "Spawn weights for Rose Gold").getInt();
        babyGrowUpTime = config.get("General", "Baby Grow Up Time (Minutes)", 30, "Minutes it takes for a baby to be ready to grow up.").getInt();
        childGrowUpTime = config.get("General", "Child Grow Up Time (Minutes)", 60, "Minutes it takes for a child to grow into an adult.").getInt();
        villagerSpawnerCap = config.get("General", "Villager Spawner Cap", 5, "Maximum number of villagers that a spawner will create in the area before it stops.").getInt();
        villagerSpawnerRateMinutes = config.get("General", "Villager Spawner Rate", 30, "The spawner will spawn 1 villager per this many minutes.").getInt();
        allowTrading = config.get("General", "Enable Trading?", true, "Is trading with villagers enabled?").getBoolean();
        logVillagerDeaths = config.get("General", "Log Villager Deaths?", true, "Should villager deaths be logged?").getBoolean();
        enableRevivals = config.get("General", "Enable Revivals?", true, "Should reviving dead villagers be enabled?").getBoolean();
        villagerChatPrefix = config.get("General", "Villager Chat Prefix", "", "Formatting prefix used for all chat with villagers.").getString();
        allowPlayerMarriage = config.get("General", "Allow Player Marriage?", true, "Enables or disables player marriage.").getBoolean();
        enableAdminCommands = config.get("General", "Enable Admin Commands?", true, "Enables or disables MCA admin commands for ops.").getBoolean();
        allowCrashReporting = config.get("General", "Allow Crash Reporting?", true, "If enabled, sends crash reports to MCA developers.").getBoolean();
        allowUpdateChecking = config.get("General", "Allow Update Checking?", true, "If enabled, notifies you when an update to MCA is available.").getBoolean();
        allowRoseGoldGeneration = config.get("General", "Allow Rose Gold World Generation", true, "If enabled, generates rose gold in your world. If disabled, generates stone instead.").getBoolean();
        villagerMaxHealth = config.get("General", "Villager Max Health", 20, "Each villager's maximum health. 1 point equals 1 heart.").getInt();
        config.save();
    }

    public List<IConfigElement> getCategories() {
        List<IConfigElement> elements = new ArrayList<>();

        for (String s : config.getCategoryNames()) {
            if (s.equals("server")) continue;

            IConfigElement element = new ConfigElement(config.getCategory(s));
            elements.addAll(element.getChildElements());
        }

        return elements;
    }
}