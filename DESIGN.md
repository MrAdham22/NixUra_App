---
name: NixUra
description: A dark, warm habit app where progress turns night into dawn.
colors:
  navy: "#1D1A39"
  purple: "#451952"
  plum: "#862549"
  rose: "#AE445A"
  orange: "#F39F5A"
  blush: "#E8BCB9"
  surface: "#262248"
  surface-raised: "#312C5A"
  border: "rgba(255, 255, 255, 0.12)"
  text-primary: "#F6EFEE"
  text-secondary: "#D6CCD6"
  text-muted: "#9B94B0"
  on-accent: "#FFF7F5"
  positive: "#4CC38A"
  danger: "#FF6B6B"
  water: "#5BB6EC"
  water-deep: "#2B64B0"
  light-background: "#FBF8F6"
  light-surface: "#FFFFFF"
  light-surface-raised: "#F4EEEC"
  light-text-secondary: "#4A4560"
  light-text-muted: "#827E88"
  light-accent-warm: "#D9772B"
  light-positive: "#2F9E6B"
  light-danger: "#B3261E"
  prayer-gold-light: "#FBE9B0"
  prayer-gold: "#E3B24C"
  prayer-gold-deep: "#B07D1E"
  prayer-bronze: "#5A3D0C"
  prayer-night: "#1B1530"
  prayer-night-plum: "#2E1D40"
  prayer-cream: "#F8E7B6"
typography:
  display:
    fontFamily: "System default (Roboto on Android, SF Pro on iOS)"
    fontSize: "34sp"
    fontWeight: 700
    lineHeight: "40sp"
    letterSpacing: "-0.8sp"
  title:
    fontFamily: "System default"
    fontSize: "28sp"
    fontWeight: 700
    lineHeight: "34sp"
    letterSpacing: "-0.5sp"
  heading:
    fontFamily: "System default"
    fontSize: "17sp"
    fontWeight: 600
    lineHeight: "22sp"
    letterSpacing: "-0.2sp"
  body:
    fontFamily: "System default"
    fontSize: "15sp"
    fontWeight: 400
    lineHeight: "21sp"
    letterSpacing: "-0.1sp"
  body-strong:
    fontFamily: "System default"
    fontSize: "15sp"
    fontWeight: 600
    lineHeight: "21sp"
    letterSpacing: "-0.1sp"
  small:
    fontFamily: "System default"
    fontSize: "13sp"
    fontWeight: 400
    lineHeight: "18sp"
  small-strong:
    fontFamily: "System default"
    fontSize: "13sp"
    fontWeight: 600
    lineHeight: "18sp"
  smaller:
    fontFamily: "System default"
    fontSize: "11sp"
    fontWeight: 400
    lineHeight: "14sp"
  eyebrow:
    fontFamily: "System default"
    fontSize: "11sp"
    fontWeight: 600
    lineHeight: "14sp"
    letterSpacing: "1sp"
  button:
    fontFamily: "System default"
    fontSize: "15sp"
    fontWeight: 600
    lineHeight: "20sp"
  counter:
    fontFamily: "System default"
    fontSize: "variable (size x 1.1 line height)"
    fontWeight: 700
    letterSpacing: "-0.5sp"
    fontFeature: "tnum"
rounded:
  xs: "8dp"
  sm: "12dp"
  field: "14dp"
  button: "16dp"
  md: "16dp"
  nav-item: "18dp"
  lg: "20dp"
  card: "22dp"
  nav: "24dp"
  sheet: "28dp"
  pill: "50%"
spacing:
  hairline: "3dp"
  xs: "6dp"
  sm: "8dp"
  md: "12dp"
  field: "14dp"
  lg: "18dp"
  gutter: "20dp"
  sheet: "22dp"
  xl: "24dp"
components:
  button-primary:
    backgroundColor: "{colors.rose}"
    textColor: "{colors.on-accent}"
    typography: "{typography.button}"
    rounded: "{rounded.button}"
    padding: "8dp 20dp"
    height: "50dp"
  button-secondary:
    backgroundColor: "{colors.surface-raised}"
    textColor: "{colors.text-primary}"
    typography: "{typography.button}"
    rounded: "{rounded.button}"
    padding: "8dp 20dp"
    height: "50dp"
  button-ghost:
    textColor: "{colors.text-secondary}"
    typography: "{typography.button}"
    rounded: "{rounded.button}"
    padding: "8dp 20dp"
    height: "50dp"
  button-danger:
    textColor: "{colors.danger}"
    typography: "{typography.button}"
    rounded: "{rounded.button}"
    padding: "8dp 20dp"
    height: "50dp"
  icon-button:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.text-primary}"
    rounded: "{rounded.pill}"
    size: "42dp"
  chip:
    backgroundColor: "{colors.surface-raised}"
    textColor: "{colors.text-secondary}"
    typography: "{typography.small-strong}"
    rounded: "{rounded.pill}"
    padding: "9dp 14dp"
  chip-selected:
    backgroundColor: "{colors.rose}"
    textColor: "{colors.on-accent}"
    typography: "{typography.small-strong}"
    rounded: "{rounded.pill}"
    padding: "9dp 14dp"
  card:
    backgroundColor: "{colors.surface}"
    rounded: "{rounded.card}"
    padding: "18dp"
  text-field:
    backgroundColor: "{colors.surface-raised}"
    textColor: "{colors.text-primary}"
    typography: "{typography.body}"
    rounded: "{rounded.field}"
    padding: "14dp 16dp"
  nav-bar:
    backgroundColor: "{colors.surface}"
    rounded: "{rounded.nav}"
    padding: "6dp"
  sheet:
    backgroundColor: "{colors.surface}"
    rounded: "{rounded.sheet}"
    padding: "22dp"
