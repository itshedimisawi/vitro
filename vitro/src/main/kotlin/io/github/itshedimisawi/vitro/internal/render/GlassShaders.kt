package io.github.itshedimisawi.vitro.internal.render

import io.github.itshedimisawi.vitro.internal.material.DarkGain
import io.github.itshedimisawi.vitro.internal.material.DarkKnee
import io.github.itshedimisawi.vitro.internal.material.DarkLift
import io.github.itshedimisawi.vitro.internal.material.DarkRimGain
import io.github.itshedimisawi.vitro.internal.material.DarkRimLift
import io.github.itshedimisawi.vitro.internal.material.DarkRimVivid
import io.github.itshedimisawi.vitro.internal.material.DarkShade
import io.github.itshedimisawi.vitro.internal.material.DarkVivid
import io.github.itshedimisawi.vitro.internal.material.LightFade
import io.github.itshedimisawi.vitro.internal.material.LightRim
import io.github.itshedimisawi.vitro.internal.material.LightRimVivid
import io.github.itshedimisawi.vitro.internal.material.LightVeil
import io.github.itshedimisawi.vitro.internal.material.LightWash
import io.github.itshedimisawi.vitro.internal.material.RimFloor

/** AGSL sources of the shader path: the shadow and the material, sharing one outline. */
internal object GlassShaders {

    const val Backdrop = "backdrop"

    /** How many shapes one pass can join into a single outline. */
    const val MaxShapes = 8

    // Every shape is a rounded rectangle given by its centre and half size, its corner radii and
    // how much it is enlarged. Shapes closer than `blend` flow into one another.
    private const val Outline = """
uniform float count;
uniform float blend;
uniform float4 frames[$MaxShapes];
uniform float4 corners[$MaxShapes];
uniform float4 grows[$MaxShapes];
uniform float density;

float radiusAt(float2 p, float4 radii) {
    float2 side = p.x < 0.0 ? radii.xw : radii.yz;
    return p.y < 0.0 ? side.x : side.y;
}

float signedDistance(float2 p, float2 halfSize, float4 radii) {
    float r = radiusAt(p, radii);
    float2 q = abs(p) - halfSize + r;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

float2 outward(float2 p, float2 halfSize, float4 radii) {
    float r = radiusAt(p, radii);
    float2 q = abs(p) - halfSize + r;
    float2 s = float2(p.x < 0.0 ? -1.0 : 1.0, p.y < 0.0 ? -1.0 : 1.0);
    if (q.x > 0.0 && q.y > 0.0) {
        return normalize(q) * s;
    }
    return q.x > q.y ? float2(s.x, 0.0) : float2(0.0, s.y);
}

float shapeDistance(float2 coord, float4 frame, float4 radii, float2 grow) {
    return signedDistance((coord - frame.xy) / grow, frame.zw, radii) * min(grow.x, grow.y);
}

// Share the nearer of two shapes takes where they meet: all of it away from the join, half at
// equal distance.
float share(float joined, float next) {
    return clamp(0.5 + 0.5 * (joined - next) / max(blend, 0.0001), 0.0, 1.0);
}

float join(float joined, float next, float h) {
    return mix(joined, next, h) - blend * h * (1.0 - h);
}
"""

    const val Shadow = Outline + """
uniform float2 shadow;

half4 main(float2 coord) {
    float sd = 100000.0;
    float dropped = 100000.0;
    float opacity = 0.0;
    for (int i = 0; i < $MaxShapes; i++) {
        if (float(i) >= count) { break; }
        float d = shapeDistance(coord, frames[i], corners[i], grows[i].xy);
        sd = join(sd, d, share(sd, d));
        float below = shapeDistance(coord - float2(0.0, shadow.y), frames[i], corners[i], grows[i].xy);
        float h = share(dropped, below);
        dropped = join(dropped, below, h);
        opacity = mix(opacity, grows[i].z, h);
    }
    float cover = clamp(0.5 - sd, 0.0, 1.0);
    float spread = max(shadow.x, 1.0);
    float soft = 1.0 - smoothstep(-spread, spread, dropped);
    return half4(0.0, 0.0, 0.0, opacity * soft * (1.0 - cover));
}
"""

