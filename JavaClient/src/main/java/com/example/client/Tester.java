package com.example.client;


import com.letsgobiking.wsdl.IService;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;

public class Tester {
    public static void main(String[] args) {
        System.out.println("Hello World!");
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(IService.class);
        factory.setAddress("http://localhost:5229/Service.svc");
        IService service = (IService) factory.create();
        System.out.println(service.geocode("Paris").getCoordinate().get(0).getLatitude());
    }
}
