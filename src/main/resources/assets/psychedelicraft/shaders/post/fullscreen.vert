#version 130

in vec2 postPosition;
out vec2 sceneUV;

void main() {
    sceneUV = postPosition * 0.5 + 0.5;
    gl_Position = vec4(postPosition, 0.0, 1.0);
}
