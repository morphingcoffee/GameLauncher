package com.morphingcoffee.gamelauncher.core.designsystem.components

/**
 * SkSL sources for launcher background themes.
 *
 * Ports of the issue #73 Shadertoy sketches with lower production intensity
 * and a center calm mask for text contrast.
 */
internal object BackgroundSksl {
    val UNIFORM_HEADER =
        """
        uniform float uTime;
        uniform float2 uResolution;
        uniform float2 uPointer;
        """.trimIndent()

    val SPECTRAL_TOPOLOGY =
        """
        $UNIFORM_HEADER

        half3 palette(float t) {
            return 0.52 + 0.48 * cos(6.28318 * (t + half3(0.00, 0.18, 0.42)));
        }

        half4 main(float2 fragCoord) {
            float2 p = (2.0 * fragCoord - uResolution) / uResolution.y;
            float2 mouse = uPointer;

            float2 q = p;
            float mouseField = exp(-2.8 * dot(p - mouse, p - mouse));

            for (int i = 0; i < 4; i++) {
                float fi = float(i);
                q += 0.12 / float(i + 1) * float2(
                    sin(q.y * (2.1 + fi * 0.7) + uTime * (0.18 + fi * 0.03) + fi),
                    cos(q.x * (2.4 + fi * 0.6) - uTime * (0.15 + fi * 0.025) - fi)
                );
                q += (mouse - q) * mouseField * 0.025;
            }

            float field = sin(q.x * 3.2 + uTime * 0.17)
                        + cos(q.y * 3.6 - uTime * 0.13)
                        + sin((q.x + q.y) * 2.2 + uTime * 0.11);

            float bands = 0.5 + 0.5 * cos(field * 3.6);
            float contour = pow(bands, 9.0);
            float silk = 0.5 + 0.5 * sin(field + uTime * 0.12);

            half3 color = half3(0.028, 0.038, 0.070);
            color += palette(field * 0.08 + uTime * 0.018) * silk * 0.16;
            color += palette(field * 0.13 + 0.35) * contour * 0.30;
            color += palette(uTime * 0.025 + length(p - mouse) * 0.12) * mouseField * 0.12;

            float centerMask = smoothstep(0.12, 0.88, length(p * float2(0.78, 1.0)));
            color *= mix(0.42, 0.92, centerMask);

            float scan = 0.97 + 0.03 * sin(fragCoord.y * 1.7);
            color *= scan;

            return half4(color, 1.0);
        }
        """.trimIndent()

