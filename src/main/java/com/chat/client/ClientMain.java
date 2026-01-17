package com.chat.client; // Đã sửa đúng package

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientMain extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Dòng này giúp tìm đúng file giao diện bạn vừa tạo
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/chat/client/view/connect_view.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("Java Chat Client");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}