    const val Material = Outline + """
uniform shader backdrop;
uniform float4 traits[$MaxShapes];
uniform float4 tints[$MaxShapes];
uniform float bevel;
uniform float depth;
uniform float dispersion;
uniform float zoom;
uniform float saturation;
uniform float brightness;
uniform float2 light;
uniform float highlight;

half4 main(float2 coord) {
    float sd = 100000.0;
    float2 normal = float2(0.0);
    float2 pivot = float2(0.0);
    float dark = 0.0;
    float dimming = 0.0;
    float glow = 0.0;
    float4 tint = float4(0.0);
    for (int i = 0; i < $MaxShapes; i++) {
        if (float(i) >= count) { break; }
        float2 p = coord - frames[i].xy;
        float2 halfSize = frames[i].zw;
        float d = shapeDistance(coord, frames[i], corners[i], grows[i].xy);
        float h = share(sd, d);
        sd = join(sd, d, h);
        normal = mix(normal, outward(p / grows[i].xy, halfSize, corners[i]), h);
        pivot = mix(pivot, frames[i].xy, h);
        dark = mix(dark, traits[i].x, h);
        dimming = mix(dimming, traits[i].y, h);
        tint = mix(tint, tints[i], h);
        float2 reach = p - traits[i].zw;
        float spot = exp(-dot(reach, reach) / max(halfSize.y * halfSize.y * 6.0, 1.0));
        glow = mix(glow, grows[i].w * (0.7 + 0.3 * spot), h);
    }
    float cover = clamp(0.5 - sd, 0.0, 1.0);
    if (cover <= 0.0) {
        return half4(0.0);
    }

    // Content is pulled toward the centre: fully at the outline, easing to nothing at `bevel`.
    float inset = max(-sd, 0.0);
    float2 dir = normal / max(length(normal), 0.0001);
    float slope = 1.0 - clamp(inset / max(bevel, 1.0), 0.0, 1.0);
    float2 shift = -dir * depth * slope * slope * slope;
    float2 p = coord - pivot;

    float fringe = 2.0 * dispersion;
    float3 color = float3(
        backdrop.eval(pivot + (p + shift * (1.0 - fringe)) * zoom).r,
        backdrop.eval(pivot + (p + shift) * zoom).g,
        backdrop.eval(pivot + (p + shift * (1.0 + fringe)) * zoom).b
    );

    float3 weights = float3(0.2126, 0.7152, 0.0722);
    color = max(mix(float3(dot(color, weights)), color, saturation), 0.0);

    float facing = dot(dir, light);
    float rim = 1.0 - smoothstep(0.75 * density, 1.25 * density, inset);
    float edge = rim * ($RimFloor + (1.0 - $RimFloor) * abs(facing)) * highlight;

    // Both materials are worked out and mixed, so glass can change from one to the other.
    float veil = brightness;
    float luma = min(dot(color, weights), 1.0);
    float3 pale = color * (1.0 + veil);
    float3 deep = pale;
    if (veil >= 0.0) {
        pale = mix(color, float3(luma), $LightWash * veil) + veil * ($LightVeil - $LightFade * luma);
        // Dark glass deepens colours, lifts black a little and holds bright content down.
        float lift = $DarkLift * pow(1.0 - luma, 16.0);
        float shade = $DarkShade * pow(max(luma - $DarkKnee, 0.0), 1.5);
        float3 vivid = max(mix(float3(luma), color, 1.0 + $DarkVivid * veil), 0.0);
        deep = max(vivid * (1.0 + $DarkGain * veil) + veil * (lift - shade), 0.0);
    }

    // The rim is lit over the veil, working from the colour as it would be shown. What shows
    // through it turns more vivid and brighter: a push toward white for light glass, a glint
    // that is strongest on dark content for dark glass, which only ever adds light.
    float3 paleSeen = min(pale, 1.0);
    float paleLit = clamp(dot(paleSeen, weights), 0.02, 1.0);
    float3 paleVivid = max(mix(float3(paleLit), paleSeen, 1.0 + $LightRimVivid * edge), 0.0);
    pale += paleVivid * (1.0 + $LightRim * edge * (1.0 - paleLit) / paleLit) - paleSeen;
    float3 deepSeen = min(deep, 1.0);
    float deepLit = clamp(dot(deepSeen, weights), 0.02, 1.0);
    float3 deepVivid = max(mix(float3(deepLit), deepSeen, 1.0 + $DarkRimVivid * edge), 0.0);
    deep += max(deepVivid * (1.0 + edge * ($DarkRimGain + $DarkRimLift / deepLit)) - deepSeen, 0.0);
    color = mix(pale, deep, dark);
    color = mix(color, tint.rgb, tint.a);
    color *= 1.0 - dimming;
    color = clamp(color + glow, 0.0, 1.0);

    return half4(color * cover, cover);
}
"""
}
