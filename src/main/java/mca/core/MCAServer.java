package mca.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import mca.api.objects.NPC;
import mca.api.objects.Player;
import mca.api.objects.Pos;
import mca.api.wrappers.WorldWrapper;
import mca.core.minecraft.ItemsMCA;
import mca.core.minecraft.VillageHelper;
import mca.entity.EntityGrimReaper;
import mca.entity.EntityVillagerMCA;
import mca.entity.data.PlayerSaveData;
import net.minecraft.item.ItemStack;

public class MCAServer {
    private static MCAServer instance;
    // Maps a player's UUID to a list of UUIDs that have proposed to them with /mca propose
    private static Map<UUID, List<UUID>> proposals;
    // List of UUIDs that initiated procreation mapped to the time the request expires.
    private static Map<UUID, Long> procreateMap;
    private int serverTicks = 0;
    private int reaperSummonTicks = 0;
    private Pos reaperSpawnPos = Pos.ORIGIN;
    private WorldWrapper reaperSpawnWorld = null;

    private MCAServer() {
        proposals = new HashMap<>();
        procreateMap = new HashMap<>();
    }

    public static MCAServer get() {
        if (instance == null) {
            instance = new MCAServer();
        }
        return instance;
    }

    public void tick() {
        serverTicks++;

        if (serverTicks >= 100) {
            VillageHelper.tick(WorldWrapper.getOverworld());
            serverTicks = 0;
        }

        if (reaperSummonTicks > 0) {
            reaperSummonTicks--;
            if (reaperSummonTicks % 20 == 0) { // every second
                reaperSpawnWorld.spawnLightningBolt(reaperSpawnPos.getX(), reaperSpawnPos.getY(), reaperSpawnPos.getZ());
            }

            if (reaperSummonTicks == 0) { // when counter reaches 0
                EntityGrimReaper reaper = new EntityGrimReaper(reaperSpawnWorld.getVanillaWorld());
                reaper.setPosition(reaperSpawnPos.getX(), reaperSpawnPos.getY(), reaperSpawnPos.getZ());
                reaperSpawnWorld.spawnEntity(reaper);
            }
        }

        // Collect all expired procreate requests and remove them.
        List<UUID> removals = new ArrayList<>();
        procreateMap.keySet().stream()
                .filter((k) -> procreateMap.get(k) < System.currentTimeMillis())
                .forEach(removals::add);
        removals.forEach(procreateMap::remove);
    }

    /**
     * Returns true if receiver has a proposal from sender.
     *
     * @param sender   Command sender
     * @param receiver Player whose name was entered by the sender
     * @return boolean
     */
    private boolean hasProposalFrom(Player sender, Player receiver) {
        return getProposalsFor(receiver).contains(sender.getUniqueID());
    }

    /**
     * Returns all proposals for the provided player
     *
     * @param player Player whose proposals should be returned.
     * @return List<UUID>
     */
    private List<UUID> getProposalsFor(Player player) {
        return proposals.getOrDefault(player.getUniqueID(), new ArrayList<>());
    }

    /**
     * Removes the provided proposer from the target's list of proposals.
     *
     * @param target   Target player who's proposal list will be modified.
     * @param proposer The proposer to the target player.
     */
    private void removeProposalFor(Player target, Player proposer) {
        List<UUID> list = getProposalsFor(target);
        list.remove(proposer.getUniqueID());
        proposals.put(target.getUniqueID(), list);
    }

    /**
     * Lists all proposals for the given player.
     *
     * @param sender Player whose active proposals will be listed.
     */
    public void listProposals(Player sender) {
        List<UUID> proposals = getProposalsFor(sender);

        if (proposals.size() == 0) {
            infoMessage(sender, "You have no active proposals.");
        } else {
            infoMessage(sender, "You have active proposals from: ");
        }

        // Send the name of all online players to the command sender.
        proposals.forEach((uuid -> {
            Optional<Player> player = sender.world.getPlayerEntityByUUID(uuid);
            player.ifPresent(p -> infoMessage(sender, "- " + p.getName()));
        }));
    }

    /**
     * Sends a proposal from the sender to the receiver.
     *
     * @param sender   The player sending the proposal.
     * @param receiver The player being proposed to.
     */
    public void sendProposal(Player sender, Player receiver) {
        // Ensure the sender isn't already married.
        if (PlayerSaveData.get(sender).isMarriedOrEngaged()) {
            failMessage(sender, "You cannot send a proposal since you are already married or engaged.");
            return;
        }

        // Ensure the sender isn't himself.
        if (sender == receiver) {
            failMessage(sender, "You cannot propose to yourself.");
            return;
        }

        // Ensure the receiver hasn't already been proposed to by this player.
        if (hasProposalFrom(sender, receiver)) {
            failMessage(sender, "You have already sent a proposal to " + receiver.getName());
        } else {
            // Send the proposal messages.
            successMessage(sender, "Your proposal to " + receiver.getName() + " has been sent!");
            infoMessage(receiver, sender.getName() + " has proposed marriage. To accept, type /mca accept " + sender.getName());

            // Add the proposal to the receiver's proposal list.
            List<UUID> list = getProposalsFor(receiver);
            list.add(sender.getUniqueID());
            proposals.put(receiver.getUniqueID(), list);
        }
    }

