#version 150
uniform mat4 InvProjection;
uniform mat4 ViewRotation;
uniform float NuxTime;
uniform int Effect;
uniform int Octaves;
uniform float Brightness;
uniform float Stars;
uniform float HorizonBlend;
uniform vec3 Tint;
in vec2 ndc;
out vec4 fragColor;
float hash(vec3 p){p=fract(p*.1031);p+=dot(p,p.yzx+33.33);return fract((p.x+p.y)*p.z);}
float noise(vec3 p){
 vec3 i=floor(p),f=fract(p);f=f*f*(3.-2.*f);
 return mix(mix(mix(hash(i),hash(i+vec3(1,0,0)),f.x),mix(hash(i+vec3(0,1,0)),hash(i+vec3(1,1,0)),f.x),f.y),mix(mix(hash(i+vec3(0,0,1)),hash(i+vec3(1,0,1)),f.x),mix(hash(i+vec3(0,1,1)),hash(i+vec3(1,1,1)),f.x),f.y),f.z);
}
float fbm(vec3 p){float a=.52,v=0.;for(int i=0;i<4;i++){if(i>=Octaves)break;v+=a*noise(p);p=p*2.03+vec3(5.7,9.2,1.4);a*=.5;}return v;}
void main(){
 vec4 view=InvProjection*vec4(ndc,1.,1.);
 vec3 d=normalize((ViewRotation*vec4(normalize(view.xyz/view.w),0.)).xyz);
 float t=NuxTime*.035;
 vec3 horizon=vec3(.302,.686,.643),zenith=vec3(.205,.105,.355),mist=vec3(.43,.48,.66);
 if(Effect==1){horizon=vec3(.396,.486,.678);zenith=vec3(.17,.065,.32);mist=vec3(.62,.31,.64);}
 if(Effect==2){horizon=vec3(.20,.216,.31);zenith=vec3(.022,.028,.075);mist=vec3(.29,.22,.49);}
 if(Effect==3){horizon=vec3(.918,.627,.490);zenith=vec3(.28,.18,.43);mist=vec3(.92,.55,.59);}
 if(Effect==4){horizon=vec3(.643,.725,.827);zenith=vec3(.20,.38,.61);mist=vec3(.88,.91,.96);}
 float elevation=clamp(d.y,0.,1.);
 vec3 base=mix(horizon,zenith,pow(elevation,.55));
 float n=fbm(d*3.4+vec3(t,0.,t*.4));
 float wisps=fbm(d*vec3(2.6,7.5,2.6)+vec3(t*.65,n*.45,t*.3));
 float cloud=smoothstep(.30,.69,wisps+n*.22);
 base=mix(base,mist,cloud*(Effect==4?.68:.37));
 if(Effect==0){
  float wave=sin(d.x*3.8+d.z*2.5+t+fbm(d*2.2)*3.2)*.5+.5;
  float curtain=pow(wave,3.)*smoothstep(.01,.24,elevation)*(1.-smoothstep(.6,1.,elevation));
  base+=vec3(.015,.16,.105)*curtain;
 }
 if(Effect==2){float belt=exp(-pow((d.y*.8+d.x*.3-.25)*4.,2.));base+=mist*belt*fbm(d*11.)*.48;}
 vec3 cell=d*210.,local=fract(cell)-.5;float rnd=hash(floor(cell));
 float star=pow(max(0.,1.-length(local)*3.),7.)*step(.994,rnd);
 base+=vec3(.82,.89,1.)*star*Stars*(Effect==2?2.4:.7)*smoothstep(.02,.25,elevation);
 float rim=(1.-smoothstep(.0,.20,elevation))*HorizonBlend;
 base=mix(base,horizon,rim*.9);
 base=mix(horizon,base,smoothstep(-.07,.025,d.y));
 base*=mix(vec3(1.),Tint,.55)*Brightness;
 fragColor=vec4(clamp(base,0.,1.),1.);
}
