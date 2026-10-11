# Clock face refinement review

[Browse the visual gallery](face-gallery.md).

## Coverage

Classic was excluded. The seven TV faces were captured before refinements in eight states and recaptured afterward in ten states, in English, Tamil, and Sinhala. Each face uses its recommended palette. The final TV matrix contains 210 screenshots.

Screens: digital, analog, azan under one hour, iqamah waiting, full azan countdown, full iqamah countdown, Friday-night bayan countdown, Friday bayan, dua, and next-day Fajr.

## Refinements and screenshot galleries

| Face | Refinements | English | Tamil | Sinhala |
| --- | --- | --- | --- | --- |
| Horizon | Centered timers; restored inline countdown; correct iqamah target; larger dial numerals. | [English](docs/face-gallery/Horizon-Slate-Steel-EN.png) | [Tamil](docs/face-gallery/Horizon-Slate-Steel-TA.png) | [Sinhala](docs/face-gallery/Horizon-Slate-Steel-SI.png) |
| Ufuq | Stronger dial ticks and numerals; correct Friday-night event labels; weather icon, temperature, and detail icons grouped in one compact block. | [English](docs/face-gallery/Ufuq-Horizon-Dusk-EN.png) | [Tamil](docs/face-gallery/Ufuq-Horizon-Dusk-TA.png) | [Sinhala](docs/face-gallery/Ufuq-Horizon-Dusk-SI.png) |
| Safaa | Readable Roman numerals and hands; separate azan/iqamah columns; spaced waiting rail with iqamah details. | [English](docs/face-gallery/Safaa-Notice-Paper-EN.png) | [Tamil](docs/face-gallery/Safaa-Notice-Paper-TA.png) | [Sinhala](docs/face-gallery/Safaa-Notice-Paper-SI.png) |
| Noor | Brighter secondary text; schedule edge clearance; next-prayer AM/PM and iqamah; localized seconds label. | [English](docs/face-gallery/Noor-Amber-Matrix-EN.png) | [Tamil](docs/face-gallery/Noor-Amber-Matrix-TA.png) | [Sinhala](docs/face-gallery/Noor-Amber-Matrix-SI.png) |
| Sahar | Approved analog dial retained; centered dua reminder and correct Friday-night title. | [English](docs/face-gallery/Sahar-Daylight-Ivory-EN.png) | [Tamil](docs/face-gallery/Sahar-Daylight-Ivory-TA.png) | [Sinhala](docs/face-gallery/Sahar-Daylight-Ivory-SI.png) |
| Kaash | Arabic font and transliteration for dua; balanced meaning and sermon reminder sizes. | [English](docs/face-gallery/Kaash-Ink-Saffron-EN.png) | [Tamil](docs/face-gallery/Kaash-Ink-Saffron-TA.png) | [Sinhala](docs/face-gallery/Kaash-Ink-Saffron-SI.png) |
| Mihrab | Clearer dial numerals and ticks; spaced prayer band; centered countdown ornaments; analog AM/PM. | [English](docs/face-gallery/Mihrab-Sand-Terracotta-EN.png) | [Tamil](docs/face-gallery/Mihrab-Sand-Terracotta-TA.png) | [Sinhala](docs/face-gallery/Mihrab-Sand-Terracotta-SI.png) |

## Full-resolution captures

Open [the final TV screenshot browser](../build/face-review-after/index.html) to inspect individual screens. [Baseline captures](../build/face-review-before/index.html) are also available locally.

## Horizon Mobile

The portrait face was also captured in all ten states and three languages at 720 × 1280. Its weather section has more vertical space and its full-screen timers use portrait sizing. Open [the mobile screenshot browser](../build/face-review-mobile-after/index.html). Horizon Mobile uses a digital dashboard in both display-mode fixtures.

| Language | Main screens | Extra countdowns |
| --- | --- | --- |
| English | [Gallery](docs/docs/face-gallery/Horizon-Mobile-English.jpg) | [Countdowns](docs/docs/face-gallery/Horizon-Mobile-English-Countdowns.jpg) |
| Tamil | [Gallery](docs/docs/face-gallery/Horizon-Mobile-Tamil.jpg) | [Countdowns](docs/docs/face-gallery/Horizon-Mobile-Tamil-Countdowns.jpg) |
| Sinhala | [Gallery](docs/docs/face-gallery/Horizon-Mobile-Sinhala.jpg) | [Countdowns](docs/docs/face-gallery/Horizon-Mobile-Sinhala-Countdowns.jpg) |

## Build

The Play Store debug variant compiled successfully with JDK 17 and Kotlin incremental compilation disabled. The review uses deterministic preview times and fixture weather; it evaluates layout and text rendering rather than live prayer calculations.
