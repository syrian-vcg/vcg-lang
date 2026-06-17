package com.syrianvcg.editor;

public class VcgInterpreter {

    public static String buildHtml(String vcgCode, String title) {
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
            "    var e2=document.createElement('span');e2.className='empty';e2.textContent='No output';_out.appendChild(e2);\n" +
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

    private static String getVcgRuntime() {
        return
            "/* VCG v2.0 JavaScript Runtime */\n" +
            "var KW=['let','const','func','return','if','else','while','for','in','repeat',\n" +
            " 'break','continue','show','input','and','or','not','html','true','false','nil','null',\n" +
            " 'import','as','struct','new','self','typeof','sizeof','assert','try','catch','throw',\n" +
            " 'match','when','public','w','x','c','youtube','facebook','instagram','xsocial',\n" +
            " 'url','btn','key','video','img','h','l','watch',\n" +
            " 'module','export','from','class','extends','implements','interface','super','this',\n" +
            " 'async','await','promise','defer','type','enum','union','generic',\n" +
            " 'file','read','write','append','http','request','response','socket',\n" +
            " 'ref','ptr','alloc','free','safe','unsafe','guard','doc','test','expect',\n" +
            " 'mock','with','case','pipeline','lambda'];\n" +
            "\n" +
            "function tokenize(src){\n" +
            "  var toks=[],i=0,ln=1;\n" +
            "  var adv=function(){var c=src[i++];if(c=='\\n')ln++;return c;};\n" +
            "  while(i<src.length){\n" +
            "    var c=src[i];\n" +
            "    if(c=='\\n'){toks.push({t:'NL',v:'\\n',ln:ln});adv();continue;}\n" +
            "    if(c==' '||c=='\\t'||c=='\\r'){i++;continue;}\n" +
            "    if(c=='#'||c=='/'&&src[i+1]=='/'){while(i<src.length&&src[i]!='\\n')i++;continue;}\n" +
            "    if(c=='/'&&src[i+1]=='*'){i+=2;while(i<src.length&&!(src[i]=='*'&&src[i+1]=='/'))i++;i+=2;continue;}\n" +
            "    if(c=='\\\\'){toks.push({t:'LAMBDA',v:'\\\\',ln:ln});adv();continue;}\n" +
            "    if(c=='|'&&src[i+1]=='>'){toks.push({t:'OP',v:'|>',ln:ln});i+=2;continue;}\n" +
            "    if(c=='-'&&src[i+1]=='>'){toks.push({t:'OP',v:'->',ln:ln});i+=2;continue;}\n" +
            "    if(c=='$'){i++;var s='';while(i<src.length&&/[a-zA-Z_]/.test(src[i]))s+=src[i++];\n" +
            "      toks.push({t:['set','get','c','w','x'].indexOf(s)>=0?'KW':'OP',v:'$'+s,ln:ln});continue;}\n" +
            "    if(c=='\"'||c==\"'\"){var q=adv(),s='';\n" +
            "      while(i<src.length&&src[i]!=q){if(src[i]=='\\\\'){adv();var e=adv();s+=e=='n'?'\\n':e=='t'?'\\t':e;}else s+=adv();}\n" +
            "      adv();toks.push({t:'STR',v:s,ln:ln});continue;}\n" +
            "    if(c=='`'){adv();var s='';while(i<src.length&&src[i]!='`')s+=adv();adv();toks.push({t:'STR',v:s,ln:ln});continue;}\n" +
            "    if(/\\d/.test(c)||c=='.'&&/\\d/.test(src[i+1])){var s='';while(i<src.length&&(/\\d/.test(src[i])||src[i]=='.'))s+=adv();\n" +
            "      if(/[eE]/.test(src[i])){s+=adv();if(/[+-]/.test(src[i]))s+=adv();while(/\\d/.test(src[i]))s+=adv();}\n" +
            "      toks.push({t:'NUM',v:parseFloat(s),ln:ln});continue;}\n" +
            "    if(c=='0'&&(src[i+1]=='x'||src[i+1]=='X')){var hs=adv()+adv();while(i<src.length&&/[0-9a-fA-F]/.test(src[i]))hs+=adv();toks.push({t:'NUM',v:parseInt(hs,16),ln:ln});continue;}\n" +
            "    if(/[a-zA-Z_\\u0600-\\u06FF]/.test(c)){var s='';\n" +
            "      while(i<src.length&&/[a-zA-Z0-9_\\u0600-\\u06FF]/.test(src[i]))s+=adv();\n" +
            "      toks.push({t:KW.indexOf(s)>=0?'KW':'ID',v:s,ln:ln});continue;}\n" +
            "    var two=src.slice(i,i+2);\n" +
            "    if(['==','!=','<=','>=','+=','-=','*=','/=','++','--','**','..','<<','>>'].indexOf(two)>=0){toks.push({t:'OP',v:two,ln:ln});i+=2;continue;}\n" +
            "    toks.push({t:'OP',v:adv(),ln:ln});\n" +
            "  }\n" +
            "  toks.push({t:'EOF',v:'',ln:ln});return toks;\n" +
            "}\n" +
            "\n" +
            "function VCG(toks){\n" +
            "  this.toks=toks.filter(function(t){return t.t!='NL';});\n" +
            "  this.pos=0;this.env=Object.create(null);\n" +
            "  this._store={};this._watchers={};this._exports={};this._channels={};this._writeonly={};this._defer=[];\n" +
            "  this.out=[];this._ret=undefined;this._hasRet=false;this._brk=false;this._cnt=false;\n" +
            "  this._initStdlib();\n" +
            "  this._classProtos={};\n" +
            "  this._modules={};\n" +
            "}\n" +
            "\n" +
            "VCG.prototype.cur=function(){return this.toks[this.pos];};\n" +
            "VCG.prototype.eat=function(v){var t=this.cur();if(v&&t.v!==v)throw new Error('Expected '+v+' got '+t.v+' line '+t.ln);this.pos++;return t;};\n" +
            "VCG.prototype.eatIf=function(v){if(this.cur().v===v){this.eat();return true;}return false;};\n" +
            "VCG.prototype._def=function(e,k,v){Object.defineProperty(e,k,{value:v,writable:true,configurable:true,enumerable:true});return v;};\n" +
            "VCG.prototype._set=function(e,k,v){var c=e;while(c){if(Object.prototype.hasOwnProperty.call(c,k)){c[k]=v;return v;}c=Object.getPrototypeOf(c);}e[k]=v;return v;};\n" +
            "VCG.prototype._get=function(e,k){var c=e;while(c){if(Object.prototype.hasOwnProperty.call(c,k))return c[k];c=Object.getPrototypeOf(c);}throw new Error('Undefined: '+k);};\n" +
            "VCG.prototype._tryGet=function(e,k){try{return this._get(e,k);}catch(x){return null;}};\n" +
            "VCG.prototype._scope=function(p){return Object.create(p||null);};\n" +
            "VCG.prototype._str=function(x){return x===null?'nil':x===true?'true':x===false?'false':Array.isArray(x)?'['+x.map(function(v){return this._str(v);},this).join(', ')+']':typeof x==='object'?JSON.stringify(x):String(x);};\n" +
            "VCG.prototype._truthy=function(v){return v!==null&&v!==false&&v!==0&&v!==''&&v!==undefined;};\n" +
            "\n" +
            "VCG.prototype._initStdlib=function(){\n" +
            "  var e=this.env,m=Math,self=this;\n" +
            "  ['abs','floor','ceil','round','sqrt','sin','cos','tan','log','log2','log10'].forEach(function(f){e[f]=function(){return m[f].apply(m,arguments);};});\n" +
            "  e.pow=function(b,x){return m.pow(b,x);};e.min=function(){return m.min.apply(m,arguments);};e.max=function(){return m.max.apply(m,arguments);};\n" +
            "  e.clamp=function(v,lo,hi){return m.max(lo,m.min(hi,v));};e.rand=function(a,b){return a===undefined?m.random():b===undefined?m.floor(m.random()*a):m.floor(m.random()*(b-a))+a;};\n" +
            "  e.range=function(a,b,s){var r=[],f=b===undefined?0:a,t=b===undefined?a:b,st=s||1;for(var i=f;st>0?i<t:i>t;i+=st)r.push(i);return r;};\n" +
            "  e.len=function(x){return Array.isArray(x)?x.length:typeof x==='string'?x.length:x&&typeof x==='object'?Object.keys(x).length:0;};\n" +
            "  e.str=function(x){return self._str(x);};e.int=function(x){return parseInt(x)||0;};e.float=function(x){return parseFloat(x)||0;};\n" +
            "  e.bool=function(x){return self._truthy(x);};e.char=function(n){return String.fromCharCode(n);};e.ord=function(s){return s?s.charCodeAt(0):0;};\n" +
            "  e.keys=function(o){return o&&typeof o==='object'?Object.keys(o):[];};e.values=function(o){return o&&typeof o==='object'?Object.values(o):[];};\n" +
            "  e.join=function(a,s){return Array.isArray(a)?a.join(s||','):String(a);};e.format=function(){var f=arguments[0],i=1,args=arguments;return String(f).replace(/%s/g,function(){return args[i++]||'';});};\n" +
            "  e.typeof=function(x){return x===null?'nil':typeof x==='boolean'?'bool':Array.isArray(x)?'array':typeof x==='object'?'struct':typeof x;};\n" +
            "  e.sizeof=function(x){return Array.isArray(x)?x.length:typeof x==='string'?x.length:x&&typeof x==='object'?Object.keys(x).length:0;};\n" +
            "  e.isnil=function(x){return x===null||x===undefined;};e.isnum=function(x){return typeof x==='number';};e.isstr=function(x){return typeof x==='string';};e.isarr=function(x){return Array.isArray(x);};e.isfunc=function(x){return typeof x==='function';};e.isstruct=function(x){return x&&typeof x==='object'&&!Array.isArray(x);};\n" +
            "  e.defined=function(x){return x!==null&&x!==undefined;};e.freeze=function(v){return Object.freeze(v)||v;};\n" +
            "  e.send=function(ch,v){if(ch&&ch._buf)ch._buf.push(v);return v;};e.recv=function(ch){if(ch&&ch._buf&&ch._buf.length>0)return ch._buf.shift();return null;};\n" +
            "  e.pipe=function(val){var fns=Array.prototype.slice.call(arguments,1);return fns.reduce(function(v,f){return typeof f==='function'?f(v):v;},val);};\n" +
            "  e.show=function(){var parts=Array.prototype.slice.call(arguments);self.out.push({t:'txt',v:parts.map(function(v){return self._str(v);}).join(' ')});return null;};\n" +
            "  e.print=e.show;\n" +
            "  e.VCG_VERSION='2.0.0';e.VCG_DATE='2026-06-06';e.PI=m.PI;e.E=m.E;e.INF=Infinity;e.TAU=6.283185307179586;e.PHI=1.618033988749895;e.true=true;e.false=false;e.nil=null;\n" +
            "  e.flat=function(a){if(!Array.isArray(a))return[];return a.reduce(function(r,v){return r.concat(Array.isArray(v)?v:[v]);},[]);};\n" +
            "  e.unique=function(a){if(!Array.isArray(a))return[];return a.filter(function(v,i,s){return s.indexOf(v)===i;});};\n" +
            "  e.sum=function(a){if(!Array.isArray(a))return 0;return a.reduce(function(s,v){return s+(typeof v==='number'?v:0);},0);};\n" +
            "  e.avg=function(a){if(!Array.isArray(a)||a.length===0)return 0;return e.sum(a)/a.length;};\n" +
            "  e.first=function(a){return Array.isArray(a)&&a.length>0?a[0]:null;};\n" +
            "  e.last=function(a){return Array.isArray(a)&&a.length>0?a[a.length-1]:null;};\n" +
            "  e.chunk=function(a,sz){if(!Array.isArray(a))return[];var r=[],sz=sz||1;for(var i=0;i<a.length;i+=sz)r.push(a.slice(i,i+sz));return r;};\n" +
            "  e.zip=function(a,b){var len=Math.min(a.length,b.length),r=[];for(var i=0;i<len;i++)r.push([a[i],b[i]]);return r;};\n" +
            "  e.repeat=function(s,n){if(typeof s!=='string')return'';return s.repeat(Math.max(0,n||0));};\n" +
            "  e.pad_start=function(s,n,pad){if(typeof s!=='string')return'';pad=pad||' ';return s.padStart(n,pad);};\n" +
            "  e.pad_end=function(s,n,pad){if(typeof s!=='string')return'';pad=pad||' ';return s.padEnd(n,pad);};\n" +
            "  e.includes=function(s,sub){if(typeof s!=='string')return false;return s.includes(sub);};\n" +
            "  e.indexof=function(s,sub){if(typeof s!=='string')return-1;return s.indexOf(sub);};\n" +
            "  e.count=function(s,sub){if(typeof s!=='string'||typeof sub!=='string')return 0;var c=0,p=0;while((p=s.indexOf(sub,p))!==-1){c++;p++;}return c;};\n" +
            "  e.merge=function(){var r={};for(var i=0;i<arguments.length;i++){var o=arguments[i];if(o&&typeof o==='object')Object.assign(r,o);}return r;};\n" +
            "  e.has=function(o,k){return o&&typeof o==='object'&&k in o;};\n" +
            "  e.del=function(o,k){if(o&&typeof o==='object')delete o[k];return o;};\n" +
            "  e.entries=function(o){return o&&typeof o==='object'?Object.entries(o):[];};\n" +
            "  e.gcd=function(a,b){a=Math.abs(a);b=Math.abs(b);while(b){var t=b;b=a%b;a=t;}return a;};\n" +
            "  e.lcm=function(a,b){return Math.abs(a*b)/e.gcd(a,b)||0;};\n" +
            "  e.fib=function(n){n=Math.floor(n);if(n<=1)return n;var p=0,q=1;for(var i=2;i<=n;i++){var t=p+q;p=q;q=t;}return q;};\n" +
            "  e.factorial=function(n){n=Math.floor(n);if(n<0)return 0;var r=1;for(var i=2;i<=n&&i<=20;i++)r*=i;return r;};\n" +
            "  e.is_prime=function(n){n=Math.floor(n);if(n<2)return false;if(n===2)return true;if(n%2===0)return false;for(var i=3;i*i<=n;i+=2)if(n%i===0)return false;return true;};\n" +
            "  e.JSON_stringify=function(x){return JSON.stringify(x);};\n" +
            "  e.JSON_parse=function(s){try{return JSON.parse(s);}catch(e){return null;}};\n" +
            "  e.assert_eq=function(a,b){if(a!==b){self.out.push({t:'txt',v:'[ASSERT_EQ FAIL] expected '+self._str(b)+' got '+self._str(a)});return false;}return true;};\n" +
            "  e.assert_ne=function(a,b){if(a===b){self.out.push({t:'txt',v:'[ASSERT_NE FAIL] values should not be equal: '+self._str(a)});return false;}return true;};\n" +
            "  e.assert_true=function(a){if(!a){self.out.push({t:'txt',v:'[ASSERT_TRUE FAIL]'});return false;}return true;};\n" +
            "  e.assert_false=function(a){if(a){self.out.push({t:'txt',v:'[ASSERT_FALSE FAIL]'});return false;}return true;};\n" +
            "  e.sleep=function(ms){return null;};\n" +
            "  e.uuid=function(){return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g,function(c){var r=Math.random()*16|0,v=c=='x'?r:(r&0x3|0x8);return v.toString(16);});};\n" +
            "  e.hash=function(s){var h=5381;for(var i=0;i<s.length;i++)h=((h<<5)+h)+s.charCodeAt(i);return h&0x7fffffff;};\n" +
            "  e.type_of=function(x){return e.typeof(x);};\n" +
            "  e.copy=function(x){if(Array.isArray(x))return x.slice();if(x&&typeof x==='object')return Object.assign({},x);return x;};\n" +
            "  e.map=function(fn,arr){if(!Array.isArray(arr))return[];return arr.map(fn);};\n" +
            "  e.filter=function(fn,arr){if(!Array.isArray(arr))return[];return arr.filter(fn);};\n" +
            "  e.reduce=function(fn,init,arr){if(!Array.isArray(arr))return init;return arr.reduce(fn,init);};\n" +
            "  e.find=function(fn,arr){if(!Array.isArray(arr))return null;return arr.find(fn)||null;};\n" +
            "  e.file_read=function(key){try{return localStorage.getItem(key)||'';}catch(e){return'';}};\n" +
            "  e.file_write=function(key,val){try{localStorage.setItem(key,String(val));return true;}catch(e){return false;}};\n" +
            "  e.file_append=function(key,val){try{var cur=localStorage.getItem(key)||'';localStorage.setItem(key,cur+String(val));return true;}catch(e){return false;}};\n" +
            "  e.file_exists=function(key){try{return localStorage.getItem(key)!==null;}catch(e){return false;}};\n" +
            "  e.watch=function(k,fn){self._watchers[k]=fn;};\n" +
            "  e.store=function(){return self._store;};\n" +
            "  e.exports=function(){return self._exports;};\n" +
            "  e.youtube=function(id){var vid=String(id),m2=vid.match(/v=([^&]+)/)||vid.match(/youtu\\.be\\/([^?]+)/);if(m2)vid=m2[1];self.out.push({t:'html',v:'<div style=\"position:relative;padding-bottom:56.25%;height:0;overflow:hidden;border-radius:12px;margin:.8rem 0\"><iframe style=\"position:absolute;top:0;left:0;width:100%;height:100%\" src=\"https://www.youtube.com/embed/'+vid+'?rel=0\" frameborder=\"0\" allowfullscreen loading=\"lazy\"></iframe></div>'});return null;};\n" +
            "  e.facebook=function(url,txt){self.out.push({t:'html',v:'<a href=\"'+url+'\" target=\"_blank\" style=\"display:inline-flex;align-items:center;gap:.4rem;background:#1877f2;color:white;padding:.5rem 1rem;border-radius:8px;text-decoration:none;font-weight:700;margin:.2rem\">'+(txt||'Facebook')+'</a>'});return null;};\n" +
            "  e.instagram=function(handle,txt){var href=String(handle).startsWith('http')?handle:'https://instagram.com/'+(handle.startsWith('@')?handle.slice(1):handle);self.out.push({t:'html',v:'<a href=\"'+href+'\" target=\"_blank\" style=\"display:inline-flex;align-items:center;gap:.4rem;background:linear-gradient(45deg,#f09433,#e6683c,#dc2743,#cc2366,#bc1888);color:white;padding:.5rem 1rem;border-radius:8px;text-decoration:none;font-weight:700;margin:.2rem\">'+(txt||handle)+'</a>'});return null;};\n" +
            "  e.xsocial=function(handle,txt){var href=String(handle).startsWith('http')?handle:'https://x.com/'+(handle.startsWith('@')?handle.slice(1):handle);self.out.push({t:'html',v:'<a href=\"'+href+'\" target=\"_blank\" style=\"display:inline-flex;align-items:center;gap:.4rem;background:#000;color:white;border:1px solid #333;padding:.5rem 1rem;border-radius:8px;text-decoration:none;font-weight:700;margin:.2rem\">'+(txt||handle)+'</a>'});return null;};\n" +
            "  e.url=function(href,txt,target){self.out.push({t:'html',v:'<a href=\"'+href+'\" target=\"'+(target||'_blank')+'\" style=\"color:#4dc95a;text-decoration:underline;font-weight:600;display:inline-block;margin:.2rem\">'+(txt||href)+'</a>'});return null;};\n" +
            "  e.btn=function(lbl,act){self.out.push({t:'html',v:'<button onclick=\"'+(act||'')+'\">'+(lbl)+'</button>'});return null;};\n" +
            "  e.key=function(k){self.out.push({t:'html',v:'<kbd>'+k+'</kbd>'});return null;};\n" +
            "  e.video=function(src,w,h){self.out.push({t:'html',v:'<video controls style=\"width:'+(w||'100%')+';border-radius:10px;max-width:100%;margin:.4rem 0\"><source src=\"'+src+'\"></video>'});return null;};\n" +
            "  e.img=function(src,alt,ww){self.out.push({t:'html',v:'<img src=\"'+src+'\" alt=\"'+(alt||'')+'\" style=\"width:'+(ww||'auto')+';border-radius:8px;max-width:100%;margin:.4rem 0;display:block\" loading=\"lazy\">'});return null;};\n" +
            "  e.h=function(lv,txt){var lvl=Math.min(6,Math.max(1,lv|0));self.out.push({t:'html',v:'<h'+lvl+'>'+(txt||'')+'</h'+lvl+'>'});return null;};\n" +
            "  e.l=function(){var items=Array.prototype.slice.call(arguments);var li=items.map(function(it){return '<li>'+it+'</li>';}).join('');self.out.push({t:'html',v:'<ul>'+li+'</ul>'});return null;};\n" +
            "};\n" +
            "\n" +
            "VCG.prototype.run=function(){while(this.cur().t!='EOF'&&!this._hasRet){this.parseStmt(this.env);}};\n" +
            "VCG.prototype.parseBlock=function(e){this.eat('{');while(this.cur().v!='}'&&this.cur().t!='EOF'){this.parseStmt(e);if(this._hasRet||this._brk||this._cnt)break;}this.eat('}');};\n" +
            "VCG.prototype._skipBlock=function(){this.eat('{');var d=1;while(d>0&&this.cur().t!='EOF'){if(this.cur().v=='{')d++;else if(this.cur().v=='}')d--;if(d>0)this.eat();else break;}this.eat('}');};\n" +
            "VCG.prototype._skipStmt=function(){var t=this.cur();if(t.v=='if'){this.eat();this.parseExpr(this.env);this._skipBlock();if(this.cur().v=='else'){this.eat();if(this.cur().v=='if')this._skipStmt();else this._skipBlock();}}else{while(this.cur().t!='NL'&&this.cur().v!='}'&&this.cur().t!='EOF')this.eat();}};\n" +
            "\n" +
            "VCG.prototype.parseExpr=function(e,minp){\n" +
            "  minp=minp||0;var left=this.parsePrimary(e);\n" +
            "  var prec={'||':1,'or':1,'&&':2,'and':2,'==':3,'!=':3,'<':4,'>':4,'<=':4,'>=':4,'+':5,'-':5,'*':6,'/':6,'%':6,'**':7,'|>':8,'..':8,'|':9,'&':9,'<<':10,'>>':10};\n" +
            "  while(true){var op=this.cur().v,p=prec[op]||0;if(p<=minp)break;this.eat();\n" +
            "    if(op=='|>'){var right=this.parseExpr(e,p-1);left=right(left);}\n" +
            "    else left=this._bin(op,left,this.parseExpr(e,op=='**'?p-1:p));\n" +
            "  }\n" +
            "  if(this.cur().v=='?'){this.eat();var th=this.parseExpr(e);this.eat(':');return left?th:this.parseExpr(e);}\n" +
            "  return left;\n" +
            "};\n" +
            "\n" +
            "VCG.prototype._bin=function(op,a,b){\n" +
            "  if(op=='+'&&(typeof a==='string'||typeof b==='string'))return String(a===null?'nil':a)+String(b===null?'nil':b);\n" +
            "  if(op=='+'&&Array.isArray(a)&&Array.isArray(b))return a.concat(b);\n" +
            "  if(op=='..')return this.env.range(a,b);\n" +
            "  switch(op){case '+':return a+b;case '-':return a-b;case '*':return a*b;\n" +
            "  case '/':if(!b)throw new Error('Division by zero');return a/b;\n" +
            "  case '%':return a%b;case '**':return Math.pow(a,b);\n" +
            "  case '==':return a==b;case '!=':return a!=b;\n" +
            "  case '<':return a<b;case '>':return a>b;case '<=':return a<=b;case '>=':return a>=b;\n" +
            "  case 'and':return !!a&&!!b;case 'or':return !!a||!!b;\n" +
            "  case '|':return(a|0)|(b|0);case '&':return(a|0)&(b|0);\n" +
            "  case '<<':return(a|0)<<(b|0);case '>>':return(a|0)>>(b|0);}\n" +
            "  return null;\n" +
            "};\n" +
            "\n" +
            "VCG.prototype.parsePrimary=function(e){\n" +
            "  var t=this.cur();\n" +
            "  if(t.t=='NUM'){this.eat();return t.v;}\n" +
            "  if(t.t=='STR'){this.eat();return t.v;}\n" +
            "  if(t.v=='true'){this.eat();return true;}if(t.v=='false'){this.eat();return false;}\n" +
            "  if(t.v=='nil'||t.v=='null'){this.eat();return null;}\n" +
            "  if(t.v=='not'||t.v=='!'){this.eat();return !this.parsePrimary(e);}\n" +
            "  if(t.v=='-'){this.eat();return -this.parsePrimary(e);}\n" +
            "  if(t.v=='('){this.eat();var v=this.parseExpr(e);this.eat(')');return v;}\n" +
            "  if(t.v=='['){this.eat();var a=[];while(this.cur().v!=']'&&this.cur().t!='EOF'){a.push(this.parseExpr(e));this.eatIf(',');}this.eat(']');return a;}\n" +
            "  if(t.v=='{'){this.eat();var o=Object.create(null);while(this.cur().v!='}'&&this.cur().t!='EOF'){var k=this.eat().v;this.eat(':');o[k]=this.parseExpr(e);this.eatIf(',');}this.eat('}');return o;}\n" +
            "  if(t.t=='LAMBDA'){this.eat();var params=[];while(this.cur().t=='ID'&&this.cur().v!='->'){params.push(this.eat().v);}this.eatIf('->');var self=this;return function(){var args=arguments;var fe=self._scope(e);params.forEach(function(p,i){self._def(fe,p,i<args.length?args[i]:null);});return self.parseExpr(fe);};}\n" +
            "  if(t.v=='|>'){this.eat();return this.parseExpr(e);}\n" +
            "  if(t.v=='$set'){this.eat();this.eat('(');var k=this.parseExpr(e);this.eat(',');var v=this.parseExpr(e);this.eat(')');this._store[k]=v;if(this._watchers[k])this._watchers[k](v);return v;}\n" +
            "  if(t.v=='$get'){this.eat();this.eat('(');var k=this.parseExpr(e);this.eat(')');return this._store[k]!==undefined?this._store[k]:null;}\n" +
            "  if(t.v=='typeof'){this.eat();this.eat('(');var v=this.parseExpr(e);this.eat(')');return this.env.typeof(v);}\n" +
            "  if(t.v=='sizeof'){this.eat();this.eat('(');var v=this.parseExpr(e);this.eat(')');return this.env.sizeof(v);}\n" +
            "  if(t.v=='new'){this.eat();var nm=this.eat().v;this.eat('(');var args=[];while(this.cur().v!=')'&&this.cur().t!='EOF'){args.push(this.parseExpr(e));this.eatIf(',');}this.eat(')');var cls=this._classProtos[nm];if(cls&&typeof cls=='function'){var obj=Object.create(cls.prototype||{});if(typeof obj.init=='function')obj.init.apply(obj,args);return obj;}throw new Error('Unknown class: '+nm);}\n" +
            "  if(t.t=='ID'||t.t=='KW'){var nm=t.v;this.eat();var val=null;try{val=this._get(e,nm);}catch(x){}return this._postfix(e,val,nm);}\n" +
            "  this.eat();return null;\n" +
            "};\n" +
            "\n" +
            "VCG.prototype._postfix=function(e,val,nm){\n" +
            "  while(true){\n" +
            "    if(this.cur().v=='('){this.eat();var args=[];while(this.cur().v!=')'&&this.cur().t!='EOF'){args.push(this.parseExpr(e));this.eatIf(',');}this.eat(')');if(typeof val=='function')val=val.apply(null,args);else throw new Error('Not callable: '+nm);}\n" +
            "    else if(this.cur().v=='['){this.eat();var idx=this.parseExpr(e);this.eat(']');val=Array.isArray(val)?(idx<0?val[val.length+idx]:val[idx]):val&&typeof val=='object'?val[idx]:null;}\n" +
            "    else if(this.cur().v=='.'){this.eat();var f=this.eat().v;if(this.cur().v=='('){this.eat();var args=[];while(this.cur().v!=')'&&this.cur().t!='EOF'){args.push(this.parseExpr(e));this.eatIf(',');}this.eat(')');val=this._method(val,f,args);}else val=val&&typeof val=='object'?val[f]:null;}\n" +
            "    else break;\n" +
            "  }\n" +
            "  return val;\n" +
            "};\n" +
            "\n" +
            "VCG.prototype._method=function(obj,name,args){\n" +
            "  if(Array.isArray(obj)){if(name=='push'){obj.push.apply(obj,args);return obj.length;}if(name=='pop')return obj.pop();if(name=='len'||name=='length')return obj.length;if(name=='join')return obj.join(args[0]||',');if(name=='contains')return obj.indexOf(args[0])>=0;if(name=='reverse'){obj.reverse();return obj;}if(name=='slice')return obj.slice(args[0]||0,args[1]);if(name=='sort'){obj.sort(function(a,b){return a-b;});return obj;}if(name=='flat')return obj.flat();if(name=='unique')return obj.filter(function(v,i,s){return s.indexOf(v)===i;});if(name=='first')return obj[0]||null;if(name=='last')return obj[obj.length-1]||null;}\n" +
            "  if(typeof obj=='string'){if(name=='len'||name=='length')return obj.length;if(name=='upper')return obj.toUpperCase();if(name=='lower')return obj.toLowerCase();if(name=='trim')return obj.trim();if(name=='split')return obj.split(args[0]!==undefined?args[0]:' ');if(name=='contains')return obj.indexOf(String(args[0]))>=0;if(name=='startswith')return obj.indexOf(String(args[0]))===0;if(name=='endswith')return obj.endsWith(String(args[0]));if(name=='replace')return obj.split(String(args[0])).join(String(args[1]));if(name=='tonum')return parseFloat(obj);if(name=='indexof')return obj.indexOf(String(args[0]));}\n" +
            "  if(obj&&typeof obj=='object'&&typeof obj[name]=='function')return obj[name].apply(obj,args);\n" +
            "  return null;\n" +
            "};\n" +
            "\n" +
            "VCG.prototype.parseStmt=function(e){\n" +
            "  var t=this.cur(),self=this;\n" +
            "  var UI=['youtube','facebook','instagram','xsocial','url','btn','key','video','img','h','l'];\n" +
            "  if(UI.indexOf(t.v)>=0){var nm=t.v;this.eat();if(this.cur().v=='('){this.eat();var args=[];while(this.cur().v!=')'&&this.cur().t!='EOF'){args.push(this.parseExpr(e));this.eatIf(',');}this.eat(')');if(typeof this.env[nm]=='function')this.env[nm].apply(null,args);}return;}\n" +
            "  if(t.v=='$set'){this.eat();this.eat('(');var k=this.parseExpr(e);this.eat(',');var v=this.parseExpr(e);this.eat(')');this._store[k]=v;if(this._watchers[k])this._watchers[k](v);return;}\n" +
            "  if(t.v=='$get'){this.eat();this.eat('(');this.parseExpr(e);this.eat(')');return;}\n" +
            "  if(t.v=='public'){this.eat();var s=this.parseStmt(e);var exports=this._exports;return;}\n" +
            "  if(t.v=='w'){this.eat();var nm=this.eat().v;if(this.cur().v=='='){this.eat();var v=this.parseExpr(e);this._def(e,nm,v);this._writeonly[nm]=true;this.out.push({t:'txt',v:'[w] '+nm+' <- '+this._str(v)});}return;}\n" +
            "  if(t.v=='c'){this.eat();if(this.cur().t=='ID'){var nm=this.eat().v;var init=null;if(this.cur().v=='='){this.eat();init=this.parseExpr(e);}var ch={_buf:[],_name:nm};if(init!==null)ch._buf.push(init);this._def(e,nm,ch);}return;}\n" +
            "  if(t.v=='watch'){this.eat();this.eat('(');var k=this.parseExpr(e);this.eat(',');var fn=this.parseExpr(e);this.eat(')');this._watchers[k]=fn;return;}\n" +
            "  if(t.v=='let'||t.v=='const'){this.eat();var nm=this.eat().v;if(this.cur().v=='='){this.eat();this._def(e,nm,this.parseExpr(e));}else this._def(e,nm,null);return;}\n" +
            "  if(t.v=='func'){this.eat();var nm=this.eat().v;this.eat('(');var params=[];while(this.cur().v!=')'&&this.cur().t!='EOF'){params.push(this.eat().v);this.eatIf(',');}this.eat(')');var bp=this.pos;this._skipBlock();var be=this.pos;var cl=e;this._def(e,nm,function(){var args=Array.prototype.slice.call(arguments);var fe=self._scope(cl);params.forEach(function(p,i){self._def(fe,p,i<args.length?args[i]:null);});var sp=self.pos;self.pos=bp;self.parseBlock(fe);self.pos=sp;var r=self._ret;self._ret=undefined;self._hasRet=false;return r!==undefined?r:null;});return;}\n" +
            "  if(t.v=='async'){this.eat();if(this.cur().v=='func'){this.eat();var nm=this.eat().v;this.eat('(');var params=[];while(this.cur().v!=')'&&this.cur().t!='EOF'){params.push(this.eat().v);this.eatIf(',');}this.eat(')');var bp=this.pos;this._skipBlock();var be=this.pos;var cl=e;this._def(e,nm,function(){var args=Array.prototype.slice.call(arguments);return new Promise(function(resolve){var fe=self._scope(cl);params.forEach(function(p,i){self._def(fe,p,i<args.length?args[i]:null);});var sp=self.pos;self.pos=bp;self.parseBlock(fe);self.pos=sp;var r=self._ret;self._ret=undefined;self._hasRet=false;resolve(r!==undefined?r:null);});});}return;}\n" +
            "  if(t.v=='class'){this.eat();var nm=this.eat().v;var base=null;if(this.cur().v=='extends'){this.eat();base=this.eat().v;}this.eat('{');var proto=base?Object.create(this._tryGet(e,base)||{}):{};var ce=this._scope(e);this._def(ce,'self',proto);while(this.cur().v!='}'&&this.cur().t!='EOF'){if(this.cur().v=='func'){this.eat();var mn=this.eat().v;this.eat('(');var mps=[];while(this.cur().v!=')'&&this.cur().t!='EOF'){mps.push(this.eat().v);this.eatIf(',');}this.eat(')');var mbp=this.pos;this._skipBlock();var mbe=this.pos;proto[mn]=function(){var args=Array.prototype.slice.call(arguments);var mfe=self._scope(ce);mps.forEach(function(p,i){self._def(mfe,p,i<args.length?args[i]:null);});self._def(mfe,'self',this);var sp=self.pos;self.pos=mbp;self.parseBlock(mfe);self.pos=sp;var r=self._ret;self._ret=undefined;self._hasRet=false;return r!==undefined?r:null;};}else{this.eat();}}this.eat('}');this._classProtos[nm]=proto;this._def(e,nm,function(){var obj=Object.create(proto);if(typeof obj.init=='function'){var args=Array.prototype.slice.call(arguments);obj.init.apply(obj,args);}return obj;});return;}\n" +
            "  if(t.v=='module'){this.eat();var nm=this.eat().v;var me=this._scope(e);this.parseBlock(me);this._modules[nm]=me;this._def(e,nm,me);return;}\n" +
            "  if(t.v=='export'){this.eat();this.parseStmt(e);return;}\n" +
            "  if(t.v=='from'){this.eat();var mod=this.eat().v;this.eat('import');while(this.cur().t=='ID'){var inm=this.eat().v;var modObj=this._modules[mod];if(modObj&&modObj[inm]!==undefined)this._def(e,inm,modObj[inm]);this.eatIf(',');}return;}\n" +
            "  if(t.v=='import'){this.eat();var mod=this.eat().v;var alias=mod;if(this.cur().v=='as'){this.eat();alias=this.eat().v;}var modObj=this._modules[mod];if(modObj)this._def(e,alias,modObj);return;}\n" +
            "  if(t.v=='enum'){this.eat();var nm=this.eat().v;this.eat('{');var vals={};var idx=0;while(this.cur().v!='}'&&this.cur().t!='EOF'){var en=this.eat().v;vals[en]=idx++;this.eatIf(',');}this.eat('}');this._def(e,nm,vals);return;}\n" +
            "  if(t.v=='type'){this.eat();var nm=this.eat().v;this.eat('=');var val=this.parseExpr(e);this._def(e,nm,val);return;}\n" +
            "  if(t.v=='struct'){this.eat();var nm=this.eat().v;this.eat('{');var fields=[];while(this.cur().v!='}'&&this.cur().t!='EOF'){fields.push(this.eat().v);this.eatIf(',');}this.eat('}');var proto=Object.create(null);proto.__type__=nm;fields.forEach(function(f){proto[f]=null;});this._def(e,nm,proto);return;}\n" +
            "  if(t.v=='if'){this.eat();var cond=this.parseExpr(e);var bp=this.pos;this._skipBlock();var be=this.pos;if(cond){this.pos=bp;this.parseBlock(this._scope(e));this.pos=be;}else this.pos=be;if(this.cur().v=='else'){this.eat();if(this.cur().v=='if'){if(cond)this._skipStmt();else this.parseStmt(this._scope(e));}else{if(cond)this._skipBlock();else this.parseBlock(this._scope(e));}}return;}\n" +
            "  if(t.v=='while'){this.eat();var cp=this.pos;while(true){this.pos=cp;var cond=this.parseExpr(e);if(!cond){this._skipBlock();break;}this.parseBlock(this._scope(e));if(this._brk){this._brk=false;break;}if(this._cnt)this._cnt=false;if(this._hasRet)break;}return;}\n" +
            "  if(t.v=='repeat'){this.eat();var cnt=Math.floor(this.parseExpr(e));var bp=this.pos;this._skipBlock();var be=this.pos;for(var ri=0;ri<cnt&&!this._hasRet;ri++){this.pos=bp;this.parseBlock(this._scope(e));if(this._brk){this._brk=false;break;}if(this._cnt)this._cnt=false;}this.pos=be;return;}\n" +
            "  if(t.v=='for'){this.eat();var nm=this.eat().v;this.eat('in');var iter=this.parseExpr(e);var bp=this.pos;this._skipBlock();var be=this.pos;var arr=Array.isArray(iter)?iter:typeof iter=='string'?iter.split(''):iter&&typeof iter=='object'&&iter._buf?iter._buf:iter&&typeof iter=='object'?Object.values(iter):[];for(var fi=0;fi<arr.length;fi++){if(this._hasRet)break;this.pos=bp;var le=this._scope(e);this._def(le,nm,arr[fi]);this.parseBlock(le);if(this._brk){this._brk=false;break;}if(this._cnt)this._cnt=false;}this.pos=be;return;}\n" +
            "  if(t.v=='return'){this.eat();this._ret=(this.cur().v!='}'&&this.cur().t!='EOF')?this.parseExpr(e):null;this._hasRet=true;return;}\n" +
            "  if(t.v=='break'){this.eat();this._brk=true;return;}\n" +
            "  if(t.v=='continue'){this.eat();this._cnt=true;return;}\n" +
            "  if(t.v=='show'||t.v=='print'){this.eat();this.eat('(');var parts=[];while(this.cur().v!=')'&&this.cur().t!='EOF'){parts.push(this.parseExpr(e));this.eatIf(',');}this.eat(')');this.out.push({t:'txt',v:parts.map(function(v){return self._str(v);}).join(' ')});return;}\n" +
            "  if(t.v=='html'){this.eat();var v=this.parseExpr(e);this.out.push({t:'html',v:String(v)});return;}\n" +
            "  if(t.v=='assert'){this.eat();this.eat('(');var c=this.parseExpr(e);var msg='Assertion failed';if(this.cur().v==','){this.eat();msg=String(this.parseExpr(e));}this.eat(')');if(!c)throw new Error(msg);return;}\n" +
            "  if(t.v=='throw'){this.eat();var v=this.parseExpr(e);throw new Error(String(v));}\n" +
            "  if(t.v=='try'){this.eat();var bp=this.pos;this._skipBlock();var be=this.pos;this.eat('catch');var nm2=null;if(this.cur().t=='ID')nm2=this.eat().v;var cp=this.pos;this._skipBlock();var ce2=this.pos;var err=null;try{this.pos=bp;this.parseBlock(this._scope(e));}catch(ex){err=ex;}if(err){this.pos=cp;var cv=this._scope(e);if(nm2)this._def(cv,nm2,err.message);this.parseBlock(cv);}this.pos=ce2;return;}\n" +
            "  if(t.v=='match'){this.eat();var val=this.parseExpr(e);this.eat('{');while(this.cur().v=='when'&&!this._hasRet){this.eat();var arm=this.parseExpr(e);if(this.cur().v=='->'||this.cur().v==':')this.eat();if(val==arm)this.parseStmt(e);else this._skipStmt();}this.eat('}');return;}\n" +
            "  if(t. **...**

_This response is too long to display in full._
