package br.com.battlebits.commons;

import br.com.battlebits.commons.core.backend.redis.RedisBackend;
import redis.clients.jedis.Jedis;

public class ClearRedis {
	public static void main(String[] args) {
		RedisBackend backend = new RedisBackend("127.0.0.1", "':kG'38b]AVk-a6@", 6379);
		backend.startConnection();

		try (Jedis jedis = backend.getPool().getResource()) {
			jedis.flushDB();
		}
		backend.closeConnection();
	}
}