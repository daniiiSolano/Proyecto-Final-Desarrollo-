package modelo;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServicioBCCR {

    public static final String ARCHIVO_TOKEN = "recursos/token_bccr.txt";

    private static final String URL_BASE = "https://apim.bccr.fi.cr/"
            + "SDDE/api/Bccr.GE.SDDE.Publico.Indicadores.API/"
            + "indicadoresEconomicos/";
    private static final String INDICADOR_TIPO_CAMBIO = "317";
    private static final int DIAS_ATRAS = 7;
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy'%2F'MM'%2F'dd");
    private static final Pattern PATRON_VALOR =
            Pattern.compile("\"valorDatoPorPeriodo\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)");

    public static String leerToken() throws IOException {
        Path ruta = Path.of(ARCHIVO_TOKEN);
        if (!Files.exists(ruta)) {
            throw new IOException("No se encontró el archivo " + ARCHIVO_TOKEN);
        }
        String token = Files.readString(ruta, StandardCharsets.UTF_8).trim();
        if (token.isEmpty()) {
            throw new IOException("El archivo " + ARCHIVO_TOKEN + " está vacío");
        }
        return token;
    }

    public static double obtenerTipoCambio(String token) throws Exception {
        LocalDate hoy = LocalDate.now();
        String url = URL_BASE + INDICADOR_TIPO_CAMBIO + "/series"
                + "?fechaInicio=" + hoy.minusDays(DIAS_ATRAS).format(FORMATO_FECHA)
                + "&fechaFin=" + hoy.format(FORMATO_FECHA)
                + "&idioma=ES";

        HttpRequest solicitud = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpClient cliente = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();
        HttpResponse<String> respuesta = cliente.send(solicitud, HttpResponse.BodyHandlers.ofString());

        if (respuesta.statusCode() != 200) {
            throw new RuntimeException("Error del BCCR: " + respuesta.statusCode());
        }
        return extraerUltimoValor(respuesta.body());
    }

    static double extraerUltimoValor(String json) {
        Matcher comparador = PATRON_VALOR.matcher(json);
        String ultimo = null;
        while (comparador.find()) {
            ultimo = comparador.group(1);
        }
        if (ultimo == null) {
            throw new RuntimeException("No se encontró el valor del tipo de cambio: " + json);
        }
        return Double.parseDouble(ultimo);
    }

    public static void main(String[] args) {
        try {
            double tipoCambio = obtenerTipoCambio(leerToken());
            System.out.println("Tipo de cambio: " + tipoCambio);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
