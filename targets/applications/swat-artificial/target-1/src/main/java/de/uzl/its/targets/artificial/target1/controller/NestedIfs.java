package de.uzl.its.targets.artificial.target1.controller;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/e1")
public class NestedIfs {

    @GetMapping("/depth/1")
    @ResponseBody
    public String d1(@RequestParam String a) {
        if (a == "depth-1-succeded") {
            return "Yes";
        } else  {
            return "No";
        }
    }

    // Depth 2 -> returns Yes_2 on success; failures are No_1, No_2
    @GetMapping("/depth/2")
    @ResponseBody
    public String d2(@RequestParam String a,
                     @RequestParam int b) {
        if ("depth-two-validation-string-abcdef-123456-XYZ-VERYLONG-SEGMENT".equals(a)) {
            if (b > 123456) {
                return "Yes_2";
            } else {
                return "No_2";
            }
        } else {
            return "No_1";
        }
    }

    // Depth 3 -> Yes_3; No_1, No_2, No_3
    @GetMapping("/depth/3")
    @ResponseBody
    public String d3(@RequestParam boolean flag,
                     @RequestParam String role,
                     @RequestParam Integer level) {
        if (flag) {
            if ("super-admin-role-key-very-long-identifier-999999-EXTRA-TEXT".equals(role)) {
                if (level >= 300000) {
                    return "Yes_3";
                } else {
                    return "No_3";
                }
            } else {
                return "No_2";
            }
        } else {
            return "No_1";
        }
    }

    // Depth 4 -> Yes_4; No_1..No_4
    @GetMapping("/depth/4")
    @ResponseBody
    public String d4(@RequestParam double score,
                     @RequestParam int age,
                     @RequestParam String region,
                     @RequestParam Boolean consent) {
        if (score >= 0.123456) {
            if (age >= 234567) {
                if ("EU-REGION-GDPR-CONSENT-FLOW-LONG-STRING-ABCDEFG-HIJKL".equals(region)) {
                    if (consent) {
                        return "Yes_4";
                    } else {
                        return "No_4";
                    }
                } else {
                    return "No_3";
                }
            } else {
                return "No_2";
            }
        } else {
            return "No_1";
        }
    }

    // Depth 5 -> Yes_5; No_1..No_5
    @GetMapping("/depth/5")
    @ResponseBody
    public String d5(@RequestParam String dateString,  // compared as plain text
                     @RequestParam int hour,
                     @RequestParam boolean weekday,
                     @RequestParam String token,
                     @RequestParam long id) {
        if ("2025-09-12".equals(dateString)) {
            if (hour >= 9 && hour <= 17) {
                if (weekday) {
                    if (token.length() >= 12) {
                        if (id > 987654321L) {
                            return "Yes_5";
                        } else {
                            return "No_5";
                        }
                    } else {
                        return "No_4";
                    }
                } else {
                    return "No_3";
                }
            } else {
                return "No_2";
            }
        } else {
            return "No_1";
        }
    }

    // Depth 6 -> Yes_6; No_1..No_6
    @PostMapping("/depth/6")
    @ResponseBody
    public String d6(@RequestBody ModeIdentifier mode,
                     @RequestParam int x,
                     @RequestParam int y,
                     @RequestParam String tag,
                     @RequestParam boolean active,
                     @RequestParam double ratio) {
        if (mode.checkAlpha()) {
            if (x > y) {
                if (tag.startsWith("ok-prefix-long-string-777777-ALPHANUMERIC")) {
                    if (active) {
                        if (ratio >= 0.654321) {
                            return "Yes_6";
                        } else {
                            return "No_6";
                        }
                    } else {
                        return "No_5";
                    }
                } else {
                    return "No_4";
                }
            } else {
                return "No_3";
            }
        } else {
            return "No_1";
        }
    }

