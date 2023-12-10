module com.example.client {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.gluonhq.maps;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires java.naming;

    opens com.example.client to javafx.fxml;
    requires java.logging;
    requires org.apache.cxf.frontend.jaxws;
    requires jakarta.xml.bind;
    requires jakarta.xml.ws;
    requires jakarta.jws;
    exports com.example.client;
    opens com.letsgobiking.wsdl;
    opens org.datacontract.schemas._2004._07.router;
    opens com.microsoft.schemas._2003._10.serialization.arrays;
}