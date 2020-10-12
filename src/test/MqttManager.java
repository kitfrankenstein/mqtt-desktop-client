package test;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
* @author Kit
* @version: 2019年4月19日 下午1:33:45
* 
*/
public class MqttManager {
	private MqttClient client;
	private MqttCallback mqttCallback;
	private MqttConnectOptions options;
	private Test UI;
	
	public MqttManager(Test test) {
		this.UI = test;
	}
	
	/**
	 * 创建连接
	 * @param brokerURL broker地址
	 * @param userName 用户名
	 * @param password 密码
	 * @param clientID 用户ID
	 * @param topic 连接断开后发送的消息的主题
	 */
	public void createConnect (String brokerURL, String userName,
			String password, String clientID, String topic) {
		options = new MqttConnectOptions();
		options.setCleanSession(true);
		options.setUserName(userName);
		options.setPassword(password.toCharArray());
		options.setKeepAliveInterval(10);//设置心跳
		options.setWill(topic, "close".getBytes(), 2, true);//连接断开后发送的消息
		options.setAutomaticReconnect(false);
		
		mqttCallback = new MyCallback();
		
		try {
			client = new MqttClient(brokerURL, clientID, new MemoryPersistence());
			client.setCallback(mqttCallback);
			client.connect(options);
		} catch (MqttException e) {
			UI.logCallback(e.toString());
		}
		
	}
	
	/**
	 * 创建和配置一个消息并发布
	 * @param topicName 消息主题
	 * @param qos 服务质量
	 * @param content 消息内容
	 */
    public void publish(String topicName, int qos, String content) {
        if (client != null && client.isConnected()) {
        	byte[] payload = content.getBytes();
            MqttMessage message = new MqttMessage(payload);
            message.setPayload(payload);
            message.setQos(qos);
            try {
                client.publish(topicName, message);
            } catch (MqttException e) {
            	UI.logCallback("mqtt publish err: " + e.toString());
            }
        }
    }
    
    /**
     * 订阅消息
     * @param topicName 消息主题
     * @param qos 最大服务质量
     */
    public void subscribe(String topicName, int qos) {
        if (client != null && client.isConnected()) {
            try {
                IMqttToken token = client.subscribeWithResponse(topicName, qos);
                UI.logCallback(token.getResponse().toString());
                UI.logCallback("Subscribe topic: " + token.getTopics()[0]);
                UI.subscribeCallback();
            } catch (MqttException e) {
            	UI.logCallback("mqtt subscribe err: " + e.toString());
            }
        }
    }
	
    /**
     * 关闭连接
     */
    public void disConnect() {
		if (client != null && client.isConnected()) {
			try {
				client.disconnect();
			} catch (Exception e) {
				UI.logCallback(e.toString());
			}
		}
	}
    
    public boolean isConnected() {
    	return client.isConnected();
    }
    
    /**
     * MQTT回调类，定义特定的回调操作
     * @author Kit
     *
     */
	class MyCallback implements MqttCallbackExtended {

	    /**
	     * 连接成功
	     */
		@Override
		public void connectComplete(boolean reconnect, String serverURI) {
	    	UI.logCallback("mqtt connected");
	    	UI.connectCallback();
		}
		
	    /**
	     * 连接中断
	     */
	    @Override
	    public void connectionLost(Throwable cause) {
	    	UI.logCallback("mqtt connection lost: " + cause.toString());
	        UI.lostCallback();
	    }


	    /**
	     * 消息送达
	     */
	    @Override
	    public void messageArrived(String topic, MqttMessage message) throws Exception {
	    	UI.logCallback("Arrive topic: " + topic + "\t message: " + message.toString());
	    	UI.arriveCallback(topic, message.toString());
	    }


	    /**
	     * 交互完成
	     */
	    @Override
	    public void deliveryComplete(IMqttDeliveryToken token) {    	
	    	try {
	    		UI.logCallback("mqtt deliver token: " + token.toString());
	    		UI.logCallback("Publish topic: " + token.getTopics()[0] + "\t message: " + token.getMessage().toString());
				UI.deliverCallback();
			} catch (MqttException e) {
				UI.logCallback("mqtt deliver err:" + e.toString());
			}
	    }
	    
	}
}
