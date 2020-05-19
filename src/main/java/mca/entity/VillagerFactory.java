package mca.entity;

import mca.api.API;
import mca.api.wrappers.WorldWrapper;
import mca.core.minecraft.ProfessionsMCA;
import mca.entity.data.ParentData;
import mca.enums.EnumGender;
import net.minecraftforge.fml.common.registry.VillagerRegistry.VillagerProfession;

public class VillagerFactory {
	private EntityVillagerMCA villager;
	private boolean isNameSet;
	private boolean isProfessionSet;
	private boolean isTextureSet;
	private boolean isGenderSet;
	private boolean isCareerSet;
	
	private VillagerFactory(WorldWrapper world) {
		this.villager = new EntityVillagerMCA(world.getVanillaWorld());
	}
	
	public static VillagerFactory newVillager(WorldWrapper world) {
		return new VillagerFactory(world);
	}
		
	public VillagerFactory withGender(EnumGender gender) {
		villager.set(EntityVillagerMCA.GENDER, gender.getId());
		isGenderSet = true;
		return this;
	}
	
	public VillagerFactory withProfession(VillagerProfession prof) {
		villager.setProfession(prof);
		isProfessionSet = true;
		return this;
	}
	
	public VillagerFactory withName(String name) {
		villager.set(EntityVillagerMCA.VILLAGER_NAME, name);
		isNameSet = true;
		return this;
	}
	
	public VillagerFactory withParents(ParentData parents) {
		villager.set(EntityVillagerMCA.PARENTS, parents.toVanillaNBT());
		return this;
	}
	
	public VillagerFactory withCareer(int careerId) {
		villager.setVanillaCareer(careerId);
		isCareerSet = true;
		return this;
	}
	
	public VillagerFactory withPosition(double posX, double posY, double posZ) {
		villager.setPosition(posX, posY, posZ);
		return this;
	}
	
	public EntityVillagerMCA build() {
		if (!isGenderSet) {
			villager.set(EntityVillagerMCA.GENDER, EnumGender.getRandom().getId());
		}
		
		if (!isNameSet) {
			villager.set(EntityVillagerMCA.VILLAGER_NAME, API.getRandomName(EnumGender.byId(villager.get(EntityVillagerMCA.GENDER))));
		}
		
		if (!isProfessionSet) {
			villager.setProfession(ProfessionsMCA.randomProfession());
		}
		
		if (!isTextureSet) {
			villager.set(EntityVillagerMCA.TEXTURE, API.getRandomSkin(villager));
		}

		if (!isCareerSet) {
			villager.setVanillaCareer(villager.getProfessionForge().getRandomCareer(villager.world.rand));
		}
		
		return villager;
	}
}