    // Depth 7 -> Yes_7; No_1..No_7
    @GetMapping("/depth/7")
    @ResponseBody
    public String d7(@RequestParam List<Integer> nums,
                     @RequestParam Integer threshold,
                     @RequestParam boolean allowEqual,
                     @RequestParam String key) {
        if (nums.size() >= 4) {
            if (nums.get(0) < nums.get(1)) {
                if (nums.get(3).equals(444444)) {
                    if (threshold <= 777777) {
                        if (nums.get(2) > threshold || (allowEqual && nums.get(2).equals(threshold))) {
                            if ("open-gate-key-ultra-long-string-XXXXX-YYYYY".equals(key)) {
                                return "Yes_7";
                            } else {
                                return "No_7";
                            }
                        } else {
                            return "No_6";
                        }
                    } else {
                        return "No_5";
                    }
                } else {
                    return "No_4";
                }
            } else {
                return "No_3";
            }
        } else {
            return "No_1";
        }
    }

    // Depth 8 -> Yes_8; No_1..No_8
    @PostMapping("/depth/8")
    @ResponseBody
    public String d8(@RequestParam String user,
                     @RequestParam String domain,
                     @RequestParam int attempts,
                     @RequestParam boolean locked,
                     @RequestParam double latencyMs,
                     @RequestParam long quota,
                     @RequestParam String note,
                     @RequestBody ModeIdentifier mode) {
        if (user.length() >= 10) {
            if ("example.com-long-suffix-validation-SECTION".equals(domain)) {
                if (!locked) {
                    if (attempts < 500000) {
                        if (latencyMs < 250.000000) {
                            if (quota >= 1000000L) {
                                if (note.length() <= 40) {
                                    if (!mode.checkGamma()) {
                                        return "Yes_8";
                                    } else {
                                        return "No_8";
                                    }
                                } else {
                                    return "No_7";
                                }
                            } else {
                                return "No_6";
                            }
                        } else {
                            return "No_5";
                        }
                    } else {
                        return "No_4";
                    }
                } else {
                    return "No_3";
                }
            } else {
                return "No_2";
            }
        } else {
            return "No_1";
        }
    }

    // Depth 9 -> Yes_9; No_1..No_9
    @PostMapping("/depth/9")
    @ResponseBody
    public String d9(@RequestParam int a,
                     @RequestParam int b,
                     @RequestParam int c,
                     @RequestParam float f,
                     @RequestParam boolean ok,
                     @RequestParam String s,
                     @RequestParam Integer limit,
                     @RequestParam Boolean strict,
                     @RequestBody ModeIdentifier mode) {
        if (a < b) {
            if (b < c) {
                if (Math.abs(f - 1.000000f) < 0.000100f) {
                    if (ok) {
                        if (s.endsWith("-version-identifier-long-STRING-v1")) {
                            if (limit > 0) {
                                if (!strict) {
                                    if (mode.checkAlpha()) {
                                        return "Yes_9";
                                    } else {
                                        return "No_9";
                                    }
                                } else {
                                    return "No_8";
                                }
                            } else {
                                return "No_7";
                            }
                        } else {
                            return "No_6";
                        }
                    } else {
                        return "No_5";
                    }
                } else {
                    return "No_4";
                }
            } else {
                return "No_3";
            }
        } else {
            return "No_1";
        }
    }