---

# Design System: NixUra

## Overview

**Creative North Star: "Night Into Dawn"**

NixUra starts in the dark and lets progress bring the light. The app lives on a deep navy night (`navy`). Every quit-habit card begins in the same darkness and warms through purple, plum and rose to orange and blush as the streak grows, so a month of holding out literally looks like sunrise. Light is earned, never decoration: the brighter something is, the more it means.

The interface is playful and lively on top of that calm night (owner direction, 2026-11-14). Things respond to the finger: they dip and spring back, liquid rises in a good-habit card with a splash, the nav bar's droplet stretches between tabs, a reached target bounces with a firm tap, and a counted prayer sends a burst of gold stars. Motion celebrates the user's own actions and progress. It always honours "remove animations".

Density is comfortable, not sparse: one column, generous 20dp gutters, cards that carry real numbers (a live counter ticking every second, a calendar, a 7-day row) rather than empty space. One NixUra look runs on Android and iPhone alike. Only native details change (safe areas, back gestures, status bar).

**Key Characteristics:**
- Dark navy night as the default world. Light theme is the alternative, not the reference.
- Brightness is earned: streak cards move from dark to light (0/1/3/7/14/30 days).
- Rose (`rose`) is the single action colour. Orange is warmth, blush is softness.
- Playful, physical motion on touch and on achievements, with a reduced-motion path for every effect.
- Soft, rounded forms everywhere (16 to 28dp corners, pills for chips and switches).
- Everything is drawn in code (liquid, droplet, stars, mosque). No image files for UI art.

## Colors

A six-colour night-to-dawn ramp on navy, with rose doing the work and a few documented exceptions (green, red, water blue, prayer gold) for meaning the ramp can't carry.

### Primary
- **Dawn Rose** (`rose`): the action colour in dark theme. Primary buttons, selected chips, switch tracks, progress bars, the active nav icon, eyebrow labels and the text cursor. In light theme the same role moves to **Deep Plum** (`plum`) for contrast.

### Secondary
- **Sunrise Orange** (`orange`): warmth. The second stop of the avatar gradient and warm accents (`accentWarm`). Light theme uses the deeper **Burnt Orange** (`light-accent-warm`).
- **First-Light Blush** (`blush`): the softest, lightest point of the ramp. The last streak stop, soft accents and text on Material containers.

### Tertiary
- **Midnight Purple** (`purple`) and **Deep Plum** (`plum`): the middle of the streak ramp, and Material container colours in dark theme.

### Neutral
- **Night Navy** (`navy`): the dark-theme background, and primary text in light theme.
- **Dusk Surface** (`surface`): cards, icon buttons, the nav bar, sheets.
- **Raised Dusk** (`surface-raised`): fields, secondary buttons, unselected chips, progress tracks.
- **Starlight Hairline** (`border`, white at 12%): the 1dp border on almost every surface.
- **Moon White** (`text-primary`), **Haze** (`text-secondary`), **Fog** (`text-muted`): the three text levels on navy.
- Light theme: **Morning Paper** (`light-background`), white surfaces (`light-surface`), **Warm Linen** (`light-surface-raised`), with navy text and a navy hairline at 12%.

### Meaning colours (exceptions to the six-colour set)
- **Done Green** (`positive`, `light-positive`): good-habit progress and done days. Its soft form is the same green at 16 to 18%.
- **Alert Red** (`danger`, `light-danger`): errors and destructive buttons only.
- **Water Blue** (`water` to `water-deep`): the liquid inside water habits.
- **Prayer Gold** (`prayer-*`): only the prayer habit's card, buttons and gold wash: a night sky, golden dawn and cream text.

