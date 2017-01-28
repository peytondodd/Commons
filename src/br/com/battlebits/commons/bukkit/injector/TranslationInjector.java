package br.com.battlebits.commons.bukkit.injector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.google.common.base.Splitter;

import br.com.battlebits.commons.bukkit.BukkitMain;
import br.com.battlebits.commons.core.account.BattlePlayer;
import br.com.battlebits.commons.core.translate.Language;
import br.com.battlebits.commons.core.translate.T;
import br.com.battlebits.commons.util.string.StringLoreUtils;

public class TranslationInjector implements Injector {
	private Pattern finder = Pattern.compile("�%(([^)]+)%�)");

	@Override
	public void inject(BukkitMain plugin) {
		plugin.getProcotolManager()
				.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, //
						PacketType.Play.Server.CHAT, //
						PacketType.Play.Server.WINDOW_ITEMS, //
						PacketType.Play.Server.SET_SLOT, //
						PacketType.Play.Server.OPEN_WINDOW, //
						PacketType.Play.Server.SCOREBOARD_OBJECTIVE, //
						PacketType.Play.Server.SCOREBOARD_TEAM, //
						PacketType.Play.Server.SCOREBOARD_SCORE) {
					@Override
					public void onPacketSending(PacketEvent event) {
						if (event.getPlayer() == null)
							return;
						if (event.getPlayer().getUniqueId() == null)
							return;
						Language lang = BattlePlayer.getLanguage(event.getPlayer().getUniqueId());
						PacketContainer packet = event.getPacket();
						if (event.getPacketType() == PacketType.Play.Server.CHAT) {
							StructureModifier<WrappedChatComponent> chatComponents = packet.getChatComponents();
							for (WrappedChatComponent component : chatComponents.getValues()) {
								component.setJson(translate(component.getJson(), lang));
							}
						} else if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
							// List<ItemStack> items = new ArrayList<>();
							for (ItemStack item : packet.getItemArrayModifier().read(0)) {
								if (item == null) {
									// items.add(null);
									continue;
								}
								// items.add(translateItemStack(item, lang));
								translateItemStack(item, lang);
							}
							// packet.getItemArrayModifier().write(0,
							// (ItemStack[]) items.toArray());
						} else if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
							ItemStack item = packet.getItemModifier().read(0);
							packet.getItemModifier().write(0, translateItemStack(item, lang));
						} else if (event.getPacketType() == PacketType.Play.Server.OPEN_WINDOW
								|| event.getPacketType() == PacketType.Play.Server.SCOREBOARD_SCORE) {
							String message = event.getPacket().getStrings().read(0);
							packet.getStrings().write(0, translate(message, lang));
						} else if (event.getPacketType() == PacketType.Play.Server.SCOREBOARD_OBJECTIVE) {
							String message = event.getPacket().getStrings().read(1);
							packet.getStrings().write(0, translate(message, lang));
						} else if (event.getPacketType() == PacketType.Play.Server.SCOREBOARD_TEAM) {
							String text = translate(packet.getStrings().read(2) + packet.getStrings().read(3), lang);
							String prefix = "";
							String suffix = "";
							Iterator<String> iterator = Splitter.fixedLength(32).split(text).iterator();
							String str = iterator.next();
							if (str.endsWith("�")) {
								str = str.substring(0, str.length() - 1);
								prefix = str;
								if (iterator.hasNext()) {
									String next = iterator.next();
									if (!next.startsWith("�")) {
										String str2 = "�" + next;
										if (str2.length() > 16)
											str2 = str2.substring(0, 16);
										suffix = str2;
									} else {
										suffix = next;
									}
								} else {
									suffix = "";
								}
							} else if (iterator.hasNext()) {
								String next = iterator.next();
								if (!next.startsWith("�")) {
									String colors = ChatColor.getLastColors(str);
									String str3 = colors + next;
									if (str3.length() > 16)
										str3 = str3.substring(0, 16);
									prefix = str;
									suffix = str3;
								} else {
									prefix = str;
									suffix = next;
								}
							} else {
								prefix = str;
								suffix = "";
							}
							packet.getStrings().write(2, prefix);
							packet.getStrings().write(3, suffix);
						}
					}

				});
	}

	private ItemStack translateItemStack(ItemStack iS, Language lang) {
		ItemMeta meta = iS.getItemMeta();
		if (meta != null) {
			if (meta.hasDisplayName()) {
				String message = meta.getDisplayName();
				message = translate(message, lang);
				meta.setDisplayName(message);
			}
			if (meta.hasLore()) {
				List<String> newlore = new ArrayList<>();
				for (String message : meta.getLore()) {
					message = translate(message, lang);
					;
					if (message.contains("\n")) {
						for (String s : message.split("\n"))
							newlore.addAll(StringLoreUtils.formatForLore(s));
					} else {
						newlore.addAll(StringLoreUtils.formatForLore(message));
					}
					message = null;
				}
				meta.setLore(newlore);
				newlore = null;
			}
			iS.setItemMeta(meta);
		}
		return iS;
	}

	private String translate(String message, Language lang) {
		Matcher matcher = finder.matcher(message);
		while (matcher.find()) {
			message = message.replace(matcher.group(), T.t(lang, matcher.group(2)));
		}
		return message;
	}

	public static void main(String[] args) {
		WrappedChatComponent component = WrappedChatComponent.fromText("Ol� eu sou um texto");
		System.out.println(component.getJson());
	}

}