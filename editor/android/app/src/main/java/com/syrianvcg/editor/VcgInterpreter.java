package com.syrianvcg.editor;

/**
 * VcgInterpreter v2.0 - Full VCG Language Support
 * Converts VCG source code to self-contained HTML+JS for execution in WebView.
 * Supports all VCG v2.0 features: variables, functions, classes, modules, enums,
 * async/await, try/catch, match/when, reactive store ($set/$get), channels,
 * UI elements, guard, safe/unsafe blocks, test blocks, lambdas, pipeline, etc.
 */
public class VcgInterpreter {

    public static String buildHtml(String vcgCode, String title) {
        // Escape VCG code for embedding in JS string (backtick template)
        String escaped = vcgCode
            .replace("\\", "\\\\")
            .replace("`", "\\`")
            .replace("$", "\\$");

        return "<!DOCTYPE html>\n<html lang='ar' dir='rtl'>\n<head>\n" +
            "<meta charset='UTF-8'>\n" +
            "<meta name='viewport' content='width=device-width,initial-scale=1'>\n" +
            "<title>" + escapeHtml(title) + " - VCG</title>\n" +
            "<style>\n" +
            getStyles() +
            "</style>\n</head>\n<body>\n" +
            "<header>\n" +
            "  <div class='logo'>V</div>\n" +
            "  <div class='info'><h1>" + escapeHtml(title) + "</h1>" +
            "<span>VCG v2.0 - Syrian VCG Language</span></div>\n" +
            "</header>\n" +
            "<div id='out'></div>\n" +
            "<script>\n" +
            getVcgRuntime() +
            "\n// --- User Program ---\n" +
            "try{\n" +
            "  var _src=`" + escaped + "`;\n" +
            "  var _toks=tokenize(_src);\n" +
            "  var _rt=new VCG(_toks);\n" +
            "  _rt.run();\n" +
            "  var _out=document.getElementById('out');\n" +
            "  if(_rt.out.length===0){\n" +
            "    _out.innerHTML='<span class=\u0022empty\u0022>No output</span>';\n" +
            "  } else {\n" +
            "    _rt.out.forEach(function(item){\n" +
            "      if(item.t==='html'){\n" +
            "        var w=document.createElement('div');\n" +
            "        w.className='html-block';\n" +
            "        w.innerHTML=item.v;\n" +
            "        _out.appendChild(w);\n" +
            "      } else {\n" +
            "        var el=document.createElement('span');\n" +
            "        el.className='line';\n" +
            "        el.textContent=item.v;\n" +
            "        _out.appendChild(el);\n" +
            "      }\n" +
            "    });\n" +
            "  }\n" +
            "} catch(e) {\n" +
            "  var _out=document.getElementById('out');\n" +
            "  var el=document.createElement('span');\n" +
            "  el.className='error';\n" +
            "  el.textContent='Error: '+e.message;\n" +
            "  _out.appendChild(el);\n" +
            "  console.error(e);\n" +
            "}\n" +
            "</script>\n</body>\n</html>";
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String getStyles() {
        return
            "@import url('https://fonts.googleapis.com/css2?family=Cairo:wght@400;700;900&family=JetBrains+Mono:wght@400;700&display=swap');\n" +
            ":root{--bg:#060c0e;--panel:#0f1e10;--border:#1a3a1a;--accent:#4dc95a;--text:#e8f5e0;--muted:#4a6a4a;--olive:#2d5a1b}\n" +
            "*{box-sizing:border-box;margin:0;padding:0}\n" +
            "body{background:var(--bg);color:var(--text);font-family:'Cairo',sans-serif;min-height:100vh;padding:1rem}\n" +
            "header{display:flex;align-items:center;gap:.8rem;margin-bottom:1.2rem;padding-bottom:.8rem;border-bottom:1px solid var(--border)}\n" +
            ".logo{width:40px;height:40px;background:linear-gradient(135deg,#1a3a0a,#2d5a1b);border-radius:10px;display:flex;align-items:center;justify-content:center;font-weight:900;font-size:1.2rem;color:white;flex-shrink:0}\n" +
            ".info h1{font-size:1rem;font-weight:700;color:var(--accent)}\n" +
            ".info span{font-size:.72rem;color:var(--muted)}\n" +
            "#out{background:var(--panel);border:1px solid var(--border);border-radius:12px;padding:1.2rem;min-height:200px;font-family:'JetBrains Mono',monospace;font-size:.88rem;line-height:2}\n" +
            ".line{display:block;padding:.15rem .4rem;border-radius:4px;color:#a8e080}\n" +
            ".line:hover{background:rgba(77,201,90,.06)}\n" +
            ".error{display:block;padding:.5rem;color:#f87171;font-weight:bold}\n" +
            ".empty{color:var(--muted);font-style:italic}\n" +
            ".html-block{margin:.4rem 0}\n" +
            "a[href]{color:#4dc95a}\n" +
            "h1,h2,h3,h4,h5,h6{color:#a8e080;border-bottom:2px solid #1e4020;padding-bottom:.25rem;margin:.6rem 0 .3rem}\n" +
            "ul{list-style:none;padding:.4rem 0}\n" +
            "li{padding:.25rem .5rem;border-right:3px solid #2d5a1b;margin:.15rem 0;color:#e8f5e0}\n" +
            "button{background:linear-gradient(135deg,#2d5a1b,#4a9020);color:white;border:none;padding:.5rem 1.2rem;border-radius:8px;font-weight:700;cursor:pointer;margin:.2rem;font-family:'Cairo',sans-serif}\n" +
            "kbd{background:#1a3a0a;color:#a8e080;border:1px solid #2d5a1b;border-radius:5px;padding:.15rem .5rem;font-family:monospace;font-size:.82rem;box-shadow:0 2px 0 #0a1a05;display:inline-block;margin:.15rem}\n" +
            "img{border-radius:8px;max-width:100%;margin:.4rem 0;display:block}\n" +
            "video{border-radius:10px;max-width:100%;margin:.4rem 0}\n" +
            "iframe{border-radius:10px;max-width:100%}\n" +
            ".test-pass{color:#4dc95a;font-weight:bold}\n" +
            ".test-fail{color:#f87171;font-weight:bold}\n" +
            ".guard-msg{color:#f5c842;font-style:italic}\n" +
            ".doc-block{color:#6ab0ff;font-style:italic;border-right:3px solid #1e3050;padding:.25rem .5rem;margin:.3rem 0}\n";
    }
