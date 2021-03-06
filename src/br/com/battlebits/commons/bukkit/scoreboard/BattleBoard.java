package br.com.battlebits.commons.bukkit.scoreboard;

import java.util.Iterator;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import com.google.common.base.Splitter;

import lombok.Getter;

@Getter
public class BattleBoard {

	private Scoreboard scoreboard;
	
	private Objective sidebar;
	private Objective belowName;
	private Objective playerList;

	public BattleBoard(Player player) {
		Scoreboard playerScoreboard = player.getScoreboard();
		Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
		scoreboard = !playerScoreboard.equals(mainScoreboard) ? playerScoreboard : Bukkit.getScoreboardManager().getNewScoreboard();
		sidebar = scoreboard.registerNewObjective("sidebar", "dummy");
		sidebar.setDisplaySlot(DisplaySlot.SIDEBAR);
		player.setScoreboard(scoreboard);
	}

	public void setDisplayName(String displayName) {
		sidebar.setDisplayName(displayName);
	}

	public void setText(Row row, String text) {
		if (!text.isEmpty()) {

			Team team = scoreboard.getTeam(row.getTeam());
			if (team == null) {
				sidebar.getScore(row.getScore()).setScore(row.getId());
				team = scoreboard.registerNewTeam(row.getTeam());
				if (!team.hasEntry(row.getScore()))
					team.addEntry(row.getScore());
			}

			Iterator<String> iterator = Splitter.fixedLength(16).split(text).iterator();
			String prefix = iterator.next();
			if (prefix.endsWith("�")) {
				prefix = prefix.substring(0, prefix.length() - 1);
				team.setPrefix(prefix);
				if (iterator.hasNext()) {
					String next = iterator.next();
					if (!next.startsWith("�")) {
						String suffix = "�" + next;
						if (suffix.length() > 16)
							suffix = suffix.substring(0, 16);
						team.setSuffix(suffix);
					} else {
						team.setSuffix(next);
					}
				} else {
					team.setSuffix("");
				}
			} else if (iterator.hasNext()) {
				String next = iterator.next();
				if (!next.startsWith("�")) {
					String colors = ChatColor.getLastColors(prefix);
					String suffix = colors + next;
					if (suffix.length() > 16)
						suffix = suffix.substring(0, 16);
					team.setPrefix(prefix);
					team.setSuffix(suffix);
				} else {
					team.setPrefix(prefix);
					team.setSuffix(next);
				}
			} else {
				team.setPrefix(prefix);
				team.setSuffix("");
			}
		}
	}
	
	public boolean hasBelowName() {
		return belowName != null;
	}
	
	public boolean hasPlayerList() {
		return playerList != null;
	}
	
	public Objective registerPlayerList() {
		if (playerList == null) {
			playerList = scoreboard.registerNewObjective("playerList", "dummy");
			playerList.setDisplaySlot(DisplaySlot.PLAYER_LIST);
		}
		return playerList;
	}
	
	public Objective registerBelowName(String displayName) {
		if (belowName == null) {
			belowName = scoreboard.registerNewObjective("belowName", "dummy");
			belowName.setDisplaySlot(DisplaySlot.BELOW_NAME);
			belowName.setDisplayName(displayName);
		}
		return belowName;
	}

	public void setRows(Map<Integer, String> rows) {
		for (Row row : Row.values()) {
			if (rows.containsKey(row.getId())) {
				setText(row, rows.get(row.getId()));
			} else {
				unregister(row);
			}
		}
	}

	public void unregister(Row row) {
		scoreboard.resetScores(row.getScore());
		Team team = scoreboard.getTeam(row.getTeam());
		if (team != null)
			team.unregister();
	}

	public void unregisterAll() {
		sidebar.unregister();
	}

	public enum Row {

		ROW_01(1), ROW_02(2), ROW_03(3), ROW_04(4), ROW_05(5), ROW_06(6), ROW_07(7), ROW_08(8), ROW_09(9), ROW_10(
				10), ROW_11(11), ROW_12(12), ROW_13(13), ROW_14(14), ROW_15(15);

		@Getter
		private int id;

		Row(int id) {
			this.id = id;
		}

		public String getTeam() {
			return name().toLowerCase();
		}

		public String getScore() {
			ChatColor color = ChatColor.values()[ordinal()];
			return color.toString() + ChatColor.RESET;
		}
	}
}
