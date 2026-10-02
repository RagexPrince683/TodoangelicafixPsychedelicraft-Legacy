#version 130
uniform sampler2D scene;
uniform sampler2D history;
uniform vec2 texel;
uniform float weight;
in vec2 sceneUV;
out vec4 postColor;
void main() {
    vec2 uv = clamp(sceneUV, texel * 0.5, 1.0 - texel * 0.5);
    vec4 current = texture(scene, uv);
    postColor = vec4(mix(current.rgb, texture(history, uv).rgb, weight), current.a);
}
