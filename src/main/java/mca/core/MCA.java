package mca.core;

import com.google.gson.Gson;
import lombok.Getter;
import mca.api.API;
import mca.command.CommandAdminMCA;
import mca.command.CommandMCA;
import mca.core.forge.EventHooks;
import mca.core.forge.GuiHandler;
import mca.core.forge.NetMCA;
import mca.core.forge.ServerProxy;
import mca.core.minecraft.ItemsMCA;
import mca.core.minecraft.MCACreativeTab;
import mca.core.minecraft.ProfessionsMCA;
import mca.core.minecraft.RoseGoldOreGenerator;
import mca.entity.EntityGrimReaper;
import mca.entity.EntityVillagerMCA;
import mca.enums.EnumGender;
import mca.util.Util;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Mod(modid = MCA.MODID, name = MCA.NAME, version = MCA.VERSION, guiFactory = "mca.client.MCAGuiFactory")
public class MCA {
    public static final String MODID = "mca";
    public static final String NAME = "Minecraft Comes Alive";
    public static final String VERSION = "6.1.0";
    @SidedProxy(clientSide = "mca.core.forge.ClientProxy", serverSide = "mca.core.forge.ServerProxy")
    public static ServerProxy proxy;
    public static CreativeTabs creativeTab;
    @Getter @Mod.Instance private static MCA instance;
    @Getter private static Logger logger;
    @Getter private static Localizer localizer;
    @Getter private static Config config;
    private static long startupTimestamp;
    public static String latestVersion = "";
    public static boolean updateAvailable = false;
    public List<String> supporters = new ArrayList<>();

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        startupTimestamp = new Date().getTime();
        instance = this;
        logger = event.getModLog();
        proxy.registerEntityRenderers();
        localizer = new Localizer();
        config = new Config(event);
        creativeTab = new MCACreativeTab();

        MinecraftForge.EVENT_BUS.register(new EventHooks());
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());
        NetMCA.registerMessages();

        if (MCA.getConfig().isAllowUpdateChecking()) {
            latestVersion = Util.httpGet("https://minecraftcomesalive.com/api/latest");
            if (!latestVersion.equals(VERSION) && !latestVersion.equals("")) {
                updateAvailable = true;
                MCA.getLogger().warn("An update for Minecraft Comes Alive is available: v" + latestVersion);
            }
        }

        supporters = Arrays.asList(Util.httpGet("https://minecraftcomesalive.com/api/supporters").split(","));
        MCA.getLogger().info("Loaded " + supporters.size() + " supporters.");
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        GameRegistry.registerWorldGenerator(new RoseGoldOreGenerator(), MCA.getConfig().getRoseGoldSpawnWeight());
        EntityRegistry.registerModEntity(new ResourceLocation(MODID, "EntityVillagerMCA"), EntityVillagerMCA.class, EntityVillagerMCA.class.getSimpleName(), 1120, this, 50, 2, true);
        EntityRegistry.registerModEntity(new ResourceLocation(MODID, "GrimReaperMCA"), EntityGrimReaper.class, EntityGrimReaper.class.getSimpleName(), 1121, this, 50, 2, true);
        ProfessionsMCA.registerCareers();

        proxy.registerModelMeshers();
        ItemsMCA.assignCreativeTabs();
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        API.init(getClass());
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandMCA());
        event.registerServerCommand(new CommandAdminMCA());
    }

    @EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        checkForCrashReports();
    }

    public String getRandomSupporter() {
        return supporters.size() > 0 ? supporters.get(new Random().nextInt(supporters.size())) : API.getRandomName(EnumGender.getRandom());
    }

    public void checkForCrashReports() {
        if (MCA.getConfig().isAllowCrashReporting()) {
            File crashReportsFolder = new File(System.getProperty("user.dir") + "/crash-reports/");
            File[] crashReportFiles = crashReportsFolder.listFiles(File::isFile);
            try {
                if (crashReportFiles != null) {
                    Optional<File> newestFile = Arrays.stream(crashReportFiles).max(Comparator.comparingLong(File::lastModified));
                    if (newestFile.isPresent() && newestFile.get().lastModified() > startupTimestamp) {
                        // Raw Java for sending the POST request as the HttpClient from Apache libs is not present on servers.
                        MCA.getLogger().warn("Crash detected! Attempting to upload report...");
                        Map<String, String> payload = new HashMap<>();
                        payload.put("minecraft_version", FMLCommonHandler.instance().getMinecraftServerInstance().getMinecraftVersion());
                        payload.put("operating_system", System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ") version " + System.getProperty("os.version"));
                        payload.put("java_version", System.getProperty("java.version") + ", " + System.getProperty("java.vendor"));
                        payload.put("mod_version", MCA.VERSION);
                        payload.put("body", FileUtils.readFileToString(newestFile.get(), "UTF-8"));

                        byte[] out = new Gson().toJson(payload).getBytes(StandardCharsets.UTF_8);
                        URL url = new URL("http://minecraftcomesalive.com/api/crash-reports");
                        URLConnection con = url.openConnection();
                        HttpURLConnection http = (HttpURLConnection)con;
                        http.setRequestMethod("POST");
                        http.setDoOutput(true);
                        http.setFixedLengthStreamingMode(out.length);
                        http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                        http.setRequestProperty("User-Agent", "Minecraft Client " + FMLCommonHandler.instance().getMinecraftServerInstance().getMinecraftVersion());
                        http.connect();
                        OutputStream os = http.getOutputStream();
                        os.write(out);
                        os.flush();
                        os.close();
                        if (http.getResponseCode() != 200) MCA.getLogger().error("Failed to submit crash report. Non-OK response code returned: " + http.getResponseCode());
                        else MCA.getLogger().warn("Crash report submitted successfully.");
                    }
                }
            } catch (IOException e) {
                MCA.getLogger().error("An unexpected error occurred while attempting to submit the crash report.", e);
            }
        }
    }
}
