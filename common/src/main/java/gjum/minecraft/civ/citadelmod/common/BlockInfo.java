package gjum.minecraft.civ.citadelmod.common;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

public class BlockInfo {
	private final int x;
	private final int y;
	private final int z;

	/**
	 * null means unknown; empty string means known unreinforced
	 */
	@Nullable
	private String group;

	/**
	 * null means unknown; empty string means known unreinforced
	 */
	@Nullable
	private String reinforcement;

	/**
	 * number of breaks until destroyed, ignoring decay and maturation
	 */
	private int health;

	/**
	 * reinforcement type's max number of breaks until destroyed
	 */
	private int healthMax;

	/**
	 * milliseconds since UNIX epoch
	 */
	private long matureTs;

	/**
	 * milliseconds since UNIX epoch
	 */
	private long lastCheckedTs;

	public BlockInfo(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public BlockInfo setFromDb(String group, String reinforcement, int health, int healthMax, long matureTs, long lastCheckedTs) {
		this.group = group;
		this.reinforcement = reinforcement;
		this.health = health;
		this.healthMax = healthMax;
		this.matureTs = matureTs;
		this.lastCheckedTs = lastCheckedTs;
		return this;
	}

	public BlockInfo setFromCti(String group, String reinforcement, int health, int healthMax, long matureTs) {
		lastCheckedTs = System.currentTimeMillis();
		this.group = group;
		this.reinforcement = reinforcement;
		this.health = health;
		this.healthMax = healthMax;
		this.matureTs = matureTs;
		return this;
	}

	public BlockInfo setUnreinforced() {
		lastCheckedTs = System.currentTimeMillis();
		group = "";
		reinforcement = "";
		health = 0;
		healthMax = 0;
		matureTs = 0;
		return this;
	}

	public boolean isUnreinforced() {
		return health == 0 && "".equals(group);
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getZ() {
		return z;
	}

	/**
	 * milliseconds since UNIX epoch
	 */
	public long getLastCheckedTs() {
		return lastCheckedTs;
	}

	public String getGroup() {
		return group;
	}

	public BlockInfo setGroup(String group) {
		lastCheckedTs = System.currentTimeMillis();
		this.group = group;
		return this;
	}

	public String getReinforcement() {
		return reinforcement;
	}

	public BlockInfo setReinforcement(String reinforcement) {
		lastCheckedTs = System.currentTimeMillis();
		this.reinforcement = reinforcement;
		return this;
	}

	/**
	 * number of breaks until destroyed, ignoring decay and maturation
	 */
	public int getHealth() {
		return health;
	}

	/**
	 * number of breaks until destroyed, ignoring decay and maturation
	 */
	public BlockInfo setHealth(int health) {
		lastCheckedTs = System.currentTimeMillis();
		this.health = health;
		return this;
	}

	public int getHealthMax() {
		return healthMax;
	}

	/**
	 * reinforcement type's max number of breaks until destroyed
	 */
	public BlockInfo setHealthMax(int healthMax) {
		lastCheckedTs = System.currentTimeMillis();
		this.healthMax = healthMax;
		return this;
	}

	/**
	 * milliseconds since UNIX epoch
	 */
	public long getMatureTs() {
		return matureTs;
	}

	/**
	 * milliseconds since UNIX epoch
	 */
	public BlockInfo setMatureTs(long matureTs) {
		lastCheckedTs = System.currentTimeMillis();
		this.matureTs = matureTs;
		return this;
	}

	public BlockPos getBlockPos() {
		return new BlockPos(getX(), getY(), getZ());
	}
}
