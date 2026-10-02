#version 130
uniform sampler2D scene;
uniform vec2 texel;
uniform float strength;
in vec2 sceneUV;
out vec4 postColor;
void main() {
    vec4 color = vec4(0.0);
    for (int i = 0; i < 9; i++) {
        vec2 uv = sceneUV + (0.5 - sceneUV) * float(i) * strength * 0.005;
        color += texture(scene, clamp(uv, texel * 0.5, 1.0 - texel * 0.5));
    }
    postColor = color / 9.0;
}
