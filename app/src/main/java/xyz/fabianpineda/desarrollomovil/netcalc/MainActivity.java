package xyz.fabianpineda.desarrollomovil.netcalc;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import com.loopj.android.http.*;

import cz.msebera.android.httpclient.Header;

/**
 * Activity principal de la aplicación.
 *
 * Presenta una calculadora sencilla con las cuatro operaciones aritméticas de suma, resta,
 * multiplicación y división. Las operaciones son procesadas remotamente; ninguna calculación
 * es hecha en el lado del cliente.
 *
 * La entrada de valores y operaciones es hecha a travéz de los 16 botones definifos en el
 * Layout de la Activity: los números del 0 al 9, el punto decimal, el signo igual, y un botón
 * correspondiente para cada operación aritmética (+, -, *, /)
 *
 * Contiene código de manejo de evento para los eventos generados por los elementos de su interfaz
 * de usuario y también métodos de manejo de respuestas HTTP.
 *
 * El servidor remoto está ubicado en la dirección "162.243.64.94" y el script que procesa los
 * requests de este cliente está disponible en la ruta "/dm.php". Sus operaciones (y los
 * parámetros POST que requieren, todos obligatorios, en donde N y M son doubles) soportadas
 * son las siguientes:
 *      - parámetro POST (o=sum); suma entre (a=N) y (b=N)
 *      - parámetro POST (o=res); resta entre (a=N) y (b=N)
 *      - parámetro POST (o=mul); multiplicación entre (a=N) y (b=N)
 *      - parámetro POST (o=div); división entre (a=N) y (b=N)
 */
public class MainActivity extends AppCompatActivity {
    // Dirección de servidor + script de aritmética.
    public static final String URL_SCRIPT_ARITMETICA = "http://162.243.64.94/dm.php";

    // Operaciones soportadas por la aplicación y por el servidor.
    public static final int OP_NINGUNA = 0;
    public static final int OP_SUMA = 1;
    public static final int OP_RESTA = 2;
    public static final int OP_MULTIPLICACION = 3;
    public static final int OP_DIVISION = 4;

    /*
     *  Views que serán alterados dinámicamente en tiempo de ejecución.
     *      - informacion: log informativo de operaciones en tiempo de ejecución.
     *      - resultado: campo que muestra resultados y valores siendo ingresados por el usuario.
     *      - scrollInfo: ScrollView contenedor de información. Hace scroll automático al fondo.
     */
    private TextView informacion;
    private TextView resultado;
    private ScrollView scrollInfo;

    /*
     *  En toda operación aritmética binaria (en términos de cantidad de operandos), vanterior será
     *  el primer operando y vanterior el segundo.
     *
     *  Generalmente, vactual contiene el valor del campo de texto resultado convertido en double.
     */
    private double vactual;
    private double vanterior;

    /*
     *  True si ya se ha ingresado un punto decimal.
     *
     *  Mantengo un registro del punto decimal ya que la entrada de dígitos y operaciones es hecha
     *  exclusivamente a travéz de botones. La variable es usada para prevenir que el usuario
     *  escriba dos puntos decimales en un valor numérico.
     */
    private boolean decimal = false;

    // En tiempo de ejecución, esta variable contendrá un registro de la operación siendo efectuada.
    private int operacion;

    // Si es true, entonces no se pueden hacer mas requests hasta que termine la actual.
    private boolean ocupado;

    /** Clase anidada privada con métodos que manejan eventos de request POST. */
    private class HandlerRespuestaOperacion extends AsyncHttpResponseHandler {
        /**
         *  Ejecutado automáticamente cuando un request recibe una respuesta con un código de
         *  estado HTTP que indica éxito.
         *
         *  Es importante mencionar que un código de estado distinto a 200, a pesar de haber
         *  sido exitoso, es tratado como un request fallido por esta aplicación.
         */
        @Override
        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
            if (statusCode != 200) {
                informacion("Error HTTP: " + statusCode + "(no es 200)");
                return;
            }

