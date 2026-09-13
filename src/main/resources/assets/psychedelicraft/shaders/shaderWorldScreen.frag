#version 120

uniform sampler2D tex0;
uniform float ticks;
uniform vec2 pixelSize;
uniform float bigWaves;
uniform float smallWaves;
uniform float wiggleWaves;
uniform float surfaceFractal;
uniform float distantWorldDeformation;
uniform vec4 pulseColor;
uniform vec4 contrastColor;
uniform vec2 sceneTexelMin;
uniform vec2 sceneTexelMax;

void main()
{
    vec2 uv = gl_TexCoord[0].st;
    vec2 centered = uv - vec2(0.5);
    float radius = length(centered);
    vec2 offset = vec2(0.0);
    offset.x += sin(uv.y * 25.0 + ticks * 0.07) * wiggleWaves * 0.018;
    offset.y += sin(uv.x * 18.0 + ticks * 0.04) * smallWaves * 0.025;
    offset += centered * sin(radius * 45.0 - ticks * 0.05) * bigWaves * 0.10;
    offset.y += sin(radius * 55.0 + ticks * 0.03) * distantWorldDeformation * radius * 0.035;

    vec2 sampleUV = clamp(uv + offset, sceneTexelMin, sceneTexelMax);
    vec4 color = texture2D(tex0, sampleUV);
    float fractal = sin((uv.x + uv.y) * 180.0 + ticks * 0.08) * 0.5 + 0.5;
    color.rgb = mix(color.rgb, color.rgb * (0.72 + fractal * 0.56), clamp(surfaceFractal, 0.0, 1.0));
    color.rgb = mix(color.rgb, pulseColor.rgb, clamp(pulseColor.a, 0.0, 1.0) * 0.35);
    color.rgb = mix(vec3(0.5) + (color.rgb - vec3(0.5)) * contrastColor.rgb, color.rgb,
        1.0 - clamp(contrastColor.a, 0.0, 1.0));
    gl_FragColor = vec4(color.rgb, 1.0);
}