    val BACKPLANE_LIVE =
        """
        $UNIFORM_HEADER

        float hash21(float2 p) {
            return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453);
        }

        half3 signalPalette(float t) {
            half3 cyan = half3(0.08, 0.84, 1.00);
            half3 green = half3(0.20, 1.00, 0.48);
            half3 amber = half3(1.00, 0.48, 0.08);
            return mix(mix(cyan, green, smoothstep(0.0, 0.55, t)), amber, smoothstep(0.72, 1.0, t));
        }

        half4 main(float2 fragCoord) {
            float2 res = uResolution;
            float2 p = (2.0 * fragCoord - res) / res.y;
            float2 mouse = uPointer;

            half3 color = half3(0.006, 0.018, 0.025);
            float horizon = 0.40;

            float2 md = p - mouse;
            float mr = length(md);
            p += normalize(md + 0.001) * sin(mr * 18.0 - uTime * 1.7) * exp(-3.4 * mr) * 0.018;

            if (p.y < horizon) {
                float persp = 1.0 / max(0.035, horizon - p.y);
                float2 world = float2(p.x * persp * 1.15, persp + uTime * 0.32);
                float2 cellId = floor(world);
                float2 local = fract(world) - 0.5;

                float vTrace = exp(-72.0 * abs(local.x));
                float hTrace = exp(-72.0 * abs(local.y));
                float vGate = step(0.26, hash21(float2(cellId.x, floor(world.y * 0.16))));
                float hGate = step(0.48, hash21(float2(floor(world.x * 0.18), cellId.y)));
                float depthFade = exp(-0.045 * persp);

                color += half3(0.05, 0.30, 0.36) * (vTrace * vGate + hTrace * hGate) * depthFade * 0.28;

                float node = exp(-95.0 * dot(local, local));
                color += half3(0.12, 0.70, 0.78) * node * step(0.66, hash21(cellId)) * depthFade * 0.40;

                float laneSeed = hash21(float2(cellId.x, 19.0));
                float packet = exp(-180.0 * pow(fract(world.y * 0.075 - uTime * (0.28 + laneSeed * 0.32) + laneSeed) - 0.5, 2.0));
                color += signalPalette(laneSeed) * vTrace * packet * step(0.60, laneSeed) * depthFade * 0.80;

                float busSeed = hash21(float2(cellId.y, 71.0));
                float bus = exp(-150.0 * pow(fract(world.x * 0.11 + uTime * (0.14 + busSeed * 0.18) + busSeed) - 0.5, 2.0));
                color += signalPalette(busSeed + 0.25) * hTrace * bus * step(0.78, busSeed) * depthFade * 0.55;

                float2 chipCell = fract(world / float2(6.0, 8.0)) - 0.5;
                float chipSeed = hash21(floor(world / float2(6.0, 8.0)));
                float chipBody = (1.0 - smoothstep(0.22, 0.25, abs(chipCell.x)))
                               * (1.0 - smoothstep(0.15, 0.18, abs(chipCell.y)))
                               * step(0.58, chipSeed);
                color = mix(color, half3(0.012, 0.035, 0.045), chipBody * 0.86);

                float chipEdge = exp(-80.0 * abs(abs(chipCell.x) - 0.25)) * (1.0 - smoothstep(0.18, 0.22, abs(chipCell.y)));
                color += half3(0.12, 0.66, 0.72) * chipEdge * step(0.58, chipSeed) * depthFade * 0.28;
            }

            color += half3(0.08, 0.72, 1.00) * exp(-75.0 * abs(p.y - horizon)) * 0.22;
            color += half3(0.18, 0.92, 1.00) * exp(-7.0 * dot(p - mouse, p - mouse)) * 0.08;

            float centerMask = smoothstep(0.15, 0.90, length(p * float2(0.70, 1.0)));
            color *= mix(0.40, 0.90, centerMask);

            return half4(color, 1.0);
        }
        """.trimIndent()

    val ISO_LATTICE =
        """
        $UNIFORM_HEADER

        float heightAt(float2 c, float2 pointer) {
            float h = 0.45 * sin(c.x * 0.55 + uTime * 0.40) * cos(c.y * 0.48 - uTime * 0.32);
            h += 0.22 * sin((c.x + c.y) * 0.33 - uTime * 0.24);
            float d = length(c - pointer);
            h += 0.85 * exp(-0.09 * d * d) * sin(d * 1.6 - uTime * 1.8);
            return h;
        }

        half4 main(float2 fragCoord) {
            float2 res = uResolution;
            float2 p = (2.0 * fragCoord - res) / res.y;
            float2 pointer = uPointer * 7.0;

            float3 rd = normalize(float3(-0.85, -0.72, -0.85));
            float3 right = normalize(cross(rd, float3(0.0, 1.0, 0.0)));
            float3 up = cross(right, rd);
            float3 ro = right * p.x * 7.0 + up * p.y * 7.0 - rd * 22.0;

            half3 col = half3(0.008, 0.018, 0.030);

            float t = 0.0;
            bool hit = false;
            float3 pos = ro;
            for (int i = 0; i < 72; i++) {
                pos = ro + rd * t;
                if (pos.y < heightAt(pos.xz, pointer)) {
                    hit = true;
                    break;
                }
                t += 0.42;
            }

            if (hit) {
                float lo = t - 0.42;
                float hi = t;
                for (int j = 0; j < 5; j++) {
                    float mid = 0.5 * (lo + hi);
                    float3 q = ro + rd * mid;
                    if (q.y < heightAt(q.xz, pointer)) {
                        hi = mid;
                    } else {
                        lo = mid;
                    }
                }
                pos = ro + rd * hi;

                float2 cell = abs(fract(pos.xz) - 0.5);
                float neon = exp(-(0.5 - max(cell.x, cell.y)) * 24.0);
                half3 tint = mix(half3(0.08, 0.85, 1.00), half3(0.85, 0.22, 0.95), clamp(pos.y * 0.5 + 0.5, 0.0, 1.0));
                float fog = exp(-hi * 0.042);
                col += tint * neon * fog * 0.70 + tint * 0.035 * fog;
            }

            col *= 1.0 - 0.28 * dot(p * float2(0.52, 0.78), p);
            float centerMask = smoothstep(0.10, 0.85, length(p * float2(0.75, 1.0)));
            col *= mix(0.38, 0.88, centerMask);

            return half4(col, 1.0);
        }
        """.trimIndent()

