// Hypercube — The Book of Shaders compatible
// Gustavo A.F (Lowingx) — 2026-09-03

#ifdef GL_ES
precision mediump float;
#endif

uniform float u_time;
uniform vec2 u_resolution;

mat4 rotXW(float a) {
    float c=cos(a), s=sin(a);
    return mat4(c,0,0,-s, 0,1,0,0, 0,0,1,0, s,0,0,c);
}
mat4 rotYW(float a) {
    float c=cos(a), s=sin(a);
    return mat4(1,0,0,0, 0,c,0,-s, 0,0,1,0, 0,s,0,c);
}
mat4 rotZW(float a) {
    float c=cos(a), s=sin(a);
    return mat4(1,0,0,0, 0,1,0,0, 0,0,c,-s, 0,0,s,c);
}
mat4 rotXY(float a) {
    float c=cos(a), s=sin(a);
    return mat4(c,-s,0,0, s,c,0,0, 0,0,1,0, 0,0,0,1);
}

vec3 p43(vec4 p) {
    float w = 3.0/(3.0+p.w);
    return p.xyz*w;
}
vec3 p32(vec3 p) {
    float w = 4.0/(4.0+p.z);
    return vec3(p.xy*w, w);
}

float seg(vec2 p, vec2 a, vec2 b) {
    vec2 pa=p-a, ba=b-a;
    float h=clamp(dot(pa,ba)/dot(ba,ba),0.0,1.0);
    return length(pa-ba*h);
}

vec3 palette(float t) {
    return vec3(0.5)+vec3(0.5)*cos(6.28318*(vec3(t)+vec3(0.0,0.1,0.2)));
}

vec3 edgeLine(vec2 uv, vec2 a, vec2 b, float depth, float hue) {
    float d = seg(uv, a, b);
    float br = smoothstep(2.0,-2.0,depth);
    vec3 ec = palette(hue)*(0.5+0.5*br);
    vec3 col = vec3(0.0);
    col += smoothstep(0.004,0.001,d)*ec*2.0;
    col += exp(-d*80.0)*br*ec*0.8;
    col += exp(-d*15.0)*br*ec*0.3;
    col += exp(-d*4.0)*br*ec*0.08;
    return col;
}

vec3 vertPoint(vec2 uv, vec2 pos, float depth, float hue) {
    float d = length(uv-pos);
    float br = smoothstep(2.0,-2.0,depth);
    vec3 vc = palette(hue);
    vec3 col = vec3(0.0);
    col += exp(-d*200.0)*br*vc*3.0;
    col += exp(-d*30.0)*br*vc*0.5;
    col += exp(-d*8.0)*br*vc*0.1;
    return col;
}

void main() {
    vec2 uv = (gl_FragCoord.xy-0.5*u_resolution)/u_resolution.y;
    float t = u_time*0.15;
    float sc = 0.38;
    mat4 r = rotXW(t*0.7)*rotYW(t*0.5)*rotZW(t*0.3)*rotXY(t*0.4);

    vec4 V[16];
    V[0]=r*vec4(-1,-1,-1,-1)*sc; V[1]=r*vec4(-1,-1,-1,1)*sc;
    V[2]=r*vec4(-1,-1,1,-1)*sc;  V[3]=r*vec4(-1,-1,1,1)*sc;
    V[4]=r*vec4(-1,1,-1,-1)*sc;  V[5]=r*vec4(-1,1,-1,1)*sc;
    V[6]=r*vec4(-1,1,1,-1)*sc;   V[7]=r*vec4(-1,1,1,1)*sc;
    V[8]=r*vec4(1,-1,-1,-1)*sc;  V[9]=r*vec4(1,-1,-1,1)*sc;
    V[10]=r*vec4(1,-1,1,-1)*sc;  V[11]=r*vec4(1,-1,1,1)*sc;
    V[12]=r*vec4(1,1,-1,-1)*sc;  V[13]=r*vec4(1,1,-1,1)*sc;
    V[14]=r*vec4(1,1,1,-1)*sc;   V[15]=r*vec4(1,1,1,1)*sc;

    vec2 P[16];
    float D[16];
    for(int i=0;i<16;i++){
        P[i]=p32(p43(V[i])).xy;
        D[i]=V[i].z;
    }

    vec3 col = vec3(0.01);

    // Edges: 32 edges of hypercube
    // Adjacent differ by exactly 1 bit in 4-bit index
    int idx[32*2];
    idx[0]=0;idx[1]=1;idx[2]=0;idx[3]=2;idx[4]=0;idx[5]=4;idx[6]=0;idx[7]=8;
    idx[8]=1;idx[9]=3;idx[10]=1;idx[11]=5;idx[12]=1;idx[13]=9;
    idx[14]=2;idx[15]=3;idx[16]=2;idx[17]=6;idx[18]=2;idx[19]=10;
    idx[20]=3;idx[21]=7;idx[22]=3;idx[23]=11;
    idx[24]=4;idx[25]=5;idx[26]=4;idx[27]=6;idx[28]=4;idx[29]=12;
    idx[30]=5;idx[31]=7;idx[32]=5;idx[33]=13;
    idx[34]=6;idx[35]=7;idx[36]=6;idx[37]=14;
    idx[38]=7;idx[39]=15;
    idx[40]=8;idx[41]=9;idx[42]=8;idx[43]=10;idx[44]=8;idx[45]=12;
    idx[46]=9;idx[47]=11;idx[48]=9;idx[49]=13;
    idx[50]=10;idx[51]=11;idx[52]=10;idx[53]=14;
    idx[54]=11;idx[55]=15;
    idx[56]=12;idx[57]=13;idx[58]=12;idx[59]=14;
    idx[60]=13;idx[61]=15;
    idx[62]=14;idx[63]=15;

    for(int i=0;i<32;i++){
        int a=idx[i*2];
        int b=idx[i*2+1];
        col += edgeLine(uv, P[a], P[b], (D[a]+D[b])*0.5, float(i)/32.0+t*0.3);
    }

    for(int i=0;i<16;i++){
        col += vertPoint(uv, P[i], D[i], float(i)/16.0+t*0.5);
    }

    col *= 1.0-0.3*dot(uv,uv);
    col = col/(1.0+col);
    col = pow(col, vec3(0.9));

    gl_FragColor = vec4(col,1.0);
}