    /**
     * Rejects and removes a proposal from the receiver to the sender.
     *
     * @param sender   The person rejecting the proposal.
     * @param receiver The initial proposer.
     */
    public void rejectProposal(Player sender, Player receiver) {
        // Ensure a proposal existed.
        if (!hasProposalFrom(receiver, sender)) {
            failMessage(sender, receiver.getName() + " hasn't proposed to you.");
        } else {
            // Notify of the proposal failure and remove it.
            successMessage(sender, "Your rejection has been sent.");
            failMessage(receiver, sender.getName() + " rejected your proposal.");
            removeProposalFor(sender, receiver);
        }
    }

    /**
     * Accepts and removes a proposal from the receiver to the sender.
     *
     * @param sender   The person accepting the proposal.
     * @param receiver The initial proposer.
     */
    public void acceptProposal(Player sender, Player receiver) {
        // Ensure a proposal is active.
        if (!hasProposalFrom(receiver, sender)) {
            failMessage(sender, receiver.getName() + " hasn't proposed to you.");
        } else {
            // Notify of acceptance.
            successMessage(receiver, sender.getName() + " has accepted your proposal!");

            // Set both player datas as married.
            PlayerSaveData senderData = PlayerSaveData.get(sender);
            PlayerSaveData receiverData = PlayerSaveData.get(receiver);
            senderData.marry(receiver.getUniqueID(), receiver.getName());
            receiverData.marry(sender.getUniqueID(), sender.getName());

            // Send success messages.
            successMessage(sender, "You and " + receiver.getName() + " are now married.");
            successMessage(receiver, "You and " + sender.getName() + " are now married.");

            // Remove the proposal.
            removeProposalFor(sender, receiver);
        }
    }

    /**
     * Ends the sender's marriage and notifies their spouse if the spouse is online.
     *
     * @param sender The person ending their marriage.
     */
    public void endMarriage(Player sender) {
        // Retrieve all data instances and an instance of the ex-spouse if they are present.
        PlayerSaveData senderData = PlayerSaveData.get(sender);

        // Ensure the sender is married
        if (!senderData.isMarriedOrEngaged()) {
            failMessage(sender, "You are not married.");
            return;
        }

        // Lookup the spouse, if it's a villager, we can't continue
        Optional<NPC> spouse = sender.world.getNPCByUUID(senderData.getSpouseUUID());
        if (spouse.isPresent() && spouse.get().getEntity() instanceof EntityVillagerMCA) {
            failMessage(sender, "You cannot use this command when married to a villager.");
            return;
        }

        PlayerSaveData receiverData = PlayerSaveData.getExisting(sender.world, senderData.getSpouseUUID());

        // Notify the sender of the success and end both marriages.
        successMessage(sender, "Your marriage to " + senderData.getSpouseName() + " has ended.");
        senderData.endMarriage();
        receiverData.endMarriage();

        // Notify the ex if they are online.
        spouse.ifPresent(e -> failMessage((Player) e, sender.getName() + " has ended their marriage with you."));
    }

    /**
     * Initiates procreation with a married player.
     *
     * @param sender The person requesting procreation.
     */
    public void procreate(Player sender) {
        // Ensure the sender is married.
        PlayerSaveData senderData = PlayerSaveData.get(sender);
        if (!senderData.isMarriedOrEngaged()) {
            failMessage(sender, "You cannot procreate if you are not married.");
            return;
        }

        // Ensure we don't already have a baby
        if (senderData.isBabyPresent()) {
            failMessage(sender, "You already have a baby.");
            return;
        }

        // Ensure the spouse is online.
        Optional<Player> spouse = sender.world.getPlayerEntityByUUID(senderData.getSpouseUUID());
        spouse.ifPresent(s -> {
            // If the spouse is online and has previously sent a procreation request that hasn't expired, we can continue.
            // Otherwise we notify the spouse that they must also enter the command.
        	 if (!procreateMap.containsKey(s.getUniqueID())) {
                 procreateMap.put(sender.getUniqueID(), System.currentTimeMillis() + 10000);
                 infoMessage(s, sender.getName() + " has requested procreation. To accept, type /mca procreate within 10 seconds.");
             } else {
                 // On success, add a randomly generated baby to the original requester.
                 successMessage(sender, "Procreation successful!");
                 successMessage(s, "Procreation successful!");
                 s.addItemStackToInventory(new ItemStack(sender.world.rand.nextBoolean() ? ItemsMCA.BABY_BOY : ItemsMCA.BABY_GIRL));

                 PlayerSaveData spouseData = PlayerSaveData.get(s);
                 spouseData.setBabyPresent(true);
                 senderData.setBabyPresent(true);
             }
        });
        
        if (!spouse.isPresent()) {
        	failMessage(sender, "Your spouse is not present on the server.");	
        }
    }

    private void successMessage(Player player, String message) {
        player.sendMessage(Constants.Color.GREEN + message);
    }

    private void failMessage(Player player, String message) {
        player.sendMessage(Constants.Color.RED + message);
    }

    private void infoMessage(Player player, String message) {
        player.sendMessage(Constants.Color.YELLOW + message);
    }

    public void setReaperSpawnPos(WorldWrapper world, Pos pos) {
        this.reaperSpawnWorld = world;
        this.reaperSpawnPos = pos;
    }

    public void startSpawnReaper() {
        this.reaperSummonTicks = 20 * 4; // 3 seconds
    }
}