            /*
             *  Contiene una representación como String del arreglo de bytes recibido como
             *  respuesta.
             *
             *  Es almacenado temporalmente ya que para realizar calculaciones, se debe verificar
             *  que el valor recibido es un double válido. Luego se intentará convertir este String
             *  en un double.
             */
            String temporalRespuestaString;
            temporalRespuestaString = responseBody.toString();

            /*
             *  Se intenta convertir el String recibido como respuesta a double. Si la respuesta
             *  no es un double válido, entonces la operación se considera como fallida ya que
             *  su valor "no es un número" y no puede ser usado en más operaciones.
             */
            try {
                Double.valueOf(temporalRespuestaString);
            } catch(ClassCastException e) {
                informacion("Respuesta HTTP no es un número válido.");
                borrar();
                return;
            }

            /*
             *  Ya que la aplicación mantiene un registro del punto decimal en los valores de
             *  entrada, se intenta encontrar un punto decimal en la respuesta.
             */
            decimal = temporalRespuestaString.contains(".");

            // Limpieza. Dejando la Activity lista para realizar más operaciones.
            operacion = OP_NINGUNA;
            ocupado = false;

            // Se actualiza el TextView de respuesta.
            resultado.setText(temporalRespuestaString);
            informacion("Respuesta recibida.");
        }

