#!/usr/bin/env python3
"""ADB UI smoke for AV entry, recreation, FIFO refusals and existing navigation.

Requires the current local_mocks APK on an unlocked Android 31+ runtime. It
force-stops the wallet without clearing data. API33+ runs English and Estonian
and restores the original per-app locale and night mode in finally. API31/32
lack per-app locale shell commands: configure the app/system language yourself,
then run separately with --legacy-locale en and --legacy-locale et. Neither pass
alone establishes both locales. No external issuance or AVSUA is performed.
"""

import argparse
import json
from pathlib import Path
import re
import shlex
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from urllib.parse import quote


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--adb", default="adb", help="adb executable")
    parser.add_argument("--serial", help="runtime serial (required with multiple connected runtimes)")
    parser.add_argument("--package", default="ee.tomkabel.walletpoc.debug", help="installed local_mocks application ID")
    parser.add_argument("--timeout", type=float, default=35, help="UI assertion timeout in seconds")
    parser.add_argument("--legacy-locale", choices=("en", "et"), help="API31/32 only: app language already configured; run both passes separately")
    args = parser.parse_args()
    adb = [args.adb] + (["-s", args.serial] if args.serial else [])
    component = args.package + "/ee.cyber.wallet.MainActivity"
    resource_root = Path(__file__).resolve().parents[1] / "app/src/main/res"
    strings = {
        locale: {node.get("name"): node.text or "" for node in ET.parse(resource_root / directory / "strings.xml").getroot()}
        for locale, directory in (("en", "values"), ("et", "values-et"))
    }
    locale = "en"

    def message(name, value=None):
        text = strings[locale].get(name, strings["en"].get(name))
        return text if value is None else text.replace("%1$s", str(value)).replace("%1$d", str(value))

    def run(*parts):
        return subprocess.run(adb + list(parts), text=True, capture_output=True, check=True, timeout=45).stdout

    def shell(*parts):
        return run("shell", shlex.join(parts))

    def ui():
        shell("uiautomator", "dump", "/sdcard/av-vci-window.xml")
        return ET.fromstring(shell("cat", "/sdcard/av-vci-window.xml"))

    def wait_condition(predicate, description, tree_predicate=lambda _tree: True):
        deadline = time.monotonic() + args.timeout
        last_text = ""
        while time.monotonic() < deadline:
            try:
                tree = ui()
                last_text = " ".join(node.get("text", "") for node in tree.iter("node"))
                if predicate(last_text) and tree_predicate(tree):
                    return tree
            except (ET.ParseError, subprocess.SubprocessError):
                pass
            time.sleep(0.4)
        raise AssertionError(f"UI did not satisfy {description}; last visible text: {last_text!r}")

    def wait_text(*expected):
        return wait_condition(lambda text: all(item in text for item in expected), repr(expected))

    def wait_absent(*forbidden):
        return wait_condition(lambda text: all(item not in text for item in forbidden), f"absence of {forbidden!r}")

    def activity_instance():
        output = shell("dumpsys", "activity", component)
        match = re.search(r"Local Activity ([0-9a-f]+) State:", output)
        return match.group(1) if match else None

    def click(text):
        tree = wait_condition(lambda _text: True, f"exact button {text!r}",
                              lambda tree: any(node.get("text") == text for node in tree.iter("node")))
        for node in tree.iter("node"):
            if node.get("text") == text:
                x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds", "")))
                shell("input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))
                return
        raise AssertionError(f"No exact button: {text}")

    def dismiss(button, previous, next_expected=None):
        click(message(button))
        wait_absent(previous)
        if next_expected:
            wait_text(*next_expected)
        else:
            wait_absent(message("av_invocation_title"), message("invocation_refused_title"))

    def invoke(uri, explicit=False):
        parts = ["am", "start", "-W", "-a", "android.intent.action.VIEW", "-c", "android.intent.category.BROWSABLE", "-d", uri]
        parts += ["-n", component] if explicit else ["-p", args.package]
        output = shell(*parts)
        assert "Error:" not in output and "Exception" not in output, output

    def valid(issuer):
        offer = {"credential_issuer": issuer, "credential_configuration_ids": ["proof_of_age"],
                 "grants": {"authorization_code": {"issuer_state": "synthetic-not-for-display"}}}
        return "av-vci://?credential_offer=" + quote(json.dumps(offer, separators=(",", ":")), safe="")

    run("get-state")
    api = int(shell("getprop", "ro.build.version.sdk").strip())
    assert api >= 31, "Android 31+ required"
    if api < 33 and not args.legacy_locale:
        parser.error("API31/32 lack cmd locale set-app-locales. Configure app/system English, run --legacy-locale en; then configure Estonian and run --legacy-locale et.")
    if api >= 33 and args.legacy_locale:
        parser.error("--legacy-locale is only for API31/32; newer runtimes exercise both locales automatically")
    original_locale = None
    if api >= 33:
        output = shell("cmd", "locale", "get-app-locales", args.package, "--user", "current")
        match = re.search(r"\[([^]]*)\]", output)
        assert match, f"Cannot read original per-app locale: {output}"
        original_locale = match.group(1)
    output = shell("cmd", "uimode", "night")
    match = re.search(r"Night mode:\s*(yes|no|auto|custom)", output)
    assert match, f"Cannot read original night mode: {output}"
    original_night = match.group(1)
    current_night = original_night

    def recreate(*expected):
        nonlocal current_night
        before = activity_instance()
        assert before, "Cannot identify the current Activity instance"
        # Normalize automatic/custom mode before toggling, so even a dark auto
        # setting causes a real configuration change.
        if current_night not in ("yes", "no"):
            shell("cmd", "uimode", "night", "no")
            current_night = "no"
            wait_text(*expected)
            before = activity_instance()
        current_night = "no" if current_night == "yes" else "yes"
        shell("cmd", "uimode", "night", current_night)
        wait_condition(lambda _text: activity_instance() not in (None, before), "new Activity instance after night-mode switch")
        wait_text(*expected)

    def refuse(uri, key, parameter, recreate_ui=False):
        invoke(uri, explicit=True)
        explanation = message(key, parameter)
        expected = (message("invocation_refused_title"), explanation)
        tree = wait_text(*expected)
        assert not any(node.get("text") == message("av_invocation_continue") for node in tree.iter("node")), "Refusal entered issuance"
        if recreate_ui:
            recreate(*expected)
        dismiss("av_invocation_close", explanation)

    try:
        uri = valid("https://cold-issuer.example.com")
        resolution = shell("cmd", "package", "resolve-activity", "--brief", "-a", "android.intent.action.VIEW",
                           "-c", "android.intent.category.BROWSABLE", "-d", uri, "-p", args.package)
        assert component in resolution, f"av-vci manifest resolution failed: {resolution}"
        print("PASS manifest VIEW/BROWSABLE resolution", flush=True)
        for locale in (("en", "et") if api >= 33 else (args.legacy_locale,)):
            if api >= 33:
                shell("cmd", "locale", "set-app-locales", args.package, "--user", "current", "--locales", locale)
            shell("am", "force-stop", args.package)
            invoke(uri)
            expected = (message("av_invocation_title"), "eu.europa.ec.av.1", "https://cold-issuer.example.com")
            wait_text(*expected)
            recreate(*expected)
            dismiss("av_invocation_cancel", "https://cold-issuer.example.com")
            warm = valid("https://warm-issuer.example.com")
            invoke(warm)
            expected = (message("av_invocation_title"), "eu.europa.ec.av.1", "https://warm-issuer.example.com")
            wait_text(*expected)
            recreate(*expected)
            click(message("av_invocation_continue"))
            wait_text(message("av_invocation_provider_unavailable"))
            recreate(message("av_invocation_provider_unavailable"), "https://warm-issuer.example.com")
            # A separate identical intent replaces the visible valid offer and resets Continue.
            invoke(warm)
            wait_text(message("av_invocation_continue"), "https://warm-issuer.example.com")
            wait_absent(message("av_invocation_provider_unavailable"))
            dismiss("av_invocation_cancel", "https://warm-issuer.example.com")
            print(f"PASS {locale}: cold/warm/provider recreation, repeated intent and actual Cancel dismissal", flush=True)

            # Send multiple outcomes before inspecting UI. Neither refusal may be overwritten by the valid offer.
            shell("am", "force-stop", args.package)
            invoke("av-vci://", explicit=True)
            invoke("av-vci://?credential_offer=%FF", explicit=True)
            invoke(valid("https://queued-issuer.example.com"))
            missing = message("invocation_missing", "credential_offer")
            malformed = message("invocation_malformed", "credential_offer")
            wait_text(message("invocation_refused_title"), missing)
            recreate(message("invocation_refused_title"), missing)
            dismiss("av_invocation_close", missing, (message("invocation_refused_title"), malformed))
            recreate(message("invocation_refused_title"), malformed)
            dismiss("av_invocation_close", malformed, (message("av_invocation_title"), "https://queued-issuer.example.com"))
            dismiss("av_invocation_cancel", "https://queued-issuer.example.com")
            print(f"PASS {locale}: queued cold/warm refusals survive recreation before valid entry", flush=True)

            for candidate, key, parameter in (
                ("av-vci://?credential_offer=%7B%7D", "invocation_malformed", "credential_issuer"),
                ("av-vci://?credential_offer=" + quote('{"credential_issuer":"https://issuer.example"}', safe=""),
                 "invocation_malformed", "credential_configuration_ids"),
                ("av-vci://?credential_offer_uri=https%3A%2F%2Fnever-contact.invalid", "invocation_unsupported", "credential_offer_uri"),
                ("av-vci://?credential_offer=%FF", "invocation_malformed", "credential_offer"),
                ("av-vci://?credential_offer=%GG", "invocation_malformed", "credential_offer"),
                (valid("https://duplicate.example") + "&credential_offer=%7B%7D", "invocation_malformed", "credential_offer"),
                ("unknown-invocation://?secret=not-for-display", "invocation_unsupported", "scheme"),
                ("eudi-wallet://unmatched-destination", "invocation_navigation_failed", "invocation"),
            ):
                refuse(candidate, key, parameter)
            print(f"PASS {locale}: complete localized refusal explanations and Close dismissal", flush=True)

            # These reach the actual NavController and destination UI, rather than a JVM callback substitute.
            invoke("haip://pid?binding_token=synthetic-smoke", explicit=True)
            wait_text(message("offer_description"), message("issuer_pid_sdjwt"))
            wait_absent(message("invocation_refused_title"))
            invoke("openid4vp://?client_id=synthetic-smoke&request_uri=not-a-url", explicit=True)
            errors = (message("error_presentation_rp_connection_failed"), message("error_presentation_verification_error"))
            wait_condition(lambda text: any(error in text for error in errors), "presentation destination error for synthetic invalid request")
            wait_absent(message("invocation_refused_title"))
            print(f"PASS {locale}: existing onboarding and presentation destinations via NavController", flush=True)

            overflow = [f"unsupported{i}" for i in range(16)]
            for parameter in overflow:
                invoke(f"av-vci://?{parameter}=x", explicit=True)
            for _ in range(5):
                invoke(valid("https://overflow.example"))
            for index, parameter in enumerate(overflow):
                following = (message("invocation_unsupported", overflow[index + 1]) if index + 1 < len(overflow)
                             else message("invocation_capacity", 5))
                dismiss("av_invocation_close", message("invocation_unsupported", parameter),
                        (message("invocation_refused_title"), following))
            wait_text(message("invocation_refused_title"), message("invocation_capacity", 5))
            dismiss("av_invocation_close", message("invocation_capacity", 5))
            print(f"PASS {locale}: visible counted capacity refusal and dismissal", flush=True)
        print("PASS AV Android surface smoke (entry only; no external issuance or AVSUA claim)", flush=True)
    finally:
        try:
            if original_locale is not None:
                shell("cmd", "locale", "set-app-locales", args.package, "--user", "current", "--locales", original_locale)
        finally:
            shell("cmd", "uimode", "night", original_night)


if __name__ == "__main__":
    try:
        main()
    except (AssertionError, OSError, subprocess.SubprocessError) as error:
        print(f"FAIL: {error}", file=sys.stderr)
        sys.exit(1)
