#version 130
in vec2 postPosition;
uniform vec4 rect;
out vec2 flareUV;
void main() {
    flareUV = vec2(postPosition.x * 0.5 + 0.5, 0.5 - postPosition.y * 0.5);
    gl_Position = vec4(rect.xy + postPosition * rect.zw, 0.0, 1.0);
}