        /**
         *  Error HTTP.
         *  Se muestra el código de estado HTTP.
         */
        @Override
        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
            informacion("Error HTTP: " + statusCode);
        }

        /**
         *  Error HTTP.
         *  Si el cliente está configurado para reintentar, muestra la cantidad de veces que se
         *  ha reiniciado el request.
         */
        @Override
        public void onRetry(int retryNo) {
            informacion("Reintento #" + retryNo);
        }

        /*
         *  DEBUG: muestra la cantidad de bytes enviados.
         *  Podría ser útil para comprobar el progreso de un upload.
         */
        /*@Override
        public void onProgress(long bytesWritten, long totalSize) {
            informacion(String.format("Progreso: %d/%d", bytesWritten, totalSize));
        }*/

        /*
         *  DEBUG: muestra que el request ha sido iniciado.
         */
        /*@Override
        public void onStart() {
            informacion("onStart()");
        }*/

        /*
         *  DEBUG: ejecutado cuando la operación termina; exitosamente o no.
         */
        /*@Override
        public void onFinish() {
            informacion("onFinish()");
        }*/
    }

    /**
     *  Agrega un nuevo mensaje al log informativo y hace scroll hacia abajo.
     */
    public void informacion(String linea) {
        informacion.setText(informacion.getText() + "\n" + linea);
        scrollInfo.fullScroll(ScrollView.FOCUS_DOWN);
    }

    /**
     *  Hace un request a servidor remoto solicitando que se calcule una operación cualquiera
     *  entre dos operandos. Los handlers usados en el interior de este método actualizan la
     *  interfaz de usuario de acuerdo a las respuestas recividas.
     *
     *  Es un request POST con tres parámetros: o=<sum|res|mul|div>; a=<N> y b=<M>, en donde "o"
     *  toma uno de cuatro posibles valores, y "a" y "b" toman valores double. La operación "o"
     *  es efectuada usando los operandos "a" y "b". Dentro del cuerpo del método, "o" toma un
     *  valor distinto dependiendo del valor de la variable "operacion", "a" equivale siempre al
     *  valor de "vanterior", y "b" equivale siempre al valor de "vactual".
     *
     *  Como el método es ejecutado al presioner el boton "=", no se hace nada si no se está
     *  efectuando una operación (es decir, si operacion es igual a OP_NINGUNA, o si no es alguna
     *  de las operaciones OP_* soportadas.)
     *
     *  Si se está esperando una respuesta de un request POST; la función no hace nada y muestra
     *  que hay un request en progreso.
     *
     *  Por último, se muestra un error si se intenta efectuar una división por 0.
     *
     *  NOTA: ver archivo "README.md" por mas detalles. El método operar() debería funcionar, pero
     *  posiblemente contiene errores ya que se me hizo imposible probar requests POST en la URL
     *  del script. Por favor leer!
     */
    public void operar() {
        // Contendrá los parámetros POST "o", "a" y "b" del request HTTP.
        RequestParams parametros = new RequestParams();

        // No se hace nada si hay un request en progreso.
        if (ocupado) {
            informacion("Request HTTP en progreso. Reintente luego.");
            return;
        }

        // Dependiendo de la operación siendo efectuada...
        switch(operacion) {
            // Hace un request de división. Es un error si "b" (vactual) es 0.
            case OP_DIVISION:
                // División por 0 es indefinida e inaceptable  por motivos de estabilidad de la aplicación.
                if (vactual == 0) {
                    informacion("Error. Intento de división por 0.");
                    borrar();
                    return;
                }

                // Configuración de parámetros.
                parametros.put("o", "div");
                parametros.put("a", Double.toString(vanterior));
                parametros.put("b", Double.toString(vactual));

                // Se inicia el request y se muestra un mensaje al usuario.
                informacion("POST: " + URL_SCRIPT_ARITMETICA);
                informacion(String.format("Parámetros: o=%s; a=%s; b=%s", "div", vanterior, vactual));
                AuxiliarHTTP.post(URL_SCRIPT_ARITMETICA, parametros, new HandlerRespuestaOperacion());
                break;

            // Hace un request de multiplicación.
            case OP_MULTIPLICACION:
                // Configuración de parámetros.
                parametros.put("o", "mul");
                parametros.put("a", Double.toString(vanterior));
                parametros.put("b", Double.toString(vactual));

                // Se inicia el request y se muestra un mensaje al usuario.
                informacion("POST: " + URL_SCRIPT_ARITMETICA);
                informacion(String.format("Parámetros: o=%s; a=%s; b=%s", "mul", vanterior, vactual));
                AuxiliarHTTP.post(URL_SCRIPT_ARITMETICA, parametros, new HandlerRespuestaOperacion());
                break;

            // Hace un request de resta.
            case OP_RESTA:
                // Configuración de parámetros.
                parametros.put("o", "res");
                parametros.put("a", Double.toString(vanterior));
                parametros.put("b", Double.toString(vactual));

                // Se inicia el request y se muestra un mensaje al usuario.
                informacion("POST: " + URL_SCRIPT_ARITMETICA);
                informacion(String.format("Parámetros: o=%s; a=%s; b=%s", "res", vanterior, vactual));
                AuxiliarHTTP.post(URL_SCRIPT_ARITMETICA, parametros, new HandlerRespuestaOperacion());
                break;
            case OP_SUMA:
                // Configuración de parámetros.
                parametros.put("o", "sum");
                parametros.put("a", Double.toString(vanterior));
                parametros.put("b", Double.toString(vactual));

                // Se inicia el request y se muestra un mensaje al usuario.
                informacion("POST: " + URL_SCRIPT_ARITMETICA);
                informacion(String.format("Parámetros: o=%s; a=%s; b=%s", "sum", vanterior, vactual));
                AuxiliarHTTP.post(URL_SCRIPT_ARITMETICA, parametros, new HandlerRespuestaOperacion());
                break;
            default:
                // No se está haciendo una operación reconocida. Nada que hacer.
                return;
        }
    }

    /**
     *  Instruye a la aplicación que se realizará una división con el siguiente valor, sólo sí
     *  no se está efectuando otra operación en el instante.
     */
    public void division() {
        // Se está realizando otra operación. Usuario debe borrar u obtener un resultado primero.
        if (operacion != OP_NINGUNA) {
            informacion("Operación pendiente. Presione \"=\" primero.");
            return;
        }

        // Cambiando estado de aplicación a modo "división."
        operacion = OP_DIVISION;
        vanterior = vactual;
        vactual = 0.0;

        // Informar al usuario.
        resultado.setText("0");
        informacion("Realizando una división de \"" + vanterior + "\".");
    }

    /**
     *  Instruye a la aplicación que se realizará una multiplicación con el siguiente valor, sólo sí
     *  no se está efectuando otra operación en el instante.
     */
    public void multiplicacion() {
        // Se está realizando otra operación. Usuario debe borrar u obtener un resultado primero.
        if (operacion != OP_NINGUNA) {
            informacion("Operación pendiente. Presione \"=\" primero.");
            return;
        }

        // Cambiando estado de aplicación a modo "multiplicación."
        operacion = OP_MULTIPLICACION;
        vanterior = vactual;
        vactual = 0.0;

        // Informar al usuario.
        resultado.setText("0");
        informacion("Realizando una multiplicación por \"" + vanterior + "\".");
    }

    /**
     *  Instruye a la aplicación que se realizará una resta con el siguiente valor, sólo sí
     *  no se está efectuando otra operación en el instante.
     */
    public void resta() {
        // Se está realizando otra operación. Usuario debe borrar u obtener un resultado primero.
        if (operacion != OP_NINGUNA) {
            informacion("Operación pendiente. Presione \"=\" primero.");
            return;
        }

        // Cambiando estado de aplicación a modo "resta."
        operacion = OP_RESTA;
        vanterior = vactual;
        vactual = 0.0;

        // Informar al usuario.
        resultado.setText("0");
        informacion("Realizando una resta de \"" + vanterior + "\".");
    }

    /**
     *  Instruye a la aplicación que se realizará una suma con el siguiente valor, sólo sí
     *  no se está efectuando otra operación en el instante.
     */
    public void suma() {
        // Se está realizando otra operación. Usuario debe borrar u obtener un resultado primero.
        if (operacion != OP_NINGUNA) {
            informacion("Operación pendiente. Presione \"=\" primero.");
            return;
        }

        // Cambiando estado de aplicación a modo "suma."
        operacion = OP_SUMA;
        vanterior = vactual;
        vactual = 0.0;

        // Informar al usuario.
        resultado.setText("0");
        informacion("Realizando una suma con \"" + vanterior + "\".");
    }

    /**
     * Intenta agregar un nuevo dígito al campo de respuesta si el dígito que se intenta agregar
     * es un número de 0 al 9 o si el dígito es el punto decimal (en este caso, sólo se agregará
     * el punto si el valor de entrada no contiene otro punto decimal.)
     *
     * Si el botón presionado corresponde a una operación, el estado de la aplicación cambia para
     * prepararse para efectuar dicha operación si y solo si no se está realizando otra operación.
     *
     * Si el botón presionado es el signo "=", entronces se efectua la operación pendiente entre
     * vanterior y vactual, sólo si hay una operación pendiente.
     */
    public void digito(String digito) {
        // Contiene el texto del TextView que contiene respuestas o valores numéricos de entrada.
        String resultado = this.resultado.getText().toString().trim();

        // Usado para confirmar si "resultado" es un double válido por medio de castings y try-catch
        double valor = 0.0;

        // Si el dígito presionado es el punto decimal y el número ya contenía uno, no se hace nada.
        if (digito == "." && decimal) {
            informacion("Error. Ya existe un punto decimal.");
            return;
        }

        /*
         *  Si el valor de entrada está vacío, o si es exactamente igual a 0, entonces:
         *      - Si se presiona el punto decimal, se agrega el punto y se marca como agregado.
         *      - Si no, si se presionó un dígito de 0 a 9, entonces se reemplaza 0 por nuevo valor.
         */
        if (resultado.compareTo("") == 0 || resultado.compareTo("0") == 0) {
            if (digito == ".") {
                this.resultado.setText("0.");
                decimal = true;
                vactual = 0;
            } else {
                this.resultado.setText(digito);
                vactual = Double.parseDouble(digito);
            }

            return;
        }

        /*
         *  Se intenta convertir el valor del campo de resultado/entrada a un double.
         *
         *  Es un error (no fatal, gracias a try-catch) si el valor ingresado no es un número
         *  double válido por cual sea la razón.
         */
        resultado = resultado + digito;
        try {
            valor = Double.valueOf(resultado);
        } catch (NumberFormatException e) {
            informacion("Error. Número fuera de rango.");
            borrar();
            return;
        }

        // Se actualiza la representación textual del valor por el nuevo double, y se asigna a vactual.
        this.resultado.setText(resultado);
        vactual = valor;
    }

    /**
     * Borra el resultado y todos los valores intermedio.
     * Deja a la aplicación lista para realizar otra operación.
     */
    public void borrar() {
        resultado.setText("0");

        vactual = 0;
        vanterior = 0;
        decimal = false;

        operacion = OP_NINGUNA;
        ocupado = false;
    }

    /**
     * Handler de click de todos los botones en MainActivity.
     * Ejecuta una función correspondiente a cada botón.
     *
     * Si se trata de botones numéricos o el punto, entonces llama el método digito().
     *
     * Si se trata de botones correspondientes a operaciones aritméticas, se llama el
     * método correspondiente al texto del botón; sea suma(), resta(), multiplicacion() o
     * division(), respectivamente.
     *
     * Si se trata del botón "=", entonces se intenta realizar una operación pendiente usando
     * el método operar().
     */
    public void botonPresionado(View boton) {
        // Temporales.
        Button b = null;
        String operacion;

        // Si el view presionado es null, no se hace nad.a
        if (boton == null) {
            return;
        }

        // Si el view presionado no es un botón, no se hace nada.
        try {
            b = (Button) boton;
        } catch (ClassCastException e) {
            return;
        }

        // Si es un botón, entonces es un botón conocido? O un boton soportado por la aplicación?
        operacion = b.getText().toString().trim();
        switch(operacion) {
            // Si se presionó un botón correspondiente a un dígito o al punto decimal, llama digito.
            case "0":
            case "1":
            case "2":
            case "3":
            case "4":
            case "5":
            case "6":
            case "7":
            case "8":
            case "9":
            case ".":
                digito(operacion);
                break;

            // Si se presionó un botón correspondiente a una operación, se llama su respectivo método.
            case "+":
                suma();
                break;
            case "-":
                resta();
                break;
            case "x":
                multiplicacion();
                break;
            case "/":
                division();
                break;

            // Si se presionó un botón correspondiente al sigo "=", se intenta efectuar operación.
            case "=":
                operar();
                break;

            // Botón inválido.
            default:
                // Nada
        }
    }

    /**
     * Inicializa MainActivity, dejándola lista para su uso.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Obteniendo referencias a views que serán usados frecuentemente.
        informacion = (TextView) findViewById(R.id.informacion);
        resultado = (TextView) findViewById(R.id.resultado);
        scrollInfo = (ScrollView) findViewById(R.id.scrollInfo);

        /*
         *  Configuración de estado inicial de la aplicación.
         *      - Los operandos son inicializados a 0.
         *      - El valor de entrada no contiene un punto decimal.
         *      - No se está realizando alguna operación.
         *      - No se está realizando un request HTTP.
         */
        vanterior = 0.0;
        vactual = 0.0;
        decimal = false; // Contiene el campo de texto resultado un punto decimal? NO.
        operacion = OP_NINGUNA; // Se está realizando una operación? NO. Ninguna.
        ocupado = false; // HTTP request en progreso? NO.

        // Dando valor inicial al campo de entrada. Corresponderá siempre al valor de vactual.
        resultado.setText("0");

        /*
         *  Asignando la función borrar() al evento click del campo de texto de resultado.
         *
         *  Será el mecanismo principal usado par eliminar números y cancelar operaciones por
         *  parte del usuario.
         *
         *  Otros métodos son capaces de llamar borrar() en caso de error.
         */
        resultado.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                borrar();
                informacion("Borrando.");
            }
        });
    }
}
