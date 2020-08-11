package mca.command;

import java.util.Arrays;
import java.util.Optional;

import mca.api.objects.Player;
import mca.core.Constants;
import mca.core.MCA;
import mca.core.MCAServer;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

public class CommandMCA extends CommandBase {
    @Override
    public String getName() {
        return "mca";
    }

    @Override
    public String getUsage(ICommandSender commandSender) {
        return "/mca <subcommand> <arguments>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender commandSender, String[] input) throws CommandException {
        try {
            if (!MCA.getConfig().allowPlayerMarriage) {
                sendMessage(commandSender, "MCA commands have been disabled by the server administrator.");
                return;
            }

            if (input.length == 0) {
                throw new WrongUsageException("");
            }

            final Player player = new Player((EntityPlayer)commandSender);
            final MCAServer mcaServer = MCAServer.get();
            String subcommand = input[0].toLowerCase();
            String[] arguments = Arrays.copyOfRange(input, 1, input.length);
            
            MCA.getLog().info(player.getName() + " entered command " + Arrays.toString(input));

            Optional<Player> target = Optional.empty();
            if (arguments.length > 0) {
            	player.world.getPlayerEntityByName(arguments[0]);
            }
            
            switch (subcommand) {
                case "help":
                    displayHelp(commandSender);
                    break;
                case "propose":
                    if (target.isPresent()) {
                        mcaServer.sendProposal(player, target.get());
                    } else {
                        player.sendMessage("Player not found on the server.");
                    }
                    break;
                case "accept":
                    if (target.isPresent()) {
                        MCAServer.get().acceptProposal(player, target.get());
                    } else {
                        player.sendMessage("Player not found on the server.");
                    }
                    break;
                case "proposals":
                    MCAServer.get().listProposals(player);
                    break;
                case "procreate":
                    MCAServer.get().procreate(player);
                    break;
                case "separate":
                    MCAServer.get().endMarriage(player);
                    break;
                case "reject":
                    if (target.isPresent()) {
                        MCAServer.get().rejectProposal(player, target.get());
                    } else {
                        player.sendMessage("Player not found on the server.");
                    }
                    break;
                default:
                    throw new WrongUsageException("");
            }
        } catch (ClassCastException e) {
            throw new CommandException("MCA commands cannot be used through rcon.");
        } catch (WrongUsageException e) {
            throw new CommandException("Your command was invalid or improperly formatted. Usage: " + getUsage(commandSender));
        }
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    private void sendMessage(ICommandSender commandSender, String message) {
        commandSender.sendMessage(new TextComponentString(Constants.Color.GOLD + "[MCA] " + Constants.Format.RESET + message));
    }

    private void sendMessage(ICommandSender commandSender, String message, boolean noPrefix) {
        if (noPrefix) {
            commandSender.sendMessage(new TextComponentString(message));
        } else {
            sendMessage(commandSender, message);
        }
    }

    private void displayHelp(ICommandSender commandSender) {
        sendMessage(commandSender, Constants.Color.DARKRED + "--- " + Constants.Color.GOLD + "PLAYER COMMANDS" + Constants.Color.DARKRED + " ---", true);
        sendMessage(commandSender, Constants.Color.WHITE + " /mca propose <PlayerName>" + Constants.Color.GOLD + " - Proposes marriage to the given player.", true);
        sendMessage(commandSender, Constants.Color.WHITE + " /mca proposals " + Constants.Color.GOLD + " - Shows all active proposals.", true);
        sendMessage(commandSender, Constants.Color.WHITE + " /mca accept <PlayerName>" + Constants.Color.GOLD + " - Accepts the player's marriage request.", true);
        sendMessage(commandSender, Constants.Color.WHITE + " /mca reject <PlayerName>" + Constants.Color.GOLD + " - Rejects the player's marriage request.", true);
        sendMessage(commandSender, Constants.Color.WHITE + " /mca procreate " + Constants.Color.GOLD + " - Starts procreation.", true);
        sendMessage(commandSender, Constants.Color.WHITE + " /mca separate " + Constants.Color.GOLD + " - Ends your marriage.", true);
        sendMessage(commandSender, Constants.Color.DARKRED + "--- " + Constants.Color.GOLD + "GLOBAL COMMANDS" + Constants.Color.DARKRED + " ---", true);
        sendMessage(commandSender, Constants.Color.WHITE + " /mca help " + Constants.Color.GOLD + " - Shows this list of commands.", true);
    }
}
