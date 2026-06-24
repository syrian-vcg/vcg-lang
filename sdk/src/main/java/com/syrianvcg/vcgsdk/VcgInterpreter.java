package com.syrianvcg.vcgsdk;

/**
 * VcgInterpreter — Syrian VCG v2.0 Full Edition
 * Builds a complete HTML+JS page that runs VCG source code
 * Fully synchronized with VCG language v2.0 keywords and features
 * Date: 2026-06-06
 */
public class VcgInterpreter {

    public static String buildHtml(String vcgCode, String title) {
        return buildHtml(vcgCode, title, "{}", "olive");
    }

    /**
     * @param vcgCode    مصدر VCG
     * @param title      عنوان الصفحة
     * @param assetsJson خريطة JSON {"asset:ID":"data:...;base64,..."} لتحويل مراجع الأصول لروابط حقيقية
     * @param theme      اسم ثيم العرض (olive, midnight, amoled, sand)
     */
    public static String buildHtml(String vcgCode, String title, String assetsJson, String theme) {
        String escaped = vcgCode
            .replace("\\", "\\\\")
            .replace("`", "\\`")
            .replace("$", "\\$");

        return "<!DOCTYPE html>\n" +
            "<html lang='ar' dir='rtl'>\n" +
            "<head>\n" +
            "<meta charset='UTF-8'>\n" +
            "<meta name='viewport' content='width=device-width,initial-scale=1'>\n" +
            "<title>" + escHtml(title) + " — VCG</title>\n" +
            "<style>" + getStyles(theme) + "</style>\n" +
            "</head>\n" +
            "<body>\n" +
            "<div id='out'></div>\n" +
            "<script>\n" +
            "var _ASSETS=" + (assetsJson == null || assetsJson.isEmpty() ? "{}" : assetsJson) + ";\n" +
            "function _resolveAsset(s){return (typeof s==='string'&&_ASSETS[s])?_ASSETS[s]:s;}\n" +
            getRuntime() +
            "\ntry{\n" +
            "  var _toks=tokenize(`" + escaped + "`);\n" +
            "  var _rt=new VCG(_toks);\n" +
            "  _rt.run();\n" +
            "  var _out=document.getElementById('out');\n" +
            "  if(_rt.out.length===0){\n" +
            "    _out.innerHTML='<p class=\"empty\">لا يوجد مخرجات</p>';\n" +
            "  } else {\n" +
            "    _rt.out.forEach(function(item){\n" +
            "      if(item.t==='html'){\n" +
            "        var w=document.createElement('div');\n" +
            "        w.className='html-block'; w.innerHTML=item.v;\n" +
            "        _out.appendChild(w);\n" +
            "      } else {\n" +
            "        var el=document.createElement('span');\n" +
            "        el.className='line'; el.textContent=item.v;\n" +
            "        _out.appendChild(el);\n" +
            "      }\n" +
            "    });\n" +
            "  }\n" +
            "  if(window.VcgAndroid&&window.VcgAndroid.onRunSuccess){\n" +
            "    window.VcgAndroid.onRunSuccess(String(_rt.out.length));\n" +
            "  }\n" +
            "} catch(e) {\n" +
            "  var el=document.createElement('span');\n" +
            "  el.className='line error';\n" +
            "  el.textContent='خطأ: '+e.message;\n" +
            "  document.getElementById('out').appendChild(el);\n" +
            "  console.error(e);\n" +
            "  if(window.VcgAndroid&&window.VcgAndroid.onRunError){\n" +
            "    window.VcgAndroid.onRunError(e.message);\n" +
            "  }\n" +
            "}\n" +
            "</script>\n" +
            "</body>\n" +
            "</html>";
    }

