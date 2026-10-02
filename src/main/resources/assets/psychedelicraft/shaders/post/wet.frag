#version 130

uniform sampler2D scene;
uniform sampler2D droplets;
uniform vec2 texel;
uniform float wetness;
uniform float ticks;
in vec2 sceneUV;
out vec4 postColor;

void main() {
    vec4 a = texture(droplets, fract(sceneUV + vec2(0.0, ticks * 0.005)));
    vec4 b = texture(droplets, fract(sceneUV + vec2(0.5, ticks * 0.007)));
    vec2 offset = clamp(a.rg + b.rg - 1.0, -1.0, 1.0);
    offset *= mix(vec2(1.0), abs(a.rg - 0.5) * 2.0, a.b);
    offset *= mix(vec2(1.0), abs(b.rg - 0.5) * 2.0, b.b);
    vec2 uv = clamp(sceneUV + offset * wetness * 0.2, texel * 0.5, vec2(1.0) - texel * 0.5);
    vec2 original = clamp(sceneUV, texel * 0.5, vec2(1.0) - texel * 0.5);
    postColor = mix(texture(scene, original), texture(scene, uv), wetness);
}
