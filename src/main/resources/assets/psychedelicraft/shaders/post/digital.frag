#version 130
uniform sampler2D scene;
uniform sampler2D glyphs;
uniform vec2 texel;
uniform vec2 resolution;
uniform float textProgress;
uniform float binaryProgress;
uniform float palette;
uniform float saturation;
in vec2 sceneUV;
out vec4 postColor;
float random(vec2 p) { return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453); }
void main() {
    vec2 cell = floor(sceneUV * resolution);
    vec2 uv = (cell + 0.5) / resolution;
    vec4 color = texture(scene, clamp(uv, texel * 0.5, 1.0 - texel * 0.5));
    float brightness = dot(color.rgb, vec3(0.3086, 0.6084, 0.0820));
    color.rgb = mix(vec3(brightness), color.rgb, saturation);
    if (palette > 0.0) color.rgb = floor(color.rgb * palette + 0.5) / palette;
    if (random(cell) < textProgress) {
        vec2 local = fract(sceneUV * resolution);
        bool binary = random(cell + 7.0) < binaryProgress;
        float glyph = binary ? step(0.5, brightness) : floor(clamp(1.0 - brightness, 0.0, 1.0) * 94.0 + 0.5);
        vec2 atlas = vec2((glyph + local.x) / 95.0, local.y * 0.5 + (binary ? 0.5 : 0.0));
        vec3 mask = texture(glyphs, atlas).rgb;
        color.rgb = mix(color.rgb, color.rgb * mask, textProgress * textProgress * textProgress);
    }
    postColor = color;
}
