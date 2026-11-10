# Filtery

A fun camera app for Android with **174 filters that all work on the phone itself, with no internet**.
Built the same way as Obdy: Kotlin + Compose, built on GitHub Actions, updated from GitHub Releases.

```
camera (CameraX) -> face finder (ML Kit, model inside the app) -> filter engine (plain Android graphics) -> screen / photo / video
```

## What it does
- **Live filters** in the camera preview; the same filter is applied again at full size when you take the photo.
- **Nearly full-screen picture** (9:16 like your screen); photos and videos are saved in the same shape you see.
- **Front and back camera** (flip button). Pinch the preview to zoom.
- **Photo and video clips** (up to 60 seconds, with sound). The filter is recorded into the video.
- **Strength slider** for every filter (one filter at a time).
- **Funny first**: the filter strip starts with the funny categories. The dice button picks a random filter (funny ones are three times as likely).
- **Favorites** (long-press a filter or press the star) and **Recent** filters.
- **Timer** (off / 3 s / 10 s), **grid lines**, **flash** (off / auto / on; on the front camera the screen flashes white), **caption** text (the Aa button).
- **Saved twice**: in Filtery's own gallery (inside the app) and, if you leave it on, in the phone gallery (Pictures/Filtery and Movies/Filtery).
- **Share** from the gallery viewer (WhatsApp, Instagram, ...).
- **Gear button** (top-right) holds all settings: mirror selfies, photo quality, preview quality, saving, sound, updates.
- **Updates from GitHub** like Obdy: checked when you open the app, downloaded once (Wi-Fi only by default), kept until you press Install. A red dot on the gear shows when one is ready.

## The filters
**Funny faces (65)**: drawn props that follow your face and are matched to your photo's light and colour, plus "real" ones made only from your own pixels (a piece of your face copied somewhere else): Party Hat, Top Hat, King Crown, Cool Shades, Nerd Glasses, Deal With It, Heart Eyes, Clown, Puppy, Kitty, Bunny, Little Devil, Angel, Mustache, Full Beard, Pirate, Reindeer, Flower Crown, Rainbow Mouth, Happy Tears, Fire Eyes, Laser Eyes, Dizzy Stars, Vampire, Hero Mask, Santa, Third Eye, Tongue Out, Big Hair, Viking, Cowboy, Wizard, Chef, Tiger Paint, Freckles & Blush, Butterflies, DJ Headphones, Teddy Bear, Panda, Fox, Lion Mane, Unicorn, Heart Crown, Sparkle Crown, Anime Eyes, Glam Makeup, Glitter Face, Neon Glasses, Robot, Skeleton, Witch, Bubblegum, Nervous Sweat, Angry, Boxer, Crying Rivers, Cupid, Ice Queen, Real 3rd Eye, Eyes on Cheeks, Mouth on Forehead, Cyclops, Two Noses, Mini Me, Upside-Down Twin

**Funny warps (44)** (real pixels of your face, bent; some animate): Big Head, Pin Head, Big Eyes, Bug Eyes, Tiny Eyes, Big Nose, Big Mouth, Tiny Mouth, Chipmunk, Alien Head, Long Chin, Cone Head, Wide Face, Tall & Skinny, Short & Wide, Swirl, Fisheye, Squeeze, Funhouse, Ripple, Melt, Baby Face, Big Smile, Surprised, Frog Eyes, Big Forehead, Long Nose, Bobblehead, Elf Ears, Wonky Eyes, Pancake Head, Sausage Head, Duck Face, Sad Face, Sleepy Eyes, Huge Lips, Wide-Set Eyes, Cross-Eyed, Double Swirl, Heartbeat Head, Jello Face, Rubber Face, Mega Mouth, Pointy Chin

**Fun effects (24)**: Cartoon, Comic Book, Glitch, Old VHS, Pixelate, Censored, Heat Vision, Night Vision, X-Ray, Kaleidoscope, Mirror Left, Mirror Right, Mirror Top, Mirror x4, Ghost Twins, Zoom Burst, Rainbow Cycle, Disco, Negative, Drunk, Old Age, Zombie, Beauty Smooth, Movie Glow

**Colors (16 + Original)**: Black & White, Noir, Sepia, Vintage, Retro 70s, Instant, Warm, Cool, Golden Hour, Teal & Orange, Cinematic, Vivid, Faded, Cross Process, Moody, Cyber Neon

**Art (12)**: Pencil Sketch, Color Pencil, Ink, Blueprint, Neon Edges, Oil Paint, Watercolor, Pop Art, Halftone, Emboss, Dream Glow, Duotone

**Frames and overlays (13)**: Polaroid, Film Strip, Soft Frame, Neon Frame, Heart Frame, Date Stamp, Cinema Bars, LOL Banner, Snowfall, Confetti, Sparkles, Hearts Rising, Bubbles

Face filters need a face in view; the preview says "Show your face" until one is found. Warps use an imaginary face in the middle when none is found.

## Real-photo props (for a realistic look)
Props drawn by code can never look like a real photo. For real ones, put transparent PNG pictures of real hats, beards, glasses... in
`app/src/main/assets/stickers/` and rebuild: each file becomes a face filter automatically and is placed on the face by its name
(`hat_wizard.png`, `beard_white.png`, `glasses_aviator.png`, `mustache_x.png`, `ears_x.png`, `crown_x.png`, `mask_x.png`, `nose_x.png`, `mouth_x.png`, `face_x.png`).
See the README.txt in that folder.

## Build it (same as Obdy)
1. Unzip so that `settings.gradle.kts` and the `app` folder sit next to `Rebuild-APK.bat`.
2. Double-click **Rebuild-APK.bat** (first time: it installs Git + GitHub CLI and asks you to sign in once). It creates the public repo `Filtery`, builds on GitHub and downloads the APK into the `APK` folder.
3. **Publish-Update.bat** publishes a Release; the app then offers it under Settings > Updates.

If a build fails, `build-error.txt` opens: send its content to Claude.

## Notes
- Everything is drawn with plain Android graphics (the same code for preview, photo and video), so it works on any phone with Android 8+. Heavy filters (Oil Paint, Kaleidoscope) can be a little slower: use Settings > Live preview quality > Fast.
- Photos are made from the full camera picture (about 3 MP, or up to 12 MP in High quality). Video is recorded at the live preview size (Fast 480x640, Balanced 576x768, Sharp 720x960).
- The face finder is Google ML Kit with its model bundled inside the APK, so it needs no internet and no Google Play download.
- The app is portrait-only.
