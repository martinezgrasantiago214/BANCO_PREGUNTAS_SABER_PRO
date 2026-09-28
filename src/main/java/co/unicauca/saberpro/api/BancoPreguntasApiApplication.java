package co.unicauca.saberpro.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del microservicio REST del Banco de Preguntas Saber PRO
 * (Taller: Creando una API REST simple con Spring Boot).
 *
 * La API es una NUEVA capa de presentacion (controladores HTTP) que reutiliza
 * exactamente las mismas capas de servicio, microkernel y acceso a datos que
 * la aplicacion de escritorio Swing, y la misma base de datos SQLite
 * (~/BancoPreguntasSaberPro/bancopreguntas.db). Por eso lo que se crea desde
 * la API se ve en "Mis preguntas" del escritorio y viceversa.
 *
 * El escaneo de componentes de Spring se limita al paquete
 * co.unicauca.saberpro.api, de modo que nada de la capa Swing se carga aqui.
 */
@SpringBootApplication
public class BancoPreguntasApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(BancoPreguntasApiApplication.class, args);
    }
}
