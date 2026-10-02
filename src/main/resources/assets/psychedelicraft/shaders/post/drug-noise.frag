#version 130
uniform sampler2D scene;
uniform vec2 texel;
uniform float strength;
uniform float ticks;
in vec2 sceneUV;
out vec4 postColor;
float random(vec2 p) { return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453); }
vec4 at(vec2 uv) { return texture(scene, clamp(uv, texel * 0.5, 1.0 - texel * 0.5)); }
void main() {
    float seed = floor(ticks * 50.0);
    vec2 uv = sceneUV + vec2(0.0, (random(vec2(seed, 0.0)) - 0.5) * strength * 0.04);
    vec4 color = at(uv);
    // A fixed sample budget replaces the legacy strength-dependent scan and per-frame Random.
    for (int i = 0; i < 8; i++) {
        float index = float(i);
        float offset = (random(vec2(seed, index + 1.0)) - 0.5) * strength * 80.0;
        if (random(floor(sceneUV / texel) + vec2(index, seed)) < clamp(strength * 0.08, 0.0, 1.0))
            color = mix(color, at(uv + vec2(0.0, offset * texel.y)), 1.0 / (offset * offset * 0.004 + 1.0));
    }
    postColor = color;
}
