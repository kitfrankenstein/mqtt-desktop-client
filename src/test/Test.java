package test;

import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
* @author Kit
* @version: 2019年4月19日 下午1:23:08
* 
*/
public class Test extends Application{

	private MqttManager mqttManager;
	private Button loginButton, logoutButton;
	private TextArea recvText, sendText, logText;
	private TextField pTopicText, sTopicText;
	private Label state;
	
	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		BorderPane pane = new BorderPane();
		Label nameLabel = new Label("Username: "),
				pwdLabel = new Label("Password: "),
				hostLabel = new Label("Host: "),
				portLabel = new Label("Port: ");
		TextField nameText = new TextField(),
					hostText = new TextField("localhost"),
					portText = new TextField("1883");
		PasswordField pwdText = new PasswordField();

		hostText.setDisable(false);
		portText.setDisable(false);		
		hostLabel.setPrefWidth(65);
		portLabel.setPrefWidth(61);
		
		loginButton = new Button("Connect");
		logoutButton = new Button("Disconnect");		
		logoutButton.setDisable(true);
		
		HBox addrBox = new HBox(hostLabel, hostText, portLabel, portText);
		addrBox.setSpacing(5);
		HBox nameBox = new HBox(nameLabel, nameText, pwdLabel, pwdText);
		nameBox.setSpacing(5);
		HBox btnBox = new HBox(loginButton, logoutButton);
		btnBox.setSpacing(320);
		
			
		pTopicText = new TextField();
		sTopicText = new TextField();
		Label pLabel = new Label("Publish topic: "),
			  sLabel = new Label("Subscribe topic: ");
		Button publish = new Button("Publish"),
			   subscribe = new Button("Subscribe");
		
		HBox pubBox = new HBox(pLabel, pTopicText, publish);
		pubBox.setSpacing(5);
		HBox subBox = new HBox(sLabel, sTopicText, subscribe);
		subBox.setSpacing(5);
		
		Label pubLabel = new Label("Publish message: "),
			  subLabel = new Label("Received message: ");
		recvText = new TextArea();
		recvText.setWrapText(true);
		recvText.setEditable(false);
		
		sendText = new TextArea();
		sendText.setWrapText(true);
		
		logText = new TextArea();
		logText.setWrapText(true);
		logText.setEditable(false);
		
		VBox left = new VBox(addrBox, nameBox, pubBox, pubLabel, sendText);
		left.setSpacing(5);
		left.setPadding(new Insets(5, 5, 5, 5));
		left.setPrefSize(500, 300);
		
		VBox right = new VBox(btnBox, subBox, subLabel, recvText);
		right.setSpacing(5);
		right.setPadding(new Insets(35, 5, 5, 5));
		right.setPrefSize(500, 300);
		
		state = new Label();
		Label logLabel = new Label("log: ");
		VBox bottom = new VBox(logLabel, logText, state);
		bottom.setSpacing(5);
		bottom.setPadding(new Insets(5, 5, 5, 5));
	
		pane.setLeft(left);
		pane.setRight(right);
		pane.setBottom(bottom);
		
		Scene scene = new Scene(pane);
		
		loginButton.setOnAction(EventHandler -> {
			if (mqttManager == null) {
				mqttManager = new MqttManager(this);
			}
			new Thread(() -> {
				String userName = nameText.getText(),
						password = pwdText.getText(),
						host = "tcp://" + hostText.getText() + ":" + portText.getText();
				mqttManager.createConnect(host, userName, password, getClientID(), getClientID());
				if (mqttManager.isConnected()) {
					Platform.runLater(() -> {
						loginButton.setDisable(true);
						logoutButton.setDisable(false);
						state.setText("Connected");
					});
				}
			}).start();
		});
		
		logoutButton.setOnAction(EventHandler -> {
			if (mqttManager != null && mqttManager.isConnected()) {
				mqttManager.disConnect();
			}
			if (!mqttManager.isConnected()) {
				loginButton.setDisable(false);
				logoutButton.setDisable(true);
				state.setText("Disconnect");
			}
		});
		
		publish.setOnAction(e -> {
			if (mqttManager != null && mqttManager.isConnected()) {
				String topic = pTopicText.getText();
				String content = sendText.getText();
				if (topic != null && content != null && !topic.trim().equals("") && !content.trim().equals("")) {
					new Thread(() -> mqttManager.publish(topic, 0, content)).start();
				} else {
					showAlert();
				}
			}
		});
		
		subscribe.setOnAction(e -> {
			if (mqttManager != null && mqttManager.isConnected()) {
				String topic = sTopicText.getText();
				if (topic != null && !topic.trim().equals("")) {
					new Thread(() -> mqttManager.subscribe(topic, 0)).start();
				} else {
					showAlert();
				}
			}
		});
		
		primaryStage.setScene(scene);
		primaryStage.setTitle("MQTT Client");
		primaryStage.show();
		primaryStage.setOnCloseRequest(EventHandler -> {
			if (mqttManager != null && mqttManager.isConnected()) {
				mqttManager.disConnect();
			}
			Platform.exit();
			System.exit(0);
		});
	}
	
	public void connectCallback() {
		Platform.runLater(() -> {
			loginButton.setDisable(true);
			logoutButton.setDisable(false);
			state.setText("Connected");
			sendText.clear();
		});
	}
	
	public void lostCallback() {
		Platform.runLater(() -> {
			loginButton.setDisable(false);
			logoutButton.setDisable(true);
			state.setText("Disconnect");
			sendText.clear();
		});
	}
	
	public void arriveCallback(String topic, String message) {
		Platform.runLater(() -> {
			recvText.appendText("Receive topic: " + topic + "\t message : " + message.toString() + "\n");
		});
	}
	
	public void deliverCallback() {
		Platform.runLater(() -> {
			sendText.clear();
		});
	}
	
	public void subscribeCallback() {
		Platform.runLater(() -> {
			sTopicText.clear();
		});
	}
	
	public void logCallback(String message) {
		Platform.runLater(() -> {
			logText.appendText(getTime() + ": " + message + "\n");
		});
	}
	
	private String getClientID() {
		return "client" + ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
	}
	
	private String getTime() {
		return LocalDateTime.now().toString();
	}
	
	private void showAlert() {
		Alert alert = new Alert(AlertType.WARNING);
		alert.setHeaderText("内容不能为空");
		alert.setContentText("请输入内容");
		alert.show();
	}

}
