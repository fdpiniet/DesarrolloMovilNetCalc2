package xyz.fabianpineda.desarrollomovil.netcalc;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    // Views que serán alterados dinámicamente.
    private TextView informacion;
    private TextView resultado;
    private ScrollView scrollInfo;

    // Corresponden al valor actual y el valor anterior, como operandos, en una operación bianria.
    private double vactual;
    private double vanterior;

    // True si ya se ha ingresado un punto decimal.
    private boolean decimal = false;

    // Operaciones soportadas.
    public static final int OP_NINGUNA = 0;
    public static final int OP_SUMA = 1;
    public static final int OP_RESTA = 2;
    public static final int OP_MULTIPLICACION = 3;
    public static final int OP_DIVISION = 4;

    // Tomará el valor de alguna de las operaciones soportadas por la aplicación.
    private int operacion;

    public void informacion(String linea) {
        informacion.setText(informacion.getText() + "\n" + linea);
        scrollInfo.fullScroll(ScrollView.FOCUS_DOWN);
    }

    public void operar() {
        String temporal;

        switch(operacion) {
            case OP_DIVISION:
                if (vactual == 0) {
                    informacion("Error. Intento de división por 0.");
                    borrar();
                    return;
                }
                vactual = vanterior / vactual;
                break;
            case OP_MULTIPLICACION:
                    vactual = vanterior * vactual;
                break;
            case OP_RESTA:
                    vactual = vanterior - vactual;
                break;
            case OP_SUMA:
                    vactual = vanterior + vactual;
                break;
            default:
                // No se está haciendo una operación. No se hace nada.
                return;
        }

        vanterior = 0;
        temporal = Double.toString(vactual);
        decimal = temporal.contains(".");
        operacion = OP_NINGUNA;
        this.resultado.setText(temporal);
    }

    public void division() {
        if (operacion != OP_NINGUNA) {
            informacion("Operación pendiente. Presione \"=\" primero.");
            return;
        }

        operacion = OP_DIVISION;

        vanterior = vactual;
        vactual = 0.0;

        resultado.setText("0");
        informacion("Realizando una división de \"" + vanterior + "\".");
    }

    public void multiplicacion() {
        if (operacion != OP_NINGUNA) {
            informacion("Operación pendiente. Presione \"=\" primero.");
            return;
        }

        operacion = OP_MULTIPLICACION;

        vanterior = vactual;
        vactual = 0.0;

        resultado.setText("0");
        informacion("Realizando una multiplicación con \"" + vanterior + "\".");
    }

    public void resta() {
        if (operacion != OP_NINGUNA) {
            informacion("Operación pendiente. Presione \"=\" primero.");
            return;
        }

        operacion = OP_RESTA;

        vanterior = vactual;
        vactual = 0.0;

        resultado.setText("0");
        informacion("Realizando una resta a \"" + vanterior + "\".");
    }

    public void suma() {
        if (operacion != OP_NINGUNA) {
            informacion("Operación pendiente. Presione \"=\" primero.");
            return;
        }

        operacion = OP_SUMA;

        vanterior = vactual;
        vactual = 0.0;

        resultado.setText("0");
        informacion("Realizando una suma con \"" + vanterior + "\".");
    }

    /**
     * Intenta agregar un nuevo dígito al campo de respuesta.
     * Funciona para los números 0-9 y para el punto.
     */
    public void digito(String digito) {
        String resultado = this.resultado.getText().toString().trim();
        double valor = 0.0;

        if (digito == "." && decimal) {
            informacion("Error. Ya existe un punto decimal.");
            return;
        }

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

        resultado = resultado + digito;

        try {
            valor = Double.valueOf(resultado);
        } catch (NumberFormatException e) {
            informacion("Error. Número fuera de rango.");
            borrar();
            return;
        }

        this.resultado.setText(resultado);
        vactual = valor;
    }

    /**
     * Borra el resultado y todo valor intermedio.
     */
    public void borrar() {
        resultado.setText("0");

        vactual = 0;
        vanterior = 0;
        decimal = false;

        operacion = OP_NINGUNA;

        informacion("Borrando.");
    }

    /**
     * Handler de click de todos los botones en MainActivity.
     * Ejecuta una función correspondiente a cada botón.
     * Si se trata de botones numéricos o el punto, entonces llama la función editar.
     *
     * @param boton View correspondiente al botón que fue presionado.
     */
    public void botonPresionado(View boton) {
        // Es este view en realidad un botón?
        Button b = null;
        String operacion;

        if (boton == null) {
            return;
        }

        try {
            b = (Button) boton;
        } catch (ClassCastException e) {
            return;
        }

        // Si es un botón, entonces es un botón conocido?
        operacion = b.getText().toString();
        switch(operacion) {
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
            case "=":
                operar();
                break;
            default:
                // Botón inválido.
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        informacion = (TextView) findViewById(R.id.informacion);
        resultado = (TextView) findViewById(R.id.resultado);
        scrollInfo = (ScrollView) findViewById(R.id.scrollInfo);

        resultado.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                borrar();
            }
        });

        resultado.setText("0");

        vanterior = 0.0;
        vactual = 0.0;
        decimal = false;
        operacion = OP_NINGUNA;
    }
}
