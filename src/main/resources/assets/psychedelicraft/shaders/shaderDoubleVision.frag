#version 120

uniform sampler2D tex0;

uniform float distance;
uniform float stretch;
uniform float totalAlpha;

void main()
{
    vec2 sourceUV = clampSceneUV(gl_TexCoord[0].st);
    gl_FragColor = texture2D(tex0, sourceUV);
    vec4 newColor = gl_FragColor * 0.35;

    float safeStretch = max(stretch, 0.0001);
    float safeDistance = clamp(distance, -1.0, 1.0);
    vec2 positiveUV = vec2(0.5 + (sourceUV.s - 0.5) / safeStretch + safeDistance, sourceUV.t);
    vec2 negativeUV = vec2(0.5 + (sourceUV.s - 0.5) / safeStretch - safeDistance, sourceUV.t);
    newColor += texture2D(tex0, clampSceneUV(positiveUV)) * 0.325;
    newColor += texture2D(tex0, clampSceneUV(negativeUV)) * 0.325;
    
    gl_FragColor = mix(gl_FragColor, newColor, totalAlpha);
}
