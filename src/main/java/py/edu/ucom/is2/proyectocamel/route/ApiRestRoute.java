package py.edu.ucom.is2.proyectocamel.route;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.stereotype.Component;

import py.edu.ucom.is2.proyectocamel.model.RespuestaApiTransferencia;
import py.edu.ucom.is2.proyectocamel.model.SolicitudTransferencia;

@Component
public class ApiRestRoute extends RouteBuilder {

    @Override
    public void configure() {
        restConfiguration().component("platform-http").bindingMode(RestBindingMode.json);

        rest("/api")
                .post("/transferencias")
                .consumes("application/json")
                .produces("application/json")
                .type(SolicitudTransferencia.class)
                .outType(RespuestaApiTransferencia.class)
                .to("direct:api-transferencias");
    }
}
