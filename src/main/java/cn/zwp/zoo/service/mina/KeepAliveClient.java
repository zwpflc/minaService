package cn.zwp.zoo.service.mina;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.filter.keepalive.KeepAliveFilter;
import org.apache.mina.filter.keepalive.KeepAliveMessageFactory;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.core.net.server.Client;

/**
 * 
 * @author Administrator
 *
 */
public class KeepAliveClient {

	private static final int PORT = 9123;
	/** 30秒后超时 */
	private static final int IDEL_TIMEOUT = 30;
	/** 15秒发送一次心跳包 */
	private static final int HEART_BEAT_RATE = 15;
	/** 心跳包内容 */
	private static final String HEART_BEAT_REQUEST = "0x11";
	private static final String HEART_BEAT_RESPONSE = "0x12";
	private static final Logger LOG = LoggerFactory.getLogger(Client.class);

	public static void main(String[] args) throws IOException {
		IoConnector connector = new NioSocketConnector();
		connector.getSessionConfig().setReadBufferSize(1024);
		connector.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, IDEL_TIMEOUT);

		connector.getFilterChain().addLast("logger", new LoggingFilter());
		connector.getFilterChain().addLast("codec", new ProtocolCodecFilter(new TextLineCodecFactory()));

		KeepAliveMessageFactory heartBeatFactory = new KeepAliveMessageFactoryImpl();
		KeepAliveFilter heartBeat = new KeepAliveFilter(heartBeatFactory, IdleStatus.BOTH_IDLE);
		// 设置是否forward到下一个filter
		heartBeat.setForwardEvent(true);
		// 设置心跳频率
		heartBeat.setRequestInterval(HEART_BEAT_RATE);

		connector.getFilterChain().addLast("heartbeat", heartBeat);

		connector.setHandler(new IoHandlerAdapter());
		connector.connect(new InetSocketAddress("127.0.0.1", PORT));
		System.out.println("Server started on port： " + PORT);
	}

	/**
	 * 被动型心跳机制，服务器在接受到客户端连接以后被动接受心跳请求，当在规定时间内没有收到客户端心跳请求时将客户端连接关闭
	 * 
	 * @ClassName KeepAliveMessageFactoryImpl
	 * @Description 内部类，实现KeepAliveMessageFactory（心跳工厂）
	 * @author cruise
	 *
	 */
	private static class KeepAliveMessageFactoryImpl implements KeepAliveMessageFactory {

		/* 服务器不会给客户端发送请求包，因此不关注请求包，直接返回 false */
		@Override
		public boolean isRequest(IoSession session, Object message) {
			return false;
		}

		/* 客户端关注请求反馈，因此判断 mesaage 是否是反馈包 */
		@Override
		public boolean isResponse(IoSession session, Object message) {
			LOG.info("响应预设信息: " + message);
			return message.equals(HEART_BEAT_RESPONSE);
		}

		/* 获取心跳请求包 non-null */
		@Override
		public Object getRequest(IoSession session) {
			LOG.info("请求预设信息: " + HEART_BEAT_REQUEST);
			return HEART_BEAT_REQUEST;
		}

		/* 服务器不会给客户端发送心跳请求，客户端当然也不用反馈，该方法返回 null */
		@Override
		public Object getResponse(IoSession session, Object request) {
			return null;
		}
	}
}
