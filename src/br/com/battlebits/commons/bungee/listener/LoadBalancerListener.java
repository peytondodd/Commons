package br.com.battlebits.commons.bungee.listener;

import br.com.battlebits.commons.BattlebitsAPI;
import br.com.battlebits.commons.bungee.BungeeMain;
import br.com.battlebits.commons.core.server.ServerManager;
import br.com.battlebits.commons.core.server.ServerType;
import br.com.battlebits.commons.core.server.loadbalancer.server.BattleServer;
import br.com.battlebits.commons.core.translate.T;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.ServerPing.Players;
import net.md_5.bungee.api.ServerPing.Protocol;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class LoadBalancerListener implements Listener {

	private ServerManager manager;

	public LoadBalancerListener(ServerManager manager) {
		this.manager = manager;
	}

	@EventHandler
	public void onLogin(ServerConnectEvent event) {
		ProxiedPlayer player = event.getPlayer();
		if (player.getPendingConnection() == null)
			return;
		if (player.getServer() != null)
			return;
		String serverIp = getServerIp(event.getPlayer().getPendingConnection());
		BattleServer server = manager.getServer(serverIp);
		if (server != null) {
			event.setTarget(server.getServerInfo());
			return;
		}

		BattleServer lobby = manager.getBalancer(ServerType.LOBBY).next();
		if (lobby != null && lobby.getServerInfo() != null) {
			event.setTarget(lobby.getServerInfo());
		} else {
			player.disconnect(TextComponent.fromLegacyText(T.t(BungeeMain.getPlugin(), BattlebitsAPI.getAccountCommon().getBattlePlayer(player.getUniqueId()).getLanguage(), "server-not-available")));
		}
	}

	@EventHandler(priority = -128)
	public void onProxyPing(ProxyPingEvent event) {
		ServerPing ping = event.getResponse();
		ping.getVersion().setName("Join with 1.7|1.8|1.9|1.10|1.11");
		event.setResponse(ping);
	}

	@EventHandler(priority = 127)
	public void onPing(ProxyPingEvent event) {
		String serverIp = getServerIp(event.getConnection());
		BattleServer server = manager.getServer(serverIp);
		ServerPing ping = event.getResponse();
		if (server != null) {
			event.registerIntent(BungeeMain.getPlugin());
			server.getServerInfo().ping(new Callback<ServerPing>() {
				@Override
				public void done(ServerPing realPing, Throwable throwable) {
					if (throwable == null) {
						ping.getPlayers().setMax(realPing.getPlayers().getMax());
						ping.getPlayers().setOnline(realPing.getPlayers().getOnline());
						ping.setDescription(realPing.getDescription());
					} else {
						ping.setPlayers(new Players(-1, -1, null));
						ping.setVersion(new Protocol("Server not found", 0));
						ping.setDescription(ChatColor.RED + "Error :(");
					}
					event.setResponse(ping);
					event.completeIntent(BungeeMain.getPlugin());
				}
			});
		}
	}

	private String getServerIp(PendingConnection con) {
		if (con == null)
			return "";
		if (con.getVirtualHost() == null)
			return "";
		String s = con.getVirtualHost().getHostName().toLowerCase();
		if (s.isEmpty())
			return "";
		for (int i = 0; i < 10; i++) {
			s = s.replace("proxy" + i + ".", "");
		}
		for (String str : new String[] { "br.", "us.", "eu." }) {
			s = s.replace(str, "");
		}
		return s;
	}
}
