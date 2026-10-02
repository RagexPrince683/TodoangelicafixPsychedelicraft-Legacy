#version 130
uniform sampler2D flare;
uniform vec4 tint;
in vec2 flareUV;
out vec4 postColor;
void main() { postColor = texture(flare, flareUV) * tint; }
