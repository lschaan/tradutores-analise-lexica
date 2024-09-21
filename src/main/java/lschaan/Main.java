package lschaan;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

public class Main {
    // Conjunto de palavras reservadas
    private static final Set<String> RESERVED_WORDS = new HashSet<>(Arrays.asList(
        "int", "double", "float", "real", "break", "case", "char", "const", "continue"
    ));
    // Padrões para reconhecimento de tokens
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9]*$");
    private static final Pattern INTEGER_PATTERN = Pattern.compile("^([0-9]|[1-9][0-9])$");
    private static final Pattern REAL_PATTERN = Pattern.compile("^([0-9]|[1-9][0-9])\\.\\d{2}$");
    private static final Pattern COMMENT_PATTERN = Pattern.compile("^//.*$");
    // Estruturas de dados
    private static final List<Token> tokens = new ArrayList<>();
    private static final Map<String, Integer> symbolTable = new LinkedHashMap<>();
    private static final List<Error> errors = new ArrayList<>();
    // Contador para IDs de símbolos
    private static int symbolIdCounter = 1;

    public static void main(String[] args) {
        Scanner inputScanner = new Scanner(System.in);
        System.out.print("Digite o nome do arquivo: ");
        String filename = inputScanner.nextLine();

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                processLine(line, lineNumber);
                lineNumber++;
            }

            // Exibir resultados
            displayTokens();
            displaySymbolTable();
            displayErrors();

        } catch (FileNotFoundException e) {
            System.err.println("Arquivo não encontrado: " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            inputScanner.close();
        }
    }

    private static void processLine(String line, int lineNumber) {
        if (COMMENT_PATTERN.matcher(line).matches()) {
            tokens.add(new Token(lineNumber, "COMENTARIO", line));
            return;
        }

        String[] words = line.split("\\s+");

        if (words.length == 0 || (words.length == 1 && words[0].isEmpty())) {
            return;
        }

        if (words.length != 1) {
            errors.add(new Error(lineNumber, line));
            return;
        }

        //Ao chegar neste ponto, é confirmado que a linha tem exatamente 1 palavra
        String word = words[0];
        String tokenType = identifyToken(word);

        if (tokenType.equals("ERRO")) {
            errors.add(new Error(lineNumber, line));
            return;
        }

        Token token = new Token(lineNumber, tokenType, word);

        // Adicionar ao token a ID do símbolo, se aplicável
        if (tokenType.equals("IDENTIFICADOR") || tokenType.equals("NUMERO_INTEIRO") || tokenType.equals("NUMERO_REAL")) {
            if (!symbolTable.containsKey(word)) {
                symbolTable.put(word, symbolIdCounter++);
            }
            token.setSymbolId(symbolTable.get(word));
        }

        tokens.add(token);
    }

    private static String identifyToken(String word) {
        if (RESERVED_WORDS.contains(word)) {
            return word.toUpperCase();
        } else if (IDENTIFIER_PATTERN.matcher(word).matches()) {
            return "IDENTIFICADOR";
        } else if (INTEGER_PATTERN.matcher(word).matches()) {
            return "NUMERO_INTEIRO";
        } else if (REAL_PATTERN.matcher(word).matches()) {
            return "NUMERO_REAL";
        } else {
            return "ERRO";
        }
    }


    //Métodos de exibição
    private static void displayTokens() {
        System.out.println("Tokens de Entrada");
        for (Token token : tokens) {
            String output = "[" + token.getLineNumber() + "] " + token.getType();
            if (token.getSymbolId() != -1) {
                output += " " + token.getSymbolId();
            }
            System.out.println(output);
        }
    }

    private static void displaySymbolTable() {
        System.out.println("\nTabela de Símbolos");
        for (Map.Entry<String, Integer> entry : symbolTable.entrySet()) {
            System.out.println(entry.getValue() + " - " + entry.getKey());
        }
    }

    private static void displayErrors() {
        if (!errors.isEmpty()) {
            System.out.println("\nErros nas linhas:");
            for (Error error : errors) {
                System.out.println(error.getLineNumber() + " (" + error.getLineContent() + ")");
            }
        }
    }

    // Enum para tipos de tokens
    enum TokenType {
        IDENTIFICADOR, NUMERO_INTEIRO, NUMERO_REAL, COMENTARIO,
        INT, DOUBLE, FLOAT, REAL, BREAK, CASE, CHAR, CONST, CONTINUE
    }

    // Classe Token
    static class Token {
        private final int lineNumber;
        private final String type;
        private final String value;
        private int symbolId = -1; // Padrão -1 se não aplicável

        public Token(int lineNumber, String type, String value) {
            this.lineNumber = lineNumber;
            this.type = type;
            this.value = value;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public String getType() {
            return type;
        }

        public int getSymbolId() {
            return symbolId;
        }

        public void setSymbolId(int symbolId) {
            this.symbolId = symbolId;
        }
    }

    // Classe Error
    static class Error {
        private final int lineNumber;
        private final String lineContent;

        public Error(int lineNumber, String lineContent) {
            this.lineNumber = lineNumber;
            this.lineContent = lineContent;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public String getLineContent() {
            return lineContent;
        }
    }
}
