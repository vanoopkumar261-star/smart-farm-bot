/* ============================================================================
   glowing-ridges.js — native WebGL animated background (zero dependencies).
   Recreates the "iridescent ridges of light rolling like folded silk" look for
   the Smart Farm Bot auth pages, tuned to the emerald telemetry palette.
   Mounts on <canvas id="ridges-bg">. Degrades to an animated CSS gradient if
   WebGL is unavailable, and respects prefers-reduced-motion + tab visibility.
   Touches no API, no app state — pure decoration.
   ============================================================================ */
(function () {
  'use strict';
  var canvas = document.getElementById('ridges-bg');
  if (!canvas) return;

  var reduce = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  // ---- CSS fallback (no WebGL / context failure) --------------------------
  function cssFallback() {
    var css = document.createElement('style');
    css.textContent =
      '@keyframes sfbRidgeDrift{0%{background-position:0% 50%,0% 0%}100%{background-position:100% 50%,0% 0%}}' +
      '#ridges-bg{background:' +
      'linear-gradient(115deg,#07140e 0%,#0a2a1c 28%,#0c3b32 50%,#0a2538 72%,#07120e 100%),' +
      'radial-gradient(1200px 700px at 70% 20%,rgba(92,227,148,.18),transparent 60%);' +
      'background-size:300% 300%,100% 100%;animation:' +
      (reduce ? 'none' : 'sfbRidgeDrift 24s ease-in-out infinite alternate') + ';}';
    document.head.appendChild(css);
  }

  var gl;
  try {
    gl = canvas.getContext('webgl', { antialias: true, alpha: false, premultipliedAlpha: false }) ||
         canvas.getContext('experimental-webgl', { antialias: true, alpha: false });
  } catch (e) { gl = null; }
  if (!gl) { cssFallback(); return; }

  var VERT =
    'attribute vec2 p;void main(){gl_Position=vec4(p,0.0,1.0);}';

  var FRAG = [
    'precision highp float;',
    'uniform vec2 u_res; uniform float u_time;',
    // IQ cosine palette, biased green -> teal -> cyan
    'vec3 pal(float t){',
    '  vec3 a=vec3(0.17,0.40,0.33); vec3 b=vec3(0.24,0.46,0.40);',
    '  vec3 c=vec3(1.0,1.0,1.0);    vec3 d=vec3(0.00,0.18,0.34);',
    '  return a+b*cos(6.28318*(c*t+d));',
    '}',
    // folded-silk ridge field (domain-warped fbm of sines)
    'float field(vec2 q,float t){',
    '  q+=0.35*vec2(sin(q.y*1.3+t*0.6),cos(q.x*1.1-t*0.5));',
    '  float v=0.0,amp=1.0;',
    '  for(int i=0;i<4;i++){',
    '    v+=amp*sin(q.x*1.6+q.y*0.7+t);',
    '    q=q*1.9+vec2(1.7,-1.3); amp*=0.55;',
    '  } return v;',
    '}',
    'void main(){',
    '  vec2 uv=gl_FragCoord.xy/u_res.xy;',
    '  vec2 p=(gl_FragCoord.xy-0.5*u_res.xy)/u_res.y; p*=3.2;',
    '  float t=u_time*0.22;',
    '  float f=field(p,t);',
    '  float fold=abs(sin(f*1.5708));',                 // creases
    '  float fx=field(p+vec2(0.01,0.0),t)-f;',
    '  float fy=field(p+vec2(0.0,0.01),t)-f;',
    '  vec3 n=normalize(vec3(-fx,-fy,0.08));',
    '  vec3 L=normalize(vec3(0.5,0.7,0.9));',
    '  float spec=pow(max(dot(n,L),0.0),8.0);',         // silky highlight
    '  float hue=0.52+0.18*f+0.25*fold+0.06*sin(t*0.5);',
    '  vec3 col=pal(hue);',
    '  col*=(0.26+0.95*fold);',
    '  col+=spec*vec3(0.6,1.0,0.85)*1.15;',
    '  float vig=smoothstep(1.3,0.2,length(uv-0.5));',
    '  col*=mix(0.32,1.0,vig);',
    '  col=mix(vec3(0.015,0.035,0.028),col,0.92);',     // sink into near-black
    '  col=pow(max(col,0.0),vec3(0.92));',
    '  gl_FragColor=vec4(col,1.0);',
    '}'
  ].join('\n');

  function compile(type, src) {
    var s = gl.createShader(type);
    gl.shaderSource(s, src); gl.compileShader(s);
    if (!gl.getShaderParameter(s, gl.COMPILE_STATUS)) {
      console.warn('[ridges] shader error:', gl.getShaderInfoLog(s));
      return null;
    }
    return s;
  }

  var vs = compile(gl.VERTEX_SHADER, VERT);
  var fs = compile(gl.FRAGMENT_SHADER, FRAG);
  if (!vs || !fs) { cssFallback(); return; }

  var prog = gl.createProgram();
  gl.attachShader(prog, vs); gl.attachShader(prog, fs); gl.linkProgram(prog);
  if (!gl.getProgramParameter(prog, gl.LINK_STATUS)) { cssFallback(); return; }
  gl.useProgram(prog);

  var buf = gl.createBuffer();
  gl.bindBuffer(gl.ARRAY_BUFFER, buf);
  gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1, -1, 3, -1, -1, 3]), gl.STATIC_DRAW);
  var loc = gl.getAttribLocation(prog, 'p');
  gl.enableVertexAttribArray(loc);
  gl.vertexAttribPointer(loc, 2, gl.FLOAT, false, 0, 0);

  var uRes = gl.getUniformLocation(prog, 'u_res');
  var uTime = gl.getUniformLocation(prog, 'u_time');

  function resize() {
    var dpr = Math.min(window.devicePixelRatio || 1, 1.75);
    var w = Math.max(1, Math.floor(innerWidth * dpr));
    var h = Math.max(1, Math.floor(innerHeight * dpr));
    if (canvas.width !== w || canvas.height !== h) {
      canvas.width = w; canvas.height = h;
      gl.viewport(0, 0, w, h);
    }
    gl.uniform2f(uRes, w, h);
  }
  window.addEventListener('resize', resize);
  resize();

  var start = performance.now();
  var raf = 0, running = true;

  function frame(now) {
    if (!running) return;
    gl.uniform1f(uTime, (now - start) / 1000);
    gl.drawArrays(gl.TRIANGLES, 0, 3);
    raf = requestAnimationFrame(frame);
  }

  if (reduce) {
    // single still frame — no animation
    gl.uniform1f(uTime, 6.0);
    gl.drawArrays(gl.TRIANGLES, 0, 3);
  } else {
    raf = requestAnimationFrame(frame);
    document.addEventListener('visibilitychange', function () {
      if (document.hidden) { running = false; cancelAnimationFrame(raf); }
      else if (!running) { running = true; start = performance.now() - 6000; raf = requestAnimationFrame(frame); }
    });
  }

  canvas.addEventListener('webglcontextlost', function (e) { e.preventDefault(); running = false; cancelAnimationFrame(raf); });
})();
