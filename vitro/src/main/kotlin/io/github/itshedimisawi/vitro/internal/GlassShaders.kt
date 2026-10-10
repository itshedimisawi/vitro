package io.github.itshedimisawi.vitro.internal

internal object GlassShaders {

    const val Backdrop = "backdrop"

    private const val Outline = """
uniform float2 center;
uniform float2 halfSize;
uniform float4 radii;
uniform float2 grow;
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

float outline(float2 p) {
    return signedDistance(p / grow) * min(grow.x, grow.y);
}
"""

    const val Shadow = Outline + """
uniform float3 shadow;

half4 main(float2 coord) {
    float2 p = coord - center;
    float cover = clamp(0.5 - outline(p), 0.0, 1.0);
    float spread = max(shadow.x, 1.0);
    float soft = 1.0 - smoothstep(-spread, spread, outline(p - float2(0.0, shadow.y)));
    return half4(0.0, 0.0, 0.0, shadow.z * soft * (1.0 - cover));
}
"""

    const val Material = Outline + """
uniform shader backdrop;
uniform float bevel;
uniform float depth;
uniform float dispersion;
uniform float zoom;
uniform float saturation;
uniform float brightness;
uniform float dark;
uniform float4 tint;
uniform float dimming;
uniform float2 light;
uniform float highlight;
uniform float2 touch;
uniform float glow;

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
    float sd = outline(p);
    float cover = clamp(0.5 - sd, 0.0, 1.0);
    if (cover <= 0.0) {
        return half4(0.0);
    }

    // Content is pulled toward the centre: fully at the outline, easing to nothing at `bevel`.
    float inset = max(-sd, 0.0);
    float2 dir = outward(p / grow);
    float slope = 1.0 - clamp(inset / max(bevel, 1.0), 0.0, 1.0);
    float2 shift = -dir * depth * slope * slope * slope;

    float fringe = 2.0 * dispersion;
    float3 color = float3(
        backdrop.eval(center + (p + shift * (1.0 - fringe)) * zoom).r,
        backdrop.eval(center + (p + shift) * zoom).g,
        backdrop.eval(center + (p + shift * (1.0 + fringe)) * zoom).b
    );

    float3 weights = float3(0.2126, 0.7152, 0.0722);
    color = max(mix(float3(dot(color, weights)), color, saturation), 0.0);

    float facing = dot(dir, light);
    float rim = 1.0 - smoothstep(0.75 * density, 1.25 * density, inset);
    float edge = rim * (0.11 + 0.89 * abs(facing)) * highlight;

    // Both materials are worked out and mixed, so glass can change from one to the other.
    float veil = brightness;
    float luma = min(dot(color, weights), 1.0);
    float3 pale = color * (1.0 + veil);
    float3 deep = pale;
    if (veil >= 0.0) {
        pale = mix(color, float3(luma), $LightWash * veil) + veil * ($LightVeil - $LightFade * luma);
        // Dark glass lifts black a little and holds bright content down.
        float lift = $DarkLift * pow(1.0 - luma, 16.0);
        float shade = $DarkShade * pow(max(luma - $DarkKnee, 0.0), 1.5);
        deep = max(color * (1.0 + $DarkGain * veil) + veil * (lift - shade), 0.0);
    }

    // The rim is lit over the veil and keeps the hue of what shows through it: a push toward
    // white for light glass, a glint that is strongest on dark content for dark glass.
    float paleLit = clamp(dot(pale, weights), 0.02, 1.0);
    pale *= 1.0 + $LightRim * edge * (1.0 - paleLit) / paleLit;
    float deepLit = clamp(dot(deep, weights), 0.02, 1.0);
    deep *= 1.0 + edge * ($DarkRimGain + $DarkRimLift / deepLit);
    color = mix(pale, deep, dark);
    color = mix(color, tint.rgb, tint.a);
    color *= 1.0 - dimming;

    float2 reach = p - touch;
    float spot = exp(-dot(reach, reach) / max(halfSize.y * halfSize.y * 6.0, 1.0));
    color += glow * (0.7 + 0.3 * spot);
    color = clamp(color, 0.0, 1.0);

    return half4(color * cover, cover);
}
"""
}