### Named Rules
**The Earned Light Rule.** On a habit card, brightness is earned by streak time. Never start a new card light, and never brighten a card for decoration. The colour is worked out from the active streak (`StreakColorMapper` or the user's `ShadeRamp`), never stored.

**The One Action Colour Rule.** Rose (plum in light theme) is the only colour for "do this". Orange, blush, green and gold never mark a primary action.

**The Not Colour Alone Rule.** Every colour that carries meaning (streak stage, done day, error) also has text or a marker next to it.

## Typography

**Display Font:** the system font (Roboto on Android, SF Pro on iOS). No custom font is bundled.
**Counter numbers:** the same font, bold, with tabular figures (`tnum`) so a ticking counter never jumps.

**Character:** plain, friendly system type, tightened with negative letter spacing at large sizes so headings feel compact and confident. The personality comes from colour and motion, not from the typeface.

### Hierarchy
- **Display** (700, 34sp, 40sp line): the biggest moments, such as a hero number or a welcome line.
- **Title** (700, 28sp, 34sp line): screen titles under the eyebrow in `ScreenHeader`.
- **Heading** (600, 17sp, 22sp line): card and section titles.
- **Body / Body Strong** (400 or 600, 15sp, 21sp line): running text, field text, list rows.
- **Small / Small Strong** (400 or 600, 13sp, 18sp line): helper text, field labels, chip labels.
- **Smaller** (400, 11sp, 14sp line): fine print, nav labels.
- **Eyebrow** (600, 11sp, 1sp tracking, UPPERCASE): the label above a screen title (in rose) and section labels (in muted text).
- **Button** (600, 15sp, 20sp line): all button labels.
- **Counter** (700, size set per use, 1.1 line height, tabular): the live day/hour/minute/second blocks. Digits shrink to fit, so seconds are never cut off.

### Named Rules
**The Steady Digits Rule.** Any number that changes while the user watches uses the counter style with tabular figures.

**The Eyebrow Rule.** A screen title always sits under an uppercase eyebrow. The eyebrow is rose on a screen header and muted on a section.

## Layout

One column on a phone. Tab screens use a 20dp side gutter (`gutter`) and 8dp top padding, and stack their blocks 18dp apart (`lg`), with 16dp of extra space at the end. Sub-screens use the same 20dp gutter with a 10dp header row, a back button, and 24dp at the end. Cards pad 18dp inside (habit cards 20dp, with 12dp between their rows). Sheets pad 22dp. Buttons are at least 50dp tall and grow when a label wraps, so labels are never cut off. Icon buttons are 42dp, nav items at least 56dp.

Screens respect the safe area on both phones (`WindowInsets.safeDrawing`). The coach is full-screen with its own compact header: no top bar, no bottom nav. Tab screens show the logo and "NixUra" on the left of the top bar and the profile avatar on the right, with the floating droplet nav bar at the bottom.

## Elevation & Depth

**Today (in the code):** depth comes from colour layers, not shadows. Navy background, then Dusk Surface cards, then Raised Dusk fields and panels, each edged with a 1dp hairline. Habit cards add their own light: a diagonal gradient plus a soft glow in the top corner, so they never read as flat blocks. The only real shadow is Material's on the bottom sheet.

**Direction (owner, 2026-11-14): add soft shadows.** Cards should lift a little off the night with gentle, low-contrast shadows. On navy a black shadow barely shows, so a lifted card should pair a soft dark shadow with its hairline and, on streak cards, a faint glow tinted by the card's own colour. Exact shadow values are not set yet. They are decided the first time a screen ships with them, then recorded here. Provisional values are in `.impeccable/design.json`.

### Named Rules
**The Lit From Within Rule.** Habit cards get their depth from their own light (gradient and corner glow) first. A shadow only supports it; it never replaces it.

**The Same Depth Rule.** Things at the same level share the same shadow. Don't give one card in a list more lift than its neighbours unless it is being touched or dragged.

## Shapes

Soft and rounded throughout. Corners grow with the size of the surface: fields 14dp, buttons and medium surfaces 16dp, nav items 18dp, cards 22dp, the nav bar 24dp, sheets 28dp on their top corners. Chips, switches, progress bars and icon buttons are full pills or circles. Almost every surface has a 1dp hairline border.

Signature shapes are drawn in code: the stretching droplet of the nav bar, the sine-topped liquid of water and good-habit cards, and on the prayer card an eight-pointed star tiling, a mosque silhouette, a crescent and a mihrab arch badge.

## Components

### Buttons
Playful and physical: they dip under the finger and spring back, with no ripple.
- **Shape:** gently rounded (16dp), at least 50dp tall, 20dp side padding, optional 18dp icon 8dp before the label.
- **Primary:** rose with near-white text. **Secondary:** Raised Dusk with a hairline. **Ghost:** transparent with Haze text. **Danger:** red at 14% with a red hairline at 35% and red text.
- **Press:** scales to 0.97 and dims to 82% on a spring (damping 0.65, medium stiffness), then springs back. Disabled: background at 45%, label at 50%. Loading: an 18dp spinner replaces the label.

### Icon buttons
- Circles of 42dp on Dusk Surface with a hairline. The icon is half the button's size. Press scales to 0.9, and some give a light haptic tap.

### Chips
- **Style:** pills, 14dp by 9dp padding, Small Strong label, an optional 10dp colour dot.
- **State:** unselected is Raised Dusk with a hairline and Haze text. Selected is rose with near-white text and no border. The colour fades in 150ms. Press scales to 0.94.

### Cards / Containers
- **Corner Style:** 22dp.
- **Background:** Dusk Surface, or a custom painting (streak gradient, liquid fill, prayer scene) inside the same shape.
- **Shadow Strategy:** see Elevation & Depth.
- **Border:** 1dp hairline.
- **Internal Padding:** 18dp (habit cards 20dp). A tappable card scales to 0.98 on press, and a long press opens its options with a firm haptic.

### Inputs / Fields
- **Style:** Raised Dusk, a hairline, 14dp corners, 16dp by 14dp padding, Body text. The label sits above in Small Strong Haze, 6dp up. The placeholder is in Fog.
- **Focus:** a rose cursor. There is no focus ring yet.
- **Switch:** a 48 by 28dp pill track, rose when on, with a 24dp thumb that slides in 160ms.

### Navigation
- **Droplet nav bar:** a floating pill (24dp corners, Dusk Surface, hairline, 6dp padding) with 12dp outside margin. Items are at least 56dp tall with a 22dp icon over a small label. Muted by default, rose when selected. A liquid droplet follows the finger while the pages are dragged, stretches toward the new tab and settles with a small overshoot. With reduced motion it simply jumps.
- **Top bar:** logo and "NixUra" on the left, avatar on the right. The avatar is a circle with a rose-to-orange gradient.
- **Sheets:** bottom sheets with 28dp top corners and 22dp padding.

### Quit-habit card (signature)
The heart of the app. A card painted with a diagonal gradient and a corner glow, coloured by the active streak (brand ramp or the user's chosen shade, starting at about 30% brightness and ending light). It holds a live counter in day/hour/minute/second blocks ticking every second (unit labels at the 11sp eyebrow size) and a stage marker: reached stages as filled dots, the rest as outlined dots, then the next milestone and the time left ("Next: 1 week · 3d 20h"). While the current run is the record it says "Your longest streak yet" (flame icon) instead of repeating the time. Top right is a ⋯ button (40dp circle, ink at 10%) that opens the card's options; at the bottom a pill "Craving? Talk it through" (ink at 14% with a 22% hairline, sparkle icon as on the Coach tab, at least 44dp tall). Text is warm near-white (#FFF7F5) or navy, whichever has more contrast on the card colour; muted text is that ink at 86% or 80%. Corners are 26dp.

### Good-habit liquid card (signature)
The card fills with liquid as today's target is reached: green for plain habits, rising water blue for water habits. Each change rises or drains with a short splash, a gentle wave moves while it is partly full, and reaching full plays a light sweep and a small bounce with a firm haptic. With reduced motion the level just jumps. A 7-day row and calendar squares fill up to solid as days are done.

### Prayer card (signature)
A night-sky card with an eight-pointed star tiling, a golden dawn, a mosque and a crescent, edged with a double gold frame. A diagonal band of light slides across it. Each counted prayer sends a burst of gold stars, and the card turns solid gold when every prayer of the day is done. Star-shaped buttons: gold and slowly turning for "+", a quiet outline for "-".

## Do's and Don'ts

### Do:
- **Do** use the six brand colours and the semantic tokens in `NixColors.kt`. Add a new colour only as a documented meaning colour.
- **Do** keep rose (plum in light theme) as the one action colour.
- **Do** make every tappable thing respond with the `pressable` dip-and-spring, not a ripple.
- **Do** celebrate the user's own progress with motion (fills, bounces, bursts, the droplet) and give each effect a reduced-motion version.
- **Do** use tabular counter digits for anything that ticks.
- **Do** put text or a marker next to every colour that carries meaning.
- **Do** keep corners soft (14 to 28dp) and edge surfaces with the 1dp hairline.
- **Do** draw UI art in code so it looks the same on Android, iOS and the widgets.

### Don't:
- **Don't** start a habit card bright, or brighten one for decoration. Light is earned by streak time.
- **Don't** use orange, blush, green or gold for a primary action.
- **Don't** use prayer gold anywhere outside the prayer habit.
- **Don't** use red for anything except errors and destructive actions.
- **Don't** add motion that plays on its own without a reason. Motion answers a touch or marks progress (the prayer card's sliding light is the one ambient effect).
- **Don't** let one look split by platform: no Material-only or Apple-only styling beyond safe areas, back gestures and the status bar.
- **Don't** hard-code text in screens. It goes through `Strings.kt`.
