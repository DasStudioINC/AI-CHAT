module spidernetwork.com.daveai {
    requires javafx.controls;
    requires javafx.fxml;


    opens spidernetwork.com.daveai to javafx.fxml;
    exports spidernetwork.com.daveai;
}