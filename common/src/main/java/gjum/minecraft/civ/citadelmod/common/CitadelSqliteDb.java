package gjum.minecraft.civ.citadelmod.common;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.*;
import java.util.*;

public class CitadelSqliteDb {
	public final String server;

	@Nullable
	private Connection conn;

	private HashMap<BlockPos, BlockInfo> blockInfoByPos = new HashMap<>();

	public CitadelSqliteDb(String server) {
		this.server = server;
		new File("CitadelMod/" + server).mkdirs();
		try {
			Class.forName("org.sqlite.JDBC"); // load driver
			conn = DriverManager.getConnection("jdbc:sqlite:CitadelMod/" + server + "/block_info.sqlite");
			createTableBlockInfo();
		} catch (Exception e) {
			e.printStackTrace();
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException eClose) {
					eClose.printStackTrace();
				}
			}
			conn = null;
		}
	}

	synchronized
	public void close() {
		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	private static final String pkeyBlocks = "x,y,z,world";

	synchronized
	private void createTableBlockInfo() {
		if (conn == null) return;
		String sql = "CREATE TABLE IF NOT EXISTS block_info" +
				"( x INT" +
				", y INT" +
				", z INT" +
				", world TEXT" +
				", group TEXT" +
				", reinforcement TEXT" +
				", health INTEGER" +
				", healthMax INTEGER" +
				", mature_ts BIGINT" +
				", last_checked_ts BIGINT" +
				" PRIMARY KEY (" + pkeyBlocks + "));";
		try (Statement stmt = conn.createStatement()) {
			stmt.execute(sql);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	synchronized
	public Collection<BlockInfo> selectAllBlockInfos() {
		final ArrayList<BlockInfo> blockInfos = new ArrayList<>();
		if (conn == null) return blockInfoByPos.values();
		try (Statement stmt = conn.createStatement()) {
			ResultSet rs = stmt.executeQuery("SELECT * FROM block_info");
			while (rs.next()) {
				final int x = rs.getInt("x");
				final int y = rs.getInt("y");
				final int z = rs.getInt("z");
				// final String world = rs.getString("world"); // TODO world
				final String group = rs.getString("group");
				final String reinforcement = rs.getString("reinforcement");
				final int health = rs.getInt("health");
				final int healthMax = rs.getInt("healthMax");
				final long mature_ts = rs.getLong("mature_ts");
				final long last_checked_ts = rs.getLong("last_checked_ts");

				final BlockInfo info = new BlockInfo(x, y, z);
				info.setFromDb(group, reinforcement, health, healthMax, mature_ts, last_checked_ts);

				blockInfos.add(info);

				blockInfoByPos.put(info.getBlockPos(), info);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return blockInfos;
	}

	synchronized
	public void upsertBlockInfo(BlockInfo info) {
		blockInfoByPos.put(info.getBlockPos(), info);

		if (conn == null) return;
		String sql = "INSERT INTO block_info (x,y,z,world,group,reinforcement,health,healthMax,mature_ts,last_checked_ts)" +
				" VALUES (?,?,?,?,?,?,?,?,?,?)" +
				"ON CONFLICT (" + pkeyBlocks + ") DO UPDATE SET " +
				"group = excluded.group," +
				"reinforcement = excluded.reinforcement," +
				"health = excluded.health," +
				"healthMax = excluded.healthMax," +
				"mature_ts = excluded.mature_ts," +
				"last_checked_ts = excluded.last_checked_ts";
		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
			int i = 0;
			pstmt.setInt(++i, info.getX());
			pstmt.setInt(++i, info.getY());
			pstmt.setInt(++i, info.getZ());
			pstmt.setString(++i, "?"); // TODO world
			pstmt.setString(++i, info.getGroup());
			pstmt.setString(++i, info.getReinforcement());
			pstmt.setInt(++i, info.getHealth());
			pstmt.setInt(++i, info.getHealthMax());
			pstmt.setLong(++i, info.getMatureTs());
			pstmt.setLong(++i, info.getLastCheckedTs());

			pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	// delete in placeholder "?" world for now TODO properly do world separation
	public void deleteBlockInfo(BlockPos pos) {
		deleteBlockInfo(pos, "?");
	}

	synchronized
	public void deleteBlockInfo(BlockPos pos, String world) {
		blockInfoByPos.remove(pos);

		if (conn == null) return;
		String sql = "DELETE FROM block_info WHERE x = ? AND y = ? AND z = ? AND world = ?";
		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
			int i = 0;
			pstmt.setInt(++i, pos.getX());
			pstmt.setInt(++i, pos.getY());
			pstmt.setInt(++i, pos.getZ());
			pstmt.setString(++i, world);

			pstmt.executeQuery();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Nullable
	synchronized
	public BlockInfo getBlockInfo(BlockPos pos) {
		return blockInfoByPos.get(pos);
	}
}
