package py.edu.ucom.is2.proyectocamel;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "sipap.jms.habilitado=false",
        "camel.component.jms.test-connection-on-startup=false",
        "server.port=0"
})
class ProyectoCamelApplicationTests {

    @Test
    void contextLoads() {
    }
}
