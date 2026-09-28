package co.unicauca.saberpro.access;

import java.io.File;

/**
 * Ubicacion fija de los datos de la aplicacion (base de datos SQLite y
 * bandeja de salida de correo simulado).
 *
 * Se guarda siempre en una carpeta dentro del directorio del usuario
 * (~/BancoPreguntasSaberPro), en vez de en el directorio "actual" desde
 * donde se lanza el proceso. Esto evita que la aplicacion parezca "perder"
 * los datos cuando se ejecuta desde el IDE una vez y desde una terminal
 * (u otra carpeta) la siguiente vez: sin esta ubicacion fija, cada lugar
 * distinto desde el que se ejecuta el programa crearia su propio archivo
 * bancopreguntas.db vacio.
 */
public final class AppPaths {

    private static final String APP_DIR_NAME = "BancoPreguntasSaberPro";

    private AppPaths() {
    }

    public static File getAppDataDirectory() {
        File dir = new File(System.getProperty("user.home"), APP_DIR_NAME);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static String getDatabaseUrl() {
        File dbFile = new File(getAppDataDirectory(), "bancopreguntas.db");
        return "jdbc:sqlite:" + dbFile.getAbsolutePath();
    }

    public static File getOutboxDirectory() {
        File dir = new File(getAppDataDirectory(), "outbox");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }
}