package xyz.fabianpineda.desarrollomovil.netcalc;

import com.loopj.android.http.*;

/**
 * Auxiliar de requests HTTP con propiedades y métodos estáticos.
 *
 * Basado en las recomendaciones de AsyncHttpClient.
 * https://loopj.com/android-async-http/#recommended-usage-make-a-static-http-client
 *
 * Crea un objeto "cliente" estático y privado con algunas propiedades de request configuradas,
 * incluyendo User Agent, configuración de reintentos, redirects y logging.
 */
public class AuxiliarHTTP {
    /** Cliente HTTP. Todos requests serán hechos a travéz de este objeto. **/
    private final static AsyncHttpClient client;

    /*
     * Bloque de inicialización estático.
     * Usado principalemnte para inicializar propiedades constantes estáticas.
     *
     * https://docs.oracle.com/javase/tutorial/java/javaOO/initial.html
     */
    static {
        client = new AsyncHttpClient();
        client.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:49.0) Gecko/20100101 Firefox/49.0");
        client.setMaxRetriesAndTimeout(0, 10);
        client.setEnableRedirects(true, true);

        // DEBUG:
        //client.setLoggingEnabled(true);
        //client.setLoggingLevel(LogInterface.VERBOSE);
    }

    /**
     * Inicia un request HTTP GET.
     * Los eventos generados por la petición/respuesta respuesta se procesan por responseHandler.
     */
    public static void get(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.get(url, params, responseHandler);
    }

    /**
     * Inicia un request HTTP POST.
     * Los eventos generados por la petición/respuesta respuesta se procesan por responseHandler.
     */
    public static void post(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.post(url, params, responseHandler);
    }
}