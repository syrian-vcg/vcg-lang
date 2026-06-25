package com.syrianvcg.vcg;

import java.util.List;
import java.util.Scanner;

public final class Repl {
    private Repl() {}

    public static void start() {
        System.out.println("VCG Java REPL — type 'exit' to quit");
        OutputSink sink = new OutputSink(true);
        Interpreter interp = new Interpreter(sink);
        Scanner scanner = new Scanner(System.in);
        StringBuilder buffer = new StringBuilder();
        int openBraces = 0;

        while (true) {
            System.out.print(openBraces > 0 ? "...  " : "vcg> ");
            if (!scanner.hasNextLine()) break;
            String line = scanner.nextLine();
            if (buffer.length() == 0 && line.trim().equals("exit")) break;

            buffer.append(line).append("\n");
            for (char c : line.toCharArray()) {
                if (c == '{') openBraces++;
                if (c == '}') openBraces--;
            }
            if (openBraces > 0) continue;

            String src = buffer.toString();
            buffer.setLength(0);
            try {
                List<Token> tokens = new Lexer(src).scanTokens();
                List<Node.Stmt> program = new Parser(tokens).parseProgram();
                interp.run(program);
            } catch (Lexer.LexError | Parser.ParseError e) {
                System.out.println("خطأ صياغي: " + e.getMessage());
            } catch (Environment.VcgRuntimeError e) {
                System.out.println("خطأ تنفيذ: " + e.getMessage());
            } catch (Interpreter.VcgThrown e) {
                System.out.println("استثناء: " + Interpreter.stringifyStatic(e.value));
            }
        }
        System.out.println("الوداع! / Goodbye!");
    }
}
