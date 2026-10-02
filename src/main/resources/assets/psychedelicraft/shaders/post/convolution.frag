#version 130
uniform sampler2D scene;
uniform vec2 texel;
uniform vec2 direction;
uniform int mode;
uniform float strength;
uniform vec3 bloomColor;
in vec2 sceneUV;
out vec4 postColor;

vec4 at(vec2 uv) { return texture(scene, clamp(uv, texel * 0.5, 1.0 - texel * 0.5)); }
float weight(int i) {
    if (mode == 0) return i == 1 ? 0.15 : i == 2 ? 0.11 : i == 3 ? 0.09 : 0.05;
    return i == 1 ? 0.028 : i == 2 ? 0.020 : i == 3 ? 0.016 : 0.012;
}
void main() {
    vec4 original = at(sceneUV);
    vec3 sum = mode == 0 ? original.rgb * 0.2 : vec3(0.0);
    float influence = 15.0 / (5.0 + original.r + original.g + original.b);
    for (int i = 1; i <= 4; i++) {
        vec3 a = at(sceneUV + direction * float(i)).rgb;
        vec3 b = at(sceneUV - direction * float(i)).rgb;
        if (mode == 2) {
            float ca = clamp(1.0 - 2.0 * dot(abs(a - bloomColor), vec3(1.0)), 0.0, 1.0);
            float cb = clamp(1.0 - 2.0 * dot(abs(b - bloomColor), vec3(1.0)), 0.0, 1.0);
            sum += vec3(ca + cb) * weight(i) * 2.0;
        } else sum += (a + b) * weight(i);
    }
    vec3 result = sum;
    if (mode == 1) {
        sum *= influence * influence;
        result = original.rgb + sum * sum * sum;
    } else if (mode == 2) result = mix(original.rgb, bloomColor, clamp(sum.r, 0.0, 1.0));
    postColor = vec4(mix(original.rgb, result, strength), original.a);
}
