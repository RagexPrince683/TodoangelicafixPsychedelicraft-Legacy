#version 130
uniform sampler2D scene;
uniform vec2 texel;
uniform float strength;
uniform float distance;
in vec2 sceneUV;
out vec4 postColor;
vec4 at(vec2 uv) { return texture(scene, clamp(uv, texel * 0.5, 1.0 - texel * 0.5)); }
void main() {
    vec4 original = at(sceneUV);
    vec2 center = vec2(0.5 + (sceneUV.x - 0.5) / (1.0 + strength), sceneUV.y);
    vec4 doubled = original * 0.35 + (at(center + vec2(distance, 0.0)) + at(center - vec2(distance, 0.0))) * 0.325;
    postColor = mix(original, doubled, strength);
}
