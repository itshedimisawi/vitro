package io.github.itshedimisawi.vitro.internal

internal object GlassShaders {

    const val Backdrop = "backdrop"

    private const val Outline = """
uniform float2 center;
uniform float2 halfSize;
uniform float4 radii;
uniform float density;

float radiusAt(float2 p) {
    float2 side = p.x < 0.0 ? radii.xw : radii.yz;
    return p.y < 0.0 ? side.x : side.y;
}

float signedDistance(float2 p) {
    float r = radiusAt(p);
    float2 q = abs(p) - halfSize + r;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}
"""

    const val Shadow = Outline + """
uniform float3 shadow;

half4 main(float2 coord) {
    float2 p = coord - center;
    float cover = clamp(0.5 - signedDistance(p), 0.0, 1.0);
    float spread = max(shadow.x, 1.0);
    float soft = 1.0 - smoothstep(-spread, spread, signedDistance(p - float2(0.0, shadow.y)));
    return half4(0.0, 0.0, 0.0, shadow.z * soft * (1.0 - cover));
}
"""

    const val Material = Outline + """
uniform shader backdrop;
uniform float bevel;
uniform float depth;
uniform float dispersion;
uniform float saturation;
uniform float brightness;
uniform float dark;
uniform float4 tint;
uniform float dimming;
uniform float2 light;
uniform float highlight;
uniform float3 touch;

float2 outward(float2 p) {
    float r = radiusAt(p);
    float2 q = abs(p) - halfSize + r;
    float2 s = float2(p.x < 0.0 ? -1.0 : 1.0, p.y < 0.0 ? -1.0 : 1.0);
    if (q.x > 0.0 && q.y > 0.0) {
        return normalize(q) * s;
    }
    return q.x > q.y ? float2(s.x, 0.0) : float2(0.0, s.y);
}

half4 main(float2 coord) {
    float2 p = coord - center;
    float sd = signedDistance(p);
    float cover = clamp(0.5 - sd, 0.0, 1.0);
    if (cover <= 0.0) {
        return half4(0.0);
    }

    // Content is pulled toward the centre: fully at the outline, not at all past `bevel`.
    float inset = max(-sd, 0.0);
    float2 dir = outward(p);
    float2 shift = -dir * depth * (1.0 - clamp(inset / max(bevel, 1.0), 0.0, 1.0));

    // A press clears the material and widens its colour fringes, like glass lifted off the page.
    float fringe = 2.0 * (dispersion + $PressDispersion * touch.z);
    float3 color = float3(
        backdrop.eval(coord + shift * (1.0 - fringe)).r,
        backdrop.eval(coord + shift).g,
        backdrop.eval(coord + shift * (1.0 + fringe)).b
    );

    float3 weights = float3(0.2126, 0.7152, 0.0722);
    color = max(mix(float3(dot(color, weights)), color, saturation), 0.0);

    float facing = dot(dir, light);
    float rim = 1.0 - smoothstep(0.75 * density, 1.25 * density, inset);
    float edge = rim * (0.04 + 0.96 * smoothstep(0.0, 1.0, abs(facing))) * highlight;
    color = color * (1.0 + 0.8 * edge) + 0.04 * edge;

    // The veil is laid over the lit rim, so highlights wash out on already bright content.
    float veil = brightness * (1.0 - $PressClearing * touch.z);
    float luma = min(dot(color, weights), 1.0);
    if (veil < 0.0) {
        color *= 1.0 + veil;
    } else if (dark > 0.5) {
        color = color * (1.0 + $DarkGain * veil) + $DarkLift * veil * pow(1.0 - luma, 6.0);
    } else {
        color += $LightVeil * veil * (1.0 - luma);
    }
    color = mix(color, tint.rgb, tint.a);
    color *= 1.0 - dimming;

    float2 reach = p - touch.xy;
    float spot = touch.z * exp(-dot(reach, reach) / max(halfSize.y * halfSize.y * 6.0, 1.0));
    color += spot * 0.12 + touch.z * 0.03;
    color = clamp(color, 0.0, 1.0);

    return half4(color * cover, cover);
}
"""
}
