package gjum.minecraft.civ.citadelmod.common;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;

public class ChatParser {
	static Pattern reinforcementInfoPattern = Pattern.compile(
			"^Reinforced at (?<healthPct>[-0-9.]+)%" +
					" \\((?<healthAbs>[-0-9.]+)" +
					"/(?<healthMax>[-0-9.]+)\\) health" +
					" with (?<reinforcement>\\S+)" +
					" on (?<group>\\S+)" +
					"(?: " +
					"(?<maturePct>[-0-9.]+)% mature" +
					"(?: (?<matureHr>[0-9]+) h)?" +
					"(?: (?<matureMin>[0-9]+) min)?" +
					"(?: (?<matureSec>[0-9]+) sec)?" +
					")?" +
					".*");
	// \\(Insecure\\) \\(Decayed x?(?<decayed>[-0-9.]+)%?\\) Acid ready Acid block mature in (?<timeLeft>[-0-9.]+).*/

	static Pattern unreinforcedInfoPattern = Pattern.compile(
			"^(?:This block is not|Not) reinforced.*");

	static Pattern lockedInfoPattern = Pattern.compile(
			"^(?<block>\\S+) is locked with (?<reinforcement>\\S+)");

	static Pattern reinforcementGroupUpdatedPattern = Pattern.compile(
			"^Updated group to (?<group>\\S+)");

	static Pattern reinforcementUpdatedPattern = Pattern.compile(
			"^Updated reinforcement to (?<reinforcement>\\S+) on (?<group>\\S+)");

	static Pattern alreadyRepairedPattern = Pattern.compile(
			"^Reinforcement is already at (?<healthPct>[-0-9.]+)% \\((?<healthAbs>[-0-9.]+)/(?<healthMax>[-0-9.]+)\\) health.?(?: with (?<reinforcement>\\S+) on (?<group>\\S+))?");

	static Pattern noBypassPermPattern = Pattern.compile(
			"^You do not have permission to bypass reinforcements on (?<group>\\S+)");

	@Nullable
	public static BlockInfo parseChat(Component message, CitadelMod mod) {
		HoverEvent hoverEvent = message.getStyle().getHoverEvent();
		if (hoverEvent == null) return null; // need coords in hover
		Component hoverText = hoverEvent.getValue(HoverEvent.Action.SHOW_TEXT);
		if (hoverText == null) return null;
		String[] locationSplit = hoverText.getString().split(" ");
		if (locationSplit.length != 4) return null;
		if (!"Location:".equals(locationSplit[0])) return null;
		int x, y, z;
		try {
			x = parseInt(locationSplit[1]);
			y = parseInt(locationSplit[2]);
			z = parseInt(locationSplit[3]);
		} catch (NumberFormatException e) {
			return null;
		}

		BlockInfo info = mod.getBlockInfo(x, y, z);
		if (info == null) info = new BlockInfo(x, y, z);

		String text = message.getString().replaceAll("§.", "");
		Matcher m;

		m = reinforcementInfoPattern.matcher(text);
		if (m.matches()) {
			int health = parseInt(m.group("healthAbs"));
			int healthMax = parseInt(m.group("healthMax"));
			String reinf = m.group("reinforcement");
			String group = m.group("group");

			final long matureMs = 1000L * (
					3600L * parseLongOrZero(m.group("matureHr"), 0) +
							60L * parseLongOrZero(m.group("matureMin"), 0) +
							parseLongOrZero(m.group("matureSec"), 0));
			long matureTs = System.currentTimeMillis() + matureMs;

			return info.setFromCti(group, reinf, health, healthMax, matureTs);
		}

		m = unreinforcedInfoPattern.matcher(text);
		if (m.matches()) {
			return info.setUnreinforced();
		}

		m = lockedInfoPattern.matcher(text);
		if (m.matches()) {
			return info.setReinforcement(m.group("reinforcement"));
		}

		m = reinforcementGroupUpdatedPattern.matcher(text);
		if (m.matches()) {
			return info.setGroup(m.group("group"));
		}

		m = reinforcementUpdatedPattern.matcher(text);
		if (m.matches()) {
			info.setReinforcement(m.group("reinforcement"));
			info.setGroup(m.group("group"));
			return info;
		}

		m = alreadyRepairedPattern.matcher(text);
		if (m.matches()) {
			info.setHealth(parseInt(m.group("healthAbs")));
			info.setHealthMax(parseInt(m.group("healthMax")));
			info.setReinforcement(m.group("reinforcement"));
			info.setGroup(m.group("group"));
			return info;
		}

		m = noBypassPermPattern.matcher(text);
		if (m.matches()) {
			return info.setGroup(m.group("group"));
		}

		return null; // unknown message
	}

	private static long parseLongOrZero(String s, long fallback) {
		if (s != null) return parseLong(s);
		else return fallback;
	}
}
