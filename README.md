# HKMC Charge Port Button

A [NEMESIS](https://github.com/xwtk/hmg_nemesis) package for Hyundai / Kia / Genesis **Gen5W** head units that lets you **open the charge-port door with a triple-press of the MAP button** — only while parked.

Reuse a button you already have: a single MAP press still opens the map as normal; a quick **3× press** pops the charge door.

> [!WARNING]
> Unofficial community mod, not affiliated with Hyundai/Kia/Genesis/HKMC. Requires [NEMESIS](https://github.com/xwtk/hmg_nemesis) (root) installed on your head unit. **Test while parked.** Confirmed on one vehicle/firmware (see below) — re-verify on yours.

## Features

- **Triple-press MAP → open charge port** (configurable guardrails).
- **Park-gated** — only fires when the vehicle is in Park (toggleable).
- **Non-destructive** — a normal single MAP press is unaffected.
- **Settings app** (launched from the NEMESIS Manager): Automation on/off, Only-in-Park on/off, plus **Open charge port now** and **Check Park status** test buttons.

## Compatibility

- Android-based **Gen5W** (`S5W_M`) head unit with [NEMESIS](https://github.com/xwtk/hmg_nemesis).
- **Confirmed on:** Hyundai Ioniq 5, US firmware `NEEV.USA.0000.V015.012.260507`.
- Other Gen5W HKMC vehicles may work; the intent/service codes come from the shared framework but should be re-verified. In particular the **MAP button must broadcast** `com.hkmc.intent.action.MAP_SHORTCUT` on your unit (most do) — see [how it works](#how-it-works).

## Install

### USB
1. Download `hkmc-chargeport-button-USB.zip` from the [latest release](../../releases/latest).
2. Extract to the root of a FAT32/exFAT USB drive (`NEMESIS.data` + `packages/chargeport.button.zip`).
3. Insert into the parked car and approve.

### CLI
```sh
adb push chargeport.button.zip /data/local/tmp/
adb shell nemesis install /data/local/tmp/chargeport.button.zip
```

## Using it

Open NEMESIS Manager → **Packages** → **Charge Port Button** → **Launch** for settings and test buttons.

- Put the car in **Park**, then **press MAP three times quickly** (within ~1.5s between presses). The charge door opens, with a toast.
- Toggle **Only in Park** off if you want it to work in any gear (not recommended).
- **Check Park status** shows the raw gear reading so you can confirm the Park gate on your vehicle.

## How it works

Hardware buttons on this unit broadcast shortcut intents. The MAP button sends
`com.hkmc.intent.action.MAP_SHORTCUT`, which a manifest `BroadcastReceiver`
catches (even when MAP switches the screen to the map). The receiver counts
presses; three within the window triggers:

| Step | Call |
|------|------|
| Check Park | `service call com.hkmc.micom.system 36` (`getGearPStatus`; `1` = Park) |
| Open door | `service call com.hkmc.micom.bcm 84 i32 0` (`requestChargeLockDoor(false)`) |

The app issues these as root through the `nemesis root` channel, so it needs no
OEM signature.

**Why MAP, and notes for other buttons:** not every button broadcasts a
catchable intent — e.g. the star/custom button does not (it appears to use a
targeted broadcast). Hardware buttons also show up as raw keycodes on
`/dev/input/event1` (not `event2`), which is a viable fallback detector, but
note that `getevent` streaming does **not** work in the NEMESIS script context
(reading the device raw with `cat` does). To find a button that broadcasts on
your unit, a small "sniffer" receiver that listens for all known shortcut
actions makes it obvious which ones are catchable.

## Build from source

```sh
export ANDROID_SDK=/path/to/android-sdk     # build-tools/30.0.3 + platforms/android-30
tools/build_apk.sh
cp app/chargeport.apk package/app/chargeport.apk
tools/make_package.sh
```

Uses the legacy `dx` dexer (DEX `035` for KitKat; also dodges a `d8` crash on
very new JDKs). The APK is self-signed; NEMESIS installs non-OEM-signed APKs.

## Credits

- Built on [NEMESIS](https://github.com/xwtk/hmg_nemesis) by Gor Mirzoyan (xwtk).
- Companion project: [hkmc-coldstart-heat](https://github.com/eclipseempire/hkmc-coldstart-heat) (auto heated wheel + seats on cold start), whose `EXTENDING.md` documents the reverse-engineering methodology and a verified service-call reference.

## License

[MIT](LICENSE) — no warranty. If you confirm it on another Gen5W model/firmware, please open an issue or PR.
