package br.com.battlebits.commons.bungee.redis;

import java.lang.reflect.Field;
import java.util.UUID;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import br.com.battlebits.commons.BattlebitsAPI;
import br.com.battlebits.commons.bungee.BungeeMain;
import br.com.battlebits.commons.bungee.loadbalancer.server.BattleServer;
import br.com.battlebits.commons.bungee.loadbalancer.server.MinigameServer;
import br.com.battlebits.commons.core.account.BattlePlayer;
import br.com.battlebits.commons.core.data.DataServer.DataServerMessage;
import br.com.battlebits.commons.core.data.DataServer.DataServerMessage.Action;
import br.com.battlebits.commons.core.data.DataServer.DataServerMessage.JoinEnablePayload;
import br.com.battlebits.commons.core.data.DataServer.DataServerMessage.StartPayload;
import br.com.battlebits.commons.core.data.DataServer.DataServerMessage.StopPayload;
import br.com.battlebits.commons.core.data.DataServer.DataServerMessage.UpdatePayload;
import br.com.battlebits.commons.core.server.ServerType;
import br.com.battlebits.commons.util.reflection.Reflection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import redis.clients.jedis.JedisPubSub;

public class BungeePubSubHandler extends JedisPubSub {

	@Override
	public void onMessage(String channel, String message) {
		if (channel.equals("account-field")) {
			JsonObject obj = (JsonObject) BattlebitsAPI.getParser().parse(message);
			if (obj.getAsJsonPrimitive("source").getAsString().equalsIgnoreCase(BattlebitsAPI.getServerId())) {
				return;
			}
			UUID uuid = UUID.fromString(obj.getAsJsonPrimitive("uniqueId").getAsString());
			ProxiedPlayer p = BungeeMain.getPlugin().getProxy().getPlayer(uuid);
			if (p == null)
				return;
			String field = obj.getAsJsonPrimitive("field").getAsString();
			BattlePlayer player = BattlePlayer.getPlayer(uuid);
			try {
				Field f = Reflection.getField(BattlePlayer.class, field);
				f.setAccessible(true);
				Object object = BattlebitsAPI.getGson().fromJson(obj.get("value"), f.getGenericType());
				f.set(player, object);
			} catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		} else if (channel.equals("server-info")) {
			JsonObject jsonObject = BattlebitsAPI.getParser().parse(message).getAsJsonObject();
			String source = jsonObject.get("source").getAsString();

			if (source.equals(BattlebitsAPI.getServerId()))
				return;
			ServerType sourceType = ServerType.getServerType(source);
			Action action = Action.valueOf(jsonObject.get("action").getAsString());
			switch (action) {
			case JOIN: {
				if (sourceType == ServerType.NETWORK) {
					break;
				}
				BattleServer server = BungeeMain.getPlugin().getServerManager().getServer(source);
				server.setOnlinePlayers(server.getOnlinePlayers() + 1);
				break;
			}
			case LEAVE: {
				if (sourceType == ServerType.NETWORK) {
					break;
				}
				BattleServer server = BungeeMain.getPlugin().getServerManager().getServer(source);
				server.setOnlinePlayers(server.getOnlinePlayers() - 1);
				break;
			}
			case JOIN_ENABLE: {
				DataServerMessage<JoinEnablePayload> payload = BattlebitsAPI.getGson().fromJson(jsonObject,
						new TypeToken<DataServerMessage<JoinEnablePayload>>() {
						}.getType());
				if (sourceType == ServerType.NETWORK) {
					break;
				}
				BungeeMain.getPlugin().getServerManager().getServer(source)
						.setJoinEnabled(payload.getPayload().isEnable());
				break;
			}
			case START: {
				DataServerMessage<StartPayload> payload = BattlebitsAPI.getGson().fromJson(jsonObject,
						new TypeToken<DataServerMessage<StartPayload>>() {
						}.getType());
				if (sourceType == ServerType.NETWORK) {
					break;
				}
				BungeeMain.getPlugin().getServerManager().addActiveServer(payload.getPayload().getServerAddress(),
						payload.getPayload().getServer().getServerId(),
						payload.getPayload().getServer().getMaxPlayers());
				break;
			}
			case STOP: {
				DataServerMessage<StopPayload> payload = BattlebitsAPI.getGson().fromJson(jsonObject,
						new TypeToken<DataServerMessage<StopPayload>>() {
						}.getType());
				if (sourceType == ServerType.NETWORK) {
					break;
				}
				BungeeMain.getPlugin().getServerManager().removeActiveServer(payload.getPayload().getServerId());
				BungeeMain.getPlugin().removeBungee(payload.getPayload().getServerId());
				break;
			}
			case UPDATE: {
				DataServerMessage<UpdatePayload> payload = BattlebitsAPI.getGson().fromJson(jsonObject,
						new TypeToken<DataServerMessage<UpdatePayload>>() {
						}.getType());
				if (sourceType == ServerType.NETWORK) {
					break;
				}
				BattleServer server = BungeeMain.getPlugin().getServerManager().getServer(source);
				if (server instanceof MinigameServer) {
					((MinigameServer) server).setState(payload.getPayload().getState());
					((MinigameServer) server).setTime(payload.getPayload().getTime());
				}
				break;
			}
			default:
				break;
			}
		}
	}

	public static Field getField(Class<?> clazz, String name) {
		try {
			Field field = clazz.getDeclaredField(name);
			field.setAccessible(true);
			return field;
		} catch (Exception e) {
			try {
				Field field = clazz.getDeclaredField(name);
				field.setAccessible(true);
				return field;
			} catch (Exception e2) {
				if (clazz.getSuperclass() != null)
					return getField(clazz.getSuperclass(), name);
			}
		}
		return null;
	}

}