DesarrolloMovilNetCalc2
=======================

Versión "mejorada" de [DesarrolloMovilNetCalc](https://github.com/fdpiniet/DesarrolloMovilNetCalc).

Detalles de Problema / Error
----------------------------

Todo request hacia el servidor al que la aplicación envía requests de operaciones aritméticas fallan usando código idéntico de requests que hago en otros dos proyectos más mios. **Creo que tiene que ver con el hosting del script, pero las causas pueden ser muchas.** Todo esto lo explico mas abajo.

El strack trace muestra lo siguiente:

> <pre>W/System.err: java.net.UnknownHostException: Unable to resolve host "gns.jairoesc.com": No address associated with hostname
W/System.err:     at java.net.InetAddress.lookupHostByName(InetAddress.java:470)
W/System.err:     at java.net.InetAddress.getAllByNameImpl(InetAddress.java:252)
W/System.err:     at java.net.InetAddress.getByName(InetAddress.java:305)
W/System.err:     at cz.msebera.android.httpclient.conn.scheme.PlainSocketFactory.connectSocket(PlainSocketFactory.java:154)
W/System.err:     at cz.msebera.android.httpclient.conn.scheme.SchemeSocketFactoryAdaptor.connectSocket(SchemeSocketFactoryAdaptor.java:65)
W/System.err:     at cz.msebera.android.httpclient.impl.conn.DefaultClientConnectionOperator.openConnection(DefaultClientConnectionOperator.java:177)
W/System.err:     at cz.msebera.android.httpclient.impl.conn.AbstractPoolEntry.open(AbstractPoolEntry.java:145)
W/System.err:     at cz.msebera.android.httpclient.impl.conn.AbstractPooledConnAdapter.open(AbstractPooledConnAdapter.java:131)
W/System.err:     at cz.msebera.android.httpclient.impl.client.DefaultRequestDirector.tryConnect(DefaultRequestDirector.java:611)
W/System.err:     at cz.msebera.android.httpclient.impl.client.DefaultRequestDirector.execute(DefaultRequestDirector.java:446)
W/System.err:     at cz.msebera.android.httpclient.impl.client.AbstractHttpClient.doExecute(AbstractHttpClient.java:860)
W/System.err:     at cz.msebera.android.httpclient.impl.client.CloseableHttpClient.execute(CloseableHttpClient.java:82)
W/System.err:     at com.loopj.android.http.AsyncHttpRequest.makeRequest(AsyncHttpRequest.java:146)
W/System.err:     at com.loopj.android.http.AsyncHttpRequest.makeRequestWithRetries(AsyncHttpRequest.java:177)
W/System.err:     at com.loopj.android.http.AsyncHttpRequest.run(AsyncHttpRequest.java:106)
W/System.err:     at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:423)
W/System.err:     at java.util.concurrent.FutureTask.run(FutureTask.java:237)
W/System.err:     at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1113)
W/System.err:     at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:588)
W/System.err:     at java.lang.Thread.run(Thread.java:818)
W/System.err: Caused by: android.system.GaiException: android_getaddrinfo failed: EAI_NODATA (No address associated with hostname)
W/System.err:     at libcore.io.Posix.android_getaddrinfo(Native Method)
W/System.err:     at libcore.io.ForwardingOs.android_getaddrinfo(ForwardingOs.java:55)
W/System.err:     at java.net.InetAddress.lookupHostByName(InetAddress.java:451)
W/System.err: 	... 19 more</pre>

Lo importante aquí: me sorprende lo de **UnknownHostException** porque el request lo estoy haciendo a una **dirección IPv4**. Segundo, abajo muestra **"no address associate with hostname."** Tercero, siempre intenta (y falla al) convertir la IP en el hostname **gns.jairoesc.co**.

He buscado sobre ambos errores en Internet, y proponen las siguientes soluciones. Todas han fallado. Aquí unas observaciones:

* Agregar permisos de ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE e INTERNET. No funciona con los tres.
* Reiniciar el teléfono porque el caché de DNS de Android "nunca sobrevive un reinicio."
* Probé con dos servidores DNS: el de mi ISP, y los de Google (8.8.8.8 y 8.8.4.4).
* Ejecutar en AsyncClient antes de los requests lo siguiente:
    - setUserAgent("user agent de un navegador moderno");
    - setEnableRedirects(true, true);, para activar redirects y redirects relativos.
* He probado tanto en mi teléfono Android 5.1 cómo en emulador AVD en Android Studio. Los resultados son siempre los mismos.
* En el AsyncHttpResponseHandler implementé y sobrescribí (respectivamente) varios métodos de la siguiente manera:
    - El método **onSuccess** nunca es llamado. No se muestra ningún mensaje de depuración incluido en su cuerpo en ningún momento.
    - El método **onStart** siempre es ejecutado; así que el request siempre inicia.
    - El método **onFailure** siempre es ejecutado; siempre falla después de un largo timeout. El método falla con un **status code igual a 0**.
    - El método **onFinish** siempre es llamado después de onFailure. Según la documentación de AsyncHttpClient, onFinish es llamado siempre al finalizar un request, sea exitoso o no.
    - El método **onRetry** parece nunca ser llamado. Probablemente porque el task asíncrono siempre falla con una **excepción.** 
    - El método **onProgress** fue implementado para depurar la aplicación, pero nunca es ejecutado porque el request hace timeout/falla antes de descargar/subit el primer byte.
* Intenté hace requests a otras direcciones por hostname y funciona.
* En **C:\Windows\System32\Drivers\etc\hosts**, agregué una entrada apuntando el hostname "**gns.jairoesc.co**" a la dirección IP del sitio, **162.243.64.94**, y el request **falla de inmediato** sin el timeout largo, en **emulador**.

Otros detalles:

* En mi teléfono rooteado, puedo usando una terminal para:
    - Hacer **ping** directamente a **162.243.64.94**, y el ping funciona.
    - El **ping** a **gns.jairoesc.co** siempre **falla**.
    - Usar **`curl -v --data "o=sum&a=1&b=2" http://162.243.64.94/dm.php`** y si recibo un response de **"3"**. Screenshot mas abajo.
    - Abrir el URL desde cualquier navegador.
* En mi PC puedo:
    - Agregar una entrada en el archivo de hosts apuntando la ip **162.243.64.94** al hostname **gns.jairoesc.co** y puedo ver el sitio en navegador usando ese hostname. Pero como mencioné anteriormente, esta linea causa que el emulador falle el request instantaneamente, sin timeout.
    - Abrir el sitio tanto por IP como en hostname en un navegador web cualquiera.
    - Usar el mismo comando **curl** que usé en Android y obtengo la misma respuesta sin error. También funciona usando el hostname.

Por último, haciendo un domain lookup, veo que **jairoesc.co** expiró en el año 2014. ¿Podrá tener algo que ver?

Aquí el screenshot de **curl** en mi teléfono que mencioné mas arriba.

![Captura de Pantalla](http://i.imgur.com/xpJchYB.png "Captura de pantalla de "curl" en celular.")
