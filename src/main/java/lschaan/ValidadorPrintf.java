package lschaan;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ValidadorPrintf {

    public static void main(String[] args) {
        Scanner inputScanner = new Scanner(System.in);
        System.out.print("Digite o nome do arquivo: ");
        String filename = inputScanner.nextLine();

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String linha;
            int numeroLinha = 1;

            while ((linha = reader.readLine()) != null) {
                boolean ehValido = processarLinha(linha, numeroLinha);

                if (ehValido) {
                    System.out.println("Linha " + numeroLinha + ": Comando válido.");
                }

                numeroLinha++;
            }

        } catch (FileNotFoundException e) {
            System.err.println("Arquivo não encontrado: " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            inputScanner.close();
        }
    }

    private static boolean processarLinha(String linha, int numeroLinha) {
        Lexer lexer = new Lexer(linha);

        try {
            List<Token> tokens = lexer.tokenize();
            Parser parser = new Parser(tokens);
            return parser.parse();
        } catch (Exception e) {
            System.out.println("Linha " + numeroLinha + ": Comando inválido. [" + e.getMessage() + "]");
            // e.printStackTrace();
            return false;
        }
    }

    enum TipoToken {
        PRINTF, PARENTESE_ESQ, PARENTESE_DIR, PONTO_VIRGULA,
        STRING_LITERAL, VIRGULA,
        IDENTIFICADOR, NUMERO, OPERADOR,
        ESPECIFICADOR_FORMATO, FUNCAO, EOF
    }

    static class Token {
        TipoToken tipo;
        String valor;

        Token(TipoToken tipo, String valor) {
            this.tipo = tipo;
            this.valor = valor;
        }
    }

    static class Lexer {
        private final String entrada;
        private final int comprimento;
        private int posicao;

        public Lexer(String entrada) {
            this.entrada = entrada;
            this.posicao = 0;
            this.comprimento = entrada.length();
        }

        public List<Token> tokenize() throws Exception {
            List<Token> tokens = new ArrayList<>();

            while (posicao < comprimento) {
                char caractereAtual = entrada.charAt(posicao);

                if (Character.isWhitespace(caractereAtual)) {
                    posicao++;
                } else if (entrada.startsWith("printf", posicao)) {
                    tokens.add(new Token(TipoToken.PRINTF, "printf"));
                    posicao += 6;
                } else if (caractereAtual == '(') {
                    tokens.add(new Token(TipoToken.PARENTESE_ESQ, "("));
                    posicao++;
                } else if (caractereAtual == ')') {
                    tokens.add(new Token(TipoToken.PARENTESE_DIR, ")"));
                    posicao++;
                } else if (caractereAtual == ';') {
                    tokens.add(new Token(TipoToken.PONTO_VIRGULA, ";"));
                    posicao++;
                } else if (caractereAtual == ',') {
                    tokens.add(new Token(TipoToken.VIRGULA, ","));
                    posicao++;
                } else if (caractereAtual == '"') {
                    String stringLiteral = lerStringLiteral();
                    tokens.add(new Token(TipoToken.STRING_LITERAL, stringLiteral));
                } else if (caractereAtual == '%') {
                    String especificadorFormato = lerEspecificadorFormato();
                    tokens.add(new Token(TipoToken.ESPECIFICADOR_FORMATO, especificadorFormato));
                } else if (Character.isLetter(caractereAtual)) {
                    String identificador = lerIdentificador();
                    tokens.add(new Token(TipoToken.IDENTIFICADOR, identificador));
                } else if (Character.isDigit(caractereAtual)) {
                    String numero = lerNumero();
                    tokens.add(new Token(TipoToken.NUMERO, numero));
                } else if ("+-*/".indexOf(caractereAtual) != -1) {
                    tokens.add(new Token(TipoToken.OPERADOR, String.valueOf(caractereAtual)));
                    posicao++;
                } else {
                    throw new Exception("Caractere inválido: " + caractereAtual);
                }
            }

            tokens.add(new Token(TipoToken.EOF, ""));
            return tokens;
        }

        private String lerStringLiteral() throws Exception {
            StringBuilder sb = new StringBuilder();
            sb.append('"');
            posicao++; // pula aspas iniciais

            while (posicao < comprimento) {
                char caractereAtual = entrada.charAt(posicao);

                if (caractereAtual == '"') {
                    sb.append('"');
                    posicao++; // pula aspas finais
                    break;
                } else {
                    sb.append(caractereAtual);
                    posicao++;
                }
            }

            return sb.toString();
        }

        private String lerEspecificadorFormato() {
            StringBuilder sb = new StringBuilder();
            sb.append('%');
            posicao++; // pula '%'

            while (posicao < comprimento) {
                char caractereAtual = entrada.charAt(posicao);

                if (Character.isLetter(caractereAtual) || caractereAtual == '.' || Character.isDigit(caractereAtual)) {
                    sb.append(caractereAtual);
                    posicao++;
                } else {
                    break;
                }
            }

            return sb.toString();
        }

        private String lerIdentificador() {
            StringBuilder sb = new StringBuilder();

            while (posicao < comprimento && (Character.isLetterOrDigit(entrada.charAt(posicao)) || entrada.charAt(posicao) == '_')) {
                sb.append(entrada.charAt(posicao));
                posicao++;
            }

            return sb.toString();
        }

        private String lerNumero() {
            StringBuilder sb = new StringBuilder();

            while (posicao < comprimento && (Character.isDigit(entrada.charAt(posicao)) || entrada.charAt(posicao) == '.')) {
                sb.append(entrada.charAt(posicao));
                posicao++;
            }

            return sb.toString();
        }
    }

    static class Parser {
        private final List<Token> tokens;
        private final List<String> tiposEsperados = new ArrayList<>();
        private final List<String> tiposFornecidos = new ArrayList<>();
        private int posicao;
        private Token tokenAtual;
        private int contadorEspecificadores = 0;
        private int contadorArgumentos = 0;

        public Parser(List<Token> tokens) {
            this.tokens = tokens;
            this.posicao = 0;
            this.tokenAtual = tokens.get(posicao);
        }

        public boolean parse() throws Exception {
            parseExpressaoPrintf();

            if (contadorEspecificadores != contadorArgumentos) {
                throw new Exception("Número de argumentos não corresponde ao número de especificadores de formato.");
            }

            // verifica compatibilidade de tipos
            for (int i = 0; i < tiposEsperados.size(); i++) {
                String tipoEsperado = tiposEsperados.get(i);
                String tipoFornecido = tiposFornecidos.get(i);

                if (!tipoEsperado.equals("desconhecido") && !tipoFornecido.equals("desconhecido")) {
                    if (!tipoEsperado.equals(tipoFornecido)) {
                        throw new Exception("Tipo do argumento não corresponde ao especificador de formato.");
                    }
                }
            }

            return true;
        }

        private void parseExpressaoPrintf() throws Exception {
            match(TipoToken.PRINTF);
            match(TipoToken.PARENTESE_ESQ);
            parseConteudoPrintf();
            match(TipoToken.PARENTESE_DIR);
            match(TipoToken.PONTO_VIRGULA);
            match(TipoToken.EOF);
        }

        private void parseConteudoPrintf() throws Exception {
            parseStringLiteral();

            if (tokenAtual.tipo == TipoToken.VIRGULA) {
                match(TipoToken.VIRGULA);
                parseListaDeArgumentos();
            } else {
                contadorArgumentos = 0; // Nenhum argumento encontrado
            }
        }

        private void parseStringLiteral() throws Exception {
            String literal = tokenAtual.valor;
            extrairEspecificadoresFormato(literal);
            match(TipoToken.STRING_LITERAL);
        }

        private void extrairEspecificadoresFormato(String literal) {
            Pattern pattern = Pattern.compile("%[\\w\\.]*");
            Matcher matcher = pattern.matcher(literal);

            contadorEspecificadores = 0;
            tiposEsperados.clear();

            while (matcher.find()) {
                String espec = matcher.group();
                String tipo = determinarTipoEspecificador(espec);
                tiposEsperados.add(tipo);
                contadorEspecificadores++;
            }
        }

        private String determinarTipoEspecificador(String espec) {
            if (espec.matches("%(d|i)")) {
                return "inteiro";
            } else if (espec.matches("%(f|\\.\\df)")) {
                return "float";
            } else if (espec.equals("%s")) {
                return "string";
            } else if (espec.equals("%c")) {
                return "char";
            } else {
                return "desconhecido";
            }
        }

        private void parseListaDeArgumentos() throws Exception {
            tiposFornecidos.clear();
            String tipo = parseExpressao();
            contadorArgumentos++;
            tiposFornecidos.add(tipo);

            while (tokenAtual.tipo == TipoToken.VIRGULA) {
                match(TipoToken.VIRGULA);
                tipo = parseExpressao();
                contadorArgumentos++;
                tiposFornecidos.add(tipo);
            }
        }

        private String parseExpressao() throws Exception {
            String tipo = parseTermo();

            while (tokenAtual.tipo == TipoToken.OPERADOR && (tokenAtual.valor.equals("+") || tokenAtual.valor.equals("-"))) {
                String operador = tokenAtual.valor;
                match(TipoToken.OPERADOR);
                String tipoDireita = parseTermo();

                if (tipo.equals("float") || tipoDireita.equals("float")) {
                    tipo = "float";
                } else {
                    tipo = "inteiro";
                }
            }

            return tipo;
        }

        private String parseTermo() throws Exception {
            String tipo = parseFator();

            while (tokenAtual.tipo == TipoToken.OPERADOR && (tokenAtual.valor.equals("*") || tokenAtual.valor.equals("/"))) {
                String operador = tokenAtual.valor;
                match(TipoToken.OPERADOR);
                String tipoDireita = parseFator();

                if (tipo.equals("float") || tipoDireita.equals("float")) {
                    tipo = "float";
                } else {
                    tipo = "inteiro";
                }
            }

            return tipo;
        }

        private String parseFator() throws Exception {
            if (tokenAtual.tipo == TipoToken.IDENTIFICADOR) {
                String id = tokenAtual.valor;
                match(TipoToken.IDENTIFICADOR);

                if (tokenAtual.tipo == TipoToken.PARENTESE_ESQ) {
                    match(TipoToken.PARENTESE_ESQ);

                    if (tokenAtual.tipo != TipoToken.PARENTESE_DIR) {
                        parseListaDeArgumentos();
                    }

                    match(TipoToken.PARENTESE_DIR);
                }

                return "desconhecido"; //tipo desconhecido para identificadores/funções
            } else if (tokenAtual.tipo == TipoToken.NUMERO) {
                String valor = tokenAtual.valor;
                match(TipoToken.NUMERO);

                if (valor.contains(".")) {
                    return "float";
                } else {
                    return "inteiro";
                }
            } else if (tokenAtual.tipo == TipoToken.PARENTESE_ESQ) {
                match(TipoToken.PARENTESE_ESQ);
                String tipo = parseExpressao();
                match(TipoToken.PARENTESE_DIR);
                return tipo;
            } else {
                throw new Exception("Esperado identificador, número ou '(', encontrado: " + tokenAtual.valor);
            }
        }

        private void match(TipoToken tipoEsperado) throws Exception {
            if (tokenAtual.tipo == tipoEsperado) {
                posicao++;
                if (posicao < tokens.size()) {
                    tokenAtual = tokens.get(posicao);
                }
            } else {
                throw new Exception("Esperado " + tipoEsperado + ", encontrado " + tokenAtual.tipo);
            }
        }
    }
}
