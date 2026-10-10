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
    float sd = signedDistance(p);
    float cover = clamp(0.5 - sd, 0.0, 1.0);
    float spread = max(shadow.x, 1.0);
    float soft = 1.0 - smoothstep(-spread, spread, signedDistance(p - float2(0.0, shadow.y)));
    float contact = 2.5 * (1.0 - smoothstep(0.0, density, max(sd, 0.0)));
    float shade = shadow.z * max(soft, contact);
    return half4(0.0, 0.0, 0.0, min(shade, 0.3) * (1.0 - cover));
}
"""

    const val Material = Outline + """
uniform shader backdrop;
uniform float bevel;
uniform float depth;
uniform float dispersion;
uniform float saturation;
uniform float brightness;
uniform float4 tint;
uniform float dimming;
uniform float2 light;
uniform float highlight;
uniform float3 touch;

const float IOR = $RefractiveIndex;

float2 outward(float2 p) {
    float r = radiusAt(p);
    float2 q = abs(p) - halfSize + r;
    float2 s = float2(p.x < 0.0 ? -1.0 : 1.0, p.y < 0.0 ? -1.0 : 1.0);
    if (q.x > 0.0 && q.y > 0.0) {
        return normalize(q) * s;
    }
    return q.x > q.y ? float2(s.x, 0.0) : float2(0.0, s.y);
}

float2 bend(float3 normal, float ior) {
    float3 ray = refract(float3(0.0, 0.0, -1.0), normal, 1.0 / ior);
    return ray.xy / max(-ray.z, 0.25);
}

half4 main(float2 coord) {
    float2 p = coord - center;
    float sd = signedDistance(p);
    float cover = clamp(0.5 - sd, 0.0, 1.0);
    if (cover <= 0.0) {
        return half4(0.0);
    }

    // The rim curves like a lens edge: steepest at the outline, flat once `bevel` is reached.
    float inset = max(-sd, 0.0);
    float tilt = 1.0 - clamp(inset / max(bevel, 1.0), 0.0, 1.0);
    tilt *= tilt;
    float2 dir = outward(p);
    float3 normal = float3(dir * tilt, sqrt(max(1.0 - tilt * tilt, 0.0)));

    float3 color = float3(
        backdrop.eval(coord + bend(normal, IOR * (1.0 - dispersion)) * depth).r,
        backdrop.eval(coord + bend(normal, IOR) * depth).g,
        backdrop.eval(coord + bend(normal, IOR * (1.0 + dispersion)) * depth).b
    );

    float luma = dot(color, float3(0.2126, 0.7152, 0.0722));
    color = max(mix(float3(luma), color, saturation), 0.0);
    color = mix(color, float3(brightness > 0.0 ? 1.0 : 0.0), abs(brightness));
    color = mix(color, tint.rgb, tint.a);
    color *= 1.0 - dimming;

    float facing = dot(dir, light);
    float rim = 1.0 - clamp(inset / (2.0 * density), 0.0, 1.0);
    rim *= rim;
    float edge = rim * (0.1 + 0.4 * facing * facing);

    float3 halfway = normalize(float3(light * 0.6, 1.8));
    float sheen = pow(max(dot(normal, halfway), 0.0), 40.0) * tilt;
    float glow = tilt * tilt * 0.04;

    float2 reach = p - touch.xy;
    float spot = touch.z * exp(-dot(reach, reach) / max(halfSize.y * halfSize.y * 6.0, 1.0));

    color += highlight * (edge + sheen * 0.12 + glow) + spot * 0.12 + touch.z * 0.03;
    color = clamp(color, 0.0, 1.0);

    return half4(color * cover, cover);
}
"""
}
