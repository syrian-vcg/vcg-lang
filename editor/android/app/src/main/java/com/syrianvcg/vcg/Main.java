package com.syrianvcg.vcg;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Native VCG interpreter — Java Edition.
 * Usage:
 *   java -jar vcgc.jar run <file.vcg>
 *   java -jar vcgc.jar run <file.vcg> --html <out.html>
 *   java -jar vcgc.jar repl
 */
public final class Main {

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }
        String cmd = args[0];
        if (cmd.equals("run")) {
            runCommand(args);
        } else if (cmd.equals("repl")) {
            Repl.start();
        } else if (cmd.equals("-v") || cmd.equals("--version")) {
            System.out.println("VCG Native Java Interpreter v2.1.0 (Java Edition, no HTML/JS runtime)");
        } else {
            // treat first arg as a file path directly: `java -jar vcgc.jar hello.vcg`
            runFile(cmd, findHtmlOut(args));
        }
    }

    private static void runCommand(String[] args) {
        if (args.length < 2) {
            System.err.println("الاستخدام: run <file.vcg> [--html out.html]");
            return;
        }
        runFile(args[1], findHtmlOut(args));
    }

    private static String findHtmlOut(String[] args) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals("--html")) return args[i + 1];
        }
        return null;
    }

    private static void runFile(String path, String htmlOut) {
        String source;
        try {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
            source = sb.toString();
        } catch (IOException e) {
            System.err.println("لا يمكن قراءة الملف / Cannot read file: " + path);
            return;
        }

        OutputSink sink = new OutputSink(htmlOut == null);
        Interpreter interp = new Interpreter(sink);
        try {
            List<Token> tokens = new Lexer(source).scanTokens();
            List<Node.Stmt> program = new Parser(tokens).parseProgram();
            interp.run(program);
        } catch (Lexer.LexError | Parser.ParseError e) {
            System.err.println("خطأ صياغي / Syntax error: " + e.getMessage());
            System.exit(1);
        } catch (Environment.VcgRuntimeError e) {
            System.err.println("خطأ تنفيذ / Runtime error: " + e.getMessage());
            System.exit(1);
        } catch (Interpreter.VcgThrown e) {
            System.err.println("استثناء / Uncaught throw: " + Interpreter.stringifyStatic(e.value));
            System.exit(1);
        }

        if (htmlOut != null) {
            String html = HtmlReport.render(sink, path);
            try {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(htmlOut))) {
                    writer.write(html);
                }
                System.out.println("تم إنشاء التقرير / Report written to: " + htmlOut);
            } catch (IOException e) {
                System.err.println("فشل الكتابة / Failed writing html: " + e.getMessage());
            }
        }
    }

    private static void printUsage() {
        System.out.println("VCG Native Java Interpreter v2.1.0");
        System.out.println("Usage:");
        System.out.println("  java -jar vcgc.jar run <file.vcg>");
        System.out.println("  java -jar vcgc.jar run <file.vcg> --html report.html");
        System.out.println("  java -jar vcgc.jar repl");
    }
}