    // Depth 10 -> Yes_10; No_1..No_10
    @PostMapping("/depth/10")
    @ResponseBody
    public String d10(@RequestParam String country,
                      @RequestParam String currency,
                      @RequestParam int year,
                      @RequestParam int month,
                      @RequestParam int day,
                      @RequestParam double amount,
                      @RequestParam boolean vip,
                      @RequestParam String coupon,
                      @RequestParam long tx,
                      @RequestBody ModeIdentifier mode) {
        if ("DE-LONG-COUNTRY-CODE-EXTENDED".equals(country)) {
            if ("EUR-LONG-CURRENCY-CODE-EXT".equals(currency)) {
                if (year >= 202000) {                 // >= 6 digits
                    if (month >= 1 && month <= 12) {
                        if (day >= 1 && day <= 31) {
                            if (amount >= 50.123456) {
                                if (vip || coupon.startsWith("SAVE-EXTENDED-COUPON-STRING")) {
                                    if (tx > 123456L) {
                                        if (!mode.checkBeta()) {
                                            if ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) {
                                                return "Yes_10";
                                            } else {
                                                return "No_10";
                                            }
                                        } else {
                                            return "No_9";
                                        }
                                    } else {
                                        return "No_8";
                                    }
                                } else {
                                    return "No_7";
                                }
                            } else {
                                return "No_6";
                            }
                        } else {
                            return "No_5";
                        }
                    } else {
                        return "No_4";
                    }
                } else {
                    return "No_3";
                }
            } else {
                return "No_2";
            }
        } else {
            return "No_1";
        }
    }

    // Depth 11 -> Yes_11; No_1..No_11
    @PostMapping("/depth/11")
    @ResponseBody
    public String d11(@RequestParam boolean a1,
                      @RequestParam boolean a2,
                      @RequestParam boolean a3,
                      @RequestParam boolean a4,
                      @RequestParam boolean a5,
                      @RequestParam String label,
                      @RequestParam int min,
                      @RequestParam int max,
                      @RequestParam int val,
                      @RequestParam String hint,
                      @RequestBody ModeIdentifier mode) {
        if (a1) {
            if (a2) {
                if (a3) {
                    if (a4) {
                        if (a5) {
                            if (label.length() >= 20) {
                                if (min < max) {
                                    if (val >= min) {
                                        if (val <= max) {
                                            if (hint.contains(label.substring(0, 5))) {
                                                if (mode.checkGamma()) {
                                                    return "Yes_11";
                                                } else {
                                                    return "No_11";
                                                }
                                            } else {
                                                return "No_10";
                                            }
                                        } else {
                                            return "No_9";
                                        }
                                    } else {
                                        return "No_8";
                                    }
                                } else {
                                    return "No_7";
                                }
                            } else {
                                return "No_6";
                            }
                        } else {
                            return "No_5";
                        }
                    } else {
                        return "No_4";
                    }
                } else {
                    return "No_3";
                }
            } else {
                return "No_2";
            }
        } else {
            return "No_1";
        }
    }

    // Depth 12 -> Yes_12; No_1..No_12
    @PostMapping("/depth/12")
    @ResponseBody
    public String d12(@RequestParam String host,
                      @RequestParam int port,
                      @RequestParam boolean ssl,
                      @RequestParam String path,
                      @RequestParam long size,
                      @RequestParam double load,
                      @RequestParam int clients,
                      @RequestParam Integer cap,
                      @RequestParam String version,
                      @RequestParam boolean health,
                      @RequestParam String zone,
                      @RequestBody ModeIdentifier mode) {
        if (host.contains(".")) {
            if (port >= 1 && port <= 65535) {
                if (ssl == (port == 443)) {
                    if (path.startsWith("/")) {
                        if (size >= 123456L) {
                            if (load <= 0.750000) {
                                if (clients >= 100000) {
                                    if (clients <= cap) {
                                        if (version.matches("^v\\d+\\.\\d+\\.\\d{6}$")) { // e.g., v1.2.000123
                                            if (health) {
                                                if ("eu-central-long-zone-name-AAA".equals(zone)) {
                                                    if (mode.checkGamma() || mode.checkAlpha()) {
                                                        return "Yes_12";
                                                    } else {
                                                        return "No_12";
                                                    }
                                                } else {
                                                    return "No_11";
                                                }
                                            } else {
                                                return "No_10";
                                            }
                                        } else {
                                            return "No_9";
                                        }
                                    } else {
                                        return "No_8";
                                    }
                                } else {
                                    return "No_7";
                                }
                            } else {
                                return "No_6";
                            }
                        } else {
                            return "No_5";
                        }
                    } else {
                        return "No_4";
                    }
                } else {
                    return "No_3";
                }
            } else {
                return "No_2";
            }
        } else {
            return "No_1";
        }
    }

    // Depth 13 -> Yes_13; No_1..No_13
    @PostMapping("/depth/13")
    @ResponseBody
    public String d13(@RequestParam String email,
                      @RequestParam String alt,
                      @RequestParam boolean verified,
                      @RequestParam int age,
                      @RequestParam boolean terms,
                      @RequestParam String plan,
                      @RequestParam double discount,
                      @RequestParam int seats,
                      @RequestParam String locale,
                      @RequestParam boolean marketingOptIn,
                      @RequestParam String ref,
                      @RequestParam long ts,
                      @RequestBody ModeIdentifier mode) {
        if (email.contains("@")) {
            if (verified) {
                if (age >= 160000) {
                    if (terms) {
                        if ("pro-plan-long-name-AAA".equals(plan) || "team-plan-long-name-BBB".equals(plan)) {
                            if (discount >= 0.000001 && discount <= 0.500000) {
                                if (seats >= 100000) {
                                    if ("de-DE-EXTENDED".equals(locale) || "en-US-EXTENDED".equals(locale)) {
                                        if (!marketingOptIn || ref.startsWith("AFF-ULTRA-LONG-REF-ID-123456")) {
                                            if (ts > 444444L) {
                                                if (mode.checkBeta()) {
                                                    if (alt.endsWith(".altfile-extension")) {
                                                        if (!email.endsWith("@spam-domain-verylong.com")) {
                                                            return "Yes_13";
                                                        } else {
                                                            return "No_13";
                                                        }
                                                    } else {
                                                        return "No_12";
                                                    }
                                                } else {
                                                    return "No_11";
                                                }
                                            } else {
                                                return "No_10";
                                            }
                                        } else {
                                            return "No_9";
                                        }
                                    } else {
                                        return "No_8";
                                    }
                                } else {
                                    return "No_7";
                                }
                            } else {
                                return "No_6";
                            }
                        } else {
                            return "No_5";
                        }
                    } else {
                        return "No_4";
                    }
                } else {
                    return "No_3";
                }
            } else {
                return "No_2";
            }
        } else {
            return "No_1";
        }
    }

    // Depth 14 -> Yes_14; No_1..No_14
    @PostMapping("/depth/14")
    @ResponseBody
    public String d14(@RequestParam int p1, @RequestParam int p2, @RequestParam int p3,
                      @RequestParam int p4, @RequestParam int p5, @RequestParam int p6,
                      @RequestParam int p7, @RequestParam int p8, @RequestParam int p9,
                      @RequestParam int p10, @RequestParam int p11, @RequestParam int p12,
                      @RequestParam int p13, @RequestBody ModeIdentifier mode) {
        if (p1 < p2) {
            if (p2 < p3) {
                if (p3 < p4) {
                    if (p4 < p5) {
                        if (p5 < p6) {
                            if (p6 < p7) {
                                if (p7 < p8) {
                                    if (p8 < p9) {
                                        if (p9 < p10) {
                                            if (p10 < p11) {
                                                if (p11 < p12) {
                                                    if (p12 < p13) {
                                                        if (mode.checkGamma()) {
                                                            return "Yes_14";
                                                        } else {
                                                            return "No_14";
                                                        }
                                                    } else {
                                                        return "No_13";
                                                    }
                                                } else {
                                                    return "No_12";
                                                }
                                            } else {
                                                return "No_11";
                                            }
                                        } else {
                                            return "No_10";
                                        }
                                    } else {
                                        return "No_9";
                                    }
                                } else {
                                    return "No_8";
                                }
                            } else {
                                return "No_7";
                            }
                        } else {
                            return "No_6";
                        }
                    } else {
                        return "No_5";
                    }
                } else {
                    return "No_4";
                }
            } else {
                return "No_3";
            }
        } else {
            return "No_1";
        }
    }

    // Depth 15 -> Yes_15; No_1..No_15
    @PostMapping("/depth/15")
    @ResponseBody
    public String d15(@RequestParam String a,
                      @RequestParam String b,
                      @RequestParam String c,
                      @RequestParam String d,
                      @RequestParam int n1,
                      @RequestParam int n2,
                      @RequestParam int n3,
                      @RequestParam double t1,
                      @RequestParam double t2,
                      @RequestParam boolean z1,
                      @RequestParam boolean z2,
                      @RequestParam String opt,
                      @RequestParam long id,
                      @RequestBody ModeIdentifier mode,
                      @RequestParam String when) {
        if ("A-VERY-LONG-ALPHA-TOKEN-STRING-SECTION-ONE".equals(a)) {
            if (b.length() >= 20) {
                if (c.startsWith("C-START-LONG-PREFIX-SECTION-222222")) {
                    if (d.endsWith("-D-ENDING-LONG-SUFFIX-333333")) {
                        if (n1 + n2 > n3) {
                            if (t1 <= t2) {
                                if (z1 || z2) {
                                    if (opt.contains("ok-embedded-flag-LLLLL")) {
                                        if (id > 1000000L) {
                                            if (mode.checkAlpha()) {
                                                if ("2024-01-01".compareTo(when) <= 0) { // string date check
                                                    if (n3 % 2 == 0) {
                                                        if (Math.abs(t2 - t1) < 10.000000) {
                                                            if (!("BLOCKED-BLOCK-BLOCK".equalsIgnoreCase(b))) {
                                                                if (c.length() + d.length() > 40) {
                                                                    return "Yes_15";
                                                                } else {
                                                                    return "No_15";
                                                                }
                                                            } else {
                                                                return "No_14";
                                                            }
                                                        } else {
                                                            return "No_13";
                                                        }
                                                    } else {
                                                        return "No_12";
                                                    }
                                                } else {
                                                    return "No_11";
                                                }
                                            } else {
                                                return "No_10";
                                            }
                                        } else {
                                            return "No_9";
                                        }
                                    } else {
                                        return "No_8";
                                    }
                                } else {
                                    return "No_7";
                                }
                            } else {
                                return "No_6";
                            }
                        } else {
                            return "No_5";
                        }
                    } else {
                        return "No_4";
                    }
                } else {
                    return "No_3";
                }
            } else {
                return "No_2";
            }
        } else {
            return "No_1";
        }
    }

    // Depth 16 -> Yes_16; No_1..No_16
    @PostMapping("/depth/16")
    @ResponseBody
    public String d16(@RequestParam String s1,
                      @RequestParam String s2,
                      @RequestParam String s3,
                      @RequestParam int i1,
                      @RequestParam int i2,
                      @RequestParam int i3,
                      @RequestParam double d1,
                      @RequestParam double d2,
                      @RequestParam double d3,
                      @RequestParam boolean b1,
                      @RequestParam boolean b2,
                      @RequestParam boolean b3,
                      @RequestBody ModeIdentifier mode,
                      @RequestParam String start,
                      @RequestParam String end,
                      @RequestParam List<Integer> extras) {
        if (s1.length() >= 15) {
            if ("ok-message-very-long-acknowledgement-STRING".equalsIgnoreCase(s2)) {
                if (s3.contains(s1)) {
                    if (i1 < i2 && i2 < i3) {
                        if (d1 <= d2 && d2 <= d3) {
                            if (b1) {
                                if (!b2 || b3) {
                                    if (!mode.checkBeta()) {
                                        if (start.compareTo("2019-12-31") > 0) {
                                            if (end.compareTo(start) >= 0) {
                                                if (extras.size() <= 5) {
                                                    if (!extras.contains(-123456)) {
                                                        if ((i3 - i1) <= 1000000) {
                                                            if ((d3 - d1) <= 1000.123456) {
                                                                if (s3.length() <= 128) {
                                                                    if (s1.charAt(0) != ' ') {
                                                                        return "Yes_16";
                                                                    } else {
                                                                        return "No_16";
                                                                    }
                                                                } else {
                                                                    return "No_15";
                                                                }
                                                            } else {
                                                                return "No_14";
                                                            }
                                                        } else {
                                                            return "No_13";
                                                        }
                                                    } else {
                                                        return "No_12";
                                                    }
                                                } else {
                                                    return "No_11";
                                                }
                                            } else {
                                                return "No_10";
                                            }
                                        } else {
                                            return "No_9";
                                        }
                                    } else {
                                        return "No_8";
                                    }
                                } else {
                                    return "No_7";
                                }
                            } else {
                                return "No_6";
                            }
                        } else {
                            return "No_5";
                        }
                    } else {
                        return "No_4";
                    }
                } else {
                    return "No_3";
                }
            } else {
                return "No_2";
            }
        } else {
            return "No_1";
        }
    }
}

