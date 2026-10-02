#version 130
uniform sampler2D scene;
uniform vec2 texel;
uniform vec3 waves;
uniform vec2 deformation;
uniform vec4 pulse;
uniform vec4 contrast;
uniform vec2 rotation;
uniform vec2 saturation;
uniform float ticks;
in vec2 sceneUV;
out vec4 postColor;

vec3 rotateColor(vec3 color, float phase) {
    float angle = fract(phase) * 3.0;
    vec3 first = angle < 1.0 ? color : angle < 2.0 ? color.brg : color.gbr;
    vec3 second = angle < 1.0 ? color.brg : angle < 2.0 ? color.gbr : color;
    return mix(first, second, fract(angle));
}

void main() {
    vec2 center = sceneUV - 0.5;
    float radius = length(center);
    vec2 offset = vec2(sin(sceneUV.y * 25.0 + ticks * 0.07) * waves.z * 0.018,
        sin(sceneUV.x * 18.0 + ticks * 0.04) * waves.y * 0.025);
    offset += center * sin(radius * 45.0 - ticks * 0.05) * waves.x * 0.10;
    offset.y += sin(radius * 55.0 + ticks * 0.03) * deformation.y * radius * 0.035;
    vec4 sampleColor = texture(scene, clamp(sceneUV + offset, texel * 0.5, 1.0 - texel * 0.5));
    vec3 color = sampleColor.rgb;
    float fractal = sin((sceneUV.x + sceneUV.y) * 180.0 + ticks * 0.08) * 0.5 + 0.5;
    color *= mix(1.0, 0.72 + fractal * 0.56, clamp(deformation.x, 0.0, 1.0));
    color = mix(color, pulse.rgb, clamp(pulse.a * 0.35, 0.0, 1.0));
    color = mix(color, 0.5 + (color - 0.5) * contrast.rgb, clamp(contrast.a, 0.0, 1.0));
    color = mix(color, rotateColor(color, ticks / 300.0), rotation.x * 0.5);
    color = mix(color, rotateColor(color, ticks / 50.0), clamp(rotation.y * 1.5, 0.0, 1.0));
    float brightness = dot(color, vec3(0.3086, 0.6084, 0.0820));
    color = mix(color, vec3(brightness), saturation.x);
    vec3 intensified = 2.0 * color - dot(color, vec3(0.3086, 0.6084, 0.0820));
    color = mix(color, clamp(intensified * intensified * 10.0, 0.0, 1.0), saturation.y);
    postColor = vec4(clamp(color, 0.0, 1.0), sampleColor.a);
}
