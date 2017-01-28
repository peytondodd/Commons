package br.com.battlebits.commons.bukkit.listener;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;

import br.com.battlebits.commons.BattlebitsAPI;
import br.com.battlebits.commons.api.chat.ChatAPI;
import br.com.battlebits.commons.bukkit.BukkitMain;
import br.com.battlebits.commons.bukkit.account.BukkitPlayer;
import br.com.battlebits.commons.bungee.manager.BanManager;
import br.com.battlebits.commons.core.account.BattlePlayer;
import br.com.battlebits.commons.core.permission.Group;
import br.com.battlebits.commons.core.punish.Mute;
import br.com.battlebits.commons.core.translate.Translate;
import br.com.battlebits.commons.util.string.StringURLUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class ChatListener implements Listener {

	// REGEX: #\w\w+

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onChatEnabled(AsyncPlayerChatEvent event) {
		Player p = event.getPlayer();
		BattlePlayer player = BattlebitsAPI.getAccountCommon().getBattlePlayer(p.getUniqueId());
		if (player == null) {
			event.setCancelled(true);
			return;
		}
		switch (ChatAPI.getInstance().getChatState()) {
		case DISABLED:
			event.setCancelled(true);
			break;
		case STAFF:
			if (!player.isStaff()) {
				event.setCancelled(true);
				break;
			}
			break;
		case YOUTUBER:
			if (player.getServerGroup().ordinal() < Group.YOUTUBER.ordinal()) {
				event.setCancelled(true);
				break;
			}
			break;
		default:
			break;
		}
		if (event.isCancelled()) {
			p.sendMessage("�%command-chat-prefix%� �%command-chat-cant-talk%�");
		}
		p = null;
		player = null;
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onMute(AsyncPlayerChatEvent event) {
		Player p = event.getPlayer();
		BattlePlayer player = BattlebitsAPI.getAccountCommon().getBattlePlayer(p.getUniqueId());
		if (player == null) {
			event.setCancelled(true);
			return;
		}
		Mute mute = player.getPunishHistoric().getActualMute();
		if (mute == null)
			return;
		p.sendMessage(BanManager.getMuteMessage(mute, player.getLanguage()));
		event.setCancelled(true);
		mute = null;
		p = null;
		player = null;
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onChat(AsyncPlayerChatEvent event) {
		event.setCancelled(true);
		BukkitPlayer player = (BukkitPlayer) BattlebitsAPI.getAccountCommon()
				.getBattlePlayer(event.getPlayer().getUniqueId());
		if (player == null) {
			event.setCancelled(true);
			return;
		}
		for (Player r : event.getRecipients()) {
			try {
				BukkitPlayer receiver = (BukkitPlayer) BattlebitsAPI.getAccountCommon()
						.getBattlePlayer(r.getUniqueId());
				if (receiver == null) {
					new BukkitRunnable() {
						@Override
						public void run() {
							Player p = Bukkit.getPlayer(r.getUniqueId());
							if (p == null)
								return;
							if (!p.isOnline())
								return;
							p.kickPlayer("ERROR");
						}
					}.runTask(BukkitMain.getPlugin());
					continue;
				}
				if ((!receiver.getConfiguration().isIgnoreAll())
						&& (!receiver.getBlockedPlayers().containsKey(player.getUniqueId())
								&& (!player.getBlockedPlayers().containsKey(receiver.getUniqueId())))) {
					TextComponent clan = null;
					int text = 3;
					if (player.getClan() != null) {
						clan = new TextComponent(ChatColor.GRAY + "[" + ChatColor.DARK_GRAY
								+ player.getClan().getAbbreviation() + ChatColor.GRAY + "] ");
						clan.setClickEvent(
								new ClickEvent(Action.SUGGEST_COMMAND, "/clan info " + player.getClan().getName()));
						TextComponent[] clanMessage = new TextComponent[] { //
								new TextComponent(Translate.getTranslation(receiver.getLanguage(), "clan-hover-info")//
										.replace("%clanName%", player.getClan().getName())//
										.replace("%clanLeague%", player.getClan().getRank().name())//
										.replace("%clanXp%", player.getClan().getXp() + "")//
										.replace("%players%", player.getClan().getParticipants().size() + "/"
												+ player.getClan().getSlots())) };
						clan.setHoverEvent(
								new HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, clanMessage));
						text += 1;
					}
					TextComponent[] textTo = new TextComponent[text + event.getMessage().split(" ").length];
					String tag = player.getTag().getPrefix(receiver.getLanguage());
					TextComponent account = new TextComponent(tag
							+ (ChatColor.stripColor(tag).trim().length() > 0 ? " " : "") + event.getPlayer().getName());
					account.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/account " + player.getName()));
					account.setHoverEvent(new HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
							TextComponent.fromLegacyText(
									Translate.getTranslation(receiver.getLanguage(), "account-hover-info"))));
					TextComponent league = new TextComponent(
							ChatColor.GRAY + " (" + player.getLeague().getSymbol() + ChatColor.GRAY + ")");
					league.setHoverEvent(new HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
							TextComponent.fromLegacyText(ChatColor.BOLD + player.getLeague().getSymbol() + " "
									+ ChatColor.BOLD + player.getLeague().name())));
					int i = 0;
					if (clan != null) {
						textTo[i] = clan;
						++i;
					}
					textTo[i] = account;
					++i;
					textTo[i] = league;
					++i;
					textTo[i] = new TextComponent(ChatColor.WHITE + ":");
					++i;
					for (String msg : event.getMessage().split(" ")) {
						msg = " " + msg;
						TextComponent text2 = new TextComponent(msg);
						List<String> url = StringURLUtils.extractUrls(msg);
						if (url.size() > 0) {
							text2.setClickEvent(new ClickEvent(Action.OPEN_URL, url.get(0)));
						}
						textTo[i] = text2;
						++i;
					}
					r.spigot().sendMessage(textTo);
				}
				receiver = null;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		BukkitMain.getPlugin().getLogger().info("<" + player.getName() + "> " + event.getMessage());
		player = null;
	}
}
