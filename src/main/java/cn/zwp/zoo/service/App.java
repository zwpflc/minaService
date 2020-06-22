package cn.zwp.zoo.service;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import cn.zwp.zoo.service.zoo.ServiceRegistry;

/**
 * Hello world!
 *
 */
@SpringBootApplication
public class App implements CommandLineRunner {
	public static void main(String[] args) {
		SpringApplication.run(App.class, args);
	}

	private int port = 8081;

	public void run(String... args) throws Exception {
		try {
//            NettyServer server = new NettyServer();
//            server.start(port);
			// 服务注册
			ServiceRegistry serviceRegistry = new ServiceRegistry();
			String ip = InetAddress.getLocalHost().getHostAddress();
			serviceRegistry.register(ip + ":" + port);

		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

	}
}
