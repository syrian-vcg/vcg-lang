package com.syrianvcg.editor;

import com.syrianvcg.vcg.Environment;
import com.syrianvcg.vcg.Interpreter;
import com.syrianvcg.vcg.Lexer;
import com.syrianvcg.vcg.Node;
import com.syrianvcg.vcg.OutputSink;
import com.syrianvcg.vcg.Parser;
import com.syrianvcg.vcg.Token;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * VcgRealRunner — الجسر بين محرر/ترمنال VCG (Android) والمفسِّر الحقيقي
 * الجديد المكتوب بالكامل بـ Java (الحزمة com.syrianvcg.vcg: Lexer → Parser
 * → Interpreter)، بدون أي توليد HTML/JavaScript في مسار التنفيذ.
 *
 * يستبدل هذا الصف منطق VcgHeadlessRunner المصغّر (regex/أرقام فقط) بتنفيذ
 * حقيقي وكامل للغة VCG (متغيرات، دوال، شروط، حلقات، كلاسات، struct،
 * pattern Balanced...). كل مشروع (projectId) يحتفظ بمفسِّر خاص به (Interpreter
 * واحد ثابت + Environment عالمي واحد)، حتى تبقى المتغيّرات محفوظة بين أوامر
 * الترمنال المتتالية لنفس المشروع — تماماً كما كانت الجلسات القديمة، لكن
 * الآن بدلالات اللغة الحقيقية كاملة، لا تقريب رياضي بسيط فقط.
 */
public final class VcgRealRunner {

    /** مفسِّر واحد محفوظ لكل مشروع، يحافظ على globals بين الاستدعاءات. */
    private static final Map<String, Interpreter> interpretersByProject = new HashMap<>();
    /** sink واحد محفوظ لكل مشروع، لإمكانية استرجاع آخر المخرجات إن احتاج المستخدم. */
    private static final Map<String, OutputSink> sinksByProject = new HashMap<>();

    private VcgRealRunner() {}

    private static String keyOf(String projectId) {
        return projectId == null ? "global" : projectId;
    }

    private static synchronized Interpreter interpreterFor(String projectId) {
        String key = keyOf(projectId);
        Interpreter interp = interpretersByProject.get(key);
        if (interp == null) {
            OutputSink sink = new OutputSink(false); // لا نُكرِّر الطباعة في System.out على أندرويد
            interp = new Interpreter(sink);
            interpretersByProject.put(key, interp);
            sinksByProject.put(key, sink);
        }
        return interp;
    }

    /**
     * ينفِّذ كود VCG (سطر واحد أو أكثر) فعلياً عبر المفسِّر الحقيقي،
     * ويُعيد كل ما أنتجه show()/print() كنص جاهز للعرض في الترمنال.
     *
     * توقيع الدالة مطابق تماماً لـ VcgHeadlessRunner.run(code, projectId)
     * القديمة، لتسهيل الاستبدال المباشر في كل نقاط الاستدعاء.
     */
    public static String run(String code, String projectId) {
        Interpreter interp = interpreterFor(projectId);
        OutputSink sink = sinksByProject.get(keyOf(projectId));
        int before = sink.entries().size();

        try {
            List<Token> tokens = new Lexer(code).scanTokens();
            List<Node.Stmt> program = new Parser(tokens).parseProgram();
            interp.run(program);
        } catch (Lexer.LexError e) {
            return "خطأ صياغي / Syntax error: " + e.getMessage() + "\n";
        } catch (Parser.ParseError e) {
            return "خطأ صياغي / Syntax error: " + e.getMessage() + "\n";
        } catch (Environment.VcgRuntimeError e) {
            return "خطأ تنفيذ / Runtime error: " + e.getMessage() + "\n";
        } catch (Interpreter.VcgThrown e) {
            return "استثناء / Uncaught throw: " + Interpreter.stringifyStatic(e.value) + "\n";
        } catch (Exception e) {
            return "ERR:" + (e.getMessage() == null ? "خطأ غير معروف" : e.getMessage()) + "\n";
        }

        StringBuilder out = new StringBuilder();
        List<OutputSink.Entry> entries = sink.entries();
        for (int i = before; i < entries.size(); i++) {
            OutputSink.Entry entry = entries.get(i);
            if (entry.kind == OutputSink.Kind.HTML) {
                out.append("[html] ").append(entry.value).append('\n');
            } else {
                out.append(entry.value).append('\n');
            }
        }
        if (out.length() == 0) out.append("(تم التنفيذ بلا مخرجات)\n");
        return out.toString();
    }

    /** يصفّر مفسِّر مشروع معيّن بالكامل (متغيّرات + مخرجات سابقة). */
    public static synchronized void resetSession(String projectId) {
        String key = keyOf(projectId);
        interpretersByProject.remove(key);
        sinksByProject.remove(key);
    }
}
