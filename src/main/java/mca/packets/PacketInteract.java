package mca.packets;

import io.netty.buffer.ByteBuf;
import mca.ai.AIMood;
import mca.ai.AISleep;
import mca.core.minecraft.ModAchievements;
import mca.data.PlayerMemory;
import mca.entity.EntityHuman;
import mca.enums.EnumInteraction;
import mca.enums.EnumPersonality;
import mca.util.TutorialManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import radixcore.math.Point3D;
import radixcore.packets.AbstractPacket;
import radixcore.util.RadixLogic;
import radixcore.util.RadixMath;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class PacketInteract extends AbstractPacket implements IMessage, IMessageHandler<PacketInteract, IMessage>
{
	private int buttonId;
	private int entityId;
	
	public PacketInteract()
	{
	}

	public PacketInteract(int buttonId, int entityId)
	{
		this.buttonId = buttonId;
		this.entityId = entityId;
	}

	@Override
	public void fromBytes(ByteBuf byteBuf)
	{
		this.buttonId = byteBuf.readInt();
		this.entityId = byteBuf.readInt();
	}

	@Override
	public void toBytes(ByteBuf byteBuf)
	{
		byteBuf.writeInt(buttonId);
		byteBuf.writeInt(entityId);
	}

	@Override
	public IMessage onMessage(PacketInteract packet, MessageContext context)
	{
		EntityHuman villager = null;
		EntityPlayer player = null;
		
		for (WorldServer world : MinecraftServer.getServer().worldServers)
		{
			player = getPlayer(context);
			villager = (EntityHuman) world.getEntityByID(packet.entityId);

			if (player != null && villager != null)
			{
				break;
			}
		}
		
		if (player != null && villager != null)
		{
			EnumInteraction interaction = EnumInteraction.fromId(packet.buttonId);
			
			if (interaction == EnumInteraction.SET_HOME)
			{
				if (villager.getAI(AISleep.class).setHomePointWithVerify(new Point3D(villager.posX, villager.posY, villager.posZ)))
				{
					villager.say("interaction.sethome.success", player);
					TutorialManager.sendMessageToPlayer(player, "Villagers go to their home points at night, and then go to sleep.", "If their home point becomes blocked, they will automatically find a new one.");
				}

				else
				{
					villager.say("interaction.sethome.fail", player);
					TutorialManager.sendMessageToPlayer(player, "Move villagers away from the edges of walls", "and other blocks before setting their home.");
				}
			}
			
			else if (interaction == EnumInteraction.TRADE)
			{
				villager.setCustomer(player);
				player.displayGUIMerchant(villager, villager.getTitle(player));
			}
			
			else if (interaction == EnumInteraction.PICK_UP)
			{
				villager.mountEntity(player);
			}
			
			else if (interaction == EnumInteraction.TAKE_GIFT)
			{
				PlayerMemory memory = villager.getPlayerMemory(player);
				memory.setHasGift(false);
				
				//TODO Drop gift
			}
			
			else if (interaction == EnumInteraction.CHAT || interaction == EnumInteraction.JOKE || interaction == EnumInteraction.SHAKE_HAND ||
					 interaction == EnumInteraction.TELL_STORY || interaction == EnumInteraction.FLIRT || interaction == EnumInteraction.HUG ||
					 interaction == EnumInteraction.KISS)
			{
				AIMood mood = villager.getAI(AIMood.class);
				PlayerMemory memory = villager.getPlayerMemory(player);
				
				int successChance = interaction.getBaseChance() - memory.getInteractionFatigue() * 6
						+ villager.getPersonality().getSuccessModifierForInteraction(interaction) 
						+ mood.getMood(villager.getPersonality()).getSuccessModifierForInteraction(interaction)
						+ interaction.getBonusChanceForCurrentPoints(memory.getHearts());
				
				int pointsModification = interaction.getBasePoints()
						+ villager.getPersonality().getHeartsModifierForInteraction(interaction) 
						+ mood.getMood(villager.getPersonality()).getPointsModifierForInteraction(interaction);
				
				boolean wasGood = RadixLogic.getBooleanWithProbability(successChance);
				
				
				if (villager.getPersonality() == EnumPersonality.FRIENDLY)
				{
					pointsModification += pointsModification * 0.15D;
				}
				
				else if (villager.getPersonality() == EnumPersonality.FLIRTY)
				{
					pointsModification += pointsModification * 0.25D;
				}
				
				else if (villager.getPersonality() == EnumPersonality.SENSITIVE && RadixLogic.getBooleanWithProbability(5))
				{
					pointsModification = -35;
					wasGood = false;
				}
				
				else if (villager.getPersonality() == EnumPersonality.STUBBORN)
				{
					pointsModification -= pointsModification * 0.15D;
				}
				
				if (wasGood)
				{
					pointsModification = RadixMath.clamp(pointsModification, 1, 100);
					mood.modifyMoodLevel(RadixMath.getNumberInRange(0.2F, 1.0F));
					villager.say(memory.getDialogueType().toString() + "." + interaction.getName() + ".good", player);
				}
				
				else
				{
					pointsModification = RadixMath.clamp(pointsModification * -1, -100, -1);
					mood.modifyMoodLevel(RadixMath.getNumberInRange(0.2F, 1.0F) * -1);
					villager.say(memory.getDialogueType().toString() + "." + interaction.getName() + ".bad", player);
				}
				
				memory.setHearts(memory.getHearts() + pointsModification);
				memory.increaseInteractionFatigue();
				
				if (memory.getHearts() >= 100)
				{
					player.triggerAchievement(ModAchievements.fullGoldHearts);
				}
				
				if (memory.getInteractionFatigue() == 4)
				{
					TutorialManager.sendMessageToPlayer(player, "Vilagers tire of conversation after a few tries.", "Talk to them later for better success chances.");
				}
			}
			
			else if (interaction == EnumInteraction.STOP)
			{
				villager.getAIManager().disableAllToggleAIs();
			}
			
			else if (interaction == EnumInteraction.INVENTORY)
			{
				villager.openInventory(player);
			}
			
			else if (interaction == EnumInteraction.ACCEPT)
			{
				PlayerMemory memory = villager.getPlayerMemory(player);
				memory.setIsHiredBy(true, 3);
				
				for (int i = 0; i < 3; i++)
				{
					player.inventory.consumeInventoryItem(Items.gold_ingot);
				}
			}
		}
		
		return null;
	}
}