    private static String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
                .replace("\"","&quot;").replace("'","&#39;");
    }

    private static String getStyles(String theme) {
        String[] palette = themePalette(theme);
        String bg = palette[0], panel = palette[1], border = palette[2],
               accent = palette[3], text = palette[4], muted = palette[5], olive = palette[6];
        return
            "@import url('https://fonts.googleapis.com/css2?family=Cairo:wght@400;700;900&family=JetBrains+Mono:wght@400;700&display=swap');" +
            ":root{--bg:" + bg + ";--panel:" + panel + ";--border:" + border + ";--accent:" + accent + ";--text:" + text + ";--muted:" + muted + ";--olive:" + olive + "}" +
            "*{box-sizing:border-box;margin:0;padding:0}" +
            "body{background:var(--bg);color:var(--text);font-family:'Cairo',sans-serif;min-height:100vh;padding:0.9rem}" +
            "#out{font-family:'JetBrains Mono',monospace;font-size:0.82rem;line-height:2}" +
            ".line{display:block;padding:0.12rem 0.4rem;border-radius:4px;color:var(--text)}" +
            ".line:hover{background:rgba(127,127,127,0.08)}" +
            ".error{display:flex;align-items:flex-start;gap:0.5rem;color:#7a1f17;background:rgba(214,72,60,0.08);border:1px solid rgba(214,72,60,0.35);border-radius:8px;padding:0.6rem 0.7rem;font-weight:600;margin:0.2rem 0}" +
            ".error::before{content:'⚠';flex-shrink:0;font-size:1rem}" +
            ".empty{color:var(--muted);font-style:italic;padding:0.5rem}" +
            ".html-block{margin:0.4rem 0}" +
            "h1,h2,h3,h4,h5,h6{color:var(--accent);font-family:'Cairo',sans-serif;border-bottom:2px solid var(--border);padding-bottom:0.25rem;margin:0.6rem 0 0.3rem}" +
            "ul{list-style:none;padding:0.4rem 0}" +
            "li{padding:0.25rem 0.5rem;border-right:3px solid "+olive+";margin:0.15rem 0;color:var(--text)}" +
            "button{background:linear-gradient(135deg,"+olive+","+accent+");color:white;border:none;padding:0.55rem 1.2rem;border-radius:8px;font-weight:700;cursor:pointer;margin:0.2rem;font-family:'Cairo',sans-serif;transition:all 0.2s}" +
            "button:hover{transform:translateY(-1px)}" +
            "kbd{background:var(--border);color:var(--accent);border:1px solid "+olive+";border-radius:5px;padding:0.2rem 0.6rem;font-family:monospace;font-size:0.82rem;box-shadow:0 2px 0 rgba(0,0,0,.3);display:inline-block;margin:0.15rem}" +
            "a{color:var(--accent)}" +
            "img{border-radius:8px;max-width:100%;margin:0.4rem 0;display:block}" +
            "video{border-radius:10px;max-width:100%;margin:0.4rem 0}" +
            "iframe{border-radius:10px;max-width:100%}";
    }

    private static String[] themePalette(String theme) {
        if (theme == null) theme = "olive";
        switch (theme) {
            case "midnight":
                return new String[]{"#0a0e1a","#10162a","#1c2542","#5b8cff","#e6ecff","#5a6a8a","#27306b"};
            case "amoled":
                return new String[]{"#000000","#0a0a0a","#1a1a1a","#4dc95a","#f0f0f0","#555555","#1f3d12"};
            case "sand":
                return new String[]{"#1c1812","#26211a","#3a3226","#e0a84d","#f2e8d8","#7a6f5a","#5a4626"};
            case "white":
                return new String[]{"#ffffff","#f7f9f6","#e2e6e1","#1f7a3d","#1b221c","#6b7568","#2e9e44"};
            default: // olive
                return new String[]{"#060c0e","#0f1e10","#1a3a1a","#4dc95a","#e8f5e0","#4a6a4a","#2d5a1b"};
        }
    }

    private static String getRuntime() {
        return
            "/* ═══════════════════════════════════════════════════\n" +
            "   Syrian VCG v2.0 — Full JavaScript Interpreter\n" +
            "   ═══════════════════════════════════════════════════ */\n" +

            "var KW=['let','const','func','return','if','else','while','for','in','repeat',\n" +
            " 'break','continue','show','input','and','or','not','html','true','false','nil','null',\n" +
            " 'import','as','struct','new','self','typeof','sizeof','assert','try','catch','throw',\n" +
            " 'match','when','public','w','x','c',\n" +
            " 'youtube','facebook','instagram','xsocial','url','btn','key','video','img','h','l',\n" +
            " 'class','extends','implements','interface','super','this',\n" +
            " 'module','export','from',\n" +
            " 'async','await','promise','defer',\n" +
            " 'type','enum','union','generic',\n" +
            " 'file','read','write','append',\n" +
            " 'http','request','response','socket',\n" +
            " 'ref','ptr','alloc','free',\n" +
            " 'safe','unsafe','guard',\n" +
            " 'map','filter','reduce','find',\n" +
            " 'doc','test','expect','mock',\n" +
            " 'with','case','pipeline','watch'];\n" +

            "function tokenize(src){\n" +
            "  var toks=[],i=0,ln=1;\n" +
            "  var adv=function(){var c=src[i++];if(c==='\\n')ln++;return c;};\n" +
            "  while(i<src.length){\n" +
            "    var c=src[i];\n" +
            "    if(c==='\\n'){toks.push({t:'NL',v:'\\n',ln:ln});adv();continue;}\n" +
            "    if(c===' '||c==='\\t'||c==='\\r'){i++;continue;}\n" +
            "    if(c==='#'||(c==='/'&&src[i+1]==='/')){\n" +
            "      while(i<src.length&&src[i]!=='\\n')i++;continue;}\n" +
            "    if(c==='/'&&src[i+1]==='*'){\n" +
            "      i+=2;while(i<src.length&&!(src[i]==='*'&&src[i+1]==='/'))i++;i+=2;continue;}\n" +
            "    if(c==='$'){i++;var s='';\n" +
            "      while(i<src.length&&/[a-zA-Z]/.test(src[i]))s+=src[i++];\n" +
            "      toks.push({t:['set','get','x'].includes(s)?'KW':'OP',v:'$'+s,ln:ln});continue;}\n" +
            "    if(c==='\"'||c==='\\''||c==='`'){\n" +
            "      var q=adv(),s='';\n" +
            "      while(i<src.length&&src[i]!==q){\n" +
            "        if(src[i]==='\\\\'){adv();var e=adv();\n" +
            "          s+=e==='n'?'\\n':e==='t'?'\\t':e;}\n" +
            "        else s+=adv();}\n" +
            "      adv();toks.push({t:'STR',v:s,ln:ln});continue;}\n" +
            "    if(/\\d/.test(c)){\n" +
            "      var s='';\n" +
            "      while(i<src.length&&(/\\d/.test(src[i])||src[i]==='.'))s+=adv();\n" +
            "      if(i<src.length&&/[eE]/.test(src[i])){\n" +
            "        s+=adv();if(i<src.length&&/[+-]/.test(src[i]))s+=adv();\n" +
            "        while(i<src.length&&/\\d/.test(src[i]))s+=adv();}\n" +
            "      toks.push({t:'NUM',v:parseFloat(s),ln:ln});continue;}\n" +
            "    if(/[a-zA-Z_\\u0600-\\u06FF]/.test(c)){\n" +
            "      var s='';\n" +
            "      while(i<src.length&&/[a-zA-Z0-9_\\u0600-\\u06FF]/.test(src[i]))s+=adv();\n" +
            "      toks.push({t:KW.includes(s)?'KW':'ID',v:s,ln:ln});continue;}\n" +
            "    var two=src.slice(i,i+2);\n" +
            "    if(['==','!=','<=','>=','+=','-=','*=','/=','++','--','**','->','..','<-','|>'].includes(two)){\n" +
            "      toks.push({t:'OP',v:two,ln:ln});i+=2;continue;}\n" +
            "    toks.push({t:'OP',v:adv(),ln:ln});\n" +
            "  }\n" +
            "  toks.push({t:'EOF',v:'',ln:ln});return toks;\n" +
            "}\n" +

            "function VCG(toks){\n" +
            "  this.toks=toks.filter(function(t){return t.t!=='NL';});\n" +
            "  this.pos=0;this.env=Object.create(null);\n" +
            "  this._store={};this._watchers={};\n" +
            "  this.out=[];this._ret=undefined;\n" +
            "  this._hasRet=false;this._brk=false;this._cnt=false;\n" +
            "  this._initStdlib();\n" +
            "}\n" +
            "VCG.prototype.cur=function(){return this.toks[this.pos];};\n" +
            "VCG.prototype.eat=function(v){\n" +
            "  var t=this.cur();\n" +
            "  if(v&&t.v!==v)throw new Error('Expected '+v+' got '+t.v+' (line '+t.ln+')');\n" +
            "  this.pos++;return t;};\n" +
            "VCG.prototype.eatIf=function(v){\n" +
            "  if(this.cur().v===v){this.eat();return true;}return false;};\n" +
            "VCG.prototype._def=function(e,k,v){\n" +
            "  Object.defineProperty(e,k,{value:v,writable:true,configurable:true,enumerable:true});\n" +
            "  return v;};\n" +
            "VCG.prototype._set=function(e,k,v){\n" +
            "  var c=e;while(c){if(Object.prototype.hasOwnProperty.call(c,k)){c[k]=v;return v;}\n" +
            "  c=Object.getPrototypeOf(c);}e[k]=v;return v;};\n" +
            "VCG.prototype._get=function(e,k){\n" +
            "  var c=e;while(c){if(Object.prototype.hasOwnProperty.call(c,k))return c[k];\n" +
            "  c=Object.getPrototypeOf(c);}throw new Error('Undefined: '+k);};\n" +
            "VCG.prototype._tryGet=function(e,k){\n" +
            "  try{return this._get(e,k);}catch(x){return null;}};\n" +
            "VCG.prototype._scope=function(p){return Object.create(p||null);};\n" +
            "VCG.prototype._str=function(x){\n" +
            "  if(x===null||x===undefined)return 'nil';\n" +
            "  if(x===true)return 'true';if(x===false)return 'false';\n" +
            "  if(Array.isArray(x))return '['+x.map(function(v){return this._str(v);},this).join(', ')+']';\n" +
            "  if(x&&typeof x==='object'&&'_buf' in x)return '<channel>';\n" +
            "  if(typeof x==='object')return '{'+Object.keys(x).map(function(k){return k+':'+this._str(x[k]);},this).join(',')+'}' ;\n" +
            "  return String(x);};\n" +

            "VCG.prototype._initStdlib=function(){\n" +
            "  var e=this.env,m=Math,self=this;\n" +
            "  ['abs','floor','ceil','round','sqrt','sin','cos','tan','log','log2','log10']\n" +
            "    .forEach(function(f){e[f]=function(){return m[f].apply(m,arguments);};});\n" +
            "  e.pow=function(b,x){return m.pow(b,x);};\n" +
            "  e.min=function(){return m.min.apply(m,arguments);};\n" +
            "  e.max=function(){return m.max.apply(m,arguments);};\n" +
            "  e.clamp=function(v,lo,hi){return m.max(lo,m.min(hi,v));};\n" +
            "  e.rand=function(a,b){return a===undefined?m.random():b===undefined?m.floor(m.random()*a):m.floor(m.random()*(b-a))+a;};\n" +
            "  e.range=function(a,b,s){var r=[],f=b===undefined?0:a,t=b===undefined?a:b,st=s||1;\n" +
            "    for(var i=f;st>0?i<t:i>t;i+=st)r.push(i);return r;};\n" +
            "  e.len=function(x){return Array.isArray(x)?x.length:typeof x==='string'?x.length:x&&typeof x==='object'?Object.keys(x).length:0;};\n" +
            "  e.str=function(x){return self._str(x);};\n" +
            "  e.int=function(x){return parseInt(x)||0;};\n" +
            "  e.float=function(x){return parseFloat(x)||0;};\n" +
            "  e.bool=function(x){return !!x;};\n" +
            "  e.char=function(n){return String.fromCharCode(n);};\n" +
            "  e.ord=function(s){return s?s.charCodeAt(0):0;};\n" +
            "  e.keys=function(o){return o&&typeof o==='object'?Object.keys(o):[];};\n" +
            "  e.values=function(o){return o&&typeof o==='object'?Object.values(o):[];};\n" +
            "  e.join=function(a,s){return Array.isArray(a)?a.join(s||','):String(a);};\n" +
            "  e.format=function(){var f=arguments[0],i=1,args=arguments;return String(f).replace(/%s/g,function(){return args[i++]||'';});};\n" +
            "  e.typeof=function(x){return x===null?'nil':typeof x==='boolean'?'bool':Array.isArray(x)?'array':x&&typeof x==='object'&&'_buf' in x?'channel':typeof x==='object'?'struct':typeof x;};\n" +
            "  e.sizeof=function(x){return Array.isArray(x)?x.length:typeof x==='string'?x.length:0;};\n" +
            "  e.isnil=function(x){return x===null||x===undefined;};\n" +
            "  e.isnum=function(x){return typeof x==='number';};\n" +
            "  e.isstr=function(x){return typeof x==='string';};\n" +
            "  e.isarr=function(x){return Array.isArray(x);};\n" +
            "  e.defined=function(x){return x!==null&&x!==undefined;};\n" +
            "  e.freeze=function(v){return v;};\n" +
            "  e.copy=function(v){if(Array.isArray(v))return v.slice();if(v&&typeof v==='object')return Object.assign({},v);return v;};\n" +
            "  e.send=function(ch,v){if(ch&&ch._buf)ch._buf.push(v);return v;};\n" +
            "  e.recv=function(ch){if(ch&&ch._buf&&ch._buf.length>0)return ch._buf.shift();return null;};\n" +
            "  e.watch=function(k,fn){self._watchers[k]=fn;};\n" +
            "  e.pipe=function(val){var fns=Array.prototype.slice.call(arguments,1);return fns.reduce(function(v,f){return typeof f==='function'?f(v):v;},val);};\n" +
            "  e.show=function(){var parts=Array.prototype.slice.call(arguments);self.out.push({t:'txt',v:parts.map(function(v){return self._str(v);}).join(' ')});return null;};\n" +
            "  e.print=e.show;\n" +
            "  e.input=function(p){return window.prompt?window.prompt(p||'')||'':'';};\n" +
            "  e.map=function(f,a){return Array.isArray(a)?a.map(function(x){return f(x);}):[];};\n" +
            "  e.filter=function(f,a){return Array.isArray(a)?a.filter(function(x){return f(x);}):[];};\n" +
            "  e.reduce=function(f,init,a){return Array.isArray(a)?a.reduce(function(acc,x){return f(acc,x);},init):init;};\n" +
            "  e.find=function(f,a){return Array.isArray(a)?(a.find(function(x){return f(x);})||null):null;};\n" +
            "  e.flat=function(a){return Array.isArray(a)?a.flat():[];};\n" +
            "  e.unique=function(a){return Array.isArray(a)?[...new Set(a)]:[];};\n" +
            "  e.sum=function(a){return Array.isArray(a)?a.reduce(function(s,x){return s+x;},0):0;};\n" +
            "  e.avg=function(a){return Array.isArray(a)&&a.length?a.reduce(function(s,x){return s+x;},0)/a.length:0;};\n" +
            "  e.first=function(a){return Array.isArray(a)&&a.length?a[0]:null;};\n" +
            "  e.last=function(a){return Array.isArray(a)&&a.length?a[a.length-1]:null;};\n" +
            "  e.chunk=function(a,n){var r=[];if(!Array.isArray(a))return r;for(var i=0;i<a.length;i+=n)r.push(a.slice(i,i+n));return r;};\n" +
            "  e.zip=function(a,b){var r=[];var l=Math.min(a.length,b.length);for(var i=0;i<l;i++)r.push([a[i],b[i]]);return r;};\n" +
            "  e.repeat=function(s,n){return String(s).repeat(n);};\n" +
            "  e.pad_start=function(s,n,c){return String(s).padStart(n,c||' ');};\n" +
            "  e.pad_end=function(s,n,c){return String(s).padEnd(n,c||' ');};\n" +
            "  e.includes=function(s,x){return typeof s==='string'?s.includes(x):Array.isArray(s)?s.includes(x):false;};\n" +
            "  e.indexof=function(s,x){return typeof s==='string'?s.indexOf(x):Array.isArray(s)?s.indexOf(x):-1;};\n" +
            "  e.count=function(s,x){if(typeof s!=='string')return 0;var c=0,i=0;while((i=s.indexOf(x,i))!==-1){c++;i+=x.length;}return c;};\n" +
            "  e.merge=function(){var r={};for(var i=0;i<arguments.length;i++)if(arguments[i])Object.assign(r,arguments[i]);return r;};\n" +
            "  e.has=function(o,k){return o&&typeof o==='object'&&k in o;};\n" +
            "  e.del=function(o,k){if(o&&k)delete o[k];return o;};\n" +
            "  e.entries=function(o){return o&&typeof o==='object'?Object.entries(o):[];};\n" +
            "  e.gcd=function(a,b){while(b){var t=b;b=a%b;a=t;}return Math.abs(a);};\n" +
            "  e.lcm=function(a,b){function g(x,y){while(y){var t=y;y=x%y;x=t;}return x;}return Math.abs(a*b)/(g(a,b)||1);};\n" +
            "  e.fib=function(n){if(n<=1)return n;var a=0,b=1;for(var i=2;i<=n;i++){var t=a+b;a=b;b=t;}return b;};\n" +
            "  e.factorial=function(n){var r=1;for(var i=2;i<=n;i++)r*=i;return r;};\n" +
            "  e.is_prime=function(n){if(n<2)return false;if(n===2)return true;if(n%2===0)return false;for(var i=3;i*i<=n;i+=2)if(n%i===0)return false;return true;};\n" +
            "  e.JSON_stringify=function(v){return JSON.stringify(v);};\n" +
            "  e.JSON_parse=function(s){try{return JSON.parse(s);}catch(x){return null;}};\n" +
            "  e.assert_eq=function(a,b){if(a!==b)throw new Error('assert_eq: '+JSON.stringify(a)+' !== '+JSON.stringify(b));return true;};\n" +
            "  e.assert_ne=function(a,b){if(a===b)throw new Error('assert_ne: should not equal '+JSON.stringify(a));return true;};\n" +
            "  e.assert_true=function(v){if(!v)throw new Error('assert_true failed');return true;};\n" +
            "  e.assert_false=function(v){if(v)throw new Error('assert_false failed');return true;};\n" +
            "  e.uuid=function(){return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g,function(c){var r=Math.random()*16|0;return(c==='x'?r:(r&0x3|0x8)).toString(16);});};\n" +
            "  e.hash=function(s){var h=5381;s=String(s);for(var i=0;i<s.length;i++)h=((h<<5)+h)+s.charCodeAt(i)|0;return Math.abs(h);};\n" +
            "  e.sleep=function(ms){return new Promise(function(r){setTimeout(r,ms||0);});};\n" +
            "  e.type_of=function(x){return e.typeof(x);};\n" +
            "  e.file_read=function(){return '';};\n" +
            "  e.file_write=function(){return false;};\n" +
            "  e.file_exists=function(){return false;};\n" +
            "  e.VCG_VERSION='2.0.0';e.VCG_DATE='2026-06-06';e.VCG_EDITION='Full Edition';\n" +
            "  e.PI=m.PI;e.E=m.E;e.TAU=2*m.PI;e.PHI=(1+m.sqrt(5))/2;e.INF=Infinity;\n" +
            "  e.true=true;e.false=false;e.nil=null;\n" +

            "  e.youtube=function(id){\n" +
            "    var vid=String(id);\n" +
            "    var m2=vid.match(/v=([^&]+)/)||vid.match(/youtu\\.be\\/([^?]+)/);\n" +
            "    if(m2)vid=m2[1];\n" +
            "    self.out.push({t:'html',v:'<div style=\"position:relative;padding-bottom:56.25%;height:0;overflow:hidden;border-radius:12px;margin:.8rem 0;box-shadow:0 8px 32px rgba(0,0,0,.5)\"><iframe style=\"position:absolute;top:0;left:0;width:100%;height:100%\" src=\"https://www.youtube.com/embed/'+vid+'?rel=0\" frameborder=\"0\" allowfullscreen loading=\"lazy\"></iframe></div>'});\n" +
            "    return null;};\n" +
            "  e.facebook=function(url,txt){\n" +
            "    self.out.push({t:'html',v:'<a href=\"'+url+'\" target=\"_blank\" style=\"display:inline-flex;align-items:center;gap:.4rem;background:#1877f2;color:white;padding:.5rem 1rem;border-radius:8px;text-decoration:none;font-weight:700;margin:.2rem\">'+(txt||'Facebook')+'</a>'});\n" +
            "    return null;};\n" +
            "  e.instagram=function(handle,txt){\n" +
            "    var href=String(handle).startsWith('http')?handle:'https://instagram.com/'+(handle.startsWith('@')?handle.slice(1):handle);\n" +
            "    self.out.push({t:'html',v:'<a href=\"'+href+'\" target=\"_blank\" style=\"display:inline-flex;align-items:center;gap:.4rem;background:linear-gradient(45deg,#f09433,#e6683c,#dc2743,#cc2366,#bc1888);color:white;padding:.5rem 1rem;border-radius:8px;text-decoration:none;font-weight:700;margin:.2rem\">'+(txt||handle)+'</a>'});\n" +
            "    return null;};\n" +
            "  e.xsocial=function(handle,txt){\n" +
            "    var href=String(handle).startsWith('http')?handle:'https://x.com/'+(handle.startsWith('@')?handle.slice(1):handle);\n" +
            "    self.out.push({t:'html',v:'<a href=\"'+href+'\" target=\"_blank\" style=\"display:inline-flex;align-items:center;gap:.4rem;background:#000;color:white;border:1px solid #333;padding:.5rem 1rem;border-radius:8px;text-decoration:none;font-weight:700;margin:.2rem\">'+(txt||handle)+'</a>'});\n" +
            "    return null;};\n" +
            "  e.url=function(href,txt,target){\n" +
            "    self.out.push({t:'html',v:'<a href=\"'+href+'\" target=\"'+(target||'_blank')+'\" style=\"color:#4dc95a;text-decoration:underline;font-weight:600;display:inline-block;margin:.2rem\">'+(txt||href)+'</a>'});\n" +
            "    return null;};\n" +
            "  e.btn=function(lbl,act){\n" +
            "    self.out.push({t:'html',v:'<button onclick=\"'+(act||'')+'\">'+(lbl)+'</button>'});\n" +
            "    return null;};\n" +
            "  e.key=function(k){\n" +
            "    self.out.push({t:'html',v:'<kbd>'+k+'</kbd>'});\n" +
            "    return null;};\n" +
            "  e.video=function(src,w,h){\n" +
            "    src=_resolveAsset(src);\n" +
            "    self.out.push({t:'html',v:'<video controls style=\"width:'+(w||'100%')+';border-radius:10px;max-width:100%;margin:.4rem 0\"><source src=\"'+src+'\"></video>'});\n" +
            "    return null;};\n" +
            "  e.img=function(src,alt,ww){\n" +
            "    src=_resolveAsset(src);\n" +
            "    self.out.push({t:'html',v:'<img src=\"'+src+'\" alt=\"'+(alt||'')+'\" style=\"width:'+(ww||'auto')+';border-radius:8px;max-width:100%;margin:.4rem 0;display:block\" loading=\"lazy\">'});\n" +
            "    return null;};\n" +
            "  e.h=function(lv,txt){\n" +
            "    var lvl=Math.min(6,Math.max(1,lv|0));\n" +
            "    var sz=['2rem','1.6rem','1.3rem','1.15rem','1rem','0.9rem'][lvl-1];\n" +
            "    self.out.push({t:'html',v:'<h'+lvl+' style=\"font-size:'+sz+'\">'+txt+'</h'+lvl+'>'});\n" +
            "    return null;};\n" +
            "  e.l=function(){\n" +
            "    var items=Array.prototype.slice.call(arguments);\n" +
            "    var li=items.map(function(it){return '<li>'+it+'</li>';}).join('');\n" +
            "    self.out.push({t:'html',v:'<ul>'+li+'</ul>'});\n" +
            "    return null;};\n" +
            "};\n" +

            "VCG.prototype.run=function(){while(this.cur().t!=='EOF'&&!this._hasRet){this.parseStmt(this.env);}};\n" +
            "VCG.prototype.parseBlock=function(e){this.eat('{');while(this.cur().v!=='}'&&this.cur().t!=='EOF'){this.parseStmt(e);if(this._hasRet||this._brk||this._cnt)break;}this.eat('}');};\n" +
            "VCG.prototype._skipBlock=function(){this.eat('{');var d=1;while(d>0&&this.cur().t!=='EOF'){if(this.cur().v==='{')d++;else if(this.cur().v==='}')d--;if(d>0)this.eat();else break;}this.eat('}');};\n" +
            "VCG.prototype._skipStmt=function(){var t=this.cur();if(t.v==='if'){this.eat();this.parseExpr(this.env);this._skipBlock();if(this.cur().v==='else'){this.eat();if(this.cur().v==='if')this._skipStmt();else this._skipBlock();}}else{while(this.cur().t!=='NL'&&this.cur().v!=='}'&&this.cur().t!=='EOF')this.eat();}};\n" +

            "VCG.prototype.parseExpr=function(e,minp){\n" +
            "  minp=minp||0;var left=this.parsePrimary(e);\n" +
            "  var P={'||':1,'or':1,'&&':2,'and':2,'==':3,'!=':3,'<':4,'>':4,'<=':4,'>=':4,'+':5,'-':5,'*':6,'/':6,'%':6,'**':7,'..':8,'|>':0};\n" +
            "  while(true){var op=this.cur().v,p=P[op]||0;if(p<=minp)break;\n" +
            "    this.eat();\n" +
            "    if(op==='|>'){var fn=this.parsePrimary(e);if(typeof fn==='function')left=fn(left);}\n" +
            "    else left=this._bin(op,left,this.parseExpr(e,op==='**'?p-1:p));}\n" +
            "  if(this.cur().v==='?'){this.eat();var th=this.parseExpr(e);this.eat(':');return left?th:this.parseExpr(e);}\n" +
            "  return left;\n" +
            "};\n" +
            "VCG.prototype._bin=function(op,a,b){\n" +
            "  if(op==='+'&&(typeof a==='string'||typeof b==='string'))return String(a===null?'nil':a)+String(b===null?'nil':b);\n" +
            "  if(op==='+'&&Array.isArray(a)&&Array.isArray(b))return a.concat(b);\n" +
            "  if(op==='..')return this.env.range(a,b+1);\n" +
            "  switch(op){case '+':return a+b;case '-':return a-b;case '*':return a*b;\n" +
            "  case '/':if(b===0)throw new Error('Division by zero');return a/b;\n" +
            "  case '%':return a%b;case '**':return Math.pow(a,b);\n" +
            "  case '==':return a==b;case '!=':return a!=b;\n" +
            "  case '<':return a<b;case '>':return a>b;case '<=':return a<=b;case '>=':return a>=b;\n" +
            "  case 'and':return !!a&&!!b;case 'or':return !!a||!!b;\n" +
            "  case '|':return(a|0)|(b|0);case '&':return(a|0)&(b|0);}\n" +
            "  return null;\n" +
            "};\n" +
            "VCG.prototype.parsePrimary=function(e){\n" +
            "  var t=this.cur();\n" +
            "  if(t.t==='NUM'){this.eat();return t.v;}\n" +
            "  if(t.t==='STR'){this.eat();return t.v;}\n" +
            "  if(t.v==='true'){this.eat();return true;}\n" +
            "  if(t.v==='false'){this.eat();return false;}\n" +
            "  if(t.v==='nil'||t.v==='null'){this.eat();return null;}\n" +
            "  if(t.v==='not'||t.v==='!'){this.eat();return !this.parsePrimary(e);}\n" +
            "  if(t.v==='-'){this.eat();return -this.parsePrimary(e);}\n" +
            "  if(t.v==='('){this.eat();var v=this.parseExpr(e);this.eat(')');return v;}\n" +
            "  if(t.v==='['){this.eat();var a=[];while(this.cur().v!==']'&&this.cur().t!=='EOF'){a.push(this.parseExpr(e));this.eatIf(',');}this.eat(']');return a;}\n" +
            "  if(t.v==='{'){this.eat();var o=Object.create(null);while(this.cur().v!=='}'&&this.cur().t!=='EOF'){var k=this.eat().v;this.eat(':');o[k]=this.parseExpr(e);this.eatIf(',');}this.eat('}');return o;}\n" +
            "  if(t.v==='$set'){this.eat();this.eat('(');var k=this.parseExpr(e);this.eat(',');var v=this.parseExpr(e);this.eat(')');this._store[k]=v;if(this._watchers[k])this._watchers[k](v);return v;}\n" +
            "  if(t.v==='$get'){this.eat();this.eat('(');var k=this.parseExpr(e);this.eat(')');return this._store[k]!==undefined?this._store[k]:null;}\n" +
            "  if(t.v==='$x'){this.eat();var v=this.parseExpr(e);return typeof v==='function'?v():v;}\n" +
            "  if(t.v==='await'){this.eat();return this.parseExpr(e);}\n" +
            "  if(t.v==='typeof'){this.eat();this.eat('(');var v=this.parseExpr(e);this.eat(')');return this.env.typeof(v);}\n" +
            "  if(t.v==='sizeof'){this.eat();this.eat('(');var v=this.parseExpr(e);this.eat(')');return this.env.sizeof(v);}\n" +
            "  if(t.v==='new'){this.eat();var nm=this.eat().v;\n" +
            "    this.eat('(');var args=[];while(this.cur().v!==')'&&this.cur().t!=='EOF'){args.push(this.parseExpr(e));this.eatIf(',');}this.eat(')');\n" +
            "    var cls=null;try{cls=this._get(e,nm);}catch(x){}\n" +
            "    if(typeof cls==='function'){return cls.apply(null,args);}\n" +
            "    var obj=Object.create(null);obj.__class__=nm;\n" +
            "    if(cls&&typeof cls==='object'){Object.assign(obj,cls);}\n" +
            "    if(typeof obj.init==='function'){obj.init.apply(obj,args);}\n" +
            "    return obj;}\n" +
            "  if(t.v==='self'){this.eat();var v=null;try{v=this._get(e,'self');}catch(x){}return this._postfix(e,v,'self');}\n" +
            "  if(t.t==='ID'||t.t==='KW'){var nm=t.v;this.eat();\n" +
            "    var val=null;try{val=this._get(e,nm);}catch(x){}\n" +
            "    return this._postfix(e,val,nm);}\n" +
            "  this.eat();return null;\n" +
            "};\n" +
            "VCG.prototype._postfix=function(e,val,nm){\n" +
            "  while(true){\n" +
            "    if(this.cur().v==='('){\n" +
            "      this.eat();var args=[];\n" +
            "      while(this.cur().v!==')'&&this.cur().t!=='EOF'){args.push(this.parseExpr(e));this.eatIf(',');}\n" +
            "      this.eat(')');\n" +
            "      if(typeof val==='function')val=val.apply(null,args);\n" +
            "      else throw new Error('Not callable: '+nm);\n" +
            "    }else if(this.cur().v==='['){\n" +
            "      this.eat();var idx=this.parseExpr(e);this.eat(']');\n" +
            "      val=Array.isArray(val)?(idx<0?val[val.length+idx]:val[idx]):val&&typeof val==='object'?val[idx]:null;\n" +
            "    }else if(this.cur().v==='.'){\n" +
            "      this.eat();var f=this.eat().v;\n" +
            "      if(this.cur().v==='('){\n" +
            "        this.eat();var args=[];\n" +
            "        while(this.cur().v!==')'&&this.cur().t!=='EOF'){args.push(this.parseExpr(e));this.eatIf(',');}\n" +
            "        this.eat(')');\n" +
            "        val=this._method(val,f,args);\n" +
            "      }else val=val&&typeof val==='object'?val[f]:null;\n" +
            "    }else break;\n" +
            "  }\n" +
            "  return val;\n" +
            "};\n" +
            "VCG.prototype._method=function(obj,name,args){\n" +
            "  if(Array.isArray(obj)){\n" +
            "    if(name==='push'){obj.push.apply(obj,args);return obj.length;}\n" +
            "    if(name==='pop')return obj.pop();\n" +
            "    if(name==='len'||name==='length')return obj.length;\n" +
            "    if(name==='join')return obj.join(args[0]||',');\n" +
            "    if(name==='contains'||name==='includes')return obj.indexOf(args[0])>=0;\n" +
            "    if(name==='reverse'){obj.reverse();return obj;}\n" +
            "    if(name==='slice')return obj.slice(args[0]||0,args[1]);\n" +
            "    if(name==='sort'){obj.sort(function(a,b){return a<b?-1:a>b?1:0;});return obj;}\n" +
            "    if(name==='map')return obj.map(function(x){return args[0](x);});\n" +
            "    if(name==='filter')return obj.filter(function(x){return args[0](x);});\n" +
            "    if(name==='find')return obj.find(function(x){return args[0](x);})||null;\n" +
            "  }\n" +
            "  if(typeof obj==='string'){\n" +
            "    if(name==='len'||name==='length')return obj.length;\n" +
            "    if(name==='upper')return obj.toUpperCase();\n" +
            "    if(name==='lower')return obj.toLowerCase();\n" +
            "    if(name==='trim')return obj.trim();\n" +
            "    if(name==='split')return obj.split(args[0]!==undefined?args[0]:' ');\n" +
            "    if(name==='contains'||name==='includes')return obj.includes(String(args[0]));\n" +
            "    if(name==='startswith')return obj.startsWith(String(args[0]));\n" +
            "    if(name==='endswith')return obj.endsWith(String(args[0]));\n" +
            "    if(name==='replace')return obj.split(String(args[0])).join(String(args[1]));\n" +
            "    if(name==='tonum')return parseFloat(obj);\n" +
            "    if(name==='toint')return parseInt(obj);\n" +
            "  }\n" +
            "  if(obj&&typeof obj==='object'&&typeof obj[name]==='function')return obj[name].apply(obj,args);\n" +
            "  if(obj&&typeof obj==='object'&&name in obj)return obj[name];\n" +
            "  return null;\n" +
            "};\n" +

            "VCG.prototype.parseStmt=function(e){\n" +
            "  var t=this.cur(),self=this;\n" +
            "  var UI=['youtube','facebook','instagram','xsocial','url','btn','key','video','img','h','l'];\n" +
            "  if(UI.indexOf(t.v)>=0){var nm=t.v;this.eat();\n" +
            "    if(this.cur().v==='('){this.eat();var args=[];\n" +
            "      while(this.cur().v!==')'&&this.cur().t!=='EOF'){args.push(this.parseExpr(e));this.eatIf(',');}\n" +
            "      this.eat(')');if(typeof this.env[nm]==='function')this.env[nm].apply(null,args);}return;}\n" +
            "  if(t.v==='$set'){this.eat();this.eat('(');var k=this.parseExpr(e);this.eat(',');var v=this.parseExpr(e);this.eat(')');this._store[k]=v;if(this._watchers[k])this._watchers[k](v);return;}\n" +
            "  if(t.v==='watch'){this.eat();this.eat('(');var k=this.parseExpr(e);this.eat(',');var fn=this.parseExpr(e);this.eat(')');this._watchers[k]=fn;return;}\n" +
            "  if(t.v==='public'||t.v==='export'||t.v==='async'||t.v==='doc'){this.eat();if(this.cur().t!=='EOF'&&this.cur().v!=='}')this.parseStmt(e);return;}\n" +
            "  if(t.v==='w'){this.eat();var nm=this.eat().v;if(this.cur().v==='='){this.eat();var v=this.parseExpr(e);this._def(e,nm,v);this.out.push({t:'txt',v:'[w] '+nm+' \u2190 '+this._str(v)});}return;}\n" +
            "  if(t.v==='c'){this.eat();if(this.cur().t==='ID'){var nm=this.eat().v;var init=null;if(this.cur().v==='='){this.eat();init=this.parseExpr(e);}var ch={_buf:[],_name:nm};if(init!==null)ch._buf.push(init);this._def(e,nm,ch);}return;}\n" +
            "  if(t.v==='let'||t.v==='const'){this.eat();var nm=this.eat().v;if(this.cur().v==='='){this.eat();this._def(e,nm,this.parseExpr(e));}else this._def(e,nm,null);return;}\n" +
            "  if(t.v==='type'){this.eat();var nm=this.eat().v;if(this.cur().v==='='){this.eat();this._def(e,nm,this.parseExpr(e));}return;}\n" +
            "  if(t.v==='enum'){this.eat();var nm=this.eat().v;this.eat('{');var en=Object.create(null);var i=0;\n" +
            "    while(this.cur().v!=='}'&&this.cur().t!=='EOF'){en[this.eat().v]=i++;this.eatIf(',');}\n" +
            "    this.eat('}');this._def(e,nm,Object.freeze(en));return;}\n" +
            "  if(t.v==='class'){this.eat();var nm=this.eat().v;\n" +
            "    var base=null;if(this.cur().v==='extends'){this.eat();try{base=this._get(e,this.eat().v);}catch(x){}}\n" +
            "    if(this.cur().v==='implements'){this.eat();while(this.cur().t==='ID'){this.eat();this.eatIf(',');}}\n" +
            "    var proto=base?Object.create(base.prototype||base):{};proto.__class__=nm;\n" +
            "    var ce=this._scope(e);this._def(ce,'self',null);\n" +
            "    this.eat('{');\n" +
            "    while(this.cur().v!=='}'&&this.cur().t!=='EOF'){this.parseStmt(ce);}\n" +
            "    this.eat('}');\n" +
            "    for(var k in ce){if(Object.prototype.hasOwnProperty.call(ce,k))proto[k]=ce[k];}\n" +
            "    var ctor=(function(p,nm_){return function(){\n" +
            "      var s=Object.create(p);s.__class__=nm_;\n" +
            "      if(typeof s.init==='function')s.init.apply(s,arguments);\n" +
            "      return s;};})(proto,nm);\n" +
            "    ctor.prototype=proto;\n" +
            "    this._def(e,nm,ctor);return;}\n" +
            "  if(t.v==='module'){this.eat();var nm=this.eat().v;\n" +
            "    var me=this._scope(e);this.eat('{');\n" +
            "    while(this.cur().v!=='}'&&this.cur().t!=='EOF'){this.parseStmt(me);}\n" +
            "    this.eat('}');this._def(e,nm,me);return;}\n" +
            "  if(t.v==='func'){this.eat();var nm=this.eat().v;this.eat('(');\n" +
            "    var params=[];while(this.cur().v!==')'&&this.cur().t!=='EOF'){params.push(this.eat().v);this.eatIf(',');}this.eat(')');\n" +
            "    var bp=this.pos;this._skipBlock();var be=this.pos;var cl=e;\n" +
            "    this._def(e,nm,function(){var args=Array.prototype.slice.call(arguments);\n" +
            "      var fe=self._scope(cl);\n" +
            "      params.forEach(function(p,i){self._def(fe,p,i<args.length?args[i]:null);});\n" +
            "      var sp=self.pos;self.pos=bp;self.parseBlock(fe);self.pos=sp;\n" +
            "      var r=self._ret;self._ret=undefined;self._hasRet=false;return r!==undefined?r:null;});return;}\n" +
            "  if(t.v==='if'){this.eat();var cond=this.parseExpr(e);var bp=this.pos;this._skipBlock();var be=this.pos;\n" +
            "    if(cond){this.pos=bp;this.parseBlock(this._scope(e));this.pos=be;}else this.pos=be;\n" +
            "    if(this.cur().v==='else'){this.eat();if(this.cur().v==='if'){if(cond)this._skipStmt();else this.parseStmt(this._scope(e));}else{if(cond)this._skipBlock();else this.parseBlock(this._scope(e));}}return;}\n" +
            "  if(t.v==='while'){this.eat();var cp=this.pos;\n" +
            "    while(true){this.pos=cp;var cond=this.parseExpr(e);if(!cond){this._skipBlock();break;}\n" +
            "      this.parseBlock(this._scope(e));if(this._brk){this._brk=false;break;}\n" +
            "      if(this._cnt)this._cnt=false;if(this._hasRet)break;}return;}\n" +
            "  if(t.v==='repeat'){this.eat();var cnt=Math.floor(this.parseExpr(e));var bp=this.pos;this._skipBlock();var be=this.pos;\n" +
            "    for(var ri=0;ri<cnt&&!this._hasRet;ri++){this.pos=bp;this.parseBlock(this._scope(e));if(this._brk){this._brk=false;break;}if(this._cnt)this._cnt=false;}this.pos=be;return;}\n" +
            "  if(t.v==='for'){this.eat();var nm=this.eat().v;this.eat('in');var iter=this.parseExpr(e);\n" +
            "    var bp=this.pos;this._skipBlock();var be=this.pos;\n" +
            "    var arr=Array.isArray(iter)?iter:typeof iter==='string'?iter.split(''):iter&&typeof iter==='object'&&iter._buf?iter._buf:iter&&typeof iter==='object'?Object.values(iter):[];\n" +
            "    for(var fi=0;fi<arr.length;fi++){if(this._hasRet)break;this.pos=bp;var le=this._scope(e);this._def(le,nm,arr[fi]);this.parseBlock(le);if(this._brk){this._brk=false;break;}if(this._cnt)this._cnt=false;}this.pos=be;return;}\n" +
            "  if(t.v==='return'){this.eat();this._ret=(this.cur().v!=='}'&&this.cur().t!=='EOF')?this.parseExpr(e):null;this._hasRet=true;return;}\n" +
            "  if(t.v==='break'){this.eat();this._brk=true;return;}\n" +
            "  if(t.v==='continue'){this.eat();this._cnt=true;return;}\n" +
            "  if(t.v==='show'||t.v==='print'){this.eat();this.eat('(');var parts=[];\n" +
            "    while(this.cur().v!==')'&&this.cur().t!=='EOF'){parts.push(this.parseExpr(e));this.eatIf(',');}this.eat(')');\n" +
            "    this.out.push({t:'txt',v:parts.map(function(v){return self._str(v);}).join(' ')});return;}\n" +
            "  if(t.v==='html'){this.eat();var v=this.parseExpr(e);this.out.push({t:'html',v:String(v)});return;}\n" +
            "  if(t.v==='assert'){this.eat();this.eat('(');var c=this.parseExpr(e);var msg='Assertion failed';if(this.cur().v===','){this.eat();msg=String(this.parseExpr(e));}this.eat(')');if(!c)throw new Error(msg);return;}\n" +
            "  if(t.v==='throw'){this.eat();var v=this.parseExpr(e);throw new Error(String(v));}\n" +
            "  if(t.v==='try'){this.eat();var bp=this.pos;this._skipBlock();var be=this.pos;\n" +
            "    this.eat('catch');var nm2=null;if(this.cur().t==='ID')nm2=this.eat().v;\n" +
            "    var cp=this.pos;this._skipBlock();var ce=this.pos;var err=null;\n" +
            "    try{this.pos=bp;this.parseBlock(this._scope(e));}catch(ex){err=ex;}\n" +
            "    if(err){this.pos=cp;var cv=this._scope(e);if(nm2)this._def(cv,nm2,err.message);this.parseBlock(cv);}this.pos=ce;return;}\n" +
            "  if(t.v==='safe'){this.eat();var bp=this.pos;this._skipBlock();var be=this.pos;\n" +
            "    try{this.pos=bp;this.parseBlock(this._scope(e));}catch(x){this.out.push({t:'txt',v:'[safe] '+x.message});}this.pos=be;return;}\n" +
            "  if(t.v==='unsafe'){this.eat();this.parseBlock(this._scope(e));return;}\n" +
            "  if(t.v==='guard'){this.eat();var cond=this.parseExpr(e);\n" +
            "    if(this.cur().v==='else'){this.eat();if(!cond){this.parseBlock(this._scope(e));}else{this._skipBlock();}}\n" +
            "    return;}\n" +
            "  if(t.v==='match'){this.eat();var val=this.parseExpr(e);this.eat('{');\n" +
            "    while(this.cur().v==='when'&&!this._hasRet){this.eat();var arm=this.parseExpr(e);\n" +
            "      if(this.cur().v==='->'||this.cur().v===':')this.eat();\n" +
            "      if(val==arm)this.parseStmt(e);else this._skipStmt();}\n" +
            "    this.eat('}');return;}\n" +
            "  if(t.v==='test'){this.eat();var nm=this.cur().t==='STR'?this.eat().v:'test';\n" +
            "    var bp=this.pos;this._skipBlock();var be=this.pos;\n" +
            "    try{this.pos=bp;this.parseBlock(this._scope(e));this.out.push({t:'txt',v:'[PASS] '+nm});}\n" +
            "    catch(x){this.out.push({t:'txt',v:'[FAIL] '+nm+': '+x.message});}this.pos=be;return;}\n" +
            "  if(t.v==='with'){this.eat();var res=this.parseExpr(e);var alias=null;\n" +
            "    if(this.cur().v==='as'){this.eat();alias=this.eat().v;}\n" +
            "    var we=this._scope(e);if(alias)this._def(we,alias,res);\n" +
            "    this.parseBlock(we);\n" +
            "    if(res&&typeof res.close==='function')res.close();return;}\n" +
            "  if(t.v==='struct'){this.eat();var nm=this.eat().v;this.eat('{');var fields=[];\n" +
            "    while(this.cur().v!=='}'&&this.cur().t!=='EOF'){fields.push(this.eat().v);this.eatIf(',');}\n" +
            "    this.eat('}');var proto=Object.create(null);proto.__type__=nm;\n" +
            "    fields.forEach(function(f){proto[f]=null;});this._def(e,nm,proto);return;}\n" +
            "  if(t.v==='defer'){this.eat();var bp=this.pos;this._skipStmt();var be=this.pos;\n" +
            "    var sp=this.pos;this.pos=bp;this.parseStmt(this._scope(e));this.pos=sp;return;}\n" +
            "  if(t.v==='promise'){this.eat();this.parseBlock(this._scope(e));return;}\n" +
            "  if(t.v==='import'||t.v==='from'){while(this.cur().v!=='{'&&this.cur().t!=='EOF'&&this.cur().t!=='NL')this.eat();return;}\n" +
            "  if(t.v==='interface'||t.v==='union'||t.v==='generic'||t.v==='ptr'||t.v==='ref'||t.v==='alloc'||t.v==='free'||t.v==='http'||t.v==='socket'||t.v==='mock'){\n" +
            "    while(this.cur().t!=='NL'&&this.cur().v!=='{'&&this.cur().v!=='}'&&this.cur().t!=='EOF')this.eat();\n" +
            "    if(this.cur().v==='{')this._skipBlock();return;}\n" +
            "  var expr=this.parseExpr(e);\n" +
            "  var op=this.cur().v;\n" +
            "  if(op==='='||op==='+='||op==='-='||op==='*='||op==='/='){\n" +
            "    this.eat();var rhs=this.parseExpr(e);\n" +
            "    if(t.t==='ID'||t.t==='KW'){var nm=t.v;var cur=this._tryGet(e,nm);\n" +
            "      var nv=op==='='?rhs:op==='+='?cur+rhs:op==='-='?cur-rhs:op==='*='?cur*rhs:cur/rhs;\n" +
            "      this._set(e,nm,nv);}}\n" +
            "};\n";
    }

    /** Public accessor — returns the JS runtime string (for SDK use) */
    public static String getPublicRuntime() {
        return getRuntime();
    }

    /** Public accessor — returns the CSS styles string (for SDK use) */
    public static String getPublicStyles(String theme) {
        return getStyles(theme);
    }
}
