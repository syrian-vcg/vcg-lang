package com.syrianvcg.editor;

/**
 * VcgInterpreterPatch — إضافات المترجم لدعم ميزات بناء التطبيقات
 *
 * هذا الملف يُوفِّر التعريفات الإضافية التي تُدمَج مع VcgInterpreter
 * لدعم:
 *   • $set.name_app() / $set.app.package() / $set.app.version() / $set.get.icon()
 *   • data_vcg و get.data
 *   • export(to_set)
 *   • make_zip() و make_pdf()
 *
 * طريقة الدمج: استدعِ getExtraRuntime() وألحِقه بـ getRuntime() في VcgInterpreter
 */
public class VcgInterpreterPatch {

    /**
     * إضافات JS تُلحَق بعد _initStdlib في VcgInterpreter.getRuntime()
     * أي بعد السطر الذي يحتوي على:
     *   e.l=function(){...};
     * };   ← نهاية _initStdlib
     */
    public static String getExtraStdlib() {
        return

        // ══════════════════════════════════════════════════
        //  AppMeta: $set.name_app / $set.app.X / $set.get.X
        // ══════════════════════════════════════════════════
        "  /* ── App Meta Store ── */\n" +
        "  self._appmeta={};\n" +

        // $set.name_app("اسم التطبيق")
        "  e['$set']=e['$set']||{};\n" +
        "  e['$set'].name_app=function(n){if(n!==undefined)self._appmeta.name=n;return n!==undefined?n:self._appmeta.name;};\n" +

        // $set.app.package / $set.app.version
        "  e['$set'].app={}\n;" +
        "  e['$set'].app.package=function(p){if(p!==undefined)self._appmeta.pkg=p;return p!==undefined?p:self._appmeta.pkg;};\n" +
        "  e['$set'].app.version=function(v){if(v!==undefined)self._appmeta.version=v;return v!==undefined?v:self._appmeta.version;};\n" +

        // $set.get.icon("path")
        "  e['$set'].get={};\n" +
        "  e['$set'].get.icon=function(p){if(p!==undefined)self._appmeta.icon=p;return p!==undefined?p:self._appmeta.icon;};\n" +

        // ══════════════════════════════════════════════════
        //  data_vcg و get.data — تخزين هياكل البيانات
        // ══════════════════════════════════════════════════
        "  /* ── data_vcg / get.data ── */\n" +
        "  self._datastore={};\n" +

        // data_vcg(name, value) — دالة تسجيل البيانات
        "  e.data_vcg=function(name,value){\n" +
        "    self._datastore[name]=value;\n" +
        "    self._store['data:'+name]=value;\n" +
        "    return value;\n" +
        "  };\n" +

        // get.data(ref) — قراءة البيانات المخزّنة
        "  e.get={data:function(ref){\n" +
        "    if(typeof ref==='string'){\n" +
        "      return self._datastore[ref]!==undefined?self._datastore[ref]:\n" +
        "             self._store['data:'+ref]!==undefined?self._store['data:'+ref]:null;\n" +
        "    }\n" +
        "    // إذا أُعطي مباشرةً كمرجع — يُعاد كما هو\n" +
        "    return ref;\n" +
        "  }};\n" +

        // ══════════════════════════════════════════════════
        //  export(to_set) — تصدير APK
        // ══════════════════════════════════════════════════
        "  /* ── export(to_set) ── */\n" +
        "  e.to_set='__export_to_apk__';\n" +
        "  e['export']=function(target){\n" +
        "    if(target==='__export_to_apk__'){\n" +
        "      var meta=self._appmeta;\n" +
        "      var payload=JSON.stringify({\n" +
        "        action:'build_apk',\n" +
        "        name:meta.name||'VCG App',\n" +
        "        pkg:meta.pkg||'com.vcg.app',\n" +
        "        version:meta.version||'1.0.0',\n" +
        "        icon:meta.icon||''\n" +
        "      });\n" +
        "      self.out.push({t:'html',v:\n" +
        "        '<div style=\"background:linear-gradient(135deg,#1a3a1a,#0f1e10);border:2px solid #4dc95a;'\n" +
        "        +'border-radius:12px;padding:1rem 1.2rem;margin:.8rem 0\">'\n" +
        "        +'<div style=\"color:#4dc95a;font-weight:700;font-size:1.1rem\">🔨 جارٍ تصدير التطبيق...</div>'\n" +
        "        +'<div style=\"color:#e8f5e0;margin-top:.4rem\">'\n" +
        "        +'<span>📱 APK</span> &nbsp; <span>📦 ZIP</span> &nbsp; <span>📄 PDF</span>'\n" +
        "        +'</div>'\n" +
        "        +'<div style=\"color:#aaa;font-size:.8rem;margin-top:.3rem\">'\n" +
        "        +'تطبيق: <b style=\"color:#4dc95a\">'+(meta.name||'VCG App')+'</b> &nbsp;'\n" +
        "        +'حزمة: <code>'+(meta.pkg||'com.vcg.app')+'</code>'\n" +
        "        +'</div></div>'\n" +
        "      });\n" +
        "      // إشعار الـ Android Bridge إذا كان متاحاً\n" +
        "      if(window.VcgAndroid&&window.VcgAndroid.onExportRequest){\n" +
        "        window.VcgAndroid.onExportRequest(payload);\n" +
        "      }\n" +
        "    }\n" +
        "    return null;\n" +
        "  };\n" +

        // ══════════════════════════════════════════════════
        //  make_zip(name, entries) و make_pdf(title, text)
        // ══════════════════════════════════════════════════
        "  /* ── make_zip / make_pdf ── */\n" +
        "  e.make_zip=function(name,entries){\n" +
        "    var lines=['[ZIP] '+name];\n" +
        "    if(Array.isArray(entries))entries.forEach(function(f){lines.push('  +'+f);});\n" +
        "    else if(typeof entries==='object')Object.keys(entries).forEach(function(k){lines.push('  +'+k);});\n" +
        "    self.out.push({t:'html',v:\n" +
        "      '<div style=\"background:#0f1e10;border:1px solid #4dc95a;border-radius:8px;'\n" +
        "      +'padding:.6rem .8rem;margin:.4rem 0;font-family:monospace\">'\n" +
        "      +'<span style=\"color:#4dc95a;font-weight:700\">📦 '+name+'</span><br>'\n" +
        "      +lines.slice(1).map(function(l){return '<span style=\"color:#aaa\">'+l+'</span>';}).join('<br>')\n" +
        "      +'</div>'\n" +
        "    });\n" +
        "    if(window.VcgAndroid&&window.VcgAndroid.onMakeZip){\n" +
        "      window.VcgAndroid.onMakeZip(name,JSON.stringify(entries||[]));\n" +
        "    }\n" +
        "    return name;\n" +
        "  };\n" +

        "  e.make_pdf=function(title,content){\n" +
        "    self.out.push({t:'html',v:\n" +
        "      '<div style=\"background:#1c1812;border:1px solid #e0a84d;border-radius:8px;'\n" +
        "      +'padding:.6rem .8rem;margin:.4rem 0\">'\n" +
        "      +'<span style=\"color:#e0a84d;font-weight:700\">📄 '+title+'.pdf</span><br>'\n" +
        "      +'<span style=\"color:#aaa;font-size:.8rem\">'+(content||'').substring(0,100)+'...</span>'\n" +
        "      +'</div>'\n" +
        "    });\n" +
        "    if(window.VcgAndroid&&window.VcgAndroid.onMakePdf){\n" +
        "      window.VcgAndroid.onMakePdf(title,content||'');\n" +
        "    }\n" +
        "    return title;\n" +
        "  };\n";
    }

    /**
     * كلمات مفتاحية جديدة تُضاف إلى مصفوفة KW في tokenizer
     */
    public static String[] getNewKeywords() {
        return new String[]{
            "data_vcg",
            "get",
            "export",
            "to_set",
            "make_zip",
            "make_pdf"
        };
    }

    /**
     * معالج parseStmt الإضافي — يُدرج قبل نهاية parseStmt في VcgInterpreter
     * يعالج:
     *   data_vcg name = value
     */
    public static String getStmtHandler() {
        return
        "  /* ── data_vcg statement ── */\n" +
        "  if(t.v==='data_vcg'){\n" +
        "    this.eat();\n" +
        "    var vname=this.eat().v;\n" +
        "    this.eat('=');\n" +
        "    var vval=this.parseExpr(e);\n" +
        "    this._datastore=this._datastore||{};\n" +
        "    this._datastore[vname]=vval;\n" +
        "    this._store['data:'+vname]=vval;\n" +
        "    e[vname]=vval;\n" +
        "    return;\n" +
        "  }\n";
    }
}