    val DRAFT_BLUEPRINT =
        """
        $UNIFORM_HEADER

        float hash11(float n) {
            return fract(sin(n * 127.1) * 43758.5453);
        }

        float sdSegment(float2 p, float2 a, float2 b) {
            float2 pa = p - a;
            float2 ba = b - a;
            float h = clamp(dot(pa, ba) / max(dot(ba, ba), 1e-6), 0.0, 1.0);
            return length(pa - ba * h);
        }

        half4 main(float2 fragCoord) {
            float2 res = uResolution;
            float2 p = (2.0 * fragCoord - res) / res.y;
            float aspect = res.x / res.y;

            half3 col = half3(0.010, 0.019, 0.026);

            float2 g = fract(p * 8.0) - 0.5;
            col += half3(0.06, 0.28, 0.36) * exp(-dot(g, g) * 150.0) * 0.12;

            for (int i = 0; i < 4; i++) {
                float fi = float(i);
                float2 c = float2((mod(fi, 2.0) < 0.5 ? -1.0 : 1.0) * aspect * 0.88,
                                  (fi < 2.0 ? -1.0 : 1.0) * 0.86);
                float ch = exp(-abs(p.y - c.y) * 300.0) * (1.0 - smoothstep(0.030, 0.045, abs(p.x - c.x)))
                         + exp(-abs(p.x - c.x) * 300.0) * (1.0 - smoothstep(0.030, 0.045, abs(p.y - c.y)));
                col += half3(0.08, 0.42, 0.54) * ch * 0.22;
            }

            for (int i = 0; i < 10; i++) {
                float fi = float(i);
                float s1 = hash11(fi + 1.0);
                float s2 = hash11(fi + 21.0);
                float s3 = hash11(fi + 41.0);
                float s4 = hash11(fi + 61.0);

                float2 a = float2(mix(-aspect, aspect, s1), mix(-0.95, 0.95, s2));
                float2 c = float2(mix(-aspect, aspect, s3), mix(-0.95, 0.95, s4));
                float2 b = float2(c.x, a.y);

                float la = abs(b.x - a.x);
                float lb = abs(c.y - b.y);
                float total = la + lb;
                float ph = fract(uTime / (9.0 + s1 * 6.0) + s2);
                float drawn = clamp(ph / 0.40, 0.0, 1.0) * total;
                float fade = 1.0 - smoothstep(0.80, 0.98, ph);

                float2 tipA = a + normalize(b - a + 1e-5) * min(drawn, la);
                float rest = max(drawn - la, 0.0);
                float2 tipB = b + normalize(c - b + 1e-5) * min(rest, lb);

                float d = sdSegment(p, a, tipA);
                if (rest > 0.0) {
                    d = min(d, sdSegment(p, b, tipB));
                }
                // Accent cyan ~ #4DD9FF
                col += half3(0.30, 0.85, 1.00) * (exp(-d * 240.0) * 0.40 + exp(-d * 38.0) * 0.035) * fade;

                float2 tip = rest > 0.0 ? tipB : tipA;
                float td = length(p - tip);
                float pen = 1.0 - smoothstep(0.38, 0.42, ph);
                col += half3(0.55, 0.95, 1.00) * (exp(-td * td * 3000.0) * 0.70 + exp(-td * 24.0) * 0.07) * pen * fade;
            }

            col *= 1.0 - 0.22 * dot(p * float2(0.50, 0.76), p);
            float centerMask = smoothstep(0.12, 0.88, length(p * float2(0.72, 1.0)));
            col *= mix(0.40, 0.90, centerMask);

            return half4(col, 1.0);
        }
        """.trimIndent()
}
