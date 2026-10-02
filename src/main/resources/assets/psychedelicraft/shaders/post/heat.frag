#version 130

uniform sampler2D scene;
uniform sampler2D noise;
uniform vec2 texel;
uniform float strength;
uniform float ticks;
in vec2 sceneUV;
out vec4 postColor;

void main() {
    vec2 layerA = texture(noise, fract(sceneUV * 4.0 + ticks * vec2(0.324823048, 0.48913801))).rg;
    vec2 layerB = texture(noise, fract(sceneUV.yx * 4.0 + ticks * vec2(0.52890348, 0.6318212))).rg;
    vec2 offset = (layerA + layerB - 1.0) * strength;
    vec2 sampleUV = clamp(sceneUV + offset, texel * 0.5, vec2(1.0) - texel * 0.5);
    postColor = texture(scene, sampleUV);
}
