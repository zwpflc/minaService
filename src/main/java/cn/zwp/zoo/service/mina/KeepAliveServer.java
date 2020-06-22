package cn.zwp.zoo.service.mina;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.filter.keepalive.KeepAliveFilter;
import org.apache.mina.filter.keepalive.KeepAliveMessageFactory;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeepAliveServer {

	private static final int PORT = 9123;
	/** 30秒后超时 */
	private static final int IDEL_TIMEOUT = 30;
	/** 15秒发送一次心跳包 */
	private static final int HEART_BEAT_RATE = 15;
	/** 心跳包内容 */
	private static final String HEART_BEAT_REQUEST = "0x11";
	private static final String HEART_BEAT_RESPONSE = "0x12";
	private static final Logger LOG = LoggerFactory.getLogger(KeepAliveServer.class);

	public static void main(String[] args) throws IOException {
		IoAcceptor acceptor = new NioSocketAcceptor();
		acceptor.getSessionConfig().setReadBufferSize(1024);
		acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, IDEL_TIMEOUT);

		acceptor.getFilterChain().addLast("logger", new LoggingFilter());
		acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new TextLineCodecFactory()));

		KeepAliveMessageFactory heartBeatFactory = new KeepAliveMessageFactoryImpl();
		KeepAliveFilter heartBeat = new KeepAliveFilter(heartBeatFactory, IdleStatus.BOTH_IDLE);
		// 设置是否forward到下一个filter
		heartBeat.setForwardEvent(true);
		// 设置心跳频率
		heartBeat.setRequestInterval(HEART_BEAT_RATE);

		acceptor.getFilterChain().addLast("heartbeat", heartBeat);

		acceptor.setHandler(new IoHandlerAdapter());
		acceptor.bind(new InetSocketAddress(PORT));
		System.out.println("Server started on port： " + PORT);
	}

	/**
	 * 被动型心跳机制，服务器在接受到客户端连接以后被动接受心跳请求，当在规定时间内没有收到客户端心跳请求时将客户端连接关闭
	 * 
	 * @ClassName KeepAliveMessageFactoryImpl
	 * @Description 内部类，实现 KeepAliveMessageFactory(心跳工厂)
	 */
	private static class KeepAliveMessageFactoryImpl implements KeepAliveMessageFactory {

		/* 判断是否心跳请求包，是的话返回true */
		@Override
		public boolean isRequest(IoSession session, Object message) {
			LOG.info("请求心跳包信息: " + message);
			return message.equals(HEART_BEAT_REQUEST);
		}

		/* 由于被动型心跳机制，没有请求当然也就不关注反馈，因此直接返回 false */
		@Override
		public boolean isResponse(IoSession session, Object message) {
			return false;
		}

		/* 被动型心跳机制无请求，因此直接返回 null */
		@Override
		public Object getRequest(IoSession session) {
			return null;
		}

		/* 根据心跳请求 request，反回一个心跳反馈消息 non-null */
		@Override
		public Object getResponse(IoSession session, Object request) {
			LOG.info("响应预设信息: " + HEART_BEAT_RESPONSE);
			return HEART_BEAT_RESPONSE;
		}
	}
}